package br.pas.lab.estoque;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService service;

    public ProdutoController(ProdutoService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProdutoResponse> listar() {
        return service.listar().stream().map(ProdutoResponse::de).toList();
    }

    @GetMapping("/{id}")
    public ProdutoResponse buscar(@PathVariable Long id) {
        return ProdutoResponse.de(service.buscar(id));
    }

    @PutMapping("/{id}/reservar")
    public ProdutoResponse reservar(
            @PathVariable Long id,
            @Valid @RequestBody ReservarEstoqueRequest request,
            @RequestHeader(name = "X-Correlation-Id", required = false) String correlationId) {
        return ProdutoResponse.de(service.reservar(id, request.quantidade(), correlationId));
    }
}
