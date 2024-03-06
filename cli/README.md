# ByteChef command-line application

It is work in progress. Until the app is published, use ```bytechef.sh``` script to execute the app.

## Step-by-step guide to generate connector

1. Crete new package with name of your connector in `server/apps/libs/modules/components/yourconnector`
2. Inside your new package create `openapi.yaml` file and write [OpenAPI specification](https://swagger.io/specification/) for your connector.
3. Change working directory to the `BYTECHEF_HOME/cli/cli-app` folder.
4. Generate YourConnector component/connector by executing the following command:
    ```bash
    $ ./bytechef.sh component init --open-api-path=../../server/libs/modules/components/yourconnector/openapi.yaml --output-path=../../server/libs/modules/components yourconnector
    ```
### Setup gradle
5. In file `bytechef/settings.gradle.kts`, add line: `include("server:libs:modules:components:yourconnector")`
6. In both files `bytechef/ee/server/apps/worker-app/build.gradle.kts` and `bytechef/server/apps/server-app/build.gradle.kts`, add line `implementation(project(":server:libs:modules:components:yourconnector"))`
7. Load gradle changes. After that IntelliJ should recognize your connector as a java module.

### Connector icon
8. Find and download from internet the user interface icon in .svg format for your connector and put it in `server/libs/modules/components/yourconnector/src/main/resources/assets/yourconnector.svg`
9. In `YourConnectorComponentHandler.class` override method `modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition)` with 
    ```
    return modifiableComponentDefinition
    .customAction(true)
    .icon("path:assets/yourconnector.svg");
    ```
### Dynamic properties
TODO
