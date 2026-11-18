package dev.stockroom.product;

import java.util.List;

public record ProductPage(
        List<Product> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
