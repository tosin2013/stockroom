package dev.stockroom.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
class ReservationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductStore productStore;

    @Autowired
    ApplicationEvents applicationEvents;

    // --- Helper to create a product and return its id ---
    private long createProduct(String name, int stock) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + name + "\", \"stock\": " + stock + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(result.getResponse().getContentAsString());
    }

    // --- US1 Scenario 1: Reserve stock, response has correct fields, stock decreases ---
    @Test
    void reserve_validQuantity_returns200WithCorrectBody() throws Exception {
        long id = createProduct("WidgetA", 5);

        mockMvc.perform(post("/api/products/" + id + "/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 2}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/json")))
                .andExpect(jsonPath("$.productId").value(id))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.remainingStock").value(3));

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(3));
    }

    // --- US1 Scenario 2: Reserve entire stock, remainingStock is 0 ---
    @Test
    void reserve_exactStock_returns200WithZeroRemaining() throws Exception {
        long id = createProduct("WidgetB", 5);

        mockMvc.perform(post("/api/products/" + id + "/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingStock").value(0));

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(0));
    }

    // --- US2 Scenario 1: Insufficient stock returns 409 problem detail ---
    @Test
    void reserve_insufficientStock_returns409ProblemDetail() throws Exception {
        long id = createProduct("WidgetC", 2);

        mockMvc.perform(post("/api/products/" + id + "/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 3}"))
                .andExpect(status().isConflict())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail", containsString("3")))
                .andExpect(jsonPath("$.detail", containsString("2")))
                .andExpect(jsonPath("$.instance").value("/api/products/" + id + "/reservations"))
                .andExpect(jsonPath("$.requested").value(3))
                .andExpect(jsonPath("$.available").value(2));

        // Stock must not change
        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(2));
    }

    // --- US2 Scenario 2a: Missing quantity returns 400 with one error for 'quantity' ---
    @Test
    void reserve_missingQuantity_returns400WithQuantityError() throws Exception {
        long id = createProduct("WidgetD", 5);

        mockMvc.perform(post("/api/products/" + id + "/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("quantity"));

        // Stock must not change
        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(5));
    }

    // --- US2 Scenario 2b: Quantity of 0 returns 400 ---
    @Test
    void reserve_zeroQuantity_returns400WithQuantityError() throws Exception {
        long id = createProduct("WidgetE", 5);

        mockMvc.perform(post("/api/products/" + id + "/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("quantity"));

        // Stock must not change
        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(5));
    }

    // --- US2 Scenario 2c: Negative quantity returns 400 ---
    @Test
    void reserve_negativeQuantity_returns400WithQuantityError() throws Exception {
        long id = createProduct("WidgetF", 5);

        mockMvc.perform(post("/api/products/" + id + "/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": -1}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("quantity"));
    }

    // --- US2 Scenario 3: Unknown product returns 404 problem detail ---
    @Test
    void reserve_unknownProduct_returns404ProblemDetail() throws Exception {
        mockMvc.perform(post("/api/products/999/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 1}"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.instance").value("/api/products/999/reservations"));
    }

    // --- US3 Scenario 1: Concurrent reservations never oversell ---
    @Test
    void reserve_concurrent_neverOversells() throws Exception {
        long id = createProduct("ConcurrentWidget", 10);
        int totalClients = 20;

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(totalClients);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < totalClients; i++) {
            futures.add(executor.submit(() -> {
                try {
                    int status = mockMvc.perform(post("/api/products/" + id + "/reservations")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"quantity\": 1}"))
                            .andReturn()
                            .getResponse()
                            .getStatus();

                    if (status == 200) {
                        successCount.incrementAndGet();
                    } else if (status == 409) {
                        conflictCount.incrementAndGet();
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

        assertEquals(10, successCount.get(), "Exactly 10 reservations should succeed");
        assertEquals(10, conflictCount.get(), "Exactly 10 reservations should be rejected with 409");

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(0));
    }

    // --- US4 Scenario 1: StockReserved event is published on success ---
    @Test
    void reserve_success_publishesStockReservedEvent() throws Exception {
        long id = createProduct("EventWidget", 5);

        mockMvc.perform(post("/api/products/" + id + "/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 2}"))
                .andExpect(status().isOk());

        List<StockReserved> events = applicationEvents.stream(StockReserved.class).toList();
        assertEquals(1, events.size(), "Exactly one StockReserved event should be published");
        StockReserved event = events.get(0);
        assertEquals(id, event.productId());
        assertEquals(2, event.quantity());
        assertEquals(3, event.remainingStock());
    }

    // --- US4 Scenario 2a: No event published on 409 ---
    @Test
    void reserve_insufficientStock_noEventPublished() throws Exception {
        long id = createProduct("NoEventWidget409", 1);

        mockMvc.perform(post("/api/products/" + id + "/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 5}"))
                .andExpect(status().isConflict());

        long count = applicationEvents.stream(StockReserved.class).count();
        assertEquals(0, count, "No StockReserved event should be published on conflict");
    }

    // --- US4 Scenario 2b: No event published on 400 ---
    @Test
    void reserve_invalidRequest_noEventPublished() throws Exception {
        long id = createProduct("NoEventWidget400", 5);

        mockMvc.perform(post("/api/products/" + id + "/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 0}"))
                .andExpect(status().isBadRequest());

        long count = applicationEvents.stream(StockReserved.class).count();
        assertEquals(0, count, "No StockReserved event should be published on bad request");
    }

    // --- US4 Scenario 2c: No event published on 404 ---
    @Test
    void reserve_unknownProduct_noEventPublished() throws Exception {
        mockMvc.perform(post("/api/products/999999/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 1}"))
                .andExpect(status().isNotFound());

        long count = applicationEvents.stream(StockReserved.class).count();
        assertEquals(0, count, "No StockReserved event should be published on not found");
    }

    // --- Edge case: reserve exactly remaining stock leaves 0 ---
    @Test
    void reserve_exactRemainingStock_leavesZero() throws Exception {
        long id = createProduct("ExactWidget", 3);

        mockMvc.perform(post("/api/products/" + id + "/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingStock").value(0));

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(0));
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
