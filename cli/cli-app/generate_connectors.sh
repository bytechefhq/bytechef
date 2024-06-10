#!/bin/sh

SCRIPTPATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

COMPONENTSFOLDER=$SCRIPTPATH/../../server/libs/modules/components

for COMPONENTPATH in "$COMPONENTSFOLDER"/*
do
    if test -f "$COMPONENTPATH/openapi.yaml"; then
        COMPONENTNAME=$(basename $COMPONENTPATH)

        echo "Generate $COMPONENTNAME OpenAPI Connector"
        $SCRIPTPATH/bytechef.sh component init --open-api-path $COMPONENTSFOLDER/$COMPONENTNAME/openapi.yaml -o $COMPONENTSFOLDER -n $COMPONENTNAME
    fi
done
