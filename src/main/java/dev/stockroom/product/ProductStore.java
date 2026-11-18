package dev.stockroom.product;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ProductStore {

    private final AtomicLong idSequence = new AtomicLong(0);
    private final ConcurrentSkipListMap<Long, Product> products = new ConcurrentSkipListMap<>();

    public Product save(String name, int stock) {
        long id = idSequence.incrementAndGet();
        Product product = new Product(id, name, stock);
        products.put(id, product);
        return product;
    }

    public Optional<Product> findById(long id) {
        return Optional.ofNullable(products.get(id));
    }

    public Collection<Product> findAll() {
        return Collections.unmodifiableCollection(products.values());
    }
}
