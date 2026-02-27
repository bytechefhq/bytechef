#!/bin/sh

SCRIPTPATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

COMPONENT_NAME=$1
VERSION=${2:-1}

if [ -z "$COMPONENT_NAME" ]; then
    echo "Usage: $0 <component_name> [version]"
    exit 1
fi

echo "Generate $COMPONENT_NAME OpenAPI Connector version $VERSION"
"$SCRIPTPATH/bytechef.sh" component init --open-api-path "$SCRIPTPATH/../../server/libs/modules/components/$COMPONENT_NAME/openapi.yaml" --output-path "$SCRIPTPATH/../../server/libs/modules/components" --name "$COMPONENT_NAME" --version "$VERSION"
