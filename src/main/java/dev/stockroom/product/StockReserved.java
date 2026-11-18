package dev.stockroom.product;

public record StockReserved(long productId, int quantity, int remainingStock) {
}
