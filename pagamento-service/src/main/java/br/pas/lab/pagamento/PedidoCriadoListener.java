package br.pas.lab.pagamento;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PedidoCriadoListener {

    private static final Logger log = LoggerFactory.getLogger(PedidoCriadoListener.class);

    private final PagamentoService service;
    private final RabbitTemplate rabbitTemplate;

    public PedidoCriadoListener(PagamentoService service, RabbitTemplate rabbitTemplate) {
        this.service = service;
        this.rabbitTemplate = rabbitTemplate;
    }

    // Independe de o Pedido Service estar no ar: a mensagem espera na fila até este consumidor ler.
    // Se algo falhar, a exceção devolve a mensagem à fila (ack só ao final) e o evento é reprocessado.
    @RabbitListener(queues = RabbitConfig.PEDIDO_CRIADO_FILA)
    public void consumir(PedidoEvento evento) {
        log.info("correlationId={} Evento pedido.criado recebido {}", evento.correlationId(), evento.pedidoId());

        // 1. Persiste o pagamento (transação própria, confirmada antes de publicar).
        Pagamento pagamento = service.registrar(evento);

        // 2. Publica o resultado. Se falhar, o reprocessamento reencontra o pagamento e só republica.
        PagamentoProcessadoEvento resultado = new PagamentoProcessadoEvento(
                evento.pedidoId(), pagamento.getStatus().name(), evento.correlationId());
        rabbitTemplate.convertAndSend(
                RabbitConfig.PAGAMENTOS_EXCHANGE, RabbitConfig.PAGAMENTO_PROCESSADO_ROUTING_KEY, resultado);
        log.info("correlationId={} Evento pagamento.processado publicado {} {}",
                evento.correlationId(), evento.pedidoId(), resultado.status());
    }
}
