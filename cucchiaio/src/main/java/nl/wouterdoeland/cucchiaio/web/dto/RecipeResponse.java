package nl.wouterdoeland.cucchiaio.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import nl.wouterdoeland.cucchiaio.domain.DietaryOption;

import java.util.List;

/**
 * Recipe
 * @param id ID of the recipe
 * @param canEdit True if the current user can modify or delete this recipe
 * @param title Title of the recipe
 * @param servingSize Serving size of the recipe
 * @param instructions Instructions for the recipe
 * @param dietaryOptions Dietary options for the recipe
 * @param ingredients List of ingredients for the recipe
 */
public record RecipeResponse(
    @Schema(description = "Id of the recipe", example = "1337")
    Long id,
    @Schema(description = "True if the current user can edit or delete the recipe", example = "true")
    Boolean canEdit,
    @Schema(description = "Title of the recipe", example = "Baked Salmon")
    String title,
    @Schema(description = "Serving size of the recipe", example = "2")
    Integer servingSize,
    @Schema(description = "Instructions for the recipe", example = "Preheat the oven at 200 degrees. Salt the salmon. Bake the salmon for 40 minutes.")
    String instructions,
    @Schema(description = "Dietary information for the recipe", example = "[ \"VEGETARIAN\" ]")
    List<DietaryOption> dietaryOptions,
    @Schema(description = "List of ingredients for the recipe", example = "[ { \"name\": \"Salmon\", \"quantity\": 500, \"unit\": \"gram\"}, { \"name\": \"Salt\" } ]")
    List<IngredientResponse> ingredients
) {}
