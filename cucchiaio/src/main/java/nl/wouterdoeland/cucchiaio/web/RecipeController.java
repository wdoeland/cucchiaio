package nl.wouterdoeland.cucchiaio.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import nl.wouterdoeland.cucchiaio.service.RecipeService;
import nl.wouterdoeland.cucchiaio.web.dto.CreateRecipeRequest;
import nl.wouterdoeland.cucchiaio.web.dto.RecipeResponse;
import nl.wouterdoeland.cucchiaio.web.dto.RecipeSearchCriteria;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/recipe")
public class RecipeController {
    private final RecipeService service;

    public RecipeController(RecipeService service) {
        this.service = service;
    }

    /**
     * The callers user ID from OIDC JWT
     * @param jwt user JWT
     * @return user ID from JWT
     */
    private static String userId(Jwt jwt) {
        return jwt.getSubject();
    }

    @Operation(summary = "Create a new recipe")
    @ApiResponse(responseCode = "201", description = "Created new recipe")
    @ApiResponse(responseCode = "400", description = "Malformed request", content = @Content())
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResponseEntity<RecipeResponse> create(
            @Valid @RequestBody CreateRecipeRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        RecipeResponse recipe = service.create(request, userId(jwt));
        return ResponseEntity
                .created(URI.create("/api/v1/recipe/" + recipe.id()))
                .body(recipe);
    }

    @Operation(summary = "Retrieve a recipe by id")
    @ApiResponse(responseCode = "200", description = "Found")
    @ApiResponse(responseCode = "404", description = "Not found", content = @Content())
    @GetMapping("/{id}")
    public RecipeResponse getById(@PathVariable Long id,
                                  @AuthenticationPrincipal Jwt jwt) {
        return service.get(id, userId(jwt));
    }

    @Operation(summary = "Delete a recipe by id", description = "Removes a recipe if it is found and if the user owns this recipe")
    @ApiResponse(responseCode = "204", description = "Removed")
    @ApiResponse(responseCode = "403", description = "Not owned")
    @ApiResponse(responseCode = "404", description = "Not found")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
                       @AuthenticationPrincipal Jwt jwt) {
        service.delete(id, userId(jwt));
    }

    @Operation(summary = "Update a recipe", description = "Update a recipe that the user owns.")
    @ApiResponse(responseCode = "201", description = "Created new recipe")
    @ApiResponse(responseCode = "400", description = "Malformed request", content = @Content())
    @ApiResponse(responseCode = "403", description = "Not owned")
    @ApiResponse(responseCode = "404", description = "Not found")
    @PutMapping("/{id}")
    public RecipeResponse update(@PathVariable Long id,
                                 @Valid @RequestBody CreateRecipeRequest update,
                                 @AuthenticationPrincipal Jwt jwt) {
        return service.update(id, update, userId(jwt));
    }

    @Operation(summary = "Search for recipes", description = "Search for recipes by entering criteria.")
    @ApiResponse(responseCode = "200", description = "Found")
    @ApiResponse(responseCode = "400", description = "Malformed request", content = @Content())
    @GetMapping("/search")
    public Slice<RecipeResponse> search(@Valid @ParameterObject RecipeSearchCriteria criteria,
                                        @PageableDefault(size = 20) @ParameterObject Pageable pageable,
                                        @AuthenticationPrincipal Jwt jwt) {
        return service.search(criteria, userId(jwt), pageable);
    }

    @Operation(summary = "Get all recipes owned by the current user")
    @ApiResponse(responseCode = "200", description = "Found")
    @GetMapping("/mine")
    public Slice<RecipeResponse> getMine(@PageableDefault(size = 20) @ParameterObject Pageable pageable,
                                         @AuthenticationPrincipal Jwt jwt) {
        return service.getAllByOwner(userId(jwt), pageable);
    }
}
