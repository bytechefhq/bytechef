#!/bin/bash

docker rm -f postgres

docker run --name postgres \
  -e POSTGRES_DB=bytechef \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -d postgres:14
