package nl.wouterdoeland.cucchiaio.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;

/**
 * An ingredient for a recipe. Contains the name, (optional) amount, (optional) unit.
 * Some example ingredients:
 * 500 gram of pasta,
 * 2 potatoes,
 * 1 clove of garlic,
 * salt
 */
@Entity
@Table(name = "recipe_ingredient")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeIngredient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @Getter
    private Long id;

    /**
     * Reference to the recipe.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "recipe_id", nullable = false)
    @Getter @Setter
    private Recipe recipe;

    /**
     * Name of the ingredient
     */
    @Size(max = 255)
    @NotNull
    @Column(name = "name", nullable = false)
    @Getter @Setter
    private String name;

    /**
     * (optional) Quantity of the ingredient
     */
    @Column(name = "quantity", precision = 10, scale = 2)
    @Getter @Setter
    private BigDecimal quantity;

    /**
     * (optional) Unit of the ingredient
     */
    @Size(max = 50)
    @Column(name = "unit", length = 50)
    @Getter @Setter
    private String unit;

    public RecipeIngredient(String name, BigDecimal quantity, String unit) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
    }
}
