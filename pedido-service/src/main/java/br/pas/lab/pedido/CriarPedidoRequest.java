package br.pas.lab.pedido;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CriarPedidoRequest(
        @NotNull(message = "produtoId é obrigatório")
        @Positive(message = "produtoId deve ser positivo")
        Long produtoId,

        @NotNull(message = "quantidade é obrigatória")
        @Positive(message = "quantidade deve ser maior que zero")
        Integer quantidade) {
}
