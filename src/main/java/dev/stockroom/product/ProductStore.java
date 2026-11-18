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
        // Atomic conditional decrement: returns 1 if stock was sufficient, 0 otherwise
        int updated = repository.decrementStockIfSufficient(id, quantity);
        if (updated == 0) {
            // Read after the failed update to get the authoritative stock (or detect missing product)
            Product product = repository.findById(id)
                    .orElseThrow(() -> new ProductNotFoundException(id));
            throw new InsufficientStockException(quantity, product.stock());
        }

        // Read the stock in the same transaction, right after the decrement, for the exact figure
        Product product = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return new Reservation(id, quantity, product.stock());
    }
}
