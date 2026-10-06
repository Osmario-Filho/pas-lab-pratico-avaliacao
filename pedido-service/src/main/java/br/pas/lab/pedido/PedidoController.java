package br.pas.lab.pedido;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> criar(@Valid @RequestBody CriarPedidoRequest request) {
        // Etapa 11: um correlationId novo por requisição, propagado ao Estoque (cabeçalho),
        // ao RabbitMQ (campo do evento) e aos logs de todos os serviços.
        String correlationId = UUID.randomUUID().toString();
        Pedido pedido = service.criar(request, correlationId);
        return ResponseEntity.created(URI.create("/pedidos/" + pedido.getId()))
                .header("X-Correlation-Id", correlationId)
                .body(PedidoResponse.de(pedido));
    }

    @GetMapping
    public List<PedidoResponse> listar() {
        return service.listar().stream().map(PedidoResponse::de).toList();
    }

    @GetMapping("/{id}")
    public PedidoResponse buscar(@PathVariable Long id) {
        return PedidoResponse.de(service.buscar(id));
    }
}
