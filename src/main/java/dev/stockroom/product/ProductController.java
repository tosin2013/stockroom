package dev.stockroom.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Collection;

@RestController
@RequestMapping("/api/products")
@Validated
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
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(1) @Max(100) Integer size) {

        boolean paging = name != null || page != null || size != null;
        if (!paging) {
            return ResponseEntity.ok(productStore.findAll());
        }

        int resolvedPage = (page != null) ? page : 0;
        int resolvedSize = (size != null) ? size : 20;
        return ResponseEntity.ok(productStore.findPage(name, resolvedPage, resolvedSize));
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
