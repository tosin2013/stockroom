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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
class ProductDeleteTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    ApplicationEvents applicationEvents;

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

    // US1 Scenario 1: DELETE an existing product → 204 with an empty body
    @Test
    void deleteExistingProduct_returns204EmptyBody() throws Exception {
        long id = createProduct("ToDelete", 5);

        mockMvc.perform(delete("/api/products/" + id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    // US1 Scenario 2: after delete, GET by id → 404 Problem Detail, list omits it
    @Test
    void deleteThenGet_returns404AndListOmitsIt() throws Exception {
        long id = createProduct("Gone", 5);

        mockMvc.perform(delete("/api/products/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]", hasSize(0)));
    }

    // US1 Scenario 3: deleting one product leaves the other unchanged
    @Test
    void deleteOneProduct_leavesOtherUnchanged() throws Exception {
        long keepId = createProduct("Keep", 7);
        long removeId = createProduct("Remove", 3);

        mockMvc.perform(delete("/api/products/" + removeId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/" + keepId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(keepId))
                .andExpect(jsonPath("$.name").value("Keep"))
                .andExpect(jsonPath("$.stock").value(7));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + keepId + ")]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.id == " + keepId + ")].name").value("Keep"));
    }

    // US2 Scenario 1: delete an id that never existed → 404 Problem Detail naming the id
    @Test
    void deleteUnknownId_returns404ProblemDetailNamingId() throws Exception {
        mockMvc.perform(delete("/api/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail", containsString("999999")))
                .andExpect(jsonPath("$.instance").value("/api/products/999999"));
    }

    // US2 Scenario 2: delete a product that was just deleted → 404
    @Test
    void deleteAlreadyDeleted_returns404() throws Exception {
        long id = createProduct("TwiceDeleted", 5);

        mockMvc.perform(delete("/api/products/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/products/" + id))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(404));
    }

    // US2 Scenario 3: an id that isn't a whole number → 400 Problem Detail
    @Test
    void deleteNonNumericId_returns400() throws Exception {
        mockMvc.perform(delete("/api/products/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/api/products/abc"));
    }

    // US3 Scenario 1: reserving a deleted product → 404 and no StockReserved event
    @Test
    void reserveDeletedProduct_returns404AndNoEventPublished() throws Exception {
        long id = createProduct("ReserveGone", 5);

        mockMvc.perform(delete("/api/products/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/products/" + id + "/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 1}"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(404));

        long count = applicationEvents.stream(StockReserved.class).count();
        assertEquals(0, count, "No StockReserved event should be published");
    }

    // Edge case: deleting doesn't renumber anything and a new product never reuses a deleted id
    @Test
    void delete_doesNotRenumberOrReuseIds() throws Exception {
        long firstId = createProduct("First", 1);
        long secondId = createProduct("Second", 2);

        mockMvc.perform(delete("/api/products/" + firstId))
                .andExpect(status().isNoContent());

        // The remaining product keeps its id
        mockMvc.perform(get("/api/products/" + secondId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(secondId));

        // A new product never reuses the deleted product's id
        long newId = createProduct("Third", 3);
        assertTrue(newId > secondId, "New product id must be greater than the remaining product's id");
        assertNotEquals(firstId, newId, "A new product must not reuse the deleted product's id");
    }

    // Edge case: paging counts only existing products after a delete
    @Test
    void delete_reducesPagedTotalElements() throws Exception {
        createProduct("A", 1);
        long idB = createProduct("B", 2);

        mockMvc.perform(delete("/api/products/" + idB))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("A"));
    }
}
