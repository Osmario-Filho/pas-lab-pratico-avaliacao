package br.pas.lab.pedido;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private final PedidoRepository repository;

    public PedidoService(PedidoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Pedido criar(CriarPedidoRequest request) {
        Pedido pedido = repository.save(new Pedido(request.produtoId(), request.quantidade()));
        log.info("Pedido {} criado: produtoId={} quantidade={} status={}",
                pedido.getId(), pedido.getProdutoId(), pedido.getQuantidade(), pedido.getStatus());
        return pedido;
    }

    @Transactional(readOnly = true)
    public List<Pedido> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Pedido buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new PedidoNaoEncontradoException(id));
    }
}
