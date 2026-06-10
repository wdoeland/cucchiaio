package nl.wouterdoeland.cucchiaio;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class RecipeEndToEndIT {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // load test JWT signing key, with which test JWTs are signed
        registry.add("jwt.secret", () -> "zm127rg6rKJuHuWYQVrJs2IBgbyDzm9b6LF0yQ004bY=");
    }

    // JWTs for testing, signed by the JWT signing key
    private static final String ALICE_JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJBTElDRS1BTElDRSIsIm5hbWUiOiJBTElDRSIsImlhdCI6MTUxNjIzOTAyMn0.0M39pPPfL1oxT5QifbO1fD-GZytT_9jGa2VO5NTcNuk";
    private static final String BOB_JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJCT0ItQk9CIiwibmFtZSI6IkJPQiIsImlhdCI6MTUxNjIzOTAyMn0.9NchM5h-zpvytpUKwsaevXW6NNyPOLTFZhAY5iGVX0s";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Basic end-to-end test of creating and fetching
     */
    @Test
    void createThenFetch() throws Exception {
        String body = """
                {
                  "title": "Vegan Gulasch",
                  "servingSize": 5,
                  "instructions": "cook the veggie meat. add the veggies. Enjoy!",
                  "dietaryOptions": [
                    "VEGETARIAN"
                  ],
                  "ingredients": [
                    {
                      "name": "veggie meat",
                      "quantity": 500,
                      "unit": "gr"
                    }
                  ]
                }
                """;

        String response = mockMvc.perform(post("/api/v1/recipe")
                        .header("Authorization", "Bearer " + ALICE_JWT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Vegan Gulasch"))
                .andExpect(jsonPath("$.ownerId").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/v1/recipe/" + id)
                        .header("Authorization", "Bearer " + ALICE_JWT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Vegan Gulasch"))
                .andExpect(jsonPath("$.canEdit").value(true))
                .andExpect(jsonPath("$.ingredients[0].name").value("veggie meat"));

        mockMvc.perform(get("/api/v1/recipe/" + id)
                        .header("Authorization", "Bearer " + BOB_JWT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Vegan Gulasch"))
                .andExpect(jsonPath("$.canEdit").value(false));
    }

    /**
     * Full CRUD test with ownership tests
     */
    void createThenUpdateThenFetchThenDelete_andTestOwnership() throws Exception {
        String body = """
                {
                  "title": "Vegan Gulasch",
                  "servingSize": 5,
                  "instructions": "cook the veggie meat. Enjoy!",
                  "dietaryOptions": [
                    "VEGETARIAN"
                  ],
                  "ingredients": [
                    {
                      "name": "veggie meat",
                      "quantity": 500,
                      "unit": "gr"
                    }
                  ]
                }
                """;

        String response = mockMvc.perform(post("/api/v1/recipe")
                        .header("Authorization", "Bearer " + ALICE_JWT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Vegan Gulasch"))
                .andExpect(jsonPath("$.ownerId").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/v1/recipe/" + id)
                        .header("Authorization", "Bearer " + ALICE_JWT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Vegan Gulasch"))
                .andExpect(jsonPath("$.canEdit").value(true))
                .andExpect(jsonPath("$.ingredients[0].name").value("veggie meat"));

        mockMvc.perform(get("/api/v1/recipe/" + id)
                        .header("Authorization", "Bearer " + BOB_JWT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Vegan Gulasch"))
                .andExpect(jsonPath("$.canEdit").value(false));

        // Test updating the recipe
        String update = """
                {
                  "title": "Improved Vegan Gulasch",
                  "servingSize": 5,
                  "instructions": "cook the veggie meat. Enjoy!",
                  "dietaryOptions": [
                    "VEGETARIAN"
                  ],
                  "ingredients": [
                    {
                      "name": "upgraded veggie meat",
                      "quantity": 1,
                      "unit": "kg"
                    }
                  ]
                }
                """;

        // should REJECT because the recipe is owned by ALICE and the request is coming from BOB
        mockMvc.perform(put("/api/v1/recipe/" + id)
                        .header("Authorization", "Bearer " + BOB_JWT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        // recipe is untouched
        mockMvc.perform(get("/api/v1/recipe/" + id)
                        .header("Authorization", "Bearer " + ALICE_JWT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Vegan Gulasch"))
                .andExpect(jsonPath("$.canEdit").value(true))
                .andExpect(jsonPath("$.ingredients[0].name").value("veggie meat"));

        // should ACCEPT changes because the recipe is owned by ALICE
        mockMvc.perform(put("/api/v1/recipe/" + id)
                        .header("Authorization", "Bearer " + ALICE_JWT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        // recipe is changed
        mockMvc.perform(get("/api/v1/recipe/" + id)
                        .header("Authorization", "Bearer " + ALICE_JWT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Improved Vegan Gulasch"))
                .andExpect(jsonPath("$.canEdit").value(true))
                .andExpect(jsonPath("$.ingredients[0].name").value("upgraded veggie meat"));

        // Test deleting the recipe

        // should REJECT because the recipe is owned by ALICE and the request is coming from BOB
        mockMvc.perform(post("/api/v1/recipe/" + id)
                        .header("Authorization", "Bearer " + BOB_JWT))
                .andExpect(status().isForbidden());

        // recipe is still exists
        mockMvc.perform(get("/api/v1/recipe/" + id)
                        .header("Authorization", "Bearer " + ALICE_JWT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Improved Vegan Gulasch"))
                .andExpect(jsonPath("$.canEdit").value(true))
                .andExpect(jsonPath("$.ingredients[0].name").value("upgraded veggie meat"));

        // should ACCEPT changes because the recipe is owned by ALICE
        mockMvc.perform(post("/api/v1/recipe/" + id)
                        .header("Authorization", "Bearer " + BOB_JWT))
                .andExpect(status().isForbidden());

        // recipe is gone
        mockMvc.perform(get("/api/v1/recipe/" + id)
                        .header("Authorization", "Bearer " + ALICE_JWT))
                .andExpect(status().isNotFound());
    }
}
