# ByteChef command-line application

It is work in progress. Until the app is published, use ```bytechef.sh``` script to execute the app.

## Step-by-step guide to generate connector

1. Crete new package with name of your connector in `server/apps/libs/modules/components/yourconnector`
2. Inside your new package create `openapi.yaml` file and write [OpenAPI specification](https://swagger.io/specification/) for your connector.
3. Change working directory to the `BYTECHEF_HOME/cli/cli-app` folder.
4. Generate YourConnector component/connector by executing the following command:
    ```bash
    $ ./bytechef.sh component init --open-api-path ../../server/libs/modules/components/yourconnector/openapi.yaml --output-path ../../server/libs/modules/components --name yourconnector
    ```
### Setup gradle
5. In file `bytechef/settings.gradle.kts`, add line: `include("server:libs:modules:components:yourconnector")`
6. In both files `bytechef/server/ee/apps/worker-app/build.gradle.kts` and `bytechef/server/apps/server-app/build.gradle.kts`, add line `implementation(project(":server:libs:modules:components:yourconnector"))`
7. Load gradle changes. After that IntelliJ should recognize your connector as a java module.

### Connector icon
8. Find and download from internet the user interface icon in .svg format for your connector and put it in `server/libs/modules/components/yourconnector/src/main/resources/assets/yourconnector.svg`
9. In `YourConnectorComponentHandler.class` override method `modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition)` with
    ```
    return modifiableComponentDefinition
    .customAction(true)
    .icon("path:assets/yourconnector.svg");
    ```
### Connection
If your authentication has some custom parameters then in `YourConnectorComponentHandler.class` override method `modifyConnection(ModifiableConnectionDefinition modifiableConnectionDefinition)`.
For example, look at `ShopifyComponentHandler.class`, `DiscordComponentHandler.class` or `PipelinerComponentHandler.class`.

### Dynamic options
Similarly, if some parameters should have dynamic options then in `YourConnectorComponentHandler.class` override method `modifyProperty(ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty)`.
For example, look at `ShopifyComponentHandler.class`.

### Dynamic properties
To mark some object as `dynamic properties` type, set `x-property-type: "dynamicProperties"` extension as part of the object definition.
