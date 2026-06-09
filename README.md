# Cucchiaio

Cucchiaio is an app for managing recipes. It is developed as part of an assessment at ABN Amro bank.

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

- main goal: keep the application as simple as possible
- single controller for all recipe operations
- supports users. users can search/view other users recipes but can only update and remove their own.
    - users connect over OIDC. to keep it simple, use self-crafted JWTs now, this can be easily swapped for an actual OIDC server later.
- data stored in a table, using structured (for recipe IDs, owner IDs, names) data and unstructured data (for dietary restrictions, ingredients, large searchable instruction text)
- PostgreSQL for database as it is stable, fast and supports unstructured data natively and efficiently
- Java framework: TODO, probably Spring or similar.
- Unit tests: TODO, probably JUnit
- Integration tests: TODO, undecided if these should be written in Java or in a completely different language to simulate a more realistic integration scenario
- deployment: simple docker images and docker compose script

## Additional features

Some additional features that might be interesting to implement:

- private recipes (visible only to the owner of the recipe)
- add more dietary restrictions, e.g. vegan, lactose-free, pescetarian, etc.
- allow users to save other users recipes in their favourites
- add recipe tags
- add cooking times
- automatic tests and builds in CI/CD
- ...
