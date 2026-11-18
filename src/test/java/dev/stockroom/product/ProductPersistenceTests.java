package dev.stockroom.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductPersistenceTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * US1 Scenario 1: The product table exists with the required columns.
     */
    @Test
    void productTable_exists_withRequiredColumns() {
        // Query for column names in the product table
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = 'PRODUCT' ORDER BY COLUMN_NAME",
                String.class);

        assertTrue(columns.contains("ID"), "Column 'id' must exist");
        assertTrue(columns.contains("NAME"), "Column 'name' must exist");
        assertTrue(columns.contains("STOCK"), "Column 'stock' must exist");
    }

    /**
     * US1 Scenario 2: A product posted through the API appears as a row in the product table.
     */
    @Test
    void createProduct_viaApi_isPersistedInDatabase() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Widget\", \"stock\": 5}"))
                .andExpect(status().isCreated())
                .andReturn();

        long id = extractId(result.getResponse().getContentAsString());

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT name, stock FROM product WHERE id = ?", id);

        assertEquals(1, rows.size(), "Exactly one row should exist for the returned id");
        assertEquals("Widget", rows.get(0).get("NAME"));
        assertEquals(5, rows.get(0).get("STOCK"));
    }

    /**
     * US1 Scenario 3: A row inserted directly into the product table is returned by the API.
     */
    @Test
    void directInsert_isReturnedByApi() throws Exception {
        jdbcTemplate.update("INSERT INTO product (name, stock) VALUES ('Bolt', 3)");

        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM product WHERE name = 'Bolt' AND stock = 3 " +
                "ORDER BY id DESC LIMIT 1",
                Long.class);

        assertNotNull(id, "Inserted row must have an id");

        // GET by id
        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Bolt"))
                .andExpect(jsonPath("$.stock").value(3));

        // GET all includes it
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].name").value("Bolt"));
    }

    /**
     * US3 Scenario 1: GET /api/products returns products in ascending id order.
     */
    @Test
    void getAllProducts_returnsInAscendingIdOrder() throws Exception {
        MvcResult first = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"OrderFirst\", \"stock\": 1}"))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult second = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"OrderSecond\", \"stock\": 2}"))
                .andExpect(status().isCreated())
                .andReturn();

        long firstId = extractId(first.getResponse().getContentAsString());
        long secondId = extractId(second.getResponse().getContentAsString());

        // Check that firstId appears before secondId in the list
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + firstId + ")].name").value("OrderFirst"))
                .andExpect(jsonPath("$[?(@.id == " + secondId + ")].name").value("OrderSecond"));

        assertTrue(firstId < secondId, "First product id must be less than second");
    }

    private long extractId(String json) {
        int idx = json.indexOf("\"id\":");
        if (idx < 0) throw new IllegalArgumentException("No id in: " + json);
        int start = idx + 5;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        return Long.parseLong(json.substring(start, end));
    }
}
