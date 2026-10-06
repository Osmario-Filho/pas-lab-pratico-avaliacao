package br.pas.lab.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// Etapas 3 e 12 no nível do serviço: compensação da reserva e atualização do status pelo evento.
@SpringBootTest
@ActiveProfiles("test")
class PedidoServiceTest {

    @Autowired
    private PedidoService service;

    @Autowired
    private PedidoRepository repository;

    @MockitoBean
    private EstoqueClient estoque;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void criarReservaEstoquePrimeiroENaoCompensaQuandoTudoDaCerto() {
        Pedido pedido = service.criar(new CriarPedidoRequest(1L, 2), "corr-ok");

        assertThat(pedido.getId()).isNotNull();
        verify(estoque).reservar(1L, 2, "corr-ok");
        verify(estoque, never()).liberar(any(), any(), anyString());
    }

    @Test
    void falhaDoBrokerNaoDesfazPedidoNemReserva() {
        doThrow(new org.springframework.amqp.AmqpConnectException(new RuntimeException("broker fora")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        Pedido pedido = service.criar(new CriarPedidoRequest(1L, 1), "corr-broker");

        assertThat(repository.findById(pedido.getId())).isPresent();
        verify(estoque, never()).liberar(any(), any(), anyString());
    }

    @Test
    void pagamentoAprovadoMarcaPedidoComoPago() {
        Pedido pedido = service.criar(new CriarPedidoRequest(1L, 1), "corr-pago");

        service.atualizarPagamento(new PagamentoProcessadoEvento(pedido.getId(), "APROVADO", "corr-pago"));

        assertThat(service.buscar(pedido.getId()).getStatus()).isEqualTo(StatusPedido.PAGO);
    }

    @Test
    void pagamentoRejeitadoMarcaPedidoComoRejeitado() {
        Pedido pedido = service.criar(new CriarPedidoRequest(1L, 1), "corr-rej");

        service.atualizarPagamento(new PagamentoProcessadoEvento(pedido.getId(), "REJEITADO", "corr-rej"));

        assertThat(service.buscar(pedido.getId()).getStatus()).isEqualTo(StatusPedido.REJEITADO);
    }

    @Test
    void eventoRepetidoOuContraditorioNaoAlteraUmPedidoJaDecidido() {
        Pedido pedido = service.criar(new CriarPedidoRequest(1L, 1), "corr-dup");
        service.atualizarPagamento(new PagamentoProcessadoEvento(pedido.getId(), "APROVADO", "corr-dup"));

        service.atualizarPagamento(new PagamentoProcessadoEvento(pedido.getId(), "APROVADO", "corr-dup"));
        service.atualizarPagamento(new PagamentoProcessadoEvento(pedido.getId(), "REJEITADO", "corr-dup"));

        assertThat(service.buscar(pedido.getId()).getStatus()).isEqualTo(StatusPedido.PAGO);
    }

    @Test
    void eventosInvalidosSaoIgnorados() {
        service.atualizarPagamento(new PagamentoProcessadoEvento(987654L, "APROVADO", "corr-x"));

        Pedido pedido = service.criar(new CriarPedidoRequest(1L, 1), "corr-y");
        service.atualizarPagamento(new PagamentoProcessadoEvento(pedido.getId(), "TALVEZ", "corr-y"));
        assertThat(service.buscar(pedido.getId()).getStatus()).isEqualTo(StatusPedido.AGUARDANDO_PAGAMENTO);

        assertThatThrownBy(() -> service.buscar(987654L)).isInstanceOf(PedidoNaoEncontradoException.class);
    }
}
