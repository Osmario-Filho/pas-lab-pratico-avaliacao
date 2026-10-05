package br.pas.lab.estoque;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReservarEstoqueRequest(
        @NotNull(message = "quantidade é obrigatória")
        @Positive(message = "quantidade deve ser maior que zero")
        Integer quantidade) {
}
