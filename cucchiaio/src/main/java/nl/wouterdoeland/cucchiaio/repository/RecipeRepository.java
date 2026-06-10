package nl.wouterdoeland.cucchiaio.repository;

import nl.wouterdoeland.cucchiaio.domain.Recipe;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for operations on Recipes. CRUD and Search operations supported. Use `findByIdAndOwnerId` and `findAllByOwnerId` for CRUD operations to recipes since this checks that the owner is correct.
 */
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    Optional<Recipe> findByIdAndOwnerId(Long id, String ownerId);

    Slice<Recipe> findAllByOwnerId(String ownerId, Pageable pageable);

    /**
     * Query recipes. All parameters are _optional_. This means that if a paremeter is left out, it will not be filtered for. Query parameters are joined, so all need to match.
     * @param ownerId Exact match on the owner id
     * @param instructionsQuery Fuzzy text search on the instructions
     * @param servingsLower Inclusive minimum number of servings
     * @param servingsUpper Inclusive upper number of servings
     * @param dietaryIncluded Dietary wishes that must be included
     * @param ingredientIncluded Ingredients that must be included
     * @param ingredientExcluded Ingredients that must not be included
     * @return A list of recipes that match, sorted by the recipe ID.
     */
    /*
    Query written by hand, can be ported to specific query in JPA.

    All query parameters are nullable and thus the query follows the structure of:
        SELECT everything FROM recipe
        WHERE (check if query_parameter_0 is null OR compare to query_parameter_0)
            AND (...)
            AND (check if query_parameter_N is null OR compare to query_parameter_N)
        ORDER BY id

    TODO: Sort by text search relevance
     */
    @Query(value = """
            SELECT r.* FROM recipe r
            WHERE (:ownerId IS NULL /* exact match on owner id */
                        OR r.owner_id = :ownerId)
                AND (:instructions IS NULL  /* text query over the instruction text, uses the index to make it fast and
                                             * converts text and query into a format more suited for full text search
                                             * (see: https://www.postgresql.org/docs/current/textsearch-intro.html) */
                            OR to_tsvector('english', r.instructions)
                                        @@ plainto_tsquery('english', :instructions))
                AND (:servings_lower IS NULL /* inclusive lower bound on servings */
                            OR r.serving_size >= :servings_lower)
                AND (:servings_upper IS NULL /* inclusive upper bound on servings */
                            OR r.serving_size <= :servings_upper)
                AND (CAST(:dietary as TEXT[]) is NULL   /* exact text match for all included dietary options, cast to
                                                         * TEXT[] so PSQL knows its TEXT[]. no LOWER is needed since
                                                         * the enum controls the input and we always achieve an exact
                                                         * match */
                            OR r.dietary_options @> CAST(:dietary AS TEXT[]))
                AND (CAST(:ingredient AS text[]) IS NULL/* match ALL queried ingredients against ingredient list,
                                                         * set to LOWER case so we do case insensitive matching.
                                                         * the ingredient names are indexed to lower case so matching 
                                                         * is fast */
                                /* map ingredient to recipe; 
                                 * select every ingredient whose name in lowercase matches a queried ingredient in lowercase; 
                                 * count the distinct items in that list */
                            OR (SELECT COUNT(DISTINCT LOWER(ri.name))
                                FROM recipe_ingredient ri
                                WHERE ri.recipe_id = r.id
                                AND LOWER(ri.name) = ANY (
                                   SELECT LOWER(x) FROM unnest(CAST(:ingredient AS text[])) x))
                                /* convert input array to rows and convert to lowercase; 
                                 * count the distinct items in that list; 
                                 * check if the size is equal to that of the found ingredients so we know ALL queried ingredients match */
                                = (SELECT COUNT(DISTINCT LOWER(x))
                                    FROM unnest(CAST(:ingredient AS text[])) x))
                AND (CAST(:ingredient_excluded AS text[]) IS NULL /* match zero queried excluded ingredients against ingredient list using similar tricks as in the query above */
                            OR NOT EXISTS (
                                SELECT 1 FROM recipe_ingredient ri
                                WHERE ri.recipe_id = r.id
                                    AND LOWER(ri.name) = ANY (
                                        SELECT LOWER(x) FROM unnest(CAST(:ingredient_excluded AS text[])) x)))
            ORDER BY r.id
            """, nativeQuery = true)
    Slice<Recipe> search(
            @Param("ownerId") String ownerId,
            @Param("instructions") String instructionsQuery,
            @Param("servings_lower") Integer servingsLower,
            @Param("servings_upper") Integer servingsUpper,
            @Param("dietary") String[] dietaryIncluded,
            @Param("ingredient") String[] ingredientIncluded,
            @Param("ingredient_excluded") String[] ingredientExcluded,
            Pageable pageable
    );
}
