package br.pas.lab.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// Experimento de consistência da Etapa 3: falha entre "reservar estoque" e "criar pedido",
// com a compensação (liberar a reserva) habilitada.
@SpringBootTest(properties = {
        "pedido.simular-falha-apos-reserva=true",
        "pedido.compensacao.habilitada=true"})
@ActiveProfiles("test")
class PedidoCompensacaoTest {

    @Autowired
    private PedidoService service;

    @Autowired
    private PedidoRepository repository;

    @MockitoBean
    private EstoqueClient estoque;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void falhaAposReservarLiberaAReservaENaoCriaPedidoNemEvento() {
        long pedidosAntes = repository.count();

        assertThatThrownBy(() -> service.criar(new CriarPedidoRequest(1L, 4), "corr-falha"))
                .isInstanceOf(FalhaSimuladaException.class);

        verify(estoque).reservar(1L, 4, "corr-falha");
        verify(estoque).liberar(1L, 4, "corr-falha");
        assertThat(repository.count()).isEqualTo(pedidosAntes);
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
