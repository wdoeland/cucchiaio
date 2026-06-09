package nl.wouterdoeland.cucchiaio.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.*;

/**
 * A recipe belonging to an owner, containing a title, serving size, instructions and dietary restrictions. Ingredients are stored in the RecipeIngredient table.
 */
@Entity
@Table(name = "recipe")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recipe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @Getter
    private Long id;

    /**
     * Owner of the Recipe
     */
    @NotNull
    @Column(name = "owner_id", nullable = false, length = Integer.MAX_VALUE)
    @Getter @Setter
    private String ownerId;

    /**
     * Title of the recipe
     */
    @NotNull
    @Column(name = "title", nullable = false, length = Integer.MAX_VALUE)
    @Getter @Setter
    private String title;

    /**
     * The serving size of the recipe. One recipe serves `serving_size` people.
     */
    @NotNull
    @Column(name = "serving_size", nullable = false)
    @Getter @Setter
    private Integer servingSize;

    /**
     * Instruction text for the recipe.
     */
    @NotNull
    @Column(name = "instructions", nullable = false, length = Integer.MAX_VALUE)
    @Getter @Setter
    private String instructions;

    /**
     * Dietary options for this recipe.
     * Restricted to values from the `DietaryOption` enum, which is also forced in the database.
     */
    @NotNull
    @ColumnDefault("'{}'")
    @Column(name = "dietary_options", nullable = false)
    private List<String> dietaryOptions;

    /**
     * Ingredients for this recipe.
     * Ingredients are stored in the `RecipeIngredient` table based on the `recipe id`.
     * Ingredients can be added and removed with the `addIngredient` and `removeIngredient` methods.
     */
    @OneToMany(
            mappedBy = "recipe", // relation is owned by Ingredient
            cascade = CascadeType.ALL, // cascade all changes in recipe automatically to ingredients through JPA
            orphanRemoval = true // remove orphaned ingredients
    )
    @Getter
    private Set<RecipeIngredient> recipeIngredients = new LinkedHashSet<>();

    // TODO: Add timestamp

    public Recipe(String ownerId, String title, Integer servingSize, String instructions, List<DietaryOption> dietaryOptions) {
        this.ownerId = ownerId;
        this.title = title;
        this.servingSize = servingSize;
        this.instructions = instructions;
        setDietaryOptions(dietaryOptions);
    }

    /**
     * Set dietary options for this recipe
     * @param dietaryOptions list of dietary options to set for this recipe
     */
    public void setDietaryOptions(List<DietaryOption> dietaryOptions) {
        this.dietaryOptions = dietaryOptions == null ? new ArrayList<>()
                : dietaryOptions.stream()
                .map(DietaryOption::name)
                .toList();
    }

    /**
     * Get dietary options for this recipe.
     * @return Dietary options that are set for this recipe.
     */
    public List<DietaryOption> getDietaryOptions() {
        // TODO: handle exception on unknown value
        return dietaryOptions
                .stream()
                .map(DietaryOption::valueOf)
                .toList();
    }

    /**
     * Add an ingredient to the recipe.
     * @param ingredient ingredient to add to the recipe
     */
    public void addIngredient(RecipeIngredient ingredient) {
        recipeIngredients.add(ingredient);
        ingredient.setRecipe(this);
    }

    /**
     * Remove an ingredient from the recipe
     * @param ingredient ingredient to remove from the recipe
     */
    public void removeIngredient(RecipeIngredient ingredient) {
        recipeIngredients.remove(ingredient);
        ingredient.setRecipe(null);
    }
}
