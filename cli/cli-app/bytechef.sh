#!/bin/sh

SCRIPTPATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

$SCRIPTPATH/../../gradlew -p $SCRIPTPATH run --args="$* --internal-component true"
