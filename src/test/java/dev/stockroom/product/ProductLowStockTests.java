package dev.stockroom.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductLowStockTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearProducts() {
        jdbcTemplate.execute("DELETE FROM product");
    }

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

    // ── US1: List the products running low ─────────────────────────────────

    /**
     * US1 Scenario 1: stocks 0, 3, 5, 9 with maxStock=5 → content is 0, 3, 5 in id order,
     * totalElements counts only those.
     */
    @Test
    void maxStock_returnsOnlyAtOrBelowLimit() throws Exception {
        long id0 = createProduct("low-zero", 0);
        long id3 = createProduct("low-three", 3);
        long id5 = createProduct("low-five", 5);
        createProduct("low-nine", 9);

        mockMvc.perform(get("/api/products?maxStock=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].id").value(id0))
                .andExpect(jsonPath("$.content[1].id").value(id3))
                .andExpect(jsonPath("$.content[2].id").value(id5));
    }

    /**
     * US1 Scenario 2: maxStock=0 → content is only the product with stock 0.
     */
    @Test
    void maxStock_zero_returnsOnlyZeroStock() throws Exception {
        long id0 = createProduct("zero-only", 0);
        createProduct("zero-three", 3);
        createProduct("zero-five", 5);
        createProduct("zero-nine", 9);

        mockMvc.perform(get("/api/products?maxStock=0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(id0));
    }

    /**
     * US1 Scenario 3: no product at or below n → empty content, totalElements 0.
     */
    @Test
    void maxStock_noMatches_returnsEmpty() throws Exception {
        createProduct("empty-a", 3);
        createProduct("empty-b", 5);
        createProduct("empty-c", 9);

        mockMvc.perform(get("/api/products?maxStock=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    /**
     * US1 Scenario 4: setting the stock (010) to a low value makes it appear in the list.
     */
    @Test
    void patchStock_toLow_thenIncludedInLowStock() throws Exception {
        long id = createProduct("patched-down", 9);

        mockMvc.perform(patch("/api/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\": 2}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products?maxStock=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id));
    }

    /**
     * US1 Scenario 4: a reservation (005) bringing the stock low makes it appear in the list.
     */
    @Test
    void reservation_toLow_thenIncludedInLowStock() throws Exception {
        long id = createProduct("reserved-down", 9);

        mockMvc.perform(post("/api/products/" + id + "/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 7}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products?maxStock=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id));
    }

    /**
     * US1 Scenario 4: a deleted product (009) is never included.
     */
    @Test
    void deletedProduct_neverIncludedInLowStock() throws Exception {
        long id = createProduct("deleted-low", 2);

        mockMvc.perform(delete("/api/products/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products?maxStock=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    // ── US2: Combine with the name filter and paging ───────────────────────

    /**
     * US2 Scenario 1: maxStock + name → only matching low-stock products.
     */
    @Test
    void maxStock_combinesWithNameFilter() throws Exception {
        long idA = createProduct("bolt-a", 2);
        long idB = createProduct("bolt-b", 4);
        createProduct("nut", 3);
        createProduct("bolt-c", 50);

        mockMvc.perform(get("/api/products?maxStock=5&name=bolt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(idA))
                .andExpect(jsonPath("$.content[1].id").value(idB));
    }

    /**
     * US2 Scenario 2: maxStock + page + size → third and fourth in id order, totals count only matches.
     */
    @Test
    void maxStock_combinesWithPaging() throws Exception {
        long id1 = createProduct("page-low-1", 0);
        long id2 = createProduct("page-low-2", 1);
        long id3 = createProduct("page-low-3", 2);
        long id4 = createProduct("page-low-4", 3);
        createProduct("page-low-5", 4);

        mockMvc.perform(get("/api/products?maxStock=5&page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(id3))
                .andExpect(jsonPath("$.content[1].id").value(id4));
    }

    /**
     * US2 Scenario 3: a name with % or _ still matches literally when combined with maxStock.
     */
    @Test
    void maxStock_withWildcardName_matchesLiterally() throws Exception {
        long idPercent = createProduct("wq-100% cotton", 2);
        long idUnderscore = createProduct("wq-sku_12", 3);
        createProduct("wq-100 cotton", 2);
        createProduct("wq-skuX12", 3);

        mockMvc.perform(get("/api/products")
                        .param("maxStock", "5")
                        .param("name", "wq-100%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(idPercent));

        mockMvc.perform(get("/api/products")
                        .param("maxStock", "5")
                        .param("name", "wq-sku_12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(idUnderscore));
    }

    // ── US3: An invalid limit is refused ───────────────────────────────────

    /**
     * US3 Scenario 1: maxStock=-1 → 400 problem+json.
     */
    @Test
    void maxStock_negative_returns400() throws Exception {
        mockMvc.perform(get("/api/products?maxStock=-1"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/api/products"));
    }

    /**
     * US3 Scenario 1: maxStock=1.5 (non-integer) → 400 problem+json.
     */
    @Test
    void maxStock_fractional_returns400() throws Exception {
        mockMvc.perform(get("/api/products?maxStock=1.5"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/api/products"));
    }

    /**
     * US3 Scenario 1: maxStock=x (non-numeric) → 400 problem+json.
     */
    @Test
    void maxStock_nonNumeric_returns400() throws Exception {
        mockMvc.perform(get("/api/products?maxStock=x"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/api/products"));
    }

    /**
     * US3 Scenario 1: maxStock= (empty) is present but invalid → 400 problem+json.
     */
    @Test
    void maxStock_empty_returns400() throws Exception {
        mockMvc.perform(get("/api/products?maxStock="))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/api/products"));
    }

    // ── Edge cases ─────────────────────────────────────────────────────────

    /**
     * GET /api/products with no parameters still answers the plain array, whatever the stock.
     */
    @Test
    void noParams_stillReturnsPlainArray() throws Exception {
        createProduct("plain-low", 0);
        createProduct("plain-high", 100);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    /**
     * A product exactly at the limit is included (<=, not <).
     */
    @Test
    void productExactlyAtLimit_isIncluded() throws Exception {
        long id = createProduct("exactly-at-limit", 5);

        mockMvc.perform(get("/api/products?maxStock=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id));
    }

    /**
     * A very large limit (Integer.MAX_VALUE) includes every product.
     */
    @Test
    void veryLargeLimit_includesEveryProduct() throws Exception {
        createProduct("huge-a", 0);
        createProduct("huge-b", 5);
        createProduct("huge-c", 100);

        mockMvc.perform(get("/api/products?maxStock=2147483647"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content", hasSize(3)));
    }
}
