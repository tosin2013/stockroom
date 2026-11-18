package dev.stockroom.product;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public Reservation reserve(long id, int quantity) {
        // Check the product exists first
        Product product = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        // Atomic conditional decrement: returns 1 if stock was sufficient, 0 otherwise
        int updated = repository.decrementStockIfSufficient(id, quantity);
        if (updated == 0) {
            throw new InsufficientStockException(quantity, product.stock());
        }

        int remainingStock = product.stock() - quantity;
        return new Reservation(id, quantity, remainingStock);
    }
}
