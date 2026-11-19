package dev.stockroom.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
class ProductDeleteTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ApplicationEvents applicationEvents;

    @BeforeEach
    void clearProducts() {
        jdbcTemplate.execute("DELETE FROM product");
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private long createProduct(String name, int stock) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + name + "\", \"stock\": " + stock + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(result.getResponse().getContentAsString());
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

    // ── US1: Delete an existing product ─────────────────────────────────────

    /**
     * US1 Scenario 1: DELETE /api/products/{id} → 204 with an empty body.
     */
    @Test
    void delete_existingProduct_returns204EmptyBody() throws Exception {
        long id = createProduct("Doomed", 5);

        mockMvc.perform(delete("/api/products/" + id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    /**
     * US1 Scenario 2: After delete, GET by id → 404 Problem Detail, and the plain list no
     * longer contains it.
     */
    @Test
    void delete_thenReads_behaveAsIfNeverExisted() throws Exception {
        long id = createProduct("Vanishing", 7);

        mockMvc.perform(delete("/api/products/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail", containsString(String.valueOf(id))));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]", hasSize(0)));
    }

    /**
     * US1 Scenario 3: Deleting one of two products leaves the other unchanged and still listed.
     */
    @Test
    void delete_oneProduct_leavesOtherUnchanged() throws Exception {
        long doomedId = createProduct("Gone", 3);
        long keptId = createProduct("Kept", 9);

        mockMvc.perform(delete("/api/products/" + doomedId))
                .andExpect(status().isNoContent());

        // The other product is unchanged: same name and stock, readable by id.
        mockMvc.perform(get("/api/products/" + keptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(keptId))
                .andExpect(jsonPath("$.name").value("Kept"))
                .andExpect(jsonPath("$.stock").value(9));

        // Still listed, while the deleted one is not.
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + keptId + ")].name").value("Kept"))
                .andExpect(jsonPath("$[?(@.id == " + doomedId + ")]", hasSize(0)));
    }

    // ── US2: Deleting what isn't there ──────────────────────────────────────

    /**
     * US2 Scenario 1: DELETE of an id that never existed → 404 Problem Detail naming the id.
     */
    @Test
    void delete_unknownId_returns404ProblemDetail() throws Exception {
        mockMvc.perform(delete("/api/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail", containsString("999999")))
                .andExpect(jsonPath("$.instance").value("/api/products/999999"));
    }

    /**
     * US2 Scenario 2: Deleting a product that was just deleted → 404.
     */
    @Test
    void delete_alreadyDeleted_returns404() throws Exception {
        long id = createProduct("Twice", 2);

        mockMvc.perform(delete("/api/products/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/products/" + id))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(404));
    }

    /**
     * US2 Scenario 3: DELETE with a non-numeric id → 400 Problem Detail.
     */
    @Test
    void delete_nonNumericId_returns400ProblemDetail() throws Exception {
        mockMvc.perform(delete("/api/products/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/api/products/abc"));
    }

    // ── US3: A deleted product can't be reserved ────────────────────────────

    /**
     * US3 Scenario 1: Reserving a deleted product → 404 Problem Detail and no StockReserved
     * event is published.
     */
    @Test
    void reserve_deletedProduct_returns404NoEvent() throws Exception {
        long id = createProduct("DeletedStock", 10);

        mockMvc.perform(delete("/api/products/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/products/" + id + "/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 1}"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(404));

        long count = applicationEvents.stream(StockReserved.class).count();
        assertEquals(0, count, "No StockReserved event should be published for a deleted product");
    }

    // ── Edge cases ──────────────────────────────────────────────────────────

    /**
     * Deleting doesn't renumber anything: other products keep their ids, and a new product never
     * reuses a deleted one's id.
     */
    @Test
    void delete_doesNotRenumber_orReuseIds() throws Exception {
        long id1 = createProduct("First", 1);
        long id2 = createProduct("Second", 2);

        mockMvc.perform(delete("/api/products/" + id1))
                .andExpect(status().isNoContent());

        // The surviving product keeps its id.
        mockMvc.perform(get("/api/products/" + id2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id2));

        // A new product never reuses the deleted id.
        long id3 = createProduct("Third", 3);
        assertTrue(id3 > id1, "New id must be greater than the deleted id");
        assertTrue(id3 > id2, "New id must be greater than the surviving id");
    }

    /**
     * Paging counts only existing products after a delete: totalElements drops by one.
     */
    @Test
    void delete_pagingTotalElementsDropsByOne() throws Exception {
        long id1 = createProduct("Keep", 10);
        long id2 = createProduct("Drop", 20);

        mockMvc.perform(get("/api/products?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content", hasSize(2)));

        mockMvc.perform(delete("/api/products/" + id2))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(id1));
    }
}
