package dev.stockroom.product;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;

@Component
public class ProductStore {

    private final ProductRepository repository;

    public ProductStore(ProductRepository repository) {
        this.repository = repository;
    }

    public Product save(String name, int stock) {
        Product product = new Product(0L, name, stock);
        return repository.save(product);
    }

    public Optional<Product> findById(long id) {
        return repository.findById(id);
    }

    public Collection<Product> findAll() {
        return repository.findAllByOrderByIdAsc();
    }
}
