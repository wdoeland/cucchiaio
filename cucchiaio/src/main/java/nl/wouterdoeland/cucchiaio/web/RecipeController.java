package nl.wouterdoeland.cucchiaio.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import nl.wouterdoeland.cucchiaio.service.RecipeService;
import nl.wouterdoeland.cucchiaio.web.dto.CreateRecipeRequest;
import nl.wouterdoeland.cucchiaio.web.dto.RecipeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

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
    @ApiResponse(responseCode = "400", description = "Malformed request")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
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
    @ApiResponse(responseCode = "404", description = "Not found")
    @GetMapping("/{id}")
    public RecipeResponse getById(@PathVariable Long id,
                                  @AuthenticationPrincipal Jwt jwt) {
        return service.get(id, userId(jwt));
    }

    @Operation(summary = "Delete a recipe by id", description = "Removes a recipe if it is found and if the user owns this recipe")
    @ApiResponse(responseCode = "204", description = "Removed")
    @ApiResponse(responseCode = "404", description = "Not found")
    @ApiResponse(responseCode = "403", description = "Not owned")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
                       @AuthenticationPrincipal Jwt jwt) {
        service.delete(id, userId(jwt));
    }
}
