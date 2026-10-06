package br.pas.lab.estoque;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProdutoService {

    private static final Logger log = LoggerFactory.getLogger(ProdutoService.class);

    private final ProdutoRepository repository;

    public ProdutoService(ProdutoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Produto> listar() {
        return repository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public Produto buscar(Long id) {
        return repository.findById(id).orElseThrow(ProdutoNaoEncontradoException::new);
    }

    // correlationId é opcional: o Pedido Service o envia no cabeçalho X-Correlation-Id (Etapa 11).
    @Transactional
    public Produto reservar(Long id, Integer quantidade, String correlationId) {
        if (repository.reservar(id, quantidade) == 1) {
            Produto produto = repository.findById(id).orElseThrow(ProdutoNaoEncontradoException::new);
            log.info("correlationId={} Produto {} reservado: quantidade={} restante={}",
                    correlationId, id, quantidade, produto.getQuantidade());
            return produto;
        }

        // Nada foi alterado: descobre se o motivo foi produto inexistente ou falta de estoque.
        if (!repository.existsById(id)) {
            log.warn("correlationId={} Reserva recusada: produto {} inexistente", correlationId, id);
            throw new ProdutoNaoEncontradoException();
        }
        log.warn("correlationId={} Reserva recusada: produto {} sem estoque suficiente para {} unidade(s)",
                correlationId, id, quantidade);
        throw new EstoqueInsuficienteException();
    }

    // Compensação: desfaz uma reserva quando o pedido correspondente não pôde ser criado.
    @Transactional
    public Produto liberar(Long id, Integer quantidade, String correlationId) {
        if (repository.liberar(id, quantidade) == 0) {
            log.warn("correlationId={} Liberação recusada: produto {} inexistente", correlationId, id);
            throw new ProdutoNaoEncontradoException();
        }
        Produto produto = repository.findById(id).orElseThrow(ProdutoNaoEncontradoException::new);
        log.info("correlationId={} Produto {} liberado (compensação): quantidade={} restante={}",
                correlationId, id, quantidade, produto.getQuantidade());
        return produto;
    }
}
