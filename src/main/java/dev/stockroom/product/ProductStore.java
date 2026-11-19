package dev.stockroom.product;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
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

    public ProductPage findPage(String name, int page, int size) {
        long offset = (long) page * size;
        String trimmed = (name == null) ? "" : name.trim();
        List<Product> content;
        long totalElements;
        if (trimmed.isEmpty()) {
            content = repository.findAllPaged(size, offset);
            totalElements = repository.count();
        } else {
            String escaped = escapeLike(trimmed);
            content = repository.findByNameContainingIgnoreCasePaged(escaped, size, offset);
            totalElements = repository.countByNameContainingIgnoreCase(escaped);
        }
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new ProductPage(content, page, size, totalElements, totalPages);
    }

    /**
     * Escapes the LIKE wildcards {@code %}, {@code _} and the escape character itself
     * ({@code \}) so they match only themselves. The backslash must be escaped first.
     */
    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
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
