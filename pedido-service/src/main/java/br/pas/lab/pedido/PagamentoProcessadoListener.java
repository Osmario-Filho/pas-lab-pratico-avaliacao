package br.pas.lab.pedido;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

// Etapa 12: o Pagamento Service avisa o resultado por evento; o Pedido Service atualiza o próprio banco.
@Component
public class PagamentoProcessadoListener {

    private final PedidoService service;

    public PagamentoProcessadoListener(PedidoService service) {
        this.service = service;
    }

    @RabbitListener(queues = RabbitConfig.PAGAMENTO_PROCESSADO_FILA)
    public void consumir(PagamentoProcessadoEvento evento) {
        service.atualizarPagamento(evento);
    }
}
