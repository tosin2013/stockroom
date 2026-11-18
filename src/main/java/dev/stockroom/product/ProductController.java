package dev.stockroom.product;

import jakarta.validation.Valid;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Collection;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductStore productStore;
    private final ApplicationEventPublisher eventPublisher;

    public ProductController(ProductStore productStore, ApplicationEventPublisher eventPublisher) {
        this.productStore = productStore;
        this.eventPublisher = eventPublisher;
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

    @PostMapping("/{id}/reservations")
    public ResponseEntity<Reservation> reserve(
            @PathVariable long id,
            @Valid @RequestBody ReservationRequest request) {
        Reservation reservation = productStore.reserve(id, request.quantity());
        eventPublisher.publishEvent(
                new StockReserved(reservation.productId(), reservation.quantity(), reservation.remainingStock()));
        return ResponseEntity.ok(reservation);
    }
}
