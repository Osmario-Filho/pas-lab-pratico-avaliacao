package br.pas.lab.pagamento;

import java.util.random.RandomGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PagamentoService {

    private static final Logger log = LoggerFactory.getLogger(PagamentoService.class);

    private final PagamentoRepository repository;
    private final RandomGenerator random;
    private final double taxaAprovacao;
    private final long atrasoMs;

    public PagamentoService(
            PagamentoRepository repository,
            RandomGenerator random,
            @Value("${pagamento.taxa-aprovacao:0.8}") double taxaAprovacao,
            @Value("${pagamento.atraso-ms:0}") long atrasoMs) {
        this.repository = repository;
        this.random = random;
        this.taxaAprovacao = taxaAprovacao;
        this.atrasoMs = atrasoMs;
    }

    // Registra (ou recupera) o pagamento do pedido. Idempotente: se o evento for reentregue, o
    // resultado já decidido é mantido em vez de sortear de novo.
    @Transactional
    public Pagamento registrar(PedidoEvento evento) {
        Pagamento existente = repository.findFirstByPedidoIdOrderByIdAsc(evento.pedidoId()).orElse(null);
        if (existente != null) {
            log.info("correlationId={} Pedido {} já tinha pagamento {}; resultado mantido",
                    evento.correlationId(), evento.pedidoId(), existente.getStatus());
            return existente;
        }

        simularProcessamento();
        // ~80% aprovados, ~20% rejeitados (pagamento.taxa-aprovacao).
        StatusPagamento status = random.nextDouble() < taxaAprovacao
                ? StatusPagamento.APROVADO
                : StatusPagamento.REJEITADO;
        Pagamento pagamento = repository.save(new Pagamento(evento.pedidoId(), status));

        if (status == StatusPagamento.APROVADO) {
            log.info("correlationId={} Pagamento aprovado {}", evento.correlationId(), evento.pedidoId());
        } else {
            log.info("correlationId={} Pagamento rejeitado {}", evento.correlationId(), evento.pedidoId());
        }
        return pagamento;
    }

    // Atraso opcional (pagamento.atraso-ms) para tornar visível, nos logs, a divisão de trabalho
    // entre instâncias no experimento de escalabilidade.
    private void simularProcessamento() {
        if (atrasoMs <= 0) {
            return;
        }
        try {
            Thread.sleep(atrasoMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
