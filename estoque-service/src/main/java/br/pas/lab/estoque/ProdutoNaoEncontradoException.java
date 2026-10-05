package br.pas.lab.estoque;

public class ProdutoNaoEncontradoException extends RuntimeException {

    public ProdutoNaoEncontradoException() {
        super("Produto inexistente");
    }
}
