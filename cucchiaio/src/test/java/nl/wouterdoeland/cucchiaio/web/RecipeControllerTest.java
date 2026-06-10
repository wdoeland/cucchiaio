package nl.wouterdoeland.cucchiaio.web;

import lombok.val;
import nl.wouterdoeland.cucchiaio.service.RecipeService;
import nl.wouterdoeland.cucchiaio.web.dto.RecipeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.parameters.P;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecipeController.class)
public class RecipeControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeService service;

    private static final String ALICE = "alice";

    @Test
    void get_returns401_whenNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/recipe/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void get_returns200_whenauthorized() throws Exception {
        RecipeResponse response = new RecipeResponse(1L, true, "Oven Salmon", 2, "bake the salmon in the oven", List.of(), List.of());
        when(service.get(eq(1L), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/recipe/1")
                        .with(jwt().jwt(j -> j.subject(ALICE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Oven Salmon"))
                .andExpect(jsonPath("$.canEdit").value(true))
                .andExpect(jsonPath("$.ownerId").doesNotExist()); // check that ownerId is not in response body
    }
}
