---
name: ByteChef Component Builder
description: This skill should be used when the user asks to "create a ByteChef component", "build a new component", "add a component", "new bytechef plugin", "scaffold a component", "create an action", "create a trigger", "create a connection definition", "add an action to a component", "add a trigger", "add a webhook trigger", "OpenAPI component", "component handler", or mentions building integrations for the ByteChef platform. Provides step-by-step guidance for creating ByteChef components with actions, triggers, connections, and tests.
---

# ByteChef Component Builder

Build new ByteChef components (integrations) that plug into the workflow automation platform. Components live in `server/libs/modules/components/` and are discovered via Java `ServiceLoader`.

## Component Structure

Every component follows this layout:

```
components/{name}/
  src/main/java/com/bytechef/component/{name}/
    {Name}ComponentHandler.java          # Entry point
    action/{Name}{ActionName}Action.java # One class per action
    trigger/{Name}{TriggerName}Trigger.java # Optional triggers
    connection/{Name}Connection.java     # Optional connection/auth
    constant/{Name}Constants.java        # String constants
    util/{Name}Utils.java               # Shared utilities
  src/main/resources/assets/{name}.svg   # Component icon
  src/test/java/com/bytechef/component/{name}/
    {Name}ComponentHandlerTest.java      # Definition snapshot test
  build.gradle.kts
```

## Step-by-Step Workflow

### 1. Scaffold the Directory

Create the component directory under `server/libs/modules/components/{name}/` with the structure above. Use lowercase for the directory name (e.g., `slack`, `google-sheets`).

### 2. Register in Build System

Add to `settings.gradle.kts` at project root:

```kotlin
include("server:libs:modules:components:{name}")
```

Create `build.gradle.kts`. For most components, it only needs:

```kotlin
version = "1.0"
```

Add integration test dependencies only when needed:

```kotlin
version = "1.0"

dependencies {
    testImplementation(project(":server:libs:atlas:atlas-execution:atlas-execution-api"))
    testImplementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-api"))
    testImplementation(project(":server:libs:platform:platform-component:platform-component-test-int-support"))
}
```

Parent `components/build.gradle.kts` already provides: `component-api`, `auto-service`, `commons-lang3`, `component-test`, and `test-support`.

### 3. Create Constants Class

Define all string literals as constants. Never use raw strings in property definitions.

```java
public class {Name}Constants {

    public static final String CONTENT = "content";
    public static final String DESCRIPTION = "description";
    public static final String NAME = "name";
    public static final String TOKEN = "token";

    private {Name}Constants() {
    }
}
```

### 4. Create the Connection (if API-based)

Define authentication and base URI. See `references/component-api-reference.md` for all auth types.

```java
public class {Name}Connection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.example.com")
        .authorizations(
            authorization(AuthorizationType.BEARER_TOKEN)
                .title("Bearer Token")
                .properties(
                    string(TOKEN)
                        .label("API Token")
                        .required(true)));
}
```

**Common auth types:** `API_KEY`, `BASIC_AUTH`, `BEARER_TOKEN`, `OAUTH2_AUTHORIZATION_CODE`, `OAUTH2_CLIENT_CREDENTIALS`.

### 5. Create Actions

Each action is a separate class with a static `ACTION_DEFINITION` and a `perform` method.

```java
public class {Name}{ActionName}Action {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("actionName")
        .title("Action Title")
        .description("What this action does.")
        .properties(
            string(FIELD_NAME)
                .label("Field Label")
                .description("Help text.")
                .required(true))
        .output(outputSchema(
            object()
                .properties(
                    string("id"),
                    string("status"))))
        .perform({Name}{ActionName}Action::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context
            .http(http -> http.post("/endpoint"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(Http.Body.of(
                FIELD_NAME, inputParameters.getRequiredString(FIELD_NAME)))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
```

**Key perform method patterns:**
- HTTP calls via `context.http(...)` — supports GET, POST, PUT, PATCH, DELETE
- File operations via `context.file(...)`
- Logging via `context.log(...)`
- JSON/XML parsing via `context.json(...)` / `context.xml(...)`
- Input values via `inputParameters.getRequiredString(KEY)`, `.getInteger(KEY)`, `.getBoolean(KEY)`, etc.

### 6. Create Triggers (Optional)

Triggers activate workflows based on external events.

```java
public class {Name}{TriggerName}Trigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("triggerName")
        .title("Trigger Title")
        .description("When this triggers.")
        .type(TriggerType.STATIC_WEBHOOK)
        .properties(...)
        .output(outputSchema(...))
        .webhookRequest({Name}{TriggerName}Trigger::webhookRequest);

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers,
        HttpParameters parameters, WebhookBody body, WebhookMethod method,
        Parameters output, TriggerContext context) {

        return body.getContent(new TypeReference<Map<String, Object>>() {});
    }
}
```

**Trigger types:** `STATIC_WEBHOOK`, `DYNAMIC_WEBHOOK`, `POLLING`, `LISTENER`, `CALLABLE`, `HYBRID`.

### 7. Create the ComponentHandler

Wire everything together:

```java
@AutoService(ComponentHandler.class)
public class {Name}ComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("{name}")
        .title("{Display Name}")
        .description("What this component integrates with.")
        .icon("path:assets/{name}.svg")
        .categories(ComponentCategory.DEVELOPER_TOOLS)
        .connection({Name}Connection.CONNECTION_DEFINITION)
        .actions(
            {Name}{Action1}Action.ACTION_DEFINITION,
            {Name}{Action2}Action.ACTION_DEFINITION)
        .triggers(
            {Name}{Trigger1}Trigger.TRIGGER_DEFINITION)
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
```

For AI-tool-enabled components, also add `.clusterElements(tool(ACTION_DEFINITION))`.

### 8. Add Component Icon

Place an SVG icon at `src/main/resources/assets/{name}.svg`.

### 9. Write Tests

Create a definition snapshot test:

```java
public class {Name}ComponentHandlerTest {

    @Test
    public void testGetComponentDefinition() {
        JsonFileAssert.assertEquals(
            "definition/{name}_v1.json", new {Name}ComponentHandler().getDefinition());
    }
}
```

Run the test once to generate the JSON snapshot, then subsequent runs validate against it. To regenerate, delete both `src/test/resources/definition/{name}_v1.json` and `build/resources/test/definition/`.

### 10. Build and Verify

```bash
./gradlew :server:libs:modules:components:{name}:compileJava
./gradlew :server:libs:modules:components:{name}:test
./gradlew spotlessApply
```

## Property Quick Reference

| Method | Java Type | UI Control |
|--------|-----------|------------|
| `string()` | String | TEXT |
| `integer()` | Long | INTEGER |
| `number()` | Double | NUMBER |
| `bool()` | Boolean | SELECT (true/false) |
| `date()` | LocalDate | DATE |
| `dateTime()` | LocalDateTime | DATE_TIME |
| `time()` | LocalTime | TIME |
| `array()` | List | ARRAY_BUILDER |
| `object()` | Map | OBJECT_BUILDER |
| `fileEntry()` | FileEntry | FILE_ENTRY |

**Common property modifiers:** `.label()`, `.description()`, `.required()`, `.defaultValue()`, `.controlType()`, `.options()`, `.optionsLookupDependsOn()`, `.displayCondition()`, `.placeholder()`, `.advancedOption()`.

## Dynamic Options Pattern

Provide dropdown options fetched from an API:

```java
string(FIELD)
    .label("Select Item")
    .options((ActionOptionsFunction<String>) (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) ->
        context.http(http -> http.get("/items"))
            .execute()
            .getBody(new TypeReference<List<Map<String, Object>>>() {})
            .stream()
            .map(item -> option(
                (String) item.get("name"),
                (String) item.get("id")))
            .collect(Collectors.toList()))
    .required(true)
```

## OpenAPI-Based Components

For API-first components, use the CLI scaffolder:

```bash
cd cli
./gradlew :cli-app:bootRun --args="component init openapi --name={name} --openapi-path=/path/to/openapi.yaml"
```

Then extend the generated `Abstract{Name}ComponentHandler` by implementing `OpenApiComponentHandler` (annotated with `@AutoService(OpenApiComponentHandler.class)`) and override `modifyActions()`, `modifyComponent()`, and `getCustomActions()` as needed.

## Additional Resources

### Reference Files

- **`references/component-api-reference.md`** — Full API reference: all property types, context methods, auth types, trigger types, Parameters getters, HTTP helpers, output definitions
- **`references/component-patterns.md`** — Real-world patterns extracted from existing components: connections (OAuth2, API key, Bearer), actions (HTTP calls, file ops, dynamic options), triggers (webhook, polling), constants, utilities

### Key Static Imports

Every component file typically starts with:

```java
import static com.bytechef.component.definition.ComponentDsl.*;
```

This imports all DSL factory methods: `component()`, `action()`, `trigger()`, `connection()`, `authorization()`, `string()`, `integer()`, `object()`, `option()`, `outputSchema()`, etc.

### License Headers

- Open-source components: Apache 2.0 license header
- EE components (`server/ee/`): ByteChef Enterprise license + `@version ee` Javadoc tag
