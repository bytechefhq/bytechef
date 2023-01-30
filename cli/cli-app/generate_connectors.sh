#!/bin/sh

SCRIPTPATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

$SCRIPTPATH/bytechef.sh component init --open-api-path=/Volumes/data/bytechef/bytechef/server/libs/modules/components/jira/openapi.yaml -o /Volumes/data/bytechef/bytechef/server/libs/modules/components jira
$SCRIPTPATH/bytechef.sh component init --open-api-path=/Volumes/data/bytechef/bytechef/server/libs/modules/components/petstore/openapi.yaml -o /Volumes/data/bytechef/bytechef/server/libs/modules/components petstore
$SCRIPTPATH/bytechef.sh component init --open-api-path=/Volumes/data/bytechef/bytechef/server/libs/modules/components/pipedrive/openapi.yaml -o /Volumes/data/bytechef/bytechef/server/libs/modules/components pipedrive
$SCRIPTPATH/bytechef.sh component init --open-api-path=/Volumes/data/bytechef/bytechef/server/libs/modules/components/shopify/openapi.yaml -o /Volumes/data/bytechef/bytechef/server/libs/modules/components shopify
