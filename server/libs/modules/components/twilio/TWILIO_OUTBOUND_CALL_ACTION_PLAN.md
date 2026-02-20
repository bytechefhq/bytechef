# Twilio Outbound Call Action with Synchronous Sub-Workflow

## Summary

Implement a Twilio outbound call action that:
1. Initiates an outbound call via Twilio API
2. Returns TwiML with WebSocket URL and status callback URL
3. Executes a sub-workflow synchronously during the call
4. Blocks until the call ends, then continues the main workflow

## Questions to Address

### 1. Status Callback URL in TwiML

**Answer: YES** - We should include the TwilioCallbackController URL in both:
- The Twilio API call (`StatusCallback` parameter) - for call lifecycle events
- The TwiML response (optional, for stream events)

This enables:
- Detection of call answered (to start sub-workflow)
- Detection of call ended (to stop sub-workflow and unblock main workflow)

### 2. Architecture for Outbound Call

```
Main Workflow
     │
     ▼
TwilioMakeCallAction.perform()
     │
     ├── 1. POST to Twilio API (create call with StatusCallback URL)
     │        └── StatusCallback: {publicUrl}/webhooks/twilio/status?callSid={callSid}
     │        └── Url: {publicUrl}/webhooks/{workflowExecutionId}/twiml?callSid={callSid}&subWorkflowId={...}
     │
     ├── 2. Register CallSession in CallSessionRegistry
     │        └── Store: callSid, subWorkflowId, mainJobId
     │
     ├── 3. Block on CountDownLatch (wait for call completion)
     │
     │   ════════════════════════════════════════════════════
     │
     │   [Twilio calls TwiML URL when answered]
     │        │
     │        ▼
     │   TwimlController returns WebSocket stream config
     │        │
     │        ▼
     │   WebhookWebSocketHandler.afterConnectionEstablished()
     │        │
     │        ▼
     │   Start sub-workflow via JobFacade.createJob()
     │        │
     │        ▼
     │   Sub-workflow runs during call (real-time processing)
     │
     │   [Twilio sends status callback when call ends]
     │        │
     │        ▼
     │   TwilioCallbackController.handleStatusCallback()
     │        │
     │        ├── Stop sub-workflow: jobFacade.stopJob(subJobId)
     │        └── Signal completion: countDownLatch.countDown()
     │
     │   ════════════════════════════════════════════════════
     │
     ├── 4. CountDownLatch unblocks
     │
     └── 5. Return call result (duration, status, etc.)
     │
     ▼
Main Workflow continues with next action
```

## Key Design Decisions

### 1. Marker Interface: `RealtimeCallAction`

Create a marker interface to identify actions that:
- Execute a sub-workflow synchronously during execution
- Block until an external event (call completion) occurs
- Need access to call session registry and job coordination

```java
public interface RealtimeCallAction {
    String getSubWorkflowIdProperty();  // Property name containing sub-workflow ID
}
```

### 2. TwiML Endpoint (New)

Need a new controller endpoint to serve TwiML for outbound calls:
- `GET/POST /webhooks/{workflowExecutionId}/twiml` → Returns WebSocket stream TwiML
- Similar to what TwilioInboundCallTrigger.webhookValidate() returns

### 3. Blocking Mechanism

Use `CallSession` to coordinate:
```java
public class CallSession {
    // Existing fields...

    // New: Completion signal
    private CountDownLatch completionLatch = new CountDownLatch(1);

    public void awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException {
        completionLatch.await(timeout, unit);
    }

    public void signalCompletion() {
        completionLatch.countDown();
    }
}
```

### 4. Status Callback URL Format

```
{publicUrl}/webhooks/twilio/status?callSid={callSid}&actionJobId={jobId}
```

The `actionJobId` allows TwilioCallbackController to signal the correct action's latch.

## Implementation Tasks

### Phase 1: Infrastructure Updates

#### 1.1 Extend CallSession with completion signaling
**File:** `CallSessionRegistry.java`
```java
// Add to CallSession:
private CountDownLatch completionLatch;
private Long actionJobId;  // The job ID of the action waiting for completion

// Methods:
void awaitCompletion(long timeout, TimeUnit unit);
void signalCompletion();
```

#### 1.2 Create TwiML Controller
**File:** `TwimlController.java` (new)
- Endpoint: `GET/POST /webhooks/{workflowExecutionId}/twiml`
- Extracts `callSid`, `subWorkflowId` from query params
- Returns TwiML with WebSocket stream URL

#### 1.3 Update TwilioCallbackController
**File:** `TwilioCallbackController.java`
- On terminal status, call `callSession.signalCompletion()`
- Extract `actionJobId` from query params for correlation

### Phase 2: Marker Interface

#### 2.1 Create RealtimeCallAction interface
**File:** `sdks/backend/java/component-api/.../RealtimeCallAction.java` (new)
```java
public interface RealtimeCallAction {
    String getSubWorkflowIdProperty();
}
```

### Phase 3: Outbound Call Action

#### 3.1 Create TwilioMakeCallAction
**File:** `TwilioMakeCallAction.java` (new)
```java
public class TwilioMakeCallAction implements RealtimeCallAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("makeCall")
        .title("Make Outbound Call")
        .description("Initiates an outbound call and executes a workflow synchronously during the call")
        .properties(
            string("to").label("To").description("Phone number to call").required(true),
            string("from").label("From").description("Your Twilio phone number").required(true),
            string(SUB_WORKFLOW).label("Real-Time Workflow").required(true),
            integer("timeout").label("Ring Timeout").defaultValue(30))
        .output(outputSchema(object().properties(
            string("callSid"),
            string("status"),
            integer("duration"),
            string("direction"))))
        .perform(TwilioMakeCallAction::perform);

    public static Object perform(Parameters input, Parameters connection, ActionContext context) {
        // Implementation below
    }
}
```

#### 3.2 Action Implementation Flow
```java
public static Object perform(Parameters input, Parameters connection, ActionContext context) {
    String to = input.getRequiredString("to");
    String from = input.getRequiredString("from");
    String subWorkflowId = input.getRequiredString(SUB_WORKFLOW);
    int timeout = input.getInteger("timeout", 30);

    // 1. Get context info
    ActionContextAware ctx = (ActionContextAware) context;
    String publicUrl = getPublicUrl();  // From config or context
    String workflowExecutionId = buildWorkflowExecutionId(ctx);

    // 2. Generate unique call reference (before Twilio call)
    String callReference = UUID.randomUUID().toString();

    // 3. Register pending call session
    CallSession session = callSessionRegistry.registerPendingCall(
        callReference, subWorkflowId, ctx.getJobId());

    // 4. Build callback URLs
    String statusCallbackUrl = publicUrl + "/webhooks/twilio/status" +
        "?callRef=" + callReference + "&actionJobId=" + ctx.getJobId();
    String twimlUrl = publicUrl + "/webhooks/" + workflowExecutionId + "/twiml" +
        "?callRef=" + callReference + "&subWorkflowId=" + subWorkflowId;

    // 5. Make Twilio API call
    Map<String, Object> response = context.http(http -> http
        .post("/Accounts/" + connection.getRequiredString(ACCOUNT_SID) + "/Calls.json"))
        .body(Body.of(Map.of(
            "To", to,
            "From", from,
            "Url", twimlUrl,
            "StatusCallback", statusCallbackUrl,
            "StatusCallbackEvent", "initiated ringing answered completed",
            "StatusCallbackMethod", "POST",
            "Timeout", String.valueOf(timeout)
        ), FORM_URL_ENCODED))
        .configuration(responseType(ResponseType.JSON))
        .execute()
        .getBody(new TypeReference<>() {});

    String callSid = (String) response.get("sid");

    // 6. Update session with real callSid
    session.setCallSid(callSid);

    // 7. Block until call completes (or timeout)
    boolean completed = session.awaitCompletion(30, TimeUnit.MINUTES);

    // 8. Return result
    return Map.of(
        "callSid", callSid,
        "status", session.getCallStatus(),
        "duration", session.getCallDuration(),
        "direction", "outbound-api"
    );
}
```

#### 3.3 Register action in TwilioComponentHandler
**File:** `TwilioComponentHandler.java`
```java
.actions(
    TwilioSendSMSAction.ACTION_DEFINITION,
    TwilioSendWhatsAppMessageAction.ACTION_DEFINITION,
    TwilioMakeCallAction.ACTION_DEFINITION)  // Add new action
```

### Phase 4: Fix Inbound Trigger TwiML

#### 4.1 Add StatusCallback to inbound TwiML
**File:** `TwilioInboundCallTrigger.java`

Update `buildTwimlResponse()` to include status callback:
```java
private static String buildTwimlResponse(
    String publicUrl, String workflowExecutionId, String callSid, String subWorkflowId) {

    String wsUrl = publicUrl.replace("https://", "wss://").replace("http://", "ws://")
        + "/webhooks/" + workflowExecutionId + "/wss?callSid=" + callSid
        + "&subWorkflowId=" + subWorkflowId;

    String statusCallbackUrl = publicUrl + "/webhooks/twilio/status?callSid=" + callSid;

    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <Response>
            <Connect action="%s">
                <Stream url="%s" statusCallback="%s">
                    <Parameter name="callSid" value="%s"/>
                </Stream>
            </Connect>
        </Response>
        """.formatted(statusCallbackUrl, wsUrl, statusCallbackUrl, callSid);
}
```

## Critical Files

### New Files

| File | Purpose |
|------|---------|
| `twilio/action/TwilioMakeCallAction.java` | Outbound call action with sync sub-workflow |
| `component-api/RealtimeCallAction.java` | Marker interface for real-time call actions |
| `platform-webhook-rest/TwimlController.java` | Serves TwiML for outbound calls |

### Modified Files

| File | Changes |
|------|---------|
| `TwilioComponentHandler.java` | Register MakeCall action |
| `TwilioInboundCallTrigger.java` | Add StatusCallback URL to TwiML |
| `CallSessionRegistry.java` | Add completion latch, actionJobId, duration tracking |
| `TwilioCallbackController.java` | Signal completion on terminal status |

## Verification

```bash
# Compile
./gradlew :server:libs:modules:components:twilio:compileJava
./gradlew :server:libs:platform:platform-webhook:platform-websocket-webhook-rest:compileJava

# Test
./gradlew :server:libs:modules:components:twilio:test

# Full build
./gradlew compileJava
```

### Manual Testing

1. **Outbound Call Flow:**
   - Create workflow with TwilioMakeCallAction
   - Configure sub-workflow for real-time processing
   - Execute workflow → verify call is made
   - Answer call → verify sub-workflow starts
   - Hang up → verify action returns and main workflow continues

2. **Inbound Call Flow (with status callback):**
   - Configure inbound trigger
   - Call the Twilio number
   - Verify status callbacks are received
   - Verify sub-workflow starts on WebSocket connect
   - Hang up → verify sub-workflow stops

## Design Decisions (Confirmed)

1. **Public URL Discovery:** Add `getPublicUrl()` to `ActionContextAware` interface
   - Implementation reads from platform configuration
   - Actions access via `((ActionContextAware) context).getPublicUrl()`

2. **Timeout Handling:** Return with timeout status
   - Action returns normally with `status="timeout"`
   - Workflow continues execution (user can handle timeout in next actions)

3. **Marker Interface Location:** `platform-component-api`
   - File: `server/libs/platform/platform-component/platform-component-api/src/main/java/.../RealtimeCallAction.java`
   - Platform-level abstraction accessible to all components

4. **Error Handling:** Twilio API failures
   - Immediate return with error status
   - No retry logic (user can configure retry in workflow)
