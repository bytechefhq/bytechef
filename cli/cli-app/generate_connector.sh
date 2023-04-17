#!/bin/sh

SCRIPTPATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

echo "Generate $1 OpenAPI Connector"
$SCRIPTPATH/bytechef.sh component init --open-api-path=/Volumes/data/bytechef/bytechef/server/libs/modules/components/$1/openapi.yaml -o /Volumes/data/bytechef/bytechef/server/libs/modules/components $1
