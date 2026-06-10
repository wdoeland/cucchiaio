package nl.wouterdoeland.cucchiaio.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import nl.wouterdoeland.cucchiaio.domain.DietaryOption;

import java.util.List;

public record RecipeSearchCriteria(
        @Schema(description = "Filter for only the current users recipes", example = "false")
        Boolean myRecipes,
        @Schema(description = "Keywords to filter the instructions on", example = "oven", defaultValue = "null")
        String instructions,
        @Schema(description = "Minimum number of servings", example = "3")
        Integer servingsLower,
        @Schema(description = "Maximum number of servings", example = "10")
        Integer servingsUpper,
        @Schema(description = "Dietary requirements", example = "[ \"VEGETARIAN\" ]", defaultValue = "null")
        DietaryOption[] dietary,
        @Schema(description = "Ingredients that should be included", example = "[ \"potatoes\" ]", defaultValue = "null")
        String[] ingredient,
        @Schema(description = "Ingredients that should not be included", example = "[ \"salmon\" ]", defaultValue = "null")
        String[] ingredientExcluded
) {}
