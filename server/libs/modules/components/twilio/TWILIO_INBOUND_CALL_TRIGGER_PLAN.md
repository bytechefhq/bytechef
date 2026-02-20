# Twilio Inbound Call Trigger with Synchronous Sub-Workflow Execution

## Implementation Plan

This document describes the architecture and implementation tasks for integrating Twilio inbound voice calls into ByteChef workflows with real-time, synchronous sub-workflow execution.

---

## 1. Architecture Overview

### Execution Flow

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           INBOUND CALL FLOW                                     │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  1. Twilio POST /webhooks/{workflowExecutionId}                                │
│     │                                                                           │
│     ▼                                                                           │
│  2. TwilioInboundCallTrigger.webhookRequest()                                  │
│     ├── Extract CallSid, From, To, Direction                                   │
│     ├── Register CallSession in CallSessionRegistry                            │
│     ├── Store sub-workflow ID from trigger parameter                           │
│     └── Return TwiML with WebSocket Stream URL                                 │
│                                                                                 │
│  3. TwiML Response to Twilio:                                                  │
│     <Response>                                                                 │
│       <Connect>                                                                 │
│         <Stream url="wss://{host}/webhooks/{workflowExecutionId}/wss?callSid=X"/>│
│       </Connect>                                                                │
│     </Response>                                                                 │
│                                                                                 │
│  4. Twilio connects to WebSocket endpoint                                      │
│     │                                                                           │
│     ▼                                                                           │
│  5. WebhookWebSocketHandler.afterConnectionEstablished()                       │
│     ├── Resolve workflowExecutionId from URI                                   │
│     ├── Look up CallSession by callSid                                         │
│     ├── Retrieve sub-workflow ID from CallSession                              │
│     └── Start synchronous sub-workflow execution via JobFacade.createJob()     │
│                                                                                 │
│  6. Sub-Workflow Execution Loop (synchronous, in-process):                     │
│     ├── JobSyncExecutor executes sub-workflow tasks                            │
│     ├── Audio/events streamed via WebSocket to/from AI nodes                   │
│     ├── CallSession status checked between tasks                               │
│     └── Loop continues until CallSession.status != "in-progress"               │
│                                                                                 │
│  7. Call Ends:                                                                  │
│     ├── Twilio sends status callback to /webhooks/twilio/status                │
│     ├── TwilioCallbackController updates CallSession.status = "completed"      │
│     ├── Sub-workflow detects status change and terminates                      │
│     └── WebSocket connection closed                                            │
│                                                                                 │
│  8. Main Workflow Continuation:                                                │
│     ├── Control returns from sub-workflow                                      │
│     ├── JobFacade.createJob() starts remaining workflow tasks                  │
│     └── CRM updates, analytics, notifications execute normally                 │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Key Components

| Component | Location | Responsibility |
|-----------|----------|----------------|
| `TwilioInboundCallTrigger` | `components/twilio/trigger/` | Handle inbound webhook, return TwiML, register session |
| `RealtimeWebhookTrigger` | `component-api/` | Marker interface for real-time triggers |
| `CallSessionRegistry` | `platform-websocket-webhook-rest/` | Session state management (already exists) |
| `WebhookWebSocketHandler` | `platform-websocket-webhook-rest/` | WebSocket handling, sub-workflow execution |
| `TwilioCallbackController` | `platform-websocket-webhook-rest/` | Status callbacks (already exists) |
| `JobSyncExecutor` | `platform-job-sync/` | Synchronous workflow execution (already exists) |

---

## 2. Implementation Tasks

### Phase 1: Core Interfaces and Types

#### Task 1.1: Create RealtimeWebhookTrigger Marker Interface

**File:** `server/libs/modules/components/components-api/src/main/java/com/bytechef/component/definition/RealtimeWebhookTrigger.java`

```java
package com.bytechef.component.definition;

/**
 * Marker interface indicating a trigger that requires real-time, synchronous
 * sub-workflow execution during an active connection (e.g., phone call).
 *
 * Triggers implementing this interface:
 * - Block main workflow progression during the connection
 * - Execute a referenced sub-workflow synchronously
 * - Monitor connection lifecycle to terminate gracefully
 */
public interface RealtimeWebhookTrigger {

    /**
     * @return the workflow ID of the sub-workflow to execute synchronously
     */
    String getSubWorkflowId();
}
```

#### Task 1.2: Extend CallSession with Sub-Workflow Context

**File:** Modify `server/libs/platform/platform-webhook/platform-websocket-webhook-rest/src/main/java/com/bytechef/platform/webhook/web/websocket/CallSessionRegistry.java`

Add fields to `CallSession`:
- `subWorkflowId` - ID of the sub-workflow to execute
- `mainJobId` - ID of the main workflow job (for continuation)
- `subJobId` - ID of the currently running sub-workflow job
- `workflowExecutionId` - The original workflow execution ID

```java
public static class CallSession {
    private final String callSid;
    private final String webSocketSessionId;
    private final CallMetadata metadata;
    private String callStatus;
    private final long createdAt;

    // New fields for sub-workflow execution
    private String subWorkflowId;
    private Long mainJobId;
    private Long subJobId;
    private String workflowExecutionId;

    // Getters and setters...
}
```

Add methods to `CallSessionRegistry`:
- `isCallActive(String callSid)` - Check if call status is "in-progress" or "ringing"
- `setSubWorkflowContext(String callSid, String subWorkflowId, Long mainJobId)`
- `setSubJobId(String callSid, Long subJobId)`

---

### Phase 2: Twilio Inbound Call Trigger

#### Task 2.1: Create TwilioInboundCallTrigger

**File:** `server/libs/modules/components/twilio/src/main/java/com/bytechef/component/twilio/trigger/TwilioInboundCallTrigger.java`

```java
package com.bytechef.component.twilio.trigger;

import static com.bytechef.component.definition.ComponentDsl.*;

public class TwilioInboundCallTrigger {

    public static final String SUB_WORKFLOW = "subWorkflow";

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("inboundCall")
        .title("Inbound Voice Call")
        .description("Triggers when an inbound voice call is received. Executes a sub-workflow " +
                     "synchronously during the call for real-time AI conversation.")
        .type(TriggerType.STATIC_WEBHOOK)
        .properties(
            string(SUB_WORKFLOW)
                .label("Real-Time Workflow")
                .description("The workflow to execute synchronously during the phone call. " +
                             "This workflow handles real-time audio processing and AI responses.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("callSid").description("Unique identifier for the call"),
                        string("from").description("Caller phone number"),
                        string("to").description("Called phone number"),
                        string("direction").description("Call direction (inbound/outbound)"),
                        string("accountSid").description("Twilio account SID"),
                        string("callStatus").description("Final call status"))))
        .webhookRequest(TwilioInboundCallTrigger::webhookRequest);

    private TwilioInboundCallTrigger() {
    }

    protected static TwiMLWebhookResponse webhookRequest(
        Parameters inputParameters,
        Parameters connectionParameters,
        HttpHeaders headers,
        HttpParameters parameters,
        WebhookBody body,
        WebhookMethod method,
        Parameters webhookEnableOutput,
        TriggerContext context) {

        // Extract Twilio parameters from form-urlencoded body
        Map<String, Object> content = body.getContent(Map.class);

        String callSid = (String) content.get("CallSid");
        String from = (String) content.get("From");
        String to = (String) content.get("To");
        String direction = (String) content.get("Direction");
        String accountSid = (String) content.get("AccountSid");

        // Get sub-workflow ID from trigger parameter
        String subWorkflowId = inputParameters.getRequiredString(SUB_WORKFLOW);

        // Store in context for WebSocket handler to retrieve
        context.data(data -> data.put(
            Data.Scope.WORKFLOW,
            "callSession:" + callSid,
            Map.of(
                "callSid", callSid,
                "subWorkflowId", subWorkflowId,
                "from", from,
                "to", to,
                "direction", direction,
                "accountSid", accountSid
            )
        ));

        // Build TwiML response with WebSocket stream
        String workflowExecutionId = context.getWorkflowExecutionId();
        String webhookBaseUrl = context.getWebhookBaseUrl();
        String wsUrl = webhookBaseUrl.replace("https://", "wss://")
            + "/webhooks/" + workflowExecutionId + "/wss?callSid=" + callSid;

        String twiml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Response>
                <Connect>
                    <Stream url="%s">
                        <Parameter name="callSid" value="%s"/>
                    </Stream>
                </Connect>
            </Response>
            """.formatted(wsUrl, callSid);

        return new TwiMLWebhookResponse(twiml);
    }
}
```

#### Task 2.2: Create TwiMLWebhookResponse Class

**File:** `server/libs/modules/components/twilio/src/main/java/com/bytechef/component/twilio/trigger/TwiMLWebhookResponse.java`

```java
package com.bytechef.component.twilio.trigger;

import com.bytechef.component.definition.TriggerDefinition.WebhookResponse;

/**
 * Special webhook response that returns TwiML content type.
 */
public record TwiMLWebhookResponse(String twiml) implements WebhookResponse {

    @Override
    public int status() {
        return 200;
    }

    @Override
    public Map<String, String> headers() {
        return Map.of("Content-Type", "application/xml");
    }

    @Override
    public Object body() {
        return twiml;
    }

    @Override
    public ResponseType type() {
        return ResponseType.RAW;
    }
}
```

#### Task 2.3: Register Trigger in TwilioComponentHandler

**File:** Modify `server/libs/modules/components/twilio/src/main/java/com/bytechef/component/twilio/TwilioComponentHandler.java`

```java
.triggers(
    TwilioNewWhatsappMessageTrigger.TRIGGER_DEFINITION,
    TwilioInboundCallTrigger.TRIGGER_DEFINITION)  // Add new trigger
```

---

### Phase 3: WebSocket Handler Enhancement

#### Task 3.1: Enhance WebhookWebSocketHandler for Sub-Workflow Execution

**File:** Modify `server/libs/platform/platform-webhook/platform-websocket-webhook-rest/src/main/java/com/bytechef/platform/webhook/web/websocket/WebhookWebSocketHandler.java`

Add dependencies:
```java
private final JobFacade jobFacade;
private final TriggerContextService triggerContextService;
private final Cache<String, Long> callSidToSubJobId;
```

Modify `afterConnectionEstablished()`:
```java
@Override
public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    URI uri = session.getUri();
    String webhookId = extractId(uri);
    String sessionKey = session.getId();
    String callSid = extractCallSid(uri);

    logger.info("WebSocket connection established: webhookId={}, sessionId={}, callSid={}",
        webhookId, sessionKey, callSid);

    if (callSid != null) {
        // Check for existing session...

        // Register new session
        CallMetadata metadata = new CallMetadata(null, null, null, null);
        callSessionRegistry.registerSession(callSid, session, metadata);
        sessionIdToCallSid.put(sessionKey, callSid);

        // Retrieve sub-workflow context from trigger data
        Optional<Map<String, Object>> contextOpt = triggerContextService.getData(
            webhookId, "callSession:" + callSid);

        if (contextOpt.isPresent()) {
            Map<String, Object> context = contextOpt.get();
            String subWorkflowId = (String) context.get("subWorkflowId");

            if (subWorkflowId != null) {
                // Update session with metadata
                CallSession callSession = callSessionRegistry.getSessionByCallSid(callSid).get();
                callSession.setSubWorkflowId(subWorkflowId);
                callSession.setWorkflowExecutionId(webhookId);

                // Start synchronous sub-workflow execution in background thread
                startSubWorkflowExecution(callSid, subWorkflowId, session);
            }
        }
    }

    // Send connected event...
}
```

Add sub-workflow execution method:
```java
private void startSubWorkflowExecution(String callSid, String subWorkflowId, WebSocketSession session) {
    // Execute in virtual thread to not block WebSocket handler
    Thread.startVirtualThread(() -> {
        try {
            logger.info("Starting sub-workflow execution: callSid={}, subWorkflowId={}", callSid, subWorkflowId);

            // Create job parameters for sub-workflow
            JobParametersDTO jobParams = new JobParametersDTO(
                subWorkflowId,
                null,  // No parent task execution
                Map.of(
                    "callSid", callSid,
                    "webSocketSessionId", session.getId()
                )
            );

            // Create and start the job via JobFacade
            long subJobId = jobFacade.createJob(jobParams);
            callSidToSubJobId.put(callSid, subJobId);

            // Update call session with sub-job ID
            callSessionRegistry.getSessionByCallSid(callSid)
                .ifPresent(cs -> cs.setSubJobId(subJobId));

            // Add listener to monitor call status during execution
            AutoCloseable statusListener = jobSyncExecutor.addJobStatusListener(subJobId, event -> {
                // Check if call is still active
                if (!callSessionRegistry.isCallActive(callSid)) {
                    logger.info("Call ended, stopping sub-workflow: callSid={}, subJobId={}",
                        callSid, subJobId);
                    jobSyncExecutor.stopJob(subJobId);
                }
            });

            // Register SSE bridge for streaming events to WebSocket
            WebSocketStreamBridge bridge = new WebSocketStreamBridge(session.getId());
            AutoCloseable bridgeHandle = jobSyncExecutor.addSseStreamBridge(subJobId, bridge);
            streamHandles.put(session.getId(), bridgeHandle);

            // Await sub-workflow completion (blocks until done)
            Job completedJob = jobSyncExecutor.awaitJob(subJobId, false);

            logger.info("Sub-workflow completed: callSid={}, status={}",
                callSid, completedJob.getStatus());

            // Clean up
            statusListener.close();

            // Notify main workflow to continue via JobFacade
            notifyMainWorkflowContinuation(callSid, completedJob);

        } catch (Exception e) {
            logger.error("Sub-workflow execution failed: callSid={}", callSid, e);

            // Send error to WebSocket
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("event", "error");
            error.put("message", "Sub-workflow execution failed: " + e.getMessage());
            sendMessage(session, error);
        }
    });
}

private void notifyMainWorkflowContinuation(String callSid, Job subJob) {
    Optional<CallSession> sessionOpt = callSessionRegistry.getSessionByCallSid(callSid);

    if (sessionOpt.isEmpty()) {
        return;
    }

    CallSession callSession = sessionOpt.get();

    // Build call result to pass to main workflow
    Map<String, Object> callResult = Map.of(
        "callSid", callSid,
        "callStatus", callSession.getCallStatus(),
        "subWorkflowStatus", subJob.getStatus().name(),
        "subWorkflowOutputs", subJob.getOutputs() != null ? subJob.getOutputs() : Map.of()
    );

    // Signal main workflow via event or callback mechanism
    // This triggers continuation of the main workflow after the trigger
    triggerCompletionService.completeTrigger(
        callSession.getWorkflowExecutionId(),
        callResult
    );
}
```

---

### Phase 4: Call Lifecycle Management

#### Task 4.1: Add isCallActive Method to CallSessionRegistry

**File:** Modify `CallSessionRegistry.java`

```java
/**
 * Check if a call is still active (not in terminal state).
 */
public boolean isCallActive(String callSid) {
    if (callSid == null) {
        return false;
    }

    CallSession session = callSessions.getIfPresent(callSid);

    if (session == null) {
        return false;
    }

    String status = session.getCallStatus();

    // Terminal states
    return status != null &&
           !status.equalsIgnoreCase("completed") &&
           !status.equalsIgnoreCase("failed") &&
           !status.equalsIgnoreCase("busy") &&
           !status.equalsIgnoreCase("no-answer") &&
           !status.equalsIgnoreCase("canceled");
}
```

#### Task 4.2: Enhance TwilioCallbackController for Sub-Workflow Termination

**File:** Modify `TwilioCallbackController.java`

```java
@PostMapping("/status")
public ResponseEntity<String> handleStatusCallback(@RequestParam Map<String, String> params) {
    String callSid = params.get("CallSid");
    String callStatus = params.get("CallStatus");

    // ... existing validation ...

    callSessionRegistry.updateCallStatus(callSid, callStatus);

    // If call ended, trigger sub-workflow termination
    if (isTerminalStatus(callStatus)) {
        Optional<CallSession> sessionOpt = callSessionRegistry.getSessionByCallSid(callSid);

        if (sessionOpt.isPresent()) {
            CallSession session = sessionOpt.get();
            Long subJobId = session.getSubJobId();

            if (subJobId != null) {
                logger.info("Call ended, requesting sub-workflow stop: callSid={}, subJobId={}",
                    callSid, subJobId);

                // Stop the sub-workflow via JobSyncExecutor
                jobSyncExecutor.stopJob(subJobId);
            }
        }
    }

    return ResponseEntity.ok("OK");
}

private boolean isTerminalStatus(String status) {
    return status != null && (
        status.equalsIgnoreCase("completed") ||
        status.equalsIgnoreCase("failed") ||
        status.equalsIgnoreCase("busy") ||
        status.equalsIgnoreCase("no-answer") ||
        status.equalsIgnoreCase("canceled")
    );
}
```

Add `JobSyncExecutor` dependency:
```java
private final JobSyncExecutor jobSyncExecutor;
```

---

### Phase 5: Trigger Completion Service

#### Task 5.1: Create TriggerCompletionService Interface

**File:** `server/libs/platform/platform-workflow/platform-workflow-coordinator/platform-workflow-coordinator-api/src/main/java/com/bytechef/platform/workflow/coordinator/TriggerCompletionService.java`

```java
package com.bytechef.platform.workflow.coordinator;

import java.util.Map;

/**
 * Service for signaling trigger completion and continuing main workflow execution.
 */
public interface TriggerCompletionService {

    /**
     * Signals that a real-time trigger has completed and the main workflow should continue.
     *
     * @param workflowExecutionId the workflow execution ID
     * @param triggerOutput the output from the trigger (call results, etc.)
     */
    void completeTrigger(String workflowExecutionId, Map<String, Object> triggerOutput);
}
```

#### Task 5.2: Create TriggerCompletionServiceImpl

**File:** `server/libs/platform/platform-workflow/platform-workflow-coordinator/platform-workflow-coordinator-impl/src/main/java/com/bytechef/platform/workflow/coordinator/TriggerCompletionServiceImpl.java`

```java
package com.bytechef.platform.workflow.coordinator;

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class TriggerCompletionServiceImpl implements TriggerCompletionService {

    private final JobFacade jobFacade;
    private final TriggerExecutionService triggerExecutionService;

    public TriggerCompletionServiceImpl(
        JobFacade jobFacade,
        TriggerExecutionService triggerExecutionService) {

        this.jobFacade = jobFacade;
        this.triggerExecutionService = triggerExecutionService;
    }

    @Override
    public void completeTrigger(String workflowExecutionId, Map<String, Object> triggerOutput) {
        // Parse workflow execution ID
        WorkflowExecutionId executionId = WorkflowExecutionId.parse(workflowExecutionId);

        // Update trigger execution with output
        triggerExecutionService.updateTriggerOutput(executionId, triggerOutput);

        // Create job to continue main workflow execution
        JobParametersDTO jobParams = new JobParametersDTO(
            executionId.getWorkflowId(),
            null,
            triggerOutput
        );

        // Start main workflow job (async)
        jobFacade.createJob(jobParams);
    }
}
```

---

### Phase 6: Testing

#### Task 6.1: Create Unit Tests for TwilioInboundCallTrigger

**File:** `server/libs/modules/components/twilio/src/test/java/com/bytechef/component/twilio/trigger/TwilioInboundCallTriggerTest.java`

Test cases:
- Verify TwiML response generation
- Verify CallSid extraction from body
- Verify sub-workflow ID stored in context
- Verify WebSocket URL construction

#### Task 6.2: Create Integration Tests for Call Flow

**File:** `server/libs/platform/platform-webhook/platform-websocket-webhook-rest/src/test/java/com/bytechef/platform/webhook/web/websocket/TwilioCallWebSocketIntTest.java`

Test cases:
- Full call lifecycle: webhook → WebSocket → sub-workflow → completion
- Call termination mid-workflow
- Status callback handling
- Error scenarios

#### Task 6.3: Create Tests for CallSessionRegistry

**File:** `server/libs/platform/platform-webhook/platform-websocket-webhook-rest/src/test/java/com/bytechef/platform/webhook/web/websocket/CallSessionRegistryTest.java`

Test cases:
- Session registration and retrieval
- Status updates
- `isCallActive()` for various statuses
- Session cleanup

---

## 3. File Summary

### New Files to Create

| File | Module |
|------|--------|
| `TwilioInboundCallTrigger.java` | `components/twilio` |
| `TwiMLWebhookResponse.java` | `components/twilio` |
| `RealtimeWebhookTrigger.java` | `components-api` |
| `TriggerCompletionService.java` | `platform-workflow-coordinator-api` |
| `TriggerCompletionServiceImpl.java` | `platform-workflow-coordinator-impl` |
| `TwilioInboundCallTriggerTest.java` | `components/twilio` (test) |
| `TwilioCallWebSocketIntTest.java` | `platform-websocket-webhook-rest` (test) |
| `CallSessionRegistryTest.java` | `platform-websocket-webhook-rest` (test) |

### Files to Modify

| File | Changes |
|------|---------|
| `TwilioComponentHandler.java` | Register new trigger |
| `CallSessionRegistry.java` | Add sub-workflow fields, `isCallActive()` |
| `WebhookWebSocketHandler.java` | Sub-workflow execution, JobFacade integration |
| `TwilioCallbackController.java` | Sub-workflow termination on call end |

---

## 4. Execution Order

1. **Phase 1** - Core interfaces (RealtimeWebhookTrigger, CallSession extensions)
2. **Phase 2** - Twilio trigger implementation (TwilioInboundCallTrigger, TwiMLWebhookResponse)
3. **Phase 3** - WebSocket handler enhancements (sub-workflow execution)
4. **Phase 4** - Call lifecycle management (status callbacks, termination)
5. **Phase 5** - Trigger completion service (main workflow continuation)
6. **Phase 6** - Testing

---

## 5. Verification

### Manual Testing Steps

1. Configure Twilio phone number webhook URL to ByteChef endpoint
2. Create main workflow with Twilio Inbound Call trigger
3. Create sub-workflow for real-time conversation handling
4. Make test call to the configured number
5. Verify:
   - TwiML response received by Twilio
   - WebSocket connection established
   - Sub-workflow executes during call
   - Call status updates received
   - Sub-workflow stops when call ends
   - Main workflow continues after call

### Automated Tests

```bash
# Run unit tests
./gradlew :server:libs:modules:components:twilio:test

# Run integration tests
./gradlew :server:libs:platform:platform-webhook:platform-websocket-webhook-rest:testIntegration

# Run all tests
./gradlew check
```

---

## 6. Configuration

### Twilio Console Setup

1. Get a Twilio phone number
2. Configure webhook URL: `https://{bytechef-host}/webhooks/{workflowExecutionId}`
3. Configure status callback URL: `https://{bytechef-host}/webhooks/twilio/status`
4. Enable Programmable Voice

### ByteChef Workflow Configuration

Main workflow:
```yaml
triggers:
  - type: twilio/inboundCall
    name: incomingCall
    parameters:
      subWorkflow: "ai-conversation-workflow-id"

tasks:
  - type: salesforce/createRecord
    name: logCall
    parameters:
      object: Task
      fields:
        Subject: "Call from ${incomingCall.from}"
        Status: "Completed"
```

Sub-workflow (real-time):
```yaml
triggers:
  - type: manual/start

tasks:
  - type: openai/chat
    name: aiResponse
    parameters:
      model: gpt-4
      messages:
        - role: system
          content: "You are a helpful phone assistant..."
```
