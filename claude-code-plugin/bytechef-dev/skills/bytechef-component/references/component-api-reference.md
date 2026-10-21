# ByteChef Component API Reference

Complete reference for the Component DSL in `sdks/backend/java/component-api/`.

## DSL Factory Methods

All methods are static imports from `com.bytechef.component.definition.ComponentDsl`:

```java
import static com.bytechef.component.definition.ComponentDsl.*;
```

### Top-Level Builders

| Method | Purpose |
|--------|---------|
| `component(String name)` | Create a component definition |
| `action(String name)` | Define an action |
| `trigger(String name)` | Define a trigger |
| `connection()` | Define a connection |
| `authorization(AuthorizationType)` | Define authorization |
| `tool(ActionDefinition)` | Create AI tool from action |
| `option(String label, T value)` | Create dropdown option |
| `outputSchema(ValueProperty<?>)` | Define output schema |
| `sampleOutput(Object)` | Define sample output |

## Property Types

### Factory Methods

| Method | Java Type | Default Control |
|--------|-----------|-----------------|
| `string(name)` | String | TEXT |
| `integer(name)` | Long | INTEGER |
| `number(name)` | Double | NUMBER |
| `bool(name)` | Boolean | SELECT |
| `date(name)` | LocalDate | DATE |
| `dateTime(name)` | LocalDateTime | DATE_TIME |
| `time(name)` | LocalTime | TIME |
| `array(name)` | List | ARRAY_BUILDER |
| `object(name)` | Map | OBJECT_BUILDER |
| `fileEntry(name)` | FileEntry | FILE_ENTRY |
| `nullable(name)` | null | NULL |
| `dynamicProperties(name)` | dynamic | — |

All accept optional `String name` parameter. Without name, used for output schema definitions.

### Common Property Modifiers

Available on all property types:

```java
.label(String)              // Display label in UI
.description(String)        // Help text / tooltip
.required(boolean)          // Mark as mandatory
.defaultValue(T)            // Default value
.exampleValue(T)            // Example for documentation
.placeholder(String)        // Placeholder text
.hidden(boolean)            // Hide from UI
.advancedOption(boolean)    // Show under "Advanced" section
.displayCondition(String)   // Conditional display (expression)
.expressionEnabled(boolean) // Allow formula/expression input
.metadata(String, String)   // Key-value metadata
```

### String-Specific Modifiers

```java
.controlType(ControlType)       // Override UI control
.minLength(int)                 // Minimum length
.maxLength(int)                 // Maximum length
.regex(String)                  // Validation regex
.languageId(String)             // Syntax highlighting ("javascript", "python", "json")
.options(Option<String>...)     // Static dropdown options
.options(BaseOptionsFunction)   // Dynamic options function
.optionsLookupDependsOn(String...) // Re-fetch options when these fields change
```

### Array-Specific Modifiers

```java
.items(ValueProperty<?>...)  // Define item type(s)
.minItems(long)              // Minimum items
.maxItems(long)              // Maximum items
.multipleValues(boolean)     // Allow multiple values
```

### Object-Specific Modifiers

```java
.properties(ValueProperty<?>...)           // Define child properties
.additionalProperties(ValueProperty<?>...) // Schema for extra properties
.multipleValues(boolean)                   // Allow multiple values
```

### UI Control Types (Property.ControlType)

| Control | Use For |
|---------|---------|
| `TEXT` | Single-line text |
| `TEXT_AREA` | Multi-line text |
| `CODE_EDITOR` | Code with syntax highlighting |
| `EMAIL` | Email addresses |
| `PASSWORD` | Sensitive values |
| `PHONE` | Phone numbers |
| `URL` | URLs |
| `SELECT` | Single dropdown |
| `MULTI_SELECT` | Multiple selection |
| `DATE` | Date picker |
| `DATE_TIME` | Date and time picker |
| `TIME` | Time picker |
| `FILE_ENTRY` | File upload |
| `RICH_TEXT` | Rich text editor |
| `ARRAY_BUILDER` | Array editor |
| `OBJECT_BUILDER` | Object editor |
| `JSON_SCHEMA_BUILDER` | JSON schema editor |

## Authorization Types

| Type | Use For |
|------|---------|
| `API_KEY` | API key in header or query |
| `BASIC_AUTH` | HTTP Basic (username/password) |
| `BEARER_TOKEN` | Bearer token |
| `CUSTOM` | Custom auth logic |
| `DIGEST_AUTH` | HTTP Digest auth |
| `OAUTH2_AUTHORIZATION_CODE` | OAuth2 auth code flow |
| `OAUTH2_AUTHORIZATION_CODE_PKCE` | OAuth2 with PKCE |
| `OAUTH2_CLIENT_CREDENTIALS` | OAuth2 client credentials |
| `OAUTH2_IMPLICIT_CODE` | OAuth2 implicit flow |
| `OAUTH2_RESOURCE_OWNER_PASSWORD` | OAuth2 resource owner password |

### Authorization Builder Methods

```java
authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
    .title(String)
    .description(String)
    .properties(Property...)            // Config properties (clientId, clientSecret, etc.)
    .authorizationUrl(Function)         // OAuth2 authorize endpoint
    .tokenUrl(Function)                 // OAuth2 token endpoint
    .refreshUrl(Function)               // OAuth2 refresh endpoint
    .scopes(Function)                   // OAuth2 scopes (Map<String, Boolean>)
    .clientId(Function)                 // Client ID provider
    .clientSecret(Function)             // Client secret provider
    .apply(ApplyFunction)               // Apply auth to request
    .acquire(AcquireFunction)           // Acquire credentials
    .refresh(RefreshFunction)           // Refresh credentials
    .refreshOn(Object...)               // Regex/status triggers for refresh
    .detectOn(String...)                // Regex for error detection
    .pkce(PkceFunction)                 // PKCE configuration
```

## Trigger Types

| Type | Description | Key Methods |
|------|-------------|-------------|
| `STATIC_WEBHOOK` | Fixed webhook URL | `webhookRequest()` |
| `DYNAMIC_WEBHOOK` | Dynamic webhook config | `webhookEnable()`, `webhookDisable()`, `webhookRequest()` |
| `POLLING` | Poll at intervals | `poll()` |
| `LISTENER` | Event listener | `listenerEnable()`, `listenerDisable()` |
| `CALLABLE` | Manual trigger | — |
| `HYBRID` | Combination | Multiple methods |

### Trigger Builder Methods

```java
trigger("name")
    .title(String)
    .description(String)
    .type(TriggerType)                          // Required
    .properties(Property...)
    .output(OutputSchema / SampleOutput)
    .webhookRequest(WebhookRequestFunction)     // Handle incoming webhook
    .webhookEnable(WebhookEnableFunction)       // Setup webhook subscription
    .webhookDisable(WebhookDisableConsumer)      // Cleanup webhook
    .webhookValidate(WebhookValidateFunction)   // Validate webhook request
    .webhookValidateOnEnable(Function)          // Validate on enable (e.g., Slack challenge)
    .webhookRawBody(boolean)                    // Use raw body
    .poll(PollFunction)                         // For POLLING triggers
    .listenerEnable(ListenerEnableConsumer)     // For LISTENER triggers
    .listenerDisable(ListenerDisableConsumer)
    .deduplicate(DeduplicateFunction)           // Dedup records
    .batch(boolean)                             // Batch processing
    .help(String body, String learnMoreUrl)
    .workflowSyncExecution(boolean)
```

## Action Builder Methods

```java
action("name")
    .title(String)
    .description(String)
    .properties(Property...)
    .perform(PerformFunction)                           // Main execution
    .output(OutputSchema / SampleOutput / OutputFunction)
    .help(String body, String learnMoreUrl)
    .metadata(Map<String, Object>)
    .batch(boolean)
    .deprecated(Boolean)
    .resumePerform(ResumePerformFunction)               // Resume after suspension
    .beforeSuspend(BeforeSuspendConsumer)
    .processErrorResponse(ProcessErrorResponseFunction)
    .workflowNodeDescription(WorkflowNodeDescriptionFunction)
```

### Perform Method Signature

```java
protected static Object perform(
    Parameters inputParameters,        // User-provided input values
    Parameters connectionParameters,   // Connection credentials
    ActionContext context)             // Runtime context (HTTP, file, logging, etc.)
```

## Component Builder Methods

```java
component("name")
    .title(String)
    .description(String)
    .icon(String)                              // "path:assets/{name}.svg"
    .version(int)
    .categories(ComponentCategory...)
    .tags(String...)
    .connection(ConnectionDefinition)
    .actions(ActionDefinition...)
    .triggers(TriggerDefinition...)
    .customAction(boolean)                     // Enable custom API actions
    .customActionHelp(String body, String url)
    .clusterElements(tool(ActionDefinition)...)  // AI tools
    .metadata(Map<String, Object>)
    .resources(String documentationUrl)
```

## Connection Builder Methods

```java
connection()
    .baseUri(BaseUriFunction)                  // API base URL
    .authorizations(ModifiableAuthorization...)
    .authorizationRequired(boolean)
    .properties(Property...)                   // Extra connection properties
    .test(TestConsumer)                        // Connection test function
    .help(String body, String learnMoreUrl)
    .version(int)
```

## Component Categories

```
ACCOUNTING, ADVERTISING, ANALYTICS, ARTIFICIAL_INTELLIGENCE,
ATS, CALENDARS_AND_SCHEDULING, COMMUNICATION, CRM,
CUSTOMER_SUPPORT, DEVELOPER_TOOLS, E_COMMERCE, FILE_STORAGE,
HELPERS, HRIS, MARKETING_AUTOMATION, PAYMENT_PROCESSING,
PRODUCTIVITY_AND_COLLABORATION, PROJECT_MANAGEMENT,
SOCIAL_MEDIA, SURVEYS_AND_FEEDBACK
```

## Context Methods

### HTTP Operations

```java
context.http(http -> http.get("/path"))       // GET request
context.http(http -> http.post("/path"))      // POST request
context.http(http -> http.put("/path"))       // PUT request
context.http(http -> http.patch("/path"))     // PATCH request
context.http(http -> http.delete("/path"))    // DELETE request

// Chain configuration:
    .header(String name, String value)
    .headers(Map<String, List<String>>)
    .queryParameter(String name, String value)
    .queryParameters(Map<String, List<String>>)
    .body(Http.Body.of(...))
    .configuration(Http.responseType(Http.ResponseType.JSON))
    .execute()                                // Returns Response
    .getBody(new TypeReference<>() {})       // Parse response body
```

### HTTP Body Types

```java
Http.Body.of(String content)                 // Raw string body
Http.Body.of(String content, String mimeType)
Http.Body.of(Map<String, ?> content)          // JSON object body
Http.Body.of(List<?> content)                 // JSON array body
Http.Body.of(FileEntry fileEntry)             // File upload
Http.Body.of(Object... keyValuePairs)         // Key-value pairs
```

### HTTP Response Types

```java
Http.ResponseType.JSON
Http.ResponseType.XML
Http.ResponseType.TEXT
Http.ResponseType.BINARY
```

### HTTP Configuration

```java
.configuration(
    Http.responseType(ResponseType.JSON)
    // Also available:
    // Http.timeout(Duration)
    // Http.followRedirect(boolean)
    // Http.allowUnauthorizedCerts(boolean)
    // Http.disableAuthorization(boolean)
)
```

### File Operations

```java
context.file(file -> file.readToString(fileEntry))
context.file(file -> file.readAllBytes(fileEntry))
context.file(file -> file.getInputStream(fileEntry))
context.file(file -> file.storeContent("filename.txt", inputStream))
context.file(file -> file.storeContent("filename.txt", stringData))
context.file(file -> file.toTempFile(fileEntry))
```

### Logging

```java
context.log(log -> log.debug("Message: {}", value))
context.log(log -> log.info("Processing item"))
context.log(log -> log.warn("Unexpected state"))
context.log(log -> log.error("Failed: {}", error))
```

### JSON Operations

```java
context.json(json -> json.read(jsonString))
context.json(json -> json.read(jsonString, TargetClass.class))
context.json(json -> json.readList(jsonString, ElementClass.class))
context.json(json -> json.readMap(jsonString))
context.json(json -> json.write(object))
```

### Data Storage (ActionContext only)

```java
context.data(data -> data.get(Data.Scope.WORKFLOW, "key"))
context.data(data -> data.put(Data.Scope.WORKFLOW, "key", value))
context.data(data -> data.remove(Data.Scope.WORKFLOW, "key"))

// Scopes: CURRENT_EXECUTION, WORKFLOW, PRINCIPAL, ACCOUNT
```

### Encoding

```java
context.encoder(encoder -> encoder.base64Encode(value))
context.encoder(encoder -> encoder.base64Decode(encoded))
```

## Parameters Methods

### String Getters

```java
inputParameters.getString("key")
inputParameters.getString("key", "default")
inputParameters.getRequiredString("key")
```

### Numeric Getters

```java
inputParameters.getInteger("key")            // Integer
inputParameters.getRequiredInteger("key")
inputParameters.getLong("key")               // Long
inputParameters.getRequiredLong("key")
inputParameters.getDouble("key")             // Double
inputParameters.getRequiredDouble("key")
inputParameters.getFloat("key")              // Float
```

### Boolean

```java
inputParameters.getBoolean("key")
inputParameters.getBoolean("key", false)
inputParameters.getRequiredBoolean("key")
```

### Date/Time

```java
inputParameters.getLocalDate("key")
inputParameters.getLocalDateTime("key")
inputParameters.getLocalTime("key")
inputParameters.getDate("key")
inputParameters.getDuration("key")
```

### Collections

```java
inputParameters.getList("key")
inputParameters.getList("key", String.class)
inputParameters.getRequiredList("key")
inputParameters.getArray("key")
inputParameters.getMap("key")
inputParameters.getMap("key", String.class)
inputParameters.getRequiredMap("key")
```

### File

```java
inputParameters.getFileEntry("key")
inputParameters.getFileEntries("key")
inputParameters.getRequiredFileEntry("key")
```

### Generic

```java
inputParameters.get("key", TargetClass.class)
inputParameters.getRequired("key")
inputParameters.getRequired("key", TargetClass.class)
inputParameters.containsPath("key")
inputParameters.toMap()
```

## Output Definitions

```java
// Schema-based output
action.output(outputSchema(
    object()
        .properties(
            string("id"),
            string("name"),
            integer("count"))))

// Sample output (infers schema)
action.output(sampleOutput(Map.of("id", "123", "name", "Test")))

// Both schema and sample
action.output(outputSchema(objectProperty), sampleOutput(sampleMap))

// Dynamic output function
action.output((inputParameters, connectionParameters, context) -> {
    // Return OutputResponse
})
```

## Dynamic Options Function

```java
string(FIELD)
    .options((ActionOptionsFunction<String>) (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> {
        return context.http(http -> http.get("/items"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<List<Map<String, Object>>>() {})
            .stream()
            .map(item -> option(
                (String) item.get("name"),
                (String) item.get("id")))
            .collect(Collectors.toList());
    })
    .optionsLookupDependsOn(DEPENDENT_FIELD)
```

## Option Factory Methods

```java
option("Label", "stringValue")
option("Label", "stringValue", "Description")
option("Label", 42L)                    // Long
option("Label", 3.14)                   // Double
option("Label", true)                   // Boolean
option("Label", LocalDate.of(2025, 1, 1))
option("Label", objectValue)
```
