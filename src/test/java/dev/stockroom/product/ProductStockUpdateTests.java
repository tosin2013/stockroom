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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductStockUpdateTests {

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

    private int extractStock(String json) {
        int idx = json.indexOf("\"stock\":");
        if (idx < 0) throw new IllegalArgumentException("No stock in: " + json);
        int start = idx + 8;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        return Integer.parseInt(json.substring(start, end));
    }

    // --- US1 Scenario 1: PATCH sets stock, answers 200 with {id, name, stock: 12} ---
    @Test
    void patchStock_returns200WithUpdatedProduct() throws Exception {
        long id = createProduct("Widget", 5);

        mockMvc.perform(patch("/api/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\": 12}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/json")))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Widget"))
                .andExpect(jsonPath("$.stock").value(12));
    }

    // --- US1 Scenario 2: after update, read returns 12 and reservation of 10 leaves 2 ---
    @Test
    void patchStock_thenReadAndReserve_seeNewStock() throws Exception {
        long id = createProduct("Delivered", 5);

        mockMvc.perform(patch("/api/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\": 12}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(12));

        mockMvc.perform(post("/api/products/" + id + "/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingStock").value(2));
    }

    // --- US1 Scenario 3: set stock to 0 → 200, reserve 1 → 409 with available 0 ---
    @Test
    void patchStock_setZero_thenReserveOne_returns409Available0() throws Exception {
        long id = createProduct("Counted", 3);

        mockMvc.perform(patch("/api/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\": 0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(0));

        mockMvc.perform(post("/api/products/" + id + "/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 1}"))
                .andExpect(status().isConflict())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.available").value(0));
    }

    // --- US2 Scenario 1: negative stock → 400, errors names 'stock', stock unchanged ---
    @Test
    void patchStock_negative_returns400AndChangesNothing() throws Exception {
        long id = createProduct("Neg", 5);

        mockMvc.perform(patch("/api/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\": -1}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("stock"));

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(5));
    }

    // --- US2 Scenario 1: non-integer stock → 400, stock unchanged ---
    @Test
    void patchStock_fractional_returns400AndChangesNothing() throws Exception {
        long id = createProduct("Fraction", 5);

        mockMvc.perform(patch("/api/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\": 1.5}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(5));
    }

    // --- US2 Scenario 1: non-numeric stock → 400, stock unchanged ---
    @Test
    void patchStock_nonNumeric_returns400AndChangesNothing() throws Exception {
        long id = createProduct("Text", 5);

        mockMvc.perform(patch("/api/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\": \"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(5));
    }

    // --- US2 Scenario 1: missing stock → 400, errors names 'stock', stock unchanged ---
    @Test
    void patchStock_missing_returns400AndChangesNothing() throws Exception {
        long id = createProduct("Missing", 5);

        mockMvc.perform(patch("/api/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("stock"));

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(5));
    }

    // --- US2 Scenario 1: body that isn't JSON → 400, stock unchanged ---
    @Test
    void patchStock_notJson_returns400AndChangesNothing() throws Exception {
        long id = createProduct("BadJson", 5);

        mockMvc.perform(patch("/api/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\": "))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(5));
    }

    // --- US2 Scenario 2: unknown id → 404 Problem Detail naming the id ---
    @Test
    void patchStock_unknownId_returns404NamingId() throws Exception {
        mockMvc.perform(patch("/api/products/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\": 12}"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail", containsString("999999")))
                .andExpect(jsonPath("$.instance").value("/api/products/999999"));
    }

    // --- US2 Scenario 3: patching a deleted product → 404 ---
    @Test
    void patchStock_deletedProduct_returns404() throws Exception {
        long id = createProduct("Gone", 5);

        mockMvc.perform(delete("/api/products/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/api/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\": 12}"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(404));
    }

    // --- US2 Scenario 4: a "name" field in the body is ignored ---
    @Test
    void patchStock_ignoresOtherFields() throws Exception {
        long id = createProduct("Original", 5);

        mockMvc.perform(patch("/api/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\": 12, \"name\": \"other\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(12));

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Original"))
                .andExpect(jsonPath("$.stock").value(12));
    }

    // --- Edge case: setting stock to its current value → 200, nothing else changes ---
    @Test
    void patchStock_sameValue_returns200AndChangesNothingElse() throws Exception {
        long id = createProduct("Unchanged", 5);

        mockMvc.perform(patch("/api/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(5));

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Unchanged"))
                .andExpect(jsonPath("$.stock").value(5));
    }

    // --- Edge case: the plain list and paging show the new stock ---
    @Test
    void patchStock_listAndPagingShowNewStock() throws Exception {
        long id = createProduct("Listed", 5);

        mockMvc.perform(patch("/api/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\": 9}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].stock").value(9));

        mockMvc.perform(get("/api/products?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + id + ")].stock").value(9));
    }

    // --- Edge case: an update is one atomic write, never interleaved with reservations ---
    @Test
    void patchStock_concurrentWithReservations_isOneAtomicWrite() throws Exception {
        long id = createProduct("Atomic", 0);
        int clients = 40;

        AtomicInteger successCount = new AtomicInteger(0);
        CyclicBarrier barrier = new CyclicBarrier(clients + 1);
        ExecutorService executor = Executors.newFixedThreadPool(clients + 1);
        List<Future<?>> futures = new ArrayList<>();

        // One thread sets the stock to 50 while the others reserve 1 each
        futures.add(executor.submit(() -> {
            try {
                barrier.await();
                mockMvc.perform(patch("/api/products/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"stock\": 50}"))
                        .andExpect(status().isOk());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));

        for (int i = 0; i < clients; i++) {
            futures.add(executor.submit(() -> {
                try {
                    barrier.await();
                    int status = mockMvc.perform(post("/api/products/" + id + "/reservations")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"quantity\": 1}"))
                            .andReturn().getResponse().getStatus();
                    if (status == 200) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();

        MvcResult result = mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andReturn();
        int finalStock = extractStock(result.getResponse().getContentAsString());

        // Because the update is a single atomic write and every reservation is a single
        // atomic decrement, no write can be lost: finalStock + successfulReservations
        // must always add up to the value that the update wrote.
        assertEquals(50, finalStock + successCount.get(),
                "finalStock + successfulReservations must equal the updated stock");
    }
}
