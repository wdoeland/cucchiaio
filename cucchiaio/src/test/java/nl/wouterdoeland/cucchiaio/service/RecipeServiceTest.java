package nl.wouterdoeland.cucchiaio.service;

import nl.wouterdoeland.cucchiaio.domain.Recipe;
import nl.wouterdoeland.cucchiaio.repository.RecipeRepository;
import nl.wouterdoeland.cucchiaio.web.dto.RecipeResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit test scoped to just the recipe service.
 */
@ExtendWith(MockitoExtension.class)
public class RecipeServiceTest {
    @Mock
    private RecipeRepository repository;

    @InjectMocks
    private RecipeService service;

    private static final String ALICE = "alice";
    private static final String BOB = "bob";

    @Test
    void get_returnsRecipe_andSetsCanEdit() {
        Recipe recipe = new Recipe(ALICE, "Oven Salmon", 2, "bake the salmon in the oven", null);
        when(repository.findById(1L)).thenReturn(Optional.of(recipe));

        RecipeResponse result = service.get(1L, ALICE);
        assertThat(result.title()).isEqualTo("Oven Salmon");
        assertThat(result.canEdit()).isTrue();

        result = service.get(1L, BOB);
        assertThat(result.title()).isEqualTo("Oven Salmon");
        assertThat(result.canEdit()).isFalse();
    }

    @Test
    void get_returnsNotFound() {
        when(repository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(2L, ALICE))
                .isInstanceOf(RecipeNotFoundException.class);
    }

    @Test
    void delete_allowsOnlyOwners() {
        Recipe recipe = new Recipe(ALICE, "Oven Salmon", 2, "bake the salmon in the oven", null);
        when(repository.findById(1L)).thenReturn(Optional.of(recipe));

        // check that BOB cannot delete ALICEs recipe
        assertThatThrownBy(() -> service.delete(1L, BOB))
                .isInstanceOf(RecipeForbiddenException.class);

        // check that ALICE can delete her recipe
        service.delete(1L, ALICE);
    }
}
