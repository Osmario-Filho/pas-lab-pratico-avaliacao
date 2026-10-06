package br.pas.lab.pedido;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PedidoControllerTest {

    @Autowired
    private MockMvc mvc;

    // Os outros serviços não participam destes testes: Estoque e RabbitMQ são substituídos por mocks.
    @MockitoBean
    private EstoqueClient estoque;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void semEstoqueRetorna409NaoCriaPedidoENaoPublicaEvento() throws Exception {
        doThrow(new EstoqueInsuficienteException()).when(estoque).reservar(eq(1L), eq(999), anyString());

        mvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produtoId\": 1, \"quantidade\": 999}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Estoque insuficiente"));

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        mvc.perform(get("/pedidos"))
                .andExpect(jsonPath("$[?(@.quantidade == 999)]").isEmpty());
    }

    @Test
    void produtoInexistenteNoEstoqueRetorna404() throws Exception {
        doThrow(new ProdutoInexistenteException()).when(estoque).reservar(eq(77L), eq(1), anyString());

        mvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produtoId\": 77, \"quantidade\": 1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Produto inexistente"));

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void estoqueForaDoArRetorna503() throws Exception {
        doThrow(new EstoqueIndisponivelException(new RuntimeException("conexão recusada")))
                .when(estoque).reservar(eq(2L), eq(1), anyString());

        mvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produtoId\": 2, \"quantidade\": 1}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensagem").value("Estoque Service indisponível"));
    }

    @Test
    void criaPedidoReservaEstoquePublicaEventoEDevolveCorrelationId() throws Exception {
        String correlationId = mvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produtoId\": 2, \"quantidade\": 3}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Correlation-Id",
                        matchesPattern("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
                .andReturn().getResponse().getHeader("X-Correlation-Id");

        verify(estoque).reservar(2L, 3, correlationId);
        verify(rabbitTemplate).convertAndSend(
                eq("pedidos.exchange"), eq("pedido.criado"),
                org.mockito.ArgumentMatchers.<Object>argThat(evento -> evento instanceof PedidoCriadoEvento e
                        && e.produtoId() == 2L
                        && e.quantidade() == 3
                        && correlationId.equals(e.correlationId())
                        && e.pedidoId() != null));
    }

    @Test
    void criaPedidoAguardandoPagamento() throws Exception {
        String location = mvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"produtoId": 1, "quantidade": 2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/pedidos/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.produtoId").value(1))
                .andExpect(jsonPath("$.quantidade").value(2))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"))
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.produtoId").value(1))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"));
    }

    @Test
    void listaPedidosCriados() throws Exception {
        mvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"produtoId": 3, "quantidade": 7}
                                """))
                .andExpect(status().isCreated());

        mvc.perform(get("/pedidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].quantidade", hasItem(7)));
    }

    @Test
    void pedidoInexistenteRetorna404() throws Exception {
        mvc.perform(get("/pedidos/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Pedido 999999 não encontrado"));
    }

    @Test
    void rejeitaPedidoSemProdutoOuComQuantidadeInvalida() throws Exception {
        mvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantidade": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem")
                        .value("produtoId é obrigatório; quantidade deve ser maior que zero"));
    }

    @Test
    void rejeitaCorpoMalFormado() throws Exception {
        mvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produtoId\": \"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Requisição em formato inválido"));
    }
}
