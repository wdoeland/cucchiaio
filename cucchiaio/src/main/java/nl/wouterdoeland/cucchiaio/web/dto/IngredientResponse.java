package nl.wouterdoeland.cucchiaio.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record IngredientResponse(
    @Schema(description = "Name of the ingredient", example = "Salmon")
    String name,
    @Schema(description = "(optional) Quantity of the ingredient", example = "500")
    BigDecimal quantity,
    @Schema(description = "(optional) Unit of the ingredient", example = "gram")
    String unit
) {}
