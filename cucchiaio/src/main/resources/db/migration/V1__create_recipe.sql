-- recipe table
CREATE TABLE recipe (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id TEXT NOT NULL,
    title TEXT NOT NULL,
    serving_size INT NOT NULL CHECK (serving_size > 0),
    instructions TEXT NOT NULL,
    dietary_options TEXT[] NOT NULL DEFAULT '{}',
    CONSTRAINT chk_dietary_options_valid CHECK ( -- check that dietary options contains only allowed values
        dietary_options <@ ARRAY['VEGETARIAN']::text[]
    )
);
-- index for fast search on recipes. normalizes keywords and creates a reversed index.
CREATE INDEX idx_recipe_instructions_fts
    ON recipe USING GIN(to_tsvector('english', instructions));
-- index for fast search in dietary options
CREATE INDEX idx_recipe_dietary_options ON recipe USING GIN(dietary_options);

-- recipe ingredients table
CREATE TABLE recipe_ingredient (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recipe_id BIGINT NOT NULL REFERENCES recipe(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    quantity NUMERIC(10, 2),
    unit VARCHAR(50)
);
-- index for finding ingredients for a recipe
CREATE INDEX idx_ingredient_recipe ON recipe_ingredient(recipe_id);
-- index for querying ingredient names
CREATE INDEX idx_ingredient_name ON recipe_ingredient(LOWER(name));