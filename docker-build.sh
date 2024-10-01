#!/bin/sh

if [ -z $1 ]; then
    echo "Required argument misses. Please provide docker image tag."
    echo ""
    echo "USAGE"
    echo "    docker-build.sh tag [latest]"
    echo "DESCRIPTION"
    echo "    tag\t\t- arbitrary docker image tag. In bytechef we use yyyyMMdd to reflect date of image build"
    echo "    latest\t- optional flag that would instruct script to tag image with additional tag with value \`latest\`"

    exit 1
fi

cd server/apps/server-app
../../../gradlew clean build -Pprod

if [ -n "$2" ] && [ "latest" = "$2" ]; then
    echo "Building docker image with tag \`$2\`"

    docker build --platform linux/amd64 -t bytechef/bytechef-server:$2 .
fi

docker build --platform linux/amd64 -t bytechef/bytechef-server:$1 .

cd ../../../client

npm install

npm run build

cd ..

if [ -n "$2" ] && [ "latest" = "$2" ]; then
    echo "Building docker image with tag \`$2\`"

    docker build --platform linux/amd64 -t bytechef/bytechef:$2 .
fi

docker build --platform linux/amd64 -t bytechef/bytechef:$1 .
