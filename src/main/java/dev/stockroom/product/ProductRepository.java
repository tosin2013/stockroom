package dev.stockroom.product;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;

public interface ProductRepository extends ListCrudRepository<Product, Long> {

    List<Product> findAllByOrderByIdAsc();

    @Modifying
    @Query("UPDATE product SET stock = stock - :quantity WHERE id = :id AND stock >= :quantity")
    int decrementStockIfSufficient(long id, int quantity);
}
