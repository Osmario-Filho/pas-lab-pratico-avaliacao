package br.pas.lab.pedido;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private final PedidoRepository repository;
    private final EstoqueClient estoque;
    private final PedidoEventoPublisher publisher;
    private final boolean compensacaoHabilitada;
    private final boolean simularFalhaAposReserva;

    public PedidoService(
            PedidoRepository repository,
            EstoqueClient estoque,
            PedidoEventoPublisher publisher,
            @Value("${pedido.compensacao.habilitada:true}") boolean compensacaoHabilitada,
            @Value("${pedido.simular-falha-apos-reserva:false}") boolean simularFalhaAposReserva) {
        this.repository = repository;
        this.estoque = estoque;
        this.publisher = publisher;
        this.compensacaoHabilitada = compensacaoHabilitada;
        this.simularFalhaAposReserva = simularFalhaAposReserva;
    }

    // Deliberadamente SEM @Transactional: reservar o estoque (outro serviço, outro banco) e gravar
    // o pedido não cabem numa transação única. Cada passo confirma sozinho; a consistência vem da
    // compensação (liberar a reserva) quando um passo posterior falha.
    public Pedido criar(CriarPedidoRequest request, String correlationId) {
        // 1. Reservar estoque. Sem estoque: não cria pedido e não publica evento (409/404/503).
        estoque.reservar(request.produtoId(), request.quantidade(), correlationId);

        // 2. Criar o pedido.
        Pedido pedido;
        try {
            if (simularFalhaAposReserva) {
                throw new FalhaSimuladaException();
            }
            pedido = repository.save(new Pedido(request.produtoId(), request.quantidade()));
        } catch (RuntimeException e) {
            log.error("correlationId={} Falha ao criar pedido depois de reservar produto {} (quantidade={}): {}",
                    correlationId, request.produtoId(), request.quantidade(), e.getMessage());
            compensarReserva(request, correlationId);
            throw e;
        }
        log.info("correlationId={} Pedido {} criado: produtoId={} quantidade={} status={}",
                correlationId, pedido.getId(), pedido.getProdutoId(), pedido.getQuantidade(), pedido.getStatus());

        // 3. Publicar o evento. Se o broker falhar aqui, o pedido já existe e o estoque já foi baixado;
        // o erro é registrado (sem evento o pedido ficaria aguardando pagamento — ver README, "Limites").
        try {
            publisher.publicarPedidoCriado(pedido, correlationId);
        } catch (RuntimeException e) {
            log.error("correlationId={} Evento NÃO publicado para o pedido {}: {}",
                    correlationId, pedido.getId(), e.getMessage());
        }
        return pedido;
    }

    private void compensarReserva(CriarPedidoRequest request, String correlationId) {
        if (!compensacaoHabilitada) {
            log.warn("correlationId={} Compensação desabilitada: {} unidade(s) do produto {} continuam reservadas "
                    + "sem pedido", correlationId, request.quantidade(), request.produtoId());
            return;
        }
        try {
            estoque.liberar(request.produtoId(), request.quantidade(), correlationId);
            log.info("correlationId={} Reserva compensada: {} unidade(s) do produto {} devolvidas ao estoque",
                    correlationId, request.quantidade(), request.produtoId());
        } catch (RuntimeException e) {
            log.error("correlationId={} Compensação FALHOU: {} unidade(s) do produto {} continuam reservadas: {}",
                    correlationId, request.quantidade(), request.produtoId(), e.getMessage());
        }
    }

    // Etapa 12: aplica o resultado do pagamento. Idempotente: reentregas do evento não alteram nada.
    @Transactional
    public void atualizarPagamento(PagamentoProcessadoEvento evento) {
        StatusPedido novoStatus = traduzir(evento.status());
        if (novoStatus == null) {
            log.warn("correlationId={} Evento pagamento.processado ignorado: status desconhecido '{}' (pedido {})",
                    evento.correlationId(), evento.status(), evento.pedidoId());
            return;
        }
        Pedido pedido = repository.findById(evento.pedidoId()).orElse(null);
        if (pedido == null) {
            log.warn("correlationId={} Evento pagamento.processado ignorado: pedido {} não existe",
                    evento.correlationId(), evento.pedidoId());
            return;
        }
        if (pedido.aplicarResultadoPagamento(novoStatus)) {
            log.info("correlationId={} Pedido {} atualizado para {}",
                    evento.correlationId(), pedido.getId(), novoStatus);
        } else {
            log.info("correlationId={} Pedido {} já está {}; evento pagamento.processado ignorado",
                    evento.correlationId(), pedido.getId(), pedido.getStatus());
        }
    }

    private static StatusPedido traduzir(String statusPagamento) {
        if ("APROVADO".equals(statusPagamento)) {
            return StatusPedido.PAGO;
        }
        if ("REJEITADO".equals(statusPagamento)) {
            return StatusPedido.REJEITADO;
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<Pedido> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Pedido buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new PedidoNaoEncontradoException(id));
    }
}
