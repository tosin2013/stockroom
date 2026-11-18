package dev.stockroom.product;

public class ProductNotFoundException extends RuntimeException {

    private final long id;

    public ProductNotFoundException(long id) {
        super("Product not found: " + id);
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
