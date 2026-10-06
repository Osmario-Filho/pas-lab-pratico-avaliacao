package br.pas.lab.pedido;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PedidoEventoPublisher {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventoPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public PedidoEventoPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    // O Pedido Service publica no exchange e não sabe quem (nem se alguém) consome o evento.
    public void publicarPedidoCriado(Pedido pedido, String correlationId) {
        PedidoCriadoEvento evento = new PedidoCriadoEvento(
                pedido.getId(), pedido.getProdutoId(), pedido.getQuantidade(), correlationId);
        rabbitTemplate.convertAndSend(
                RabbitConfig.PEDIDOS_EXCHANGE, RabbitConfig.PEDIDO_CRIADO_ROUTING_KEY, evento);
        log.info("correlationId={} Evento publicado {}", correlationId, pedido.getId());
    }
}
