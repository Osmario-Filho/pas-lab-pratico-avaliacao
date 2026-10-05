package br.pas.lab.estoque;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// @Transactional desfaz as reservas ao fim de cada teste, então todos partem do estoque inicial
// (1 Notebook 10, 2 Mouse 50, 3 Teclado 20).
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProdutoControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void listaOsProdutosIniciais() throws Exception {
        mvc.perform(get("/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nome").value("Notebook"))
                .andExpect(jsonPath("$[0].quantidade").value(10))
                .andExpect(jsonPath("$[1].nome").value("Mouse"))
                .andExpect(jsonPath("$[1].quantidade").value(50))
                .andExpect(jsonPath("$[2].nome").value("Teclado"))
                .andExpect(jsonPath("$[2].quantidade").value(20));
    }

    @Test
    void consultaProdutoPorId() throws Exception {
        mvc.perform(get("/produtos/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.nome").value("Mouse"))
                .andExpect(jsonPath("$.quantidade").value(50));
    }

    @Test
    void produtoInexistenteRetorna404NaConsulta() throws Exception {
        mvc.perform(get("/produtos/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Produto inexistente"));
    }

    @Test
    void reservaComEstoqueSuficienteReduzAQuantidade() throws Exception {
        mvc.perform(reservar(1, "{\"quantidade\": 2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.quantidade").value(8));

        mvc.perform(get("/produtos/1"))
                .andExpect(jsonPath("$.quantidade").value(8));
    }

    @Test
    void reservaDeTodoOEstoqueZeraAQuantidade() throws Exception {
        mvc.perform(reservar(1, "{\"quantidade\": 10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(0));
    }

    @Test
    void reservaMaiorQueOEstoqueRetorna409ENaoAlteraAQuantidade() throws Exception {
        mvc.perform(reservar(1, "{\"quantidade\": 11}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Estoque insuficiente"));

        mvc.perform(get("/produtos/1"))
                .andExpect(jsonPath("$.quantidade").value(10));
    }

    @Test
    void reservaDeProdutoInexistenteRetorna404() throws Exception {
        mvc.perform(reservar(999, "{\"quantidade\": 1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Produto inexistente"));

        mvc.perform(get("/produtos"))
                .andExpect(jsonPath("$[0].quantidade").value(10))
                .andExpect(jsonPath("$[1].quantidade").value(50))
                .andExpect(jsonPath("$[2].quantidade").value(20));
    }

    @Test
    void aceitaOCabecalhoDeCorrelacao() throws Exception {
        mvc.perform(reservar(3, "{\"quantidade\": 1}")
                        .header("X-Correlation-Id", "b1f6c1a2-5c5e-4c39-9f52-0d3b0d2c9d10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(19));
    }

    @Test
    void rejeitaQuantidadeAusenteOuNaoPositiva() throws Exception {
        mvc.perform(reservar(1, "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("quantidade é obrigatória"));

        mvc.perform(reservar(1, "{\"quantidade\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("quantidade deve ser maior que zero"));

        mvc.perform(reservar(1, "{\"quantidade\": -3}"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/produtos/1"))
                .andExpect(jsonPath("$.quantidade").value(10));
    }

    @Test
    void rejeitaCorpoMalFormado() throws Exception {
        mvc.perform(reservar(1, "{\"quantidade\": \"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Requisição em formato inválido"));
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder reservar(
            long id, String corpo) {
        return put("/produtos/" + id + "/reservar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo);
    }
}
