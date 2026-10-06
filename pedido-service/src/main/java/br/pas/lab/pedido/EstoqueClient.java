package br.pas.lab.pedido;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// Única porta de comunicação do Pedido Service com o Estoque Service: REST, nunca o banco dele.
@Component
public class EstoqueClient {

    private static final Logger log = LoggerFactory.getLogger(EstoqueClient.class);

    static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final RestClient restClient;

    public EstoqueClient(@Value("${estoque.service.url}") String baseUrl) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    /** PUT /produtos/{id}/reservar. Lança EstoqueInsuficienteException (409) ou ProdutoInexistenteException (404). */
    public void reservar(Long produtoId, Integer quantidade, String correlationId) {
        try {
            restClient.put()
                    .uri("/produtos/{id}/reservar", produtoId)
                    .header(CORRELATION_HEADER, correlationId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("quantidade", quantidade))
                    .retrieve()
                    .onStatus(status -> status.value() == 409, (req, res) -> {
                        throw new EstoqueInsuficienteException();
                    })
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new ProdutoInexistenteException();
                    })
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("correlationId={} Estoque Service indisponível ao reservar produto {}: {}",
                    correlationId, produtoId, e.getMessage());
            throw new EstoqueIndisponivelException(e);
        }
    }

    /** PUT /produtos/{id}/liberar: compensação de uma reserva já feita. */
    public void liberar(Long produtoId, Integer quantidade, String correlationId) {
        try {
            restClient.put()
                    .uri("/produtos/{id}/liberar", produtoId)
                    .header(CORRELATION_HEADER, correlationId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("quantidade", quantidade))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new EstoqueIndisponivelException(e);
        }
    }
}
