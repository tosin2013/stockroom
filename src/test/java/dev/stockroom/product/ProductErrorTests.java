package dev.stockroom.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductErrorTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductStore productStore;

    // US1 Scenario 1: Both name empty and stock negative → 400 problem detail with 2 errors
    @Test
    void createProduct_bothInvalid_returns400WithTwoErrors() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"stock\": -1}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.instance").value("/api/products"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty())
                .andExpect(jsonPath("$.errors[1].field").value("stock"))
                .andExpect(jsonPath("$.errors[1].message").isNotEmpty());
    }

    // US1 Scenario 2: Missing stock → 400 with exactly one error for stock
    @Test
    void createProduct_missingStock_returns400WithOneError() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Widget\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value("stock"));
    }

    // US1 Scenario 3: Name of 101 chars with valid stock → 400 with exactly one error for name
    @Test
    void createProduct_nameTooLong_returns400WithOneError() throws Exception {
        String longName = "A".repeat(101);
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + longName + "\", \"stock\": 5}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    // US1 Scenario 4: Validation failures don't store anything
    @Test
    void createProduct_validationFailure_nothingStored() throws Exception {
        int countBefore = productStore.findAll().size();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"stock\": -1}"))
                .andExpect(status().isBadRequest());

        int countAfter = productStore.findAll().size();
        org.junit.jupiter.api.Assertions.assertEquals(countBefore, countAfter,
                "Nothing should have been stored after a validation failure");
    }

    // US2 Scenario 1: GET unknown id → 404 problem detail with id in detail
    @Test
    void getProductById_nonExistent_returns404ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail", containsString("999")))
                .andExpect(jsonPath("$.instance").value("/api/products/999"));
    }

    // US3 Scenario 1: Malformed JSON → 400 problem detail
    @Test
    void createProduct_malformedJson_returns400ProblemDetail() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/api/products"));
    }

    // US3 Scenario 2: Non-numeric id → 400 problem detail
    @Test
    void getProductById_nonNumericId_returns400ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/products/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/api/products/abc"));
    }

    // Edge case: a field that breaks more than one rule still appears exactly once
    @Test
    void createProduct_nameBreaksMultipleRules_appearsOnceInErrors() throws Exception {
        // name is blank (NotBlank) AND too long (Size) — should appear once
        String longBlankName = " ".repeat(101);
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + longBlankName + "\", \"stock\": 5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'name')]", hasSize(1)));
    }

    // Successful responses remain unchanged (FR-005)
    @Test
    void createProduct_valid_returnsApplicationJson() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Gadget\", \"stock\": 3}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Content-Type", containsString("application/json")));
    }

    @Test
    void getProductById_existing_returnsApplicationJson() throws Exception {
        // Create a product first
        org.springframework.test.web.servlet.MvcResult result =
                mockMvc.perform(post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": \"Sprocket\", \"stock\": 7}"))
                        .andExpect(status().isCreated())
                        .andReturn();

        String location = result.getResponse().getHeader("Location");
        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/json")));
    }
}
