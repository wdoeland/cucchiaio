#!/usr/bin/env bash

# simple script to set up a database for testing the application, will be replaced with docker compose setup

echo "Starting Postgres cucchiaio on localhost:5432"
docker run \
        -d \
        --name postgres_cucchiaio \
        --net=host \
        --rm \
        -e POSTGRES_PASSWORD=cucchiaio \
        -e POSTGRES_USER=cucchiaio \
        postgres:18 -p 5432
