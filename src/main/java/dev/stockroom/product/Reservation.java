package dev.stockroom.product;

public record Reservation(long productId, int quantity, int remainingStock) {
}
