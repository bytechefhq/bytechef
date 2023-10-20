#!/bin/sh

SCRIPTPATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

echo "Generate Airtable OpenAPI Connector"
$SCRIPTPATH/bytechef.sh component init --open-api-path=/Volumes/data/bytechef/bytechef/server/libs/modules/components/airtable/openapi.yaml -o /Volumes/data/bytechef/bytechef/server/libs/modules/components airtable


echo "Generate Jira OpenAPI Connector"
$SCRIPTPATH/bytechef.sh component init --open-api-path=/Volumes/data/bytechef/bytechef/server/libs/modules/components/jira/openapi.yaml -o /Volumes/data/bytechef/bytechef/server/libs/modules/components jira

echo "Generate MailChimp OpenAPI Connector"
$SCRIPTPATH/bytechef.sh component init --open-api-path=/Volumes/data/bytechef/bytechef/server/libs/modules/components/mailchimp/openapi.yaml -o /Volumes/data/bytechef/bytechef/server/libs/modules/components mailchimp

echo "Generate PetStore OpenAPI Connector"
$SCRIPTPATH/bytechef.sh component init --open-api-path=/Volumes/data/bytechef/bytechef/server/libs/modules/components/petstore/openapi.yaml -o /Volumes/data/bytechef/bytechef/server/libs/modules/components petstore

echo "Generate Pipedrive OpenAPI Connector"
$SCRIPTPATH/bytechef.sh component init --open-api-path=/Volumes/data/bytechef/bytechef/server/libs/modules/components/pipedrive/openapi.yaml -o /Volumes/data/bytechef/bytechef/server/libs/modules/components pipedrive

#echo "Generate Shopify OpenAPI Connector"
#$SCRIPTPATH/bytechef.sh component init --open-api-path=/Volumes/data/bytechef/bytechef/server/libs/modules/components/shopify/openapi.yaml -o /Volumes/data/bytechef/bytechef/server/libs/modules/components shopify
