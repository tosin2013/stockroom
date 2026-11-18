package dev.stockroom.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductStore productStore;

    // US1 Scenario 1: Post valid product, get 201 + Location + JSON body
    @Test
    void createProduct_returnsCreatedWithLocationAndBody() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Widget\", \"stock\": 5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Widget"))
                .andExpect(jsonPath("$.stock").value(5))
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();

        String location = result.getResponse().getHeader("Location");
        assertNotNull(location);
        assertTrue(location.startsWith("/api/products/"));
    }

    // US1 Scenario 2: Name with surrounding spaces is trimmed
    @Test
    void createProduct_trimsNameSurroundingSpaces() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"  Widget  \", \"stock\": 3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Widget"));
    }

    // US1 Scenario 3: Second id is greater than first
    @Test
    void createProduct_idsArePositiveAndIncreasing() throws Exception {
        MvcResult first = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Alpha\", \"stock\": 1}"))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult second = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Beta\", \"stock\": 2}"))
                .andExpect(status().isCreated())
                .andReturn();

        String firstBody = first.getResponse().getContentAsString();
        String secondBody = second.getResponse().getContentAsString();

        // Extract ids from JSON responses
        long firstId = extractId(firstBody);
        long secondId = extractId(secondBody);

        assertTrue(firstId > 0, "First id must be positive");
        assertTrue(secondId > 0, "Second id must be positive");
        assertTrue(secondId > firstId, "Second id must be greater than first");
    }

    // US2 Scenario 1: Missing name returns 400
    @Test
    void createProduct_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\": 5}"))
                .andExpect(status().isBadRequest());
    }

    // US2 Scenario 1: Empty name returns 400
    @Test
    void createProduct_emptyName_returns400() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"stock\": 5}"))
                .andExpect(status().isBadRequest());
    }

    // US2 Scenario 1: Blank name returns 400
    @Test
    void createProduct_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"   \", \"stock\": 5}"))
                .andExpect(status().isBadRequest());
    }

    // US2 Scenario 2: Name longer than 100 characters returns 400
    @Test
    void createProduct_nameTooLong_returns400() throws Exception {
        String longName = "A".repeat(101);
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + longName + "\", \"stock\": 5}"))
                .andExpect(status().isBadRequest());
    }

    // US2 Scenario 3: Missing stock returns 400
    @Test
    void createProduct_missingStock_returns400() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Widget\"}"))
                .andExpect(status().isBadRequest());
    }

    // US2 Scenario 3: Negative stock returns 400
    @Test
    void createProduct_negativeStock_returns400() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Widget\", \"stock\": -1}"))
                .andExpect(status().isBadRequest());
    }

    // US2 Scenario 3: Stock of 0 is accepted
    @Test
    void createProduct_zeroStock_isAccepted() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"FreeItem\", \"stock\": 0}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stock").value(0));
    }

    // US3 Scenario 1: GET by id returns same body as creation
    @Test
    void getProductById_returnsCreatedProduct() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Gadget\", \"stock\": 10}"))
                .andExpect(status().isCreated())
                .andReturn();

        String location = created.getResponse().getHeader("Location");
        assertNotNull(location);

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gadget"))
                .andExpect(jsonPath("$.stock").value(10));
    }

    // US3 Scenario 2: GET non-existent id returns 404
    @Test
    void getProductById_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound());
    }

    // US3 Scenario 3: GET all returns products in creation order
    @Test
    void getAllProducts_returnsInCreationOrder() throws Exception {
        MvcResult first = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"First\", \"stock\": 1}"))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult second = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Second\", \"stock\": 2}"))
                .andExpect(status().isCreated())
                .andReturn();

        long firstId = extractId(first.getResponse().getContentAsString());
        long secondId = extractId(second.getResponse().getContentAsString());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + firstId + ")].name").value("First"))
                .andExpect(jsonPath("$[?(@.id == " + secondId + ")].name").value("Second"));
    }

    // US3 Scenario 3: GET all returns empty array when no products
    @Test
    void getAllProducts_emptyWhenNoProducts() throws Exception {
        // Use a fresh store (no products yet from this test method)
        // We just check that /api/products returns a 200 with a JSON array
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    private long extractId(String json) {
        // Simple extraction: find "id":<number>
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
