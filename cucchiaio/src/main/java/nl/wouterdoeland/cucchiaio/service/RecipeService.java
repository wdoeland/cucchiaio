package nl.wouterdoeland.cucchiaio.service;

import nl.wouterdoeland.cucchiaio.domain.DietaryOption;
import nl.wouterdoeland.cucchiaio.domain.Recipe;
import nl.wouterdoeland.cucchiaio.domain.RecipeIngredient;
import nl.wouterdoeland.cucchiaio.repository.RecipeRepository;
import nl.wouterdoeland.cucchiaio.web.dto.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class RecipeService {
    private final RecipeRepository repository;

    public RecipeService(RecipeRepository repository) {
        this.repository = repository;
    }

    /**
     * Create a new recipe owned by the user
     * @param request The recipe
     * @param ownerId The ID of the owner
     * @return The recipe
     */
    public RecipeResponse create(CreateRecipeRequest request, String ownerId) {
        Recipe recipe = new Recipe(ownerId, request.title(), request.servingSize(), request.instructions(), request.dietaryOptions());
        addIngredients(recipe, request.ingredients());
        return toResponse(repository.save(recipe), ownerId);
    }

    /**
     * Deletes a recipe. The user needs to own the recipe.
     * @param id ID of the recipe
     * @param userId ID of the user
     * @throws RecipeNotFoundException if the recipe was not found
     * @throws RecipeForbiddenException if the user does not have delete rights to this recipe
     */
    public void delete(Long id, String userId) {
        // permanently delete recipe
        repository.delete(getPrivilegedRecipe(id, userId));
    }

    /**
     * Retrieve a recipe by ID.
     * @param id Recipe ID
     * @param userId ID of the requestee, to determine the 'canEdit' variable
     * @return a recipe if found
     * @throws RecipeNotFoundException if recipe was not found
     */
    @Transactional(readOnly = true)
    public RecipeResponse get(long id, String userId) {
        return toResponse(getRecipe(id), userId);
    }

    /**
     * Get all recipes from a single user
     * @param userId The owner of the recipes
     * @param pageable
     * @return Slice over the recipes owned by this user
     */
    @Transactional(readOnly = true)
    public Slice<RecipeResponse> getAllByOwner(String userId, Pageable pageable) {
        return repository.findAllByOwnerId(userId, pageable).map(r -> toResponse(r, userId));
    }

    /**
     * Search for recipes
     * @param criteria Criteria to match recipes against
     * @param userId Id of the user
     * @param pageable
     * @return A slice of the recipes found
     */
    @Transactional(readOnly = true)
    public Slice<RecipeResponse> search(RecipeSearchCriteria criteria,
                                        String userId,
                                        Pageable pageable) {
        return repository.search(
                criteria.myRecipes() == null || !criteria.myRecipes() ? null : userId,
                criteria.instructions(),
                criteria.servingsLower(),
                criteria.servingsUpper(),
                toStringArrayOrNull(criteria.dietary()),
                criteria.ingredient(),
                criteria.ingredientExcluded(),
                pageable
        ).map(r -> toResponse(r, userId));
    }

    /**
     * Convert an array of dietary options into an array of strings, based on the enum name. Returns null if input is null or empty.
     * @param dietaryOptions Array of dietary options
     * @return Array of dietary options as strings or null
     */
    private String[] toStringArrayOrNull(DietaryOption[] dietaryOptions) {
        if (dietaryOptions == null || dietaryOptions.length == 0) {
            return null;
        }
        return Arrays.stream(dietaryOptions).map(Enum::toString).toArray(String[]::new);
    }

    /**
     * Add ingredients to a recipe
     * @param recipe recipe to add ingredients to
     * @param ingredients ingredients to add to the recipe
     */
    private void addIngredients(Recipe recipe, List<IngredientRequest> ingredients) {
        if (ingredients == null) {
            return;
        }

        for (IngredientRequest i : ingredients) {
            recipe.addIngredient(new RecipeIngredient(i.name(), i.quantity(), i.unit()));
        }
    }

    /**
     * Retrieve a recipe by id or throw a RecipeNotFoundException
     * @param id Id of the recipe to find
     * @return the recipe if found
     * @throws RecipeNotFoundException if the recipe was not found
     */
    private Recipe getRecipe(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));
    }

    /**
     * Retrieve a recipe for privileged access, i.e. deletion or modification. Checks if the recipe is owned by the given owner.
     * @param id Id of the recipe
     * @param ownerId Id of the owner who should own this recipe
     * @return Recipe if found
     * @throws RecipeNotFoundException if the recipe was not found
     * @throws RecipeForbiddenException if the owner does not have privileged access to this recipe
     */
    private Recipe getPrivilegedRecipe(Long id, String ownerId) {
        Recipe recipe = getRecipe(id);
        // check ownership of recipe
        if (!recipe.getOwnerId().equals(ownerId)) {
            throw new RecipeForbiddenException(id);
        }

        return recipe;
    }

    /**
     * Convert a Recipe to a RecipeResponse
     * @param recipe the recipe to convert
     * @param userId the ID of the user to respond to. Is used to set the 'canEdit' variable
     * @return RecipeResponse object mapping to the Recipe
     */
    private RecipeResponse toResponse(Recipe recipe, String userId) {
        List<IngredientResponse> ingredients = recipe.getRecipeIngredients().stream()
                .map(i -> new IngredientResponse(i.getId(), i.getName(), i.getQuantity(), i.getUnit()))
                .toList();
        return new RecipeResponse(
                recipe.getId(),
                recipe.getOwnerId().equals(userId),
                recipe.getTitle(),
                recipe.getServingSize(),
                recipe.getInstructions(),
                recipe.getDietaryOptions(),
                ingredients);
    }
}
