# ByteChef Component Patterns

Real-world patterns extracted from existing ByteChef components. Use these as templates when building new components.

## Connection Patterns

### Bearer Token (Simplest)

```java
// Source: Discord component
public class ExampleConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.example.com/v1")
        .authorizations(
            authorization(AuthorizationType.BEARER_TOKEN)
                .title("Bearer Token")
                .properties(
                    string(TOKEN)
                        .label("API Token")
                        .required(true)));
}
```

### API Key

```java
public class ExampleConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.example.com")
        .authorizations(
            authorization(AuthorizationType.API_KEY)
                .title("API Key")
                .properties(
                    string(KEY)
                        .label("API Key")
                        .required(true),
                    string(VALUE)
                        .label("API Key Value")
                        .required(true)));
}
```

### Basic Auth

```java
public class ExampleConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.example.com")
        .authorizations(
            authorization(AuthorizationType.BASIC_AUTH)
                .title("Basic Authentication")
                .properties(
                    string(USERNAME)
                        .label("Username")
                        .required(true),
                    string(PASSWORD)
                        .label("Password")
                        .required(true)));
}
```

### OAuth2 Authorization Code

```java
// Source: Slack component
public class ExampleConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.example.com")
        .authorizations(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(CLIENT_ID)
                        .label("Client Id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .required(true))
                .authorizationUrl((connection, context) -> "https://example.com/oauth/authorize")
                .tokenUrl((connection, context) -> "https://example.com/oauth/token")
                .scopes((connection, context) -> {
                    Map<String, Boolean> scopeMap = new LinkedHashMap<>();

                    scopeMap.put("read", true);
                    scopeMap.put("write", true);

                    return scopeMap;
                }))
        .version(1);
}
```

## Action Patterns

### Simple HTTP GET

```java
public class ExampleListAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listItems")
        .title("List Items")
        .description("List all items.")
        .output(outputSchema(
            array()
                .items(
                    object()
                        .properties(
                            string("id"),
                            string("name")))))
        .perform(ExampleListAction::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context
            .http(http -> http.get("/items"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
```

### HTTP POST with Body

```java
public class ExampleCreateAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createItem")
        .title("Create Item")
        .description("Create a new item.")
        .properties(
            string(NAME)
                .label("Name")
                .description("Name of the item.")
                .required(true),
            string(DESCRIPTION)
                .label("Description")
                .description("Item description.")
                .required(false))
        .output(outputSchema(
            object()
                .properties(
                    string("id"),
                    string("name"),
                    string("description"))))
        .perform(ExampleCreateAction::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context
            .http(http -> http.post("/items"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(Http.Body.of(
                NAME, inputParameters.getRequiredString(NAME),
                DESCRIPTION, inputParameters.getString(DESCRIPTION)))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
```

### Action with Dynamic Options

```java
public class ExampleUpdateAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateItem")
        .title("Update Item")
        .description("Update an existing item.")
        .properties(
            string(ITEM_ID)
                .label("Item")
                .description("Select the item to update.")
                .options((ActionOptionsFunction<String>) ExampleUtils::getItemOptions)
                .required(true),
            string(NAME)
                .label("New Name")
                .required(true))
        .perform(ExampleUpdateAction::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context
            .http(http -> http.put("/items/" + inputParameters.getRequiredString(ITEM_ID)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(Http.Body.of(NAME, inputParameters.getRequiredString(NAME)))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
```

### Dynamic Options Utility

```java
public class ExampleUtils {

    public static List<Option<String>> getItemOptions(
        Parameters inputParameters, Parameters connectionParameters,
        Map<String, String> lookupDependsOnPaths, String searchText, ActionContext context) {

        return context
            .http(http -> http.get("/items"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<List<Map<String, Object>>>() {})
            .stream()
            .map(item -> option(
                (String) item.get("name"),
                (String) item.get("id")))
            .collect(Collectors.toList());
    }

    private ExampleUtils() {
    }
}
```

### Action with Dependent Dynamic Options

When one dropdown depends on another:

```java
string(WORKSPACE_ID)
    .label("Workspace")
    .options((ActionOptionsFunction<String>) ExampleUtils::getWorkspaceOptions)
    .required(true),
string(PROJECT_ID)
    .label("Project")
    .options((ActionOptionsFunction<String>) ExampleUtils::getProjectOptions)
    .optionsLookupDependsOn(WORKSPACE_ID)  // Re-fetch when workspace changes
    .required(true)
```

### Action with Conditional Display

```java
string(TYPE)
    .label("Type")
    .options(
        option("Email", "email"),
        option("SMS", "sms"))
    .required(true),
string(EMAIL_ADDRESS)
    .label("Email Address")
    .displayCondition("%s == '%s'".formatted(TYPE, "email"))
    .required(true),
string(PHONE_NUMBER)
    .label("Phone Number")
    .displayCondition("%s == '%s'".formatted(TYPE, "sms"))
    .required(true)
```

### Action with File Upload

```java
public class ExampleUploadAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("uploadFile")
        .title("Upload File")
        .description("Upload a file.")
        .properties(
            fileEntry(FILE)
                .label("File")
                .description("The file to upload.")
                .required(true))
        .perform(ExampleUploadAction::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context
            .http(http -> http.post("/files"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(Http.Body.of(inputParameters.getRequiredFileEntry(FILE)))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
```

## Trigger Patterns

### Static Webhook

The simplest webhook — ByteChef provides a fixed URL to receive events.

```java
public class ExampleNewItemTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newItem")
        .title("New Item")
        .description("Triggers when a new item is created.")
        .type(TriggerType.STATIC_WEBHOOK)
        .output(outputSchema(
            object()
                .properties(
                    string("id"),
                    string("name"),
                    string("createdAt"))))
        .webhookRequest(ExampleNewItemTrigger::webhookRequest);

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers,
        HttpParameters parameters, WebhookBody body, WebhookMethod method,
        Parameters output, TriggerContext context) {

        return body.getContent(new TypeReference<Map<String, Object>>() {});
    }
}
```

### Static Webhook with Challenge Validation (Slack pattern)

Some APIs require responding to a verification challenge when enabling the webhook.

```java
public class ExampleEventTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("anyEvent")
        .title("Any Event")
        .description("Triggers on any subscribed event.")
        .type(TriggerType.STATIC_WEBHOOK)
        .output()
        .webhookRequest(ExampleEventTrigger::webhookRequest)
        .webhookValidateOnEnable(ExampleEventTrigger::webhookValidateOnEnable);

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers,
        HttpParameters parameters, WebhookBody body, WebhookMethod method,
        Parameters output, TriggerContext context) {

        Map<String, Object> content = body.getContent(new TypeReference<>() {});

        return content.get("event");
    }

    public static WebhookValidateResponse webhookValidateOnEnable(
        Parameters inputParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, TriggerContext context) {

        Map<String, Object> content = body.getContent(new TypeReference<>() {});

        return new WebhookValidateResponse(
            content.get("challenge"), Map.of("Content-type", List.of("text/plain")), 200);
    }
}
```

### Dynamic Webhook

Register/unregister webhook subscription with the external API.

```java
public class ExampleDynamicTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newRecord")
        .title("New Record")
        .description("Triggers when a new record is created.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(TABLE_ID)
                .label("Table")
                .options((TriggerOptionsFunction<String>) ExampleUtils::getTableOptions)
                .required(true))
        .output(outputSchema(object()))
        .webhookEnable(ExampleDynamicTrigger::webhookEnable)
        .webhookDisable(ExampleDynamicTrigger::webhookDisable)
        .webhookRequest(ExampleDynamicTrigger::webhookRequest);

    protected static DynamicWebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        Map<String, Object> response = context
            .http(http -> http.post("/webhooks"))
            .body(Http.Body.of("url", webhookUrl, "events", List.of("record.created")))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return new DynamicWebhookEnableOutput(
            Map.of("webhookId", response.get("id")), null);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters,
        Parameters outputParameters, String workflowExecutionId, TriggerContext context) {

        context
            .http(http -> http.delete("/webhooks/" + outputParameters.getString("webhookId")))
            .execute();
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers,
        HttpParameters parameters, WebhookBody body, WebhookMethod method,
        Parameters output, TriggerContext context) {

        return body.getContent(new TypeReference<Map<String, Object>>() {});
    }
}
```

### Polling Trigger

Poll an API endpoint at intervals for new data.

```java
public class ExamplePollingTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newItems")
        .title("New Items")
        .description("Checks for new items at regular intervals.")
        .type(TriggerType.POLLING)
        .properties(
            string(STATUS)
                .label("Status Filter")
                .options(
                    option("Active", "active"),
                    option("All", "all"))
                .defaultValue("active")
                .required(false))
        .output(outputSchema(
            array()
                .items(object()
                    .properties(
                        string("id"),
                        string("name")))))
        .poll(ExamplePollingTrigger::poll);

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext context) {

        String lastId = closureParameters.getString("lastId");

        List<Map<String, Object>> items = context
            .http(http -> http.get("/items"))
            .queryParameter("since_id", lastId != null ? lastId : "")
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (items.isEmpty()) {
            return new PollOutput(List.of(), Map.of("lastId", lastId != null ? lastId : ""), false);
        }

        String newLastId = (String) items.getLast().get("id");

        return new PollOutput(items, Map.of("lastId", newLastId), false);
    }
}
```

## Constants Pattern

```java
public class ExampleConstants {

    public static final String CHANNEL = "channel";
    public static final String CONTENT = "content";
    public static final String DESCRIPTION = "description";
    public static final String FILE = "file";
    public static final String ITEM_ID = "itemId";
    public static final String NAME = "name";
    public static final String STATUS = "status";
    public static final String TABLE_ID = "tableId";
    public static final String TOKEN = "token";
    public static final String TYPE = "type";

    private ExampleConstants() {
    }
}
```

## ComponentHandler Pattern

### Standard Component

```java
@AutoService(ComponentHandler.class)
public class ExampleComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("example")
        .title("Example Service")
        .description("Integrates with the Example API for data management.")
        .icon("path:assets/example.svg")
        .categories(ComponentCategory.DEVELOPER_TOOLS)
        .connection(ExampleConnection.CONNECTION_DEFINITION)
        .actions(
            ExampleCreateAction.ACTION_DEFINITION,
            ExampleListAction.ACTION_DEFINITION,
            ExampleUpdateAction.ACTION_DEFINITION)
        .triggers(
            ExampleNewItemTrigger.TRIGGER_DEFINITION)
        .clusterElements(
            tool(ExampleCreateAction.ACTION_DEFINITION),
            tool(ExampleListAction.ACTION_DEFINITION))
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
```

### Minimal Component (No Connection, No Triggers)

```java
// Source: var, logger components
@AutoService(ComponentHandler.class)
public class HelperComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("helper")
        .title("Helper")
        .description("Utility actions for workflow data processing.")
        .icon("path:assets/helper.svg")
        .categories(ComponentCategory.HELPERS)
        .actions(HelperProcessAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
```

## Test Patterns

### Definition Snapshot Test (Required)

```java
public class ExampleComponentHandlerTest {

    @Test
    public void testGetComponentDefinition() {
        JsonFileAssert.assertEquals(
            "definition/example_v1.json", new ExampleComponentHandler().getDefinition());
    }
}
```

### Action Unit Test with Mocking

```java
class ExampleCreateActionTest {

    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(NAME, "Test Item", DESCRIPTION, "A test"));

    @Test
    void testPerform() {
        Map<String, Object> expectedResult = Map.of("id", "123", "name", "Test Item");

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(expectedResult);

        Object result = ExampleCreateAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(expectedResult, result);
    }
}
```

### Integration Test

```java
@ComponentIntTest
public class ExampleComponentHandlerIntTest {

    @Autowired
    private ComponentJobTestExecutor componentJobTestExecutor;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @Test
    public void testCreateItem() {
        Job job = componentJobTestExecutor.execute(
            Base64.getEncoder().encodeToString("example_v1".getBytes(StandardCharsets.UTF_8)),
            Map.of("name", "Test"));

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        assertNotNull(outputs.get("id"));
    }
}
```

## OpenAPI Component Pattern

For OpenAPI-based components, the CLI generates an abstract handler. Extend it:

```java
@AutoService(OpenApiComponentHandler.class)
public class ExampleComponentHandler extends AbstractExampleComponentHandler {

    @Override
    public List<ModifiableActionDefinition> getCustomActions() {
        return List.of(ExampleCustomAction.ACTION_DEFINITION);
    }

    @Override
    public List<ModifiableClusterElementDefinition<?>> getCustomClusterElements() {
        return List.of(tool(ExampleCustomAction.ACTION_DEFINITION));
    }

    @Override
    public List<ModifiableActionDefinition> modifyActions(
        ModifiableActionDefinition... actionDefinitions) {

        for (ModifiableActionDefinition actionDefinition : actionDefinitions) {
            if (Objects.equals(actionDefinition.getName(), "listItems")) {
                // Modify generated action (add properties, change options, etc.)
            }
        }

        return super.modifyActions(actionDefinitions);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(
        ModifiableComponentDefinition modifiableComponentDefinition) {

        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/example.svg")
            .categories(ComponentCategory.DEVELOPER_TOOLS);
    }
}
```

## Shared Reusable Properties

Define common properties in the Constants class for reuse across actions:

```java
// Source: Slack SlackConstants
public static final ModifiableStringProperty TEXT_PROPERTY = string(TEXT)
    .label("Message")
    .description("The text of your message.")
    .controlType(ControlType.TEXT_AREA)
    .required(true);

public static final ModifiableObjectProperty RESPONSE_PROPERTY = object()
    .properties(
        bool("ok"),
        string("error"));
```
