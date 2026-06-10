package nl.wouterdoeland.cucchiaio.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import nl.wouterdoeland.cucchiaio.domain.DietaryOption;

import java.util.List;

public record CreateRecipeRequest(
        @Schema(description = "Title of the recipe", example = "Baked Salmon")
        @NotBlank @Size(max = 255) String title,
        @Schema(description = "Serving size of the recipe", example = "2")
        @NotNull @Positive Integer servingSize,
        @Schema(description = "Instructions for the recipe", example = "Preheat the oven at 200 degrees. Salt the salmon. Bake the salmon for 40 minutes.")
        @NotBlank String instructions,
        @Schema(description = "Dietary information for the recipe", example = "[ \"VEGETARIAN\" ]")
        List<DietaryOption> dietaryOptions,
        @Schema(description = "List of ingredients for the recipe", example = "[ { \"name\": \"Salmon\", \"quantity\": 500, \"unit\": \"gram\"}, { \"name\": \"Salt\" } ]")
        @Valid List<IngredientRequest> ingredients
) {}
