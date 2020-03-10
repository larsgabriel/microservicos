package br.com.original.microservice.fornecedor.repository;

import org.springframework.data.repository.CrudRepository;

import br.com.original.microservice.fornecedor.model.Pedido;

public interface PedidoRepository extends CrudRepository<Pedido, Long>{

}
