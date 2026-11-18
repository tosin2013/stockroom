package dev.stockroom.product;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Collection;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductStore productStore;

    public ProductController(ProductStore productStore) {
        this.productStore = productStore;
    }

    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody CreateProductRequest request) {
        String trimmedName = request.name().trim();
        Product product = productStore.save(trimmedName, request.stock());
        URI location = URI.create("/api/products/" + product.id());
        return ResponseEntity.created(location).body(product);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable long id) {
        return productStore.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @GetMapping
    public ResponseEntity<Collection<Product>> getAll() {
        return ResponseEntity.ok(productStore.findAll());
    }
}
