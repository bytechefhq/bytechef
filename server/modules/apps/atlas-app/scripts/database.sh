#!/bin/bash

docker rm -f mysql

docker run --name mysql \
  -e MYSQL_DATABASE=atlas \
  -e MYSQL_ROOT_PASSWORD=root \
  -p 3306:3306 \
  -d mysql/mysql-server:latest
