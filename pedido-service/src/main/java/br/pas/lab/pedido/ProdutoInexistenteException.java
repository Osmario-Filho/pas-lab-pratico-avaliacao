package br.pas.lab.pedido;

public class ProdutoInexistenteException extends RuntimeException {

    public ProdutoInexistenteException() {
        super("Produto inexistente");
    }
}
