package br.pas.lab.pagamento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Referência lógica ao pedido: o Pagamento Service nunca consulta o banco do Pedido Service.
    private Long pedidoId;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private StatusPagamento status;

    protected Pagamento() {
        // exigido pelo JPA
    }

    public Pagamento(Long pedidoId, StatusPagamento status) {
        this.pedidoId = pedidoId;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public StatusPagamento getStatus() {
        return status;
    }
}
