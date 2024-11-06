#!/bin/sh

SCRIPTPATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

echo "Generate $1 OpenAPI Connector"
$SCRIPTPATH/bytechef.sh component init --open-api-path $SCRIPTPATH/../../server/libs/modules/components/pipedrive/openapi.yaml -o $SCRIPTPATH/../../server/libs/modules/components -n pipedrive
