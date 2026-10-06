package br.pas.lab.pedido;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// Erros no mesmo formato usado pelo Estoque Service: {"mensagem": "..."}
@RestControllerAdvice
public class ErroHandler {

    @ExceptionHandler(PedidoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> pedidoNaoEncontrado(PedidoNaoEncontradoException e) {
        return Map.of("mensagem", e.getMessage());
    }

    // Sem estoque: o pedido não é criado e nenhum evento é publicado.
    @ExceptionHandler(EstoqueInsuficienteException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> estoqueInsuficiente(EstoqueInsuficienteException e) {
        return Map.of("mensagem", e.getMessage());
    }

    @ExceptionHandler(ProdutoInexistenteException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> produtoInexistente(ProdutoInexistenteException e) {
        return Map.of("mensagem", e.getMessage());
    }

    @ExceptionHandler(EstoqueIndisponivelException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, String> estoqueIndisponivel(EstoqueIndisponivelException e) {
        return Map.of("mensagem", e.getMessage());
    }

    @ExceptionHandler(FalhaSimuladaException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> falhaSimulada(FalhaSimuladaException e) {
        return Map.of("mensagem", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> requisicaoInvalida(MethodArgumentNotValidException e) {
        String mensagem = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return Map.of("mensagem", mensagem);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> formatoInvalido(Exception e) {
        return Map.of("mensagem", "Requisição em formato inválido");
    }
}
