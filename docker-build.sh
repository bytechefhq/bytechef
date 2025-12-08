#!/bin/sh

if [ -z "$1" ]; then
    echo "Required argument misses. Please provide docker image tag or registry url flag."
    echo ""
    echo "USAGE"
    echo "    docker-build.sh [--registry-url url] tag1 [tag2 tag3 ...]"
    echo "DESCRIPTION"
    echo "    --registry-url url\t- optional flag to push image to registry other than dockerhub.io If AWS ECR URL script would attemt AWS login."
    echo "    tag\t\t- arbitrary docker image tag(s). In bytechef we use yyyyMMdd to reflect date of image build."

    exit 1
fi

dckr_img_registry_bytechef_server="bytechef/bytechef-server"
dckr_img_registry_bytechef="bytechef/bytechef"

if [ "$1" = "--registry-url" ]; then
    if [ -z "$2" ]; then
        echo "Registry URL is required when using the --registry-url flag."

        exit 1
    fi

    echo "Logging in to AWS ECR with URL: $2"
    aws ecr get-login-password --region eu-west-1 | docker login --username AWS --password-stdin "$2"

    dckr_img_registry_bytechef="$2/bc-prod-app"

    shift 2
fi

cd server/apps/server-app
../../../gradlew clean build -Pprod

for tag in "$@"; do
    echo "Building docker image with tag \`$tag\`"
    docker build --progress=plain --platform linux/amd64 --no-cache -t $dckr_img_registry_bytechef_server:$tag .
done

cd ../../../client

npm install

npm run build

cd ..

for tag in "$@"; do
    echo "Building docker image with tag \`$tag\`"
    docker build --progress=plain --platform linux/amd64 --no-cache -t $dckr_img_registry_bytechef:$tag .
done

echo "Push images to the remote docker registry \`$dckr_img_registry_bytechef\`"

for tag in "$@"; do
    dckr_push_argument="$dckr_img_registry_bytechef_server:$tag"

    echo "Pushing image \`$dckr_push_argument\`"

    docker push $dckr_push_argument

    dckr_push_argument="$dckr_img_registry_bytechef:$tag"

    echo "Pushing image \`$dckr_push_argument\`"

    docker push $dckr_push_argument
done
