package dev.stockroom.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductPagingTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearProducts() {
        jdbcTemplate.execute("DELETE FROM product");
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private long createProduct(String name, int stock) throws Exception {
        var result = mockMvc.perform(post("/api/products")
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

    // ── US1: Page through the catalogue ─────────────────────────────────────

    /**
     * US1 Scenario 1: 5 products, page=0&size=2 → first two by id.
     */
    @Test
    void pageFirstPage_returnsTwoProducts() throws Exception {
        long id1 = createProduct("Product1", 10);
        long id2 = createProduct("Product2", 10);
        createProduct("Product3", 10);
        createProduct("Product4", 10);
        createProduct("Product5", 10);

        mockMvc.perform(get("/api/products?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(id1))
                .andExpect(jsonPath("$.content[1].id").value(id2));
    }

    /**
     * US1 Scenario 2: page=2&size=2 → fifth product only; page=3&size=2 → empty content.
     */
    @Test
    void pageLastPage_returnsOneProduct() throws Exception {
        createProduct("Product1", 10);
        createProduct("Product2", 10);
        createProduct("Product3", 10);
        createProduct("Product4", 10);
        long id5 = createProduct("Product5", 10);

        mockMvc.perform(get("/api/products?page=2&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(id5));
    }

    /**
     * US1 Scenario 2: page past the end → 200 with empty content, totals unchanged.
     */
    @Test
    void pagePastEnd_returnsEmptyContent() throws Exception {
        createProduct("Product1", 10);
        createProduct("Product2", 10);
        createProduct("Product3", 10);
        createProduct("Product4", 10);
        createProduct("Product5", 10);

        mockMvc.perform(get("/api/products?page=3&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(3))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    /**
     * US1 Scenario 3: only size=2 → page defaults to 0.
     */
    @Test
    void onlySize_pageDefaultsToZero() throws Exception {
        createProduct("A", 1);
        createProduct("B", 2);
        createProduct("C", 3);

        mockMvc.perform(get("/api/products?size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    /**
     * US1 Scenario 3: only page=0 → size defaults to 20.
     */
    @Test
    void onlyPage_sizeDefaultsTwenty() throws Exception {
        for (int i = 0; i < 5; i++) {
            createProduct("Item" + i, i);
        }

        mockMvc.perform(get("/api/products?page=0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content", hasSize(5)));
    }

    // ── US2: Filter by name ──────────────────────────────────────────────────

    /**
     * US2 Scenario 1: filter by "widget" → Red Widget and Blue widget in id order.
     */
    @Test
    void filterByName_returnsMatchingProducts() throws Exception {
        long id1 = createProduct("Red Widget", 5);
        long id2 = createProduct("Blue widget", 3);
        createProduct("Gadget", 7);

        mockMvc.perform(get("/api/products?name=widget"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(id1))
                .andExpect(jsonPath("$.content[1].id").value(id2));
    }

    /**
     * US2 Scenario 2: name=WIDGET&page=0&size=1 → only Red Widget, totalElements=2, totalPages=2.
     */
    @Test
    void filterByNamePaged_returnsFirstPageOnly() throws Exception {
        long id1 = createProduct("Red Widget", 5);
        createProduct("Blue widget", 3);
        createProduct("Gadget", 7);

        mockMvc.perform(get("/api/products?name=WIDGET&page=0&size=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(id1));
    }

    /**
     * US2 Scenario 3: name= (empty) → no filter applies, all products returned.
     */
    @Test
    void filterByEmptyName_noFilterApplied() throws Exception {
        createProduct("Red Widget", 5);
        createProduct("Blue widget", 3);
        createProduct("Gadget", 7);

        mockMvc.perform(get("/api/products?name="))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content", hasSize(3)));
    }

    /**
     * US2 Scenario 3: name with only spaces → no filter applies.
     */
    @Test
    void filterByBlankName_noFilterApplied() throws Exception {
        createProduct("Red Widget", 5);
        createProduct("Blue widget", 3);
        createProduct("Gadget", 7);

        mockMvc.perform(get("/api/products?name=   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content", hasSize(3)));
    }

    // ── US3: Plain list stays as it is ──────────────────────────────────────

    /**
     * US3 Scenario 1: GET /api/products with no paging params → plain JSON array.
     */
    @Test
    void noParams_returnsPlainArray() throws Exception {
        createProduct("Alpha", 1);
        createProduct("Beta", 2);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }

    // ── US4: Bad paging parameters ───────────────────────────────────────────

    /**
     * US4 Scenario 1: page=-1 → 400 problem+json.
     */
    @Test
    void pageNegative_returns400() throws Exception {
        mockMvc.perform(get("/api/products?page=-1"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/api/products"));
    }

    /**
     * US4 Scenario 1: size=0 → 400 problem+json.
     */
    @Test
    void sizeZero_returns400() throws Exception {
        mockMvc.perform(get("/api/products?size=0"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/api/products"));
    }

    /**
     * US4 Scenario 1: size=101 → 400 problem+json.
     */
    @Test
    void sizeOverMax_returns400() throws Exception {
        mockMvc.perform(get("/api/products?size=101"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/api/products"));
    }

    /**
     * US4 Scenario 1: size=abc (non-numeric) → 400 problem+json.
     */
    @Test
    void sizeNonNumeric_returns400() throws Exception {
        mockMvc.perform(get("/api/products?size=abc"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/api/products"));
    }
}
