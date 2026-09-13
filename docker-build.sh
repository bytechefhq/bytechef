#!/bin/sh

usage() {
    echo "Required argument misses. Please provide at least one docker image tag."
    echo ""
    echo "USAGE"
    echo "    docker-build.sh [--no-push] [--runtime-job] [--registry-url url] tag1 [tag2 tag3 ...]"
    echo "DESCRIPTION"
    echo "    --no-push\t\t- optional flag to build the images without pushing them to the registry."
    echo "    --runtime-job\t- optional flag to build only the bytechef/bytechef-runtime-job image instead of"
    echo "    \t\t\t  the default bytechef/bytechef image."
    echo "    \t\t\t  Builds for the host architecture only and loads the images into the local docker image store."
    echo "    --registry-url url\t- optional flag to push image to registry other than dockerhub.io If AWS ECR URL script would attempt AWS login."
    echo "    tag\t\t- arbitrary docker image tag(s). In bytechef we use yyyyMMdd to reflect date of image build."
    echo ""
    echo "IMAGES"
    echo "    bytechef/bytechef-server\t\t- the ByteChef server without the client bundle, the base of bytechef/bytechef."
    echo "    bytechef/bytechef\t\t\t- the ByteChef server with the client bundle. Built by default."
    echo "    bytechef/bytechef-runtime-job\t- the standalone runtime job app that executes a single workflow. Built with --runtime-job."
}

root_dir=$(cd "$(dirname "$0")" && pwd)

dckr_img_registry_bytechef_server="bytechef/bytechef-server"
dckr_img_registry_bytechef="bytechef/bytechef"
dckr_img_registry_bytechef_runtime_job="bytechef/bytechef-runtime-job"

build_runtime_job=false
push_images=true
tags=""

while [ $# -gt 0 ]; do
    case "$1" in
        --no-push)
            push_images=false

            shift
        ;;
        --runtime-job)
            build_runtime_job=true

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

builder_name="bytechef-multiarch"
target_platforms="linux/amd64 linux/arm64"

builder_supports_target_platforms() {
    builder_info=$(docker buildx inspect "$builder_name" 2>/dev/null) || return 1

    for target_platform in $target_platforms; do
        case "$builder_info" in
            *"$target_platform"*)
            ;;
            *)
                return 1
            ;;
        esac
    done

    return 0
}

register_emulators() {
    echo "Registering QEMU emulators for \`$target_platforms\`"

    docker run --privileged --rm tonistiigi/binfmt --install arm64,amd64 >/dev/null || return 1

    docker buildx stop "$builder_name" >/dev/null 2>&1

    docker buildx inspect --bootstrap "$builder_name" >/dev/null || return 1
}

if [ "$push_images" = "true" ]; then
    platforms="linux/amd64,linux/arm64"

    if ! docker buildx inspect "$builder_name" >/dev/null 2>&1; then
        echo "Creating buildx builder \`$builder_name\`"

        docker buildx create --name "$builder_name" --driver docker-container --bootstrap >/dev/null || exit 1
    fi

    builder_driver=$(docker buildx inspect "$builder_name" | sed -n 's/^Driver: *//p')

    if [ "$builder_driver" = "docker" ]; then
        echo "Builder \`$builder_name\` uses the docker driver, which writes to the local image store"
        echo "and so holds a single architecture per tag. Remove that builder, or the docker context"
        echo "of the same name, and run the build again."

        exit 1
    fi

    if ! builder_supports_target_platforms; then
        register_emulators

        if ! builder_supports_target_platforms; then
            echo "Builder \`$builder_name\` cannot build \`$platforms\`. Register the QEMU/binfmt emulators"
            echo "on the docker host, or attach native amd64 and arm64 nodes to the builder."

            exit 1
        fi
    fi

    docker_build_command="docker buildx build --builder $builder_name --platform $platforms --push"
else
    platforms="$(docker version --format '{{.Server.Os}}/{{.Server.Arch}}')"

    docker_build_command="docker build"
fi

build_runtime_job_image() {
    cd "$root_dir/server/ee/apps/runtime-job-app" || exit 1

    "$root_dir/gradlew" :server:ee:apps:runtime-job-app:clean :server:ee:apps:runtime-job-app:build -Pprod || exit 1

    for tag in $tags; do
        echo "Building docker image \`$dckr_img_registry_bytechef_runtime_job:$tag\` for platform(s) \`$platforms\`"

        $docker_build_command --progress=plain --no-cache \
            -t $dckr_img_registry_bytechef_runtime_job:$tag . || exit 1
    done
}

build_bytechef_image() {
    echo "Validating Node.js version required for client build..."
    node "$root_dir/client/scripts/check-node-version.mjs" || exit 1

    cd "$root_dir/server/apps/server-app" || exit 1

    "$root_dir/gradlew" clean build -Pprod || exit 1

    # bytechef/bytechef is layered on top of bytechef/bytechef-server, so the server image has to exist first.
    for tag in $tags; do
        echo "Building docker image \`$dckr_img_registry_bytechef_server:$tag\` for platform(s) \`$platforms\`"

        $docker_build_command --progress=plain --no-cache \
            -t $dckr_img_registry_bytechef_server:$tag . || exit 1
    done

    cd "$root_dir/client" || exit 1

    rm -rf node_modules

    npm install || exit 1

    npm run build || exit 1

    cd "$root_dir" || exit 1

    for tag in $tags; do
        echo "Building docker image \`$dckr_img_registry_bytechef:$tag\` for platform(s) \`$platforms\`"

        $docker_build_command --progress=plain --no-cache \
            --build-arg BASE_IMAGE="$dckr_img_registry_bytechef_server:$tag" \
            -t $dckr_img_registry_bytechef:$tag . || exit 1
    done
}

if [ "$build_runtime_job" = "true" ]; then
    build_runtime_job_image
else
    build_bytechef_image
fi

if [ "$push_images" = "false" ]; then
    echo "Built \`$platforms\` images into the local docker image store"
    echo "Skipping push to the remote docker registry"

    exit 0
fi

echo "Pushed images to the remote docker registry"
