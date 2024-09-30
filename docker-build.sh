#!/bin/sh

cd server/apps/server-app
../../../gradlew clean build -Pprod
docker build -t bytechef-server .
cd ../../../
docker build -t bytechef .
