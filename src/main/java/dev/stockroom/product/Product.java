package dev.stockroom.product;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("product")
public record Product(@Id long id, String name, int stock) {
}
