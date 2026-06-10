package nl.wouterdoeland.cucchiaio.service;

import nl.wouterdoeland.cucchiaio.domain.Recipe;
import nl.wouterdoeland.cucchiaio.domain.RecipeIngredient;
import nl.wouterdoeland.cucchiaio.repository.RecipeRepository;
import nl.wouterdoeland.cucchiaio.web.dto.CreateRecipeRequest;
import nl.wouterdoeland.cucchiaio.web.dto.IngredientRequest;
import nl.wouterdoeland.cucchiaio.web.dto.IngredientResponse;
import nl.wouterdoeland.cucchiaio.web.dto.RecipeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
