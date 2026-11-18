package dev.stockroom.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryTests {

    @Autowired
    private ProductRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void saveAndFindById_roundTrips() {
        Product saved = repository.save(new Product(0L, "Widget", 5));

        assertTrue(saved.id() > 0, "Saved product should have a positive id");

        Optional<Product> found = repository.findById(saved.id());
        assertTrue(found.isPresent());
        assertEquals("Widget", found.get().name());
        assertEquals(5, found.get().stock());
    }

    @Test
    void savedIds_areIncreasing() {
        Product first = repository.save(new Product(0L, "Alpha", 1));
        Product second = repository.save(new Product(0L, "Beta", 2));

        assertTrue(second.id() > first.id(), "Second id must be greater than first");
    }

    @Test
    void insert_nullName_isRefused() {
        assertThrows(Exception.class, () ->
                jdbcTemplate.update("INSERT INTO product (name, stock) VALUES (NULL, 1)")
        );
    }

    @Test
    void insert_nameTooLong_isRefused() {
        String longName = "A".repeat(101);
        assertThrows(Exception.class, () ->
                jdbcTemplate.update("INSERT INTO product (name, stock) VALUES (?, 1)", longName)
        );
    }

    @Test
    void insert_negativeStock_isRefused() {
        assertThrows(Exception.class, () ->
                jdbcTemplate.update("INSERT INTO product (name, stock) VALUES ('Bolt', -1)")
        );
    }

    @Test
    void findAllByOrderByIdAsc_returnsInAscendingOrder() {
        Product first = repository.save(new Product(0L, "First", 1));
        Product second = repository.save(new Product(0L, "Second", 2));
        Product third = repository.save(new Product(0L, "Third", 3));

        List<Product> all = repository.findAllByOrderByIdAsc();

        // Filter to only the ones we just inserted
        List<Product> ours = all.stream()
                .filter(p -> p.id() == first.id() || p.id() == second.id() || p.id() == third.id())
                .toList();

        assertEquals(3, ours.size());
        assertTrue(ours.get(0).id() < ours.get(1).id());
        assertTrue(ours.get(1).id() < ours.get(2).id());
    }

    @Test
    void tableSchema_hasRequiredColumns() {
        // Insert a row without id; the database assigns one
        jdbcTemplate.update("INSERT INTO product (name, stock) VALUES ('Bolt', 3)");

        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM product WHERE name = 'Bolt' AND stock = 3", Long.class);

        assertFalse(ids.isEmpty(), "Row should exist");
        assertTrue(ids.get(0) > 0, "Database-assigned id should be positive");
    }
}
