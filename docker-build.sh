#!/bin/sh

usage() {
    echo "Required argument misses. Please provide at least one docker image tag."
    echo ""
    echo "USAGE"
    echo "    docker-build.sh [--no-push] [--registry-url url] tag1 [tag2 tag3 ...]"
    echo "DESCRIPTION"
    echo "    --no-push\t\t- optional flag to build the images without pushing them to the registry."
    echo "    --registry-url url\t- optional flag to push image to registry other than dockerhub.io If AWS ECR URL script would attempt AWS login."
    echo "    tag\t\t- arbitrary docker image tag(s). In bytechef we use yyyyMMdd to reflect date of image build."
}

dckr_img_registry_bytechef_server="bytechef/bytechef-server"
dckr_img_registry_bytechef="bytechef/bytechef"

push_images=true
tags=""

while [ $# -gt 0 ]; do
    case "$1" in
        --no-push)
            push_images=false

            shift
        ;;
        --registry-url)
            if [ -z "$2" ]; then
                echo "Registry URL is required when using the --registry-url flag."

                exit 1
            fi

            echo "Logging in to AWS ECR with URL: $2"
            aws ecr get-login-password --region eu-west-1 | docker login --username AWS --password-stdin "$2"

            dckr_img_registry_bytechef="$2/bc-prod-app"

            shift 2
        ;;
        *)
            tags="$tags $1"

            shift
        ;;
    esac
done

if [ -z "$tags" ]; then
    usage

    exit 1
fi

echo "Validating Node.js version required for client build..."
node client/scripts/check-node-version.mjs || exit 1

cd server/apps/server-app
../../../gradlew clean build -Pprod

for tag in $tags; do
    echo "Building docker image with tag \`$tag\`"
    docker build --progress=plain --platform linux/amd64 --no-cache -t $dckr_img_registry_bytechef_server:$tag .
done

cd ../../../client

rm -rf node_modules

npm install

npm run build

cd ..

for tag in $tags; do
    echo "Building docker image with tag \`$tag\`"
    docker build --progress=plain --platform linux/amd64 --no-cache -t $dckr_img_registry_bytechef:$tag .
done

if [ "$push_images" = "false" ]; then
    echo "Skipping push to the remote docker registry"

    exit 0
fi

echo "Push images to the remote docker registry \`$dckr_img_registry_bytechef\`"

for tag in $tags; do
    dckr_push_argument="$dckr_img_registry_bytechef_server:$tag"

    echo "Pushing image \`$dckr_push_argument\`"

    docker push $dckr_push_argument

    dckr_push_argument="$dckr_img_registry_bytechef:$tag"

    echo "Pushing image \`$dckr_push_argument\`"

    docker push $dckr_push_argument
done
