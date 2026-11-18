package dev.stockroom.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Concurrency tests verifying that reservations report the stock they left
 * (spec 007-reservation-stock).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReservationConcurrencyTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StockReservedCollector collector;

    // --- Helpers ---

    private long createProduct(String name, int stock) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + name + "\", \"stock\": " + stock + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return extractIntField(result.getResponse().getContentAsString(), "id");
    }

    /** Extract an integer field value from a flat JSON string, e.g. {"foo":42,...} → 42. */
    private static int extractIntField(String json, String field) {
        String key = "\"" + field + "\":";
        int idx = json.indexOf(key);
        if (idx < 0) throw new IllegalArgumentException("Field '" + field + "' not found in: " + json);
        int start = idx + key.length();
        while (start < json.length() && json.charAt(start) == ' ') start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        return Integer.parseInt(json.substring(start, end));
    }

    // -------------------------------------------------------------------------
    // US1 Scenario 1
    // -------------------------------------------------------------------------

    /**
     * Given a product with stock 100, when 50 clients each reserve 1 at the same time,
     * then all 50 get 200, the 50 remainingStock values are exactly 50 to 99 (each once),
     * and the stock ends at 50.
     */
    @Test
    void us1_scenario1_remainingStockValuesAreDistinctAndCorrect() throws Exception {
        long id = createProduct("US1S1Product", 100);
        int clients = 50;

        List<Integer> remainingStocks = new CopyOnWriteArrayList<>();
        CyclicBarrier barrier = new CyclicBarrier(clients);
        ExecutorService executor = Executors.newFixedThreadPool(clients);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < clients; i++) {
            futures.add(executor.submit(() -> {
                try {
                    barrier.await();
                    MvcResult result = mockMvc.perform(post("/api/products/" + id + "/reservations")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"quantity\": 1}"))
                            .andReturn();
                    int status = result.getResponse().getStatus();
                    assertEquals(200, status, "All 50 reservations should succeed");
                    remainingStocks.add(extractIntField(result.getResponse().getContentAsString(), "remainingStock"));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        for (Future<?> f : futures) f.get();
        executor.shutdown();

        assertEquals(50, remainingStocks.size());

        Set<Integer> expected = new HashSet<>();
        for (int v = 50; v <= 99; v++) expected.add(v);
        assertEquals(expected, new HashSet<>(remainingStocks),
                "remainingStock values must be exactly 50 to 99, each once");

        MvcResult stockResult = mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(50, extractIntField(stockResult.getResponse().getContentAsString(), "stock"));
    }

    // -------------------------------------------------------------------------
    // US1 Scenario 2
    // -------------------------------------------------------------------------

    /**
     * Given a product with stock 100, when 30 clients reserve with quantities 1, 2 or 3,
     * every response's remainingStock differs, and sorted by remainingStock descending
     * each equals the one before it minus its own quantity.
     */
    @Test
    void us1_scenario2_remainingStockChainIsConsistent() throws Exception {
        long id = createProduct("US1S2Product", 100);
        int clients = 30;

        int[] quantities = new int[clients];
        for (int i = 0; i < clients; i++) quantities[i] = (i % 3) + 1;

        record ReservationResult(int quantity, int remainingStock) {}
        List<ReservationResult> successes = new CopyOnWriteArrayList<>();

        CyclicBarrier barrier = new CyclicBarrier(clients);
        ExecutorService executor = Executors.newFixedThreadPool(clients);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < clients; i++) {
            final int qty = quantities[i];
            futures.add(executor.submit(() -> {
                try {
                    barrier.await();
                    MvcResult result = mockMvc.perform(post("/api/products/" + id + "/reservations")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"quantity\": " + qty + "}"))
                            .andReturn();
                    if (result.getResponse().getStatus() == 200) {
                        int rs = extractIntField(result.getResponse().getContentAsString(), "remainingStock");
                        successes.add(new ReservationResult(qty, rs));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        for (Future<?> f : futures) f.get();
        executor.shutdown();

        // Every remainingStock must be distinct
        List<Integer> rsList = successes.stream().map(ReservationResult::remainingStock).toList();
        assertEquals(rsList.size(), new HashSet<>(rsList).size(),
                "Every response's remainingStock must differ");

        // Sort descending by remainingStock
        List<ReservationResult> sorted = new ArrayList<>(successes);
        sorted.sort(Comparator.comparingInt(ReservationResult::remainingStock).reversed());

        // The first entry's remainingStock == 100 - its quantity
        ReservationResult first = sorted.get(0);
        assertEquals(100 - first.quantity(), first.remainingStock(),
                "First (highest remainingStock) must equal 100 minus its quantity");

        // Each subsequent entry's remainingStock == previous remainingStock - its own quantity
        for (int i = 1; i < sorted.size(); i++) {
            ReservationResult prev = sorted.get(i - 1);
            ReservationResult cur = sorted.get(i);
            assertEquals(prev.remainingStock() - cur.quantity(), cur.remainingStock(),
                    "Each remainingStock must equal the previous one minus its own quantity (index " + i + ")");
        }
    }

    // -------------------------------------------------------------------------
    // US2 Scenario 1 – Events carry the same figure
    // -------------------------------------------------------------------------

    /**
     * Given 50 concurrent reservations of 1 against stock 100,
     * when all StockReserved events are collected,
     * there are 50 and their remainingStock values are exactly 50 to 99, each once.
     */
    @Test
    void us2_scenario1_eventsCarryCorrectRemainingStock() throws Exception {
        collector.clear();

        long id = createProduct("US2S1Product", 100);
        int clients = 50;

        CyclicBarrier barrier = new CyclicBarrier(clients);
        ExecutorService executor = Executors.newFixedThreadPool(clients);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < clients; i++) {
            futures.add(executor.submit(() -> {
                try {
                    barrier.await();
                    mockMvc.perform(post("/api/products/" + id + "/reservations")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"quantity\": 1}"))
                            .andReturn();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        for (Future<?> f : futures) f.get();
        executor.shutdown();

        List<StockReserved> events = collector.getEvents().stream()
                .filter(e -> e.productId() == id)
                .toList();

        assertEquals(50, events.size(), "Exactly 50 StockReserved events must be published");

        Set<Integer> expected = new HashSet<>();
        for (int v = 50; v <= 99; v++) expected.add(v);
        Set<Integer> actual = new HashSet<>();
        for (StockReserved e : events) actual.add(e.remainingStock());
        assertEquals(expected, actual,
                "Event remainingStock values must be exactly 50 to 99, each once");
    }

    // -------------------------------------------------------------------------
    // US3 Scenario 1 – Refusals report what is really available
    // -------------------------------------------------------------------------

    /**
     * Given a product with stock 5, when 15 clients each reserve 1 at the same time,
     * then exactly 5 get 200 and 10 get 409, and every 409 reports "available": 0.
     */
    @Test
    void us3_scenario1_refusalsReportZeroAvailableWhenStockExhausted() throws Exception {
        long id = createProduct("US3S1Product", 5);
        int clients = 15;

        List<Integer> availableOnConflict = new CopyOnWriteArrayList<>();
        List<Integer> successStatuses = new CopyOnWriteArrayList<>();

        CyclicBarrier barrier = new CyclicBarrier(clients);
        ExecutorService executor = Executors.newFixedThreadPool(clients);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < clients; i++) {
            futures.add(executor.submit(() -> {
                try {
                    barrier.await();
                    MvcResult result = mockMvc.perform(post("/api/products/" + id + "/reservations")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"quantity\": 1}"))
                            .andReturn();
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        successStatuses.add(200);
                    } else if (status == 409) {
                        int available = extractIntField(result.getResponse().getContentAsString(), "available");
                        availableOnConflict.add(available);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        for (Future<?> f : futures) f.get();
        executor.shutdown();

        assertEquals(5, successStatuses.size(), "Exactly 5 reservations should succeed");
        assertEquals(10, availableOnConflict.size(), "Exactly 10 reservations should get 409");

        for (int available : availableOnConflict) {
            assertEquals(0, available, "Every 409 must report available == 0");
        }
    }
}
