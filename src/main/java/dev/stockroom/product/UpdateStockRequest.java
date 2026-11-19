package dev.stockroom.product;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateStockRequest(
        @NotNull @PositiveOrZero Integer stock
) {
}
