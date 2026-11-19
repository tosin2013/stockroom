package dev.stockroom.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductFilterWildcardTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductStore productStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearProducts() {
        jdbcTemplate.execute("DELETE FROM product");
    }

    // ── US1: % matches only a percent sign ─────────────────────────────────

    /**
     * US1 Scenario 1: name=wq-100%25 finds only the product whose name really contains "%".
     */
    @Test
    void percentSign_matchesOnlyPercentSign() throws Exception {
        productStore.save("wq-100% cotton", 10);
        productStore.save("wq-100 cotton", 10);

        mockMvc.perform(get("/api/products").param("name", "wq-100%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("wq-100% cotton"));
    }

    /**
     * US1 Scenario 2: name=%25 returns only products with a "%" in their name.
     */
    @Test
    void percentSign_alone_matchesOnlyProductsContainingPercent() throws Exception {
        productStore.save("wq-100% cotton", 10);
        productStore.save("wq-100 cotton", 10);

        mockMvc.perform(get("/api/products").param("name", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("wq-100% cotton"))
                .andExpect(jsonPath("$.content[?(@.name == 'wq-100 cotton')]", hasSize(0)));
    }

    // ── US2: _ matches only an underscore ──────────────────────────────────

    /**
     * US2 Scenario 1: name=wu-sku_12 finds only the product with a literal underscore.
     */
    @Test
    void underscore_matchesOnlyUnderscore() throws Exception {
        productStore.save("wu-sku_12", 10);
        productStore.save("wu-skuX12", 10);

        mockMvc.perform(get("/api/products").param("name", "wu-sku_12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("wu-sku_12"));
    }

    // ── US3: the escape character itself is literal ────────────────────────

    /**
     * US3 Scenario 1: name=wb-a%5Cb finds only the product with a literal backslash.
     */
    @Test
    void backslash_matchesOnlyBackslash() throws Exception {
        productStore.save("wb-a\\b", 10);
        productStore.save("wb-ab", 10);

        mockMvc.perform(get("/api/products").param("name", "wb-a\\b"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("wb-a\\b"));
    }

    /**
     * US3 Scenario 2: name=wb-c%5C%25d finds only the product with both a backslash and a
     * literal percent sign.
     */
    @Test
    void backslashAndPercent_matchLiterally() throws Exception {
        productStore.save("wb-c\\%d", 10);
        productStore.save("wb-c%d", 10);

        mockMvc.perform(get("/api/products").param("name", "wb-c\\%d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("wb-c\\%d"));
    }

    // ── Edge cases ─────────────────────────────────────────────────────────

    /**
     * Matching still ignores case and still matches anywhere in the name.
     */
    @Test
    void matching_ignoresCaseAndMatchesAnywhere() throws Exception {
        productStore.save("wq-100% cotton", 10);
        productStore.save("wq-100 cotton", 10);

        mockMvc.perform(get("/api/products").param("name", "WQ-100%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("wq-100% cotton"));
    }

    /**
     * Paging counts only the literal matches.
     */
    @Test
    void paging_countsOnlyLiteralMatches() throws Exception {
        productStore.save("wq-100% cotton", 10);
        productStore.save("wq-100 cotton", 10);

        mockMvc.perform(get("/api/products")
                        .param("name", "wq-100%")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("wq-100% cotton"));
    }

    /**
     * Text without any wildcard character still matches exactly as in 006.
     */
    @Test
    void textWithoutWildcards_matchesAsIn006() throws Exception {
        productStore.save("wq-100% cotton", 10);
        productStore.save("wq-100 cotton", 10);

        mockMvc.perform(get("/api/products").param("name", "wq-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content", hasSize(2)));
    }
}
