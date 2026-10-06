package br.pas.lab.pagamento;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    Optional<Pagamento> findFirstByPedidoIdOrderByIdAsc(Long pedidoId);
}
