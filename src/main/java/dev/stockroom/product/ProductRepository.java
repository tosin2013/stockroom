package dev.stockroom.product;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;

public interface ProductRepository extends ListCrudRepository<Product, Long> {

    List<Product> findAllByOrderByIdAsc();

    @Query("SELECT * FROM product WHERE LOWER(name) LIKE LOWER(CONCAT('%', :name, '%')) ESCAPE '\\' ORDER BY id ASC LIMIT :size OFFSET :offset")
    List<Product> findByNameContainingIgnoreCasePaged(String name, int size, long offset);

    @Query("SELECT COUNT(*) FROM product WHERE LOWER(name) LIKE LOWER(CONCAT('%', :name, '%')) ESCAPE '\\'")
    long countByNameContainingIgnoreCase(String name);

    @Query("SELECT * FROM product ORDER BY id ASC LIMIT :size OFFSET :offset")
    List<Product> findAllPaged(int size, long offset);

    @Modifying
    @Query("UPDATE product SET stock = stock - :quantity WHERE id = :id AND stock >= :quantity")
    int decrementStockIfSufficient(long id, int quantity);

    @Modifying
    @Query("UPDATE product SET stock = :stock WHERE id = :id")
    int setStock(long id, int stock);
}
