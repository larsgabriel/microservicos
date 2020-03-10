package br.com.original.microservicos.loja.service;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import br.com.original.microservicos.loja.client.FornecedorClient;
import br.com.original.microservicos.loja.client.TransportadorClient;
import br.com.original.microservicos.loja.dto.CompraDTO;
import br.com.original.microservicos.loja.dto.InfoEntregaDTO;
import br.com.original.microservicos.loja.dto.InfoFornecedorDTO;
import br.com.original.microservicos.loja.dto.InfoPedidoDTO;
import br.com.original.microservicos.loja.dto.VoucherDTO;
import br.com.original.microservicos.loja.model.Compra;
import br.com.original.microservicos.loja.repository.CompraRepository;

@Service
public class CompraService {
	
	@Autowired
	private FornecedorClient fornecedorClient;
	
	@Autowired
	private TransportadorClient transportadorClient;
	
	@Autowired
	private CompraRepository compraRepository;
	
	@HystrixCommand(threadPoolKey = "getByIdThreadPool")
	public Compra getById(Long id) {
		return compraRepository.findById(id).orElse(new Compra());
	}
	
	public Compra reprocessaCompra(Long id) {
		return null;
	}
	
	public Compra cancelaCompra(Long id) {
		return null;
	}
	
	@HystrixCommand(fallbackMethod = "realizaCompraFallback",
			threadPoolKey = "realizaCompraThreadPool")
	public Compra realizaCompra(CompraDTO compra) {
		
		Compra compraSalva = new Compra();
		compraSalva.setState(CompraState.RECEBIDO);
		compraSalva.setEnderecoDestino(compra.getEndereco().toString());
		compraRepository.save(compraSalva);
		compra.setCompraId(compraSalva.getId());
		
		InfoFornecedorDTO info = fornecedorClient.getInfoPorEstado(compra.getEndereco().getEstado());
		
		InfoPedidoDTO pedido = fornecedorClient.realizaPedido(compra.getItens());
		compraSalva.setState(CompraState.PEDIDO_REALIZADO);
		compraSalva.setPedidoId(pedido.getId());
		compraSalva.setTempoDePreparo(pedido.getTempoDePreparo());
		compraRepository.save(compraSalva);
		
		
		InfoEntregaDTO entregaDto = new InfoEntregaDTO();
		entregaDto.setPedidoId(pedido.getId());
		entregaDto.setDataParaEntrega(LocalDate.now().plusDays(pedido.getTempoDePreparo()));
		entregaDto.setEnderecoOrigem(info.getEndereco());
		VoucherDTO voucher = transportadorClient.reservaEntrega(entregaDto);
		compraSalva.setState(CompraState.RESERVA_ENTREGA_REALIZADA);
		compraSalva.setDataParaEntrega(voucher.getPrevisaoParaEntrega());
		compraSalva.setVoucher(voucher.getNumero());
		compraRepository.save(compraSalva);
		
		return compraSalva;
	}

	public Compra realizaCompraFallback(CompraDTO compra) {
		if(compra.getCompraId() != null) {
			return compraRepository.findById(compra.getCompraId()).get();
		}
		
		Compra compraFallback = new Compra();
		compraFallback.setEnderecoDestino(compra.getEndereco().toString());
		return compraFallback;
	}

}
