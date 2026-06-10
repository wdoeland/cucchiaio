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
    @Test
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
                        .content(update))
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
                        .content(update))
                .andExpect(status().isOk());

        // recipe is changed
        mockMvc.perform(get("/api/v1/recipe/" + id)
                        .header("Authorization", "Bearer " + ALICE_JWT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Improved Vegan Gulasch"))
                .andExpect(jsonPath("$.canEdit").value(true))
                .andExpect(jsonPath("$.ingredients[0].name").value("upgraded veggie meat"));

        // Test deleting the recipe

        // should REJECT because the recipe is owned by ALICE and the request is coming from BOB
        mockMvc.perform(delete("/api/v1/recipe/" + id)
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
        mockMvc.perform(delete("/api/v1/recipe/" + id)
                        .header("Authorization", "Bearer " + ALICE_JWT))
                .andExpect(status().isNoContent());

        // recipe is gone
        mockMvc.perform(get("/api/v1/recipe/" + id)
                        .header("Authorization", "Bearer " + ALICE_JWT))
                .andExpect(status().isNotFound());
    }

    /**
     * Query system tests, including queries from the exercise
     */
    @Test
    void queryRecipes() throws Exception {
        String gnocchiRecipe = """
                {
                  "title": "Gnocchi",
                  "servingSize": 4,
                  "instructions": "boil the potatoes. make the gnocchi. add cheese to taste",
                  "dietaryOptions": [
                    "VEGETARIAN"
                  ],
                  "ingredients": [
                    {
                      "name": "potatoes",
                      "quantity": 0.5,
                      "unit": "kg"
                    },
                    {
                      "name": "Parmigiano Reggiano"
                    }
                  ]
                }
                """;
        String lasagnaRecipe = """
                {
                  "title": "Lasagna",
                  "servingSize": 6,
                  "instructions": "pre-heat the oven at 180 degrees. chop the veggies. cook the meat and veggies in a pan. lay in a pan with pasta sheets. bake in the oven for 40 minutes.",
                  "ingredients": [
                    {
                      "name": "minced meat",
                      "quantity": 300,
                      "unit": "gr"
                    },
                    {
                      "name": "vegetables",
                      "quantity": 1,
                      "unit": "kg"
                    },
                    {
                      "name": "onion",
                      "quantity": 2
                    },
                    {
                      "name": "lasagna sheets",
                      "quantity": 300,
                      "unit": "gr"
                    }
                  ]
                }
                """;
        String ovenSalmonRecipe = """
                {
                   "title": "Oven Salmon",
                   "servingSize": 2,
                   "instructions": "pre-heat the oven to 200 degrees. season the salmon with salt. cook the salmon in the oven for 40 minutes.",
                   "dietaryOptions": [
                   ],
                   "ingredients": [
                     {
                       "name": "salmon",
                       "quantity": 200,
                       "unit": "gr"
                     },
                     {
                       "name": "salt"
                     }
                   ]
                 }
                """;

        String response = mockMvc.perform(post("/api/v1/recipe")
                        .header("Authorization", "Bearer " + ALICE_JWT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gnocchiRecipe))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Gnocchi"))
                .andExpect(jsonPath("$.ownerId").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        Long gnocchiId = objectMapper.readTree(response).get("id").asLong();
        response = mockMvc.perform(post("/api/v1/recipe")
                        .header("Authorization", "Bearer " + ALICE_JWT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lasagnaRecipe))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Lasagna"))
                .andExpect(jsonPath("$.ownerId").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        Long lasagnaId = objectMapper.readTree(response).get("id").asLong();
        response = mockMvc.perform(post("/api/v1/recipe")
                        .header("Authorization", "Bearer " + ALICE_JWT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ovenSalmonRecipe))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Oven Salmon"))
                .andExpect(jsonPath("$.ownerId").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        Long ovenSalmonId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/v1/recipe/search")
                        .header("Authorization", "Bearer " + ALICE_JWT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].title").value("Gnocchi"))
                .andExpect(jsonPath("$.content[1].title").value("Lasagna"))
                .andExpect(jsonPath("$.content[2].title").value("Oven Salmon"));

        // "All vegetarian recipes"
        mockMvc.perform(get("/api/v1/recipe/search")
                        .header("Authorization", "Bearer " + ALICE_JWT)
                        .param("dietary", "VEGETARIAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Gnocchi"));

        // "Recipes that can serve 4 persons and have “potatoes” as an ingredient"
        mockMvc.perform(get("/api/v1/recipe/search")
                        .header("Authorization", "Bearer " + ALICE_JWT)
                        .param("servingsLower", "4")
                        .param("ingredient", "potatoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Gnocchi"));

        // "Recipes without “salmon” as an ingredient that has “oven” in the instructions."
        mockMvc.perform(get("/api/v1/recipe/search")
                        .header("Authorization", "Bearer " + ALICE_JWT)
                        .param("instructions", "oven")
                        .param("ingredientExcluded", "salmon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Lasagna"));
    }
}
