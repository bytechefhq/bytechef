#!/bin/bash

docker rm -f postgres

docker run --name postgres \
  -e POSTGRES_DB=piper \
  -e POSTGRES_USER=piper \
  -e POSTGRES_PASSWORD=piper \
  -p 5432:5432 \
  -d postgres:11
