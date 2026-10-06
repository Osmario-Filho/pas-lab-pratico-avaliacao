package br.pas.lab.pagamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.random.RandomGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class PedidoCriadoListenerTest {

    @Autowired
    private PedidoCriadoListener listener;

    @Autowired
    private PagamentoRepository repository;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private RandomGenerator random;

    @BeforeEach
    void limpar() {
        repository.deleteAll();
        reset(rabbitTemplate, random);
    }

    @Test
    void sorteioBaixoAprovaPersisteEPublicaOResultado() {
        when(random.nextDouble()).thenReturn(0.10);

        listener.consumir(new PedidoEvento(10L, 1L, 2, "corr-1"));

        Pagamento pagamento = repository.findFirstByPedidoIdOrderByIdAsc(10L).orElseThrow();
        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.APROVADO);
        verify(rabbitTemplate).convertAndSend(
                "pagamentos.exchange", "pagamento.processado",
                (Object) new PagamentoProcessadoEvento(10L, "APROVADO", "corr-1"));
    }

    @Test
    void sorteioAltoRejeitaPersisteEPublicaOResultado() {
        when(random.nextDouble()).thenReturn(0.95);

        listener.consumir(new PedidoEvento(11L, 1L, 2, "corr-2"));

        assertThat(repository.findFirstByPedidoIdOrderByIdAsc(11L).orElseThrow().getStatus())
                .isEqualTo(StatusPagamento.REJEITADO);
        verify(rabbitTemplate).convertAndSend(
                "pagamentos.exchange", "pagamento.processado",
                (Object) new PagamentoProcessadoEvento(11L, "REJEITADO", "corr-2"));
    }

    @Test
    void eventoReentregueMantemOResultadoEApenasRepublica() {
        when(random.nextDouble()).thenReturn(0.10, 0.95);

        listener.consumir(new PedidoEvento(12L, 1L, 2, "corr-3"));
        listener.consumir(new PedidoEvento(12L, 1L, 2, "corr-3"));

        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findFirstByPedidoIdOrderByIdAsc(12L).orElseThrow().getStatus())
                .isEqualTo(StatusPagamento.APROVADO);
        verify(rabbitTemplate, org.mockito.Mockito.times(2)).convertAndSend(
                eq("pagamentos.exchange"), eq("pagamento.processado"),
                eq((Object) new PagamentoProcessadoEvento(12L, "APROVADO", "corr-3")));
    }

    @Test
    void cadaPedidoGeraSeuProprioPagamento() {
        when(random.nextDouble()).thenReturn(0.10);

        listener.consumir(new PedidoEvento(20L, 1L, 1, "a"));
        listener.consumir(new PedidoEvento(21L, 2L, 1, "b"));

        assertThat(repository.count()).isEqualTo(2);
        assertThat(repository.findFirstByPedidoIdOrderByIdAsc(20L)).isPresent();
        assertThat(repository.findFirstByPedidoIdOrderByIdAsc(21L)).isPresent();
        verify(rabbitTemplate, org.mockito.Mockito.times(2))
                .convertAndSend(eq("pagamentos.exchange"), eq("pagamento.processado"), any(Object.class));
    }
}
