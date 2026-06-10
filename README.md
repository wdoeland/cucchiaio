# Cucchiaio

Cucchiaio is an app for managing recipes. It is developed as part of an assessment at ABN Amro bank. I have used Claude to help me with explaining .NET concepts in Spring Boot terms, with generating framework boilerplate and SQL queries.

## Objective

Cucchiaio is a standalone Java application whihc allows users to manage their favourite recipes. It allows adding, updating, removing and fetching recipes. Users are able to filter available recipes based on one or more of the following criteria:

1. Whether or not the dish is vegetarian
2. The number of servings
3. Specific ingredients (inclusive and exclusive)
4. Text search within instructions

## Requirements

1. REST application built in Java
2. Code must be production ready
3. REST API must be documented
4. Data must be persisted in a database
5. Unit tests must be present
6. Integration tests must be present

## Architecture

This section describes the architectural choices made for Cucchiaio.

- single controller for all recipe operations
- supports users. users can search/view other users recipes but can only update and remove their own.
    - users connect over OIDC. to keep it simple, use self-crafted JWTs now, this can be easily swapped for an actual OIDC server later.
- recipes stored in a table, using structured (for recipe IDs, owner IDs, names, servings) data and unstructured and semi-structured data (for dietary restrictions, large searchable instruction text)
- ingredients per recipe stored in a separate table containing name, quantity and unit.
- querying for recipes returns a `slice`, so for example infinite scrolling can be supported for recipe search.
- PostgreSQL for database as it is stable, fast and supports unstructured data natively and eff
- Java framework: Spring Boot, include spring security, JPA, Lombok, springdoc
- Unit tests: JUnit
- Integration tests: TODO, undecided if these should be written in Java or in a completely different language to simulate a more realistic integration scenario
- deployment: simple docker images and docker compose script

### Database
We use PostgreSQL for its reliability, speed, ease of use and feature-set. We use Spring JPA (with Hibernate) to create a repository to access the data. We use Flyway for applying migrations. We write database creation by hand so we can optimally use Postgres features for indexing and text search to create a fast application. We use Hibernate's ddl-auto in `validate` mode so we check that the database schema is the same as what is in the Hibernate POJO's. IntelliJ can create POJO's from a database fairly alright.

(note that in .NET I'd use Entity Framework which can do all this built-in in the code, but I am unsure if there's anything in Java that has the same capabilities and is as robust. If I had a bit more experience, I'd try to do define everything in the POJO to have all definitions in a single place.)

#### Recipes
We create a table for the recipes:

- `id` auto generated primary key
- `owner_id` id of the user that owns the recipe, use TEXT so it's flexible
- `title` title of the recipe
- `serving_size` nr of servings, always > 0
- `instructions` instructions of the recipe. should be searchable: https://www.postgresql.org/docs/current/textsearch-intro.html
- `dietary_options` list of dietary information for this recipe (e.g. `vegetarian`). should be filterable. can only be set to specific values, which is forced in both the application and database.

Custom indexes:

- `idx_instructions_fts` enable full text search over the instruction text

Use reverse indexes for all of these indexes so that search is fast when searching for keywords. Postgres has a feature that can normalize text into normalized keywords (`to_tsvector`) that are easily indexable.

#### Recipe Ingredients
We create a table for the ingredients in a recipe:

- `id` auto generated primary key
- `recipe_id` id of the recipe
- `name` ingredient name. forced to be lowercase for ease of filtering
- `quantity` amount as numeric with scale of 2, so decimals are possible (e.g. 0.5 clove), optionally null
- `unit` e.g. gram, ml, cup, clove, pinch, optionally null

Custom indexes:

- `idx_ingredient_recipe` maps ingredient to recipe (besides the constraint in the table itself)
- `idx_ingredient_name_lower` makes ingredient name query fast on lower case, so we can do case-insensitive matching.

----
The ingredients could also be stored as a JSON blob inside the recipe table. We chose to store the ingredients in a separate table for a few reasons:

- more structured approach
- better indexing of ingredient names
- easier searching for quantities of an ingredient at a later point
- more consistent when migrating. If new properties need to be added to ingredients, this can be migrated more consistently when in a separate table instead of a JSON blob

## API

The API uses a controller that provides the front-facing REST API. The controller talks to a service that recieves and 
returns DTO records. This service is responsible for fetching and creating data from/with the repository. The service
also checks for ownership of recipes. For this ownership the subject from the OIDC provided JWT is used.

### Endpoints

All endpoints are placed under `/api/v1/`.

Try out the API using Swagger UI: http://localhost:8080/swagger-ui/index.html

#### POST /recipe

Create a new recipe owned by the user.

#### GET /recipe/{id}

Get a recipe for a specific ID

#### GET /recipe

Search over recipes

#### PUT /recipe/{id}

Update a recipe that the user owns.

#### DELETE /recipe/{id}

Delete a recipe that the user owns.

## Additional features

Some additional features that might be interesting to implement:

- private recipes (visible only to the owner of the recipe)
- add more dietary restrictions, e.g. vegan, lactose-free, pescetarian, etc.
- allow users to save other users recipes in their favourites
- add recipe tags
- add cooking times
- automatic tests and builds in CI/CD
- application health checks
- ...


# Running the app

## Testing
Run the app with some default credentials as follows:

Start a database:
`./rundb.sh`

Start the app:
`...`

### Authentication

There is a 'default' JWT set up for testing, use JWT.io to create tokens for it using the key:
`zm127rg6rKJuHuWYQVrJs2IBgbyDzm9b6LF0yQ004bY`

Or use a testing JWT:

User 'Wouter':
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0IiwibmFtZSI6IldvdXRlciIsImlhdCI6MTUxNjIzMDAwMH0.OYcv9RhAM1GCqUK17v56qFL8OayISP6LtC36mYYsIps
```

User 'Alice':
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjEzMTMxMzEzIiwibmFtZSI6IkFsaWNlIiwiaWF0IjoxNTE2MjMwMDAwfQ.AcGuNZQ9MVDk1nlLDg8Llsmgh8zhqge-qm8q9y1nRXI
```

### Create a recipe:

```
POST /api/v1/recipe
{
  "title": "Vegan Gulasch",
  "servingSize": 5,
  "instructions": "cook the veggie meat. add the veggies. Enjoy!",
  "dietaryOptions": [
    "VEGETARIAN"
  ],
  "ingredients": [
    {
      "name": "veggie meat",
      "quantity": 500,
      "unit": "gr"
    },
    {
      "name": "veggies",
      "quantity": 1,
      "unit": "kg"
    }
  ]
}
```

And another recipe:
```
POST /api/v1/recipe
{
  "title": "Oven Salmon",
  "servingSize": 2,
  "instructions": "pre-heat the oven to 200 degrees. season the salmon with salt. cook the salmon in the oven for 40 minutes.",
  "dietaryOptions": [
  ],
  "ingredients": [
    {
      "name": "salmon",
      "quantity": 200,
      "unit": "gr"
    },
    {
      "name": "salt"
    }
  ]
}
```

### Retrieve a recipe

`GET /api/v1/recipe/v1/{id}`

### Delete a recipe

`DELETE /api/v1/recipe/{id}` only works if you are the owner of the recipe.

## Production

1. Set up a postgresql database
2. Configure a keystore with database credentials and oauth credentials and configure this in `application.yml`
3. Deploy the app behind the firewall
4. Proxy to the internet
