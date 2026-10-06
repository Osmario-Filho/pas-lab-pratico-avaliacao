package br.pas.lab.pedido;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Topologia do RabbitMQ. As declarações são idempotentes e repetidas no Pagamento Service, então
// a ordem de subida dos serviços não importa. O Pedido Service declara também a fila pedido.criado
// para que ela exista (e retenha mensagens) mesmo quando o Pagamento Service está parado.
@Configuration
public class RabbitConfig {

    public static final String PEDIDOS_EXCHANGE = "pedidos.exchange";
    public static final String PEDIDO_CRIADO_FILA = "pedido.criado";
    public static final String PEDIDO_CRIADO_ROUTING_KEY = "pedido.criado";

    public static final String PAGAMENTOS_EXCHANGE = "pagamentos.exchange";
    public static final String PAGAMENTO_PROCESSADO_FILA = "pagamento.processado";
    public static final String PAGAMENTO_PROCESSADO_ROUTING_KEY = "pagamento.processado";

    @Bean
    DirectExchange pedidosExchange() {
        return new DirectExchange(PEDIDOS_EXCHANGE, true, false);
    }

    @Bean
    Queue pedidoCriadoFila() {
        return QueueBuilder.durable(PEDIDO_CRIADO_FILA).build();
    }

    @Bean
    Binding pedidoCriadoBinding(Queue pedidoCriadoFila, DirectExchange pedidosExchange) {
        return BindingBuilder.bind(pedidoCriadoFila).to(pedidosExchange).with(PEDIDO_CRIADO_ROUTING_KEY);
    }

    @Bean
    DirectExchange pagamentosExchange() {
        return new DirectExchange(PAGAMENTOS_EXCHANGE, true, false);
    }

    @Bean
    Queue pagamentoProcessadoFila() {
        return QueueBuilder.durable(PAGAMENTO_PROCESSADO_FILA).build();
    }

    @Bean
    Binding pagamentoProcessadoBinding(Queue pagamentoProcessadoFila, DirectExchange pagamentosExchange) {
        return BindingBuilder.bind(pagamentoProcessadoFila).to(pagamentosExchange)
                .with(PAGAMENTO_PROCESSADO_ROUTING_KEY);
    }

    // Eventos em JSON. O consumidor deduz o tipo pelo parâmetro do @RabbitListener, então Pedido e
    // Pagamento não precisam compartilhar classes (cada um tem o seu record do evento).
    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
