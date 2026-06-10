package nl.wouterdoeland.cucchiaio.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record IngredientRequest (
        @Schema(description = "Name of the ingredient", example = "Salmon")
        @NotBlank @Size(max = 50) String name,
        @Schema(description = "(optional) Quantity of the ingredient", example = "500")
        @Positive BigDecimal quantity,
        @Schema(description = "(optional) Unit of the ingredient", example = "gram")
        @Size(max = 50) String unit
) {}
