/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.infobip.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.infobip.constant.InfobipConstants.FROM;
import static com.bytechef.component.infobip.constant.InfobipConstants.TO;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.RealtimeCallAction;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Infobip outbound call action that initiates a call and executes a sub-workflow synchronously during the call.
 *
 * <p>
 * This action mirrors {@code TwilioMakeCallAction} but targets Infobip's Calls API. It:
 * <ol>
 * <li>Registers a media-stream configuration that points at the platform WebSocket bridge</li>
 * <li>Places an outbound call via the Calls API</li>
 * <li>Once the call is established, starts bidirectional media streaming (raw PCM) toward the bridge</li>
 * <li>Blocks by polling the call state until the call reaches a terminal state</li>
 * <li>Returns the call outcome</li>
 * </ol>
 *
 * <p>
 * The Infobip Calls API endpoints and request/response field names below follow Infobip's publicly documented
 * conventions. They are marked as <em>inferred</em> where they could not be verified against the live API reference and
 * should be confirmed against the account's Calls API version before production use.
 * </p>
 *
 * @author Ivica Cardic
 */
public class InfobipMakeCallAction implements RealtimeCallAction {

    private static final Logger log = LoggerFactory.getLogger(InfobipMakeCallAction.class);

    public static final String SUB_WORKFLOW = "subWorkflow";
    public static final String CALLS_CONFIGURATION_ID = "callsConfigurationId";
    public static final String APPLICATION_ID = "applicationId";
    private static final String TIMEOUT = "timeout";
    private static final String MAX_DURATION = "maxDuration";
    private static final long POLL_INTERVAL_MILLIS = 2000L;

    // Infobip call states (inferred from the documented Calls API state machine). FINISHED is the only terminal state;
    // failures are surfaced as FINISHED with an errorCode.
    private static final Set<String> TERMINAL_STATES = Set.of("FINISHED");
    private static final String STATE_ESTABLISHED = "ESTABLISHED";

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("makeCall")
        .title("Make Outbound Call")
        .description(
            "Initiates an outbound voice call via the Infobip Calls API and executes a real-time workflow " +
                "synchronously during the call. The action blocks until the call completes, allowing real-time " +
                "audio processing and AI conversations over media streaming.")
        .properties(
            string(TO)
                .label("To")
                .description("The phone number to call in E.164 format.")
                .controlType(ControlType.PHONE)
                .exampleValue("+15551234567")
                .required(true),
            string(FROM)
                .label("From")
                .description("The caller ID (your Infobip voice number) in E.164 format.")
                .controlType(ControlType.PHONE)
                .exampleValue("+15559876543")
                .required(true),
            string(CALLS_CONFIGURATION_ID)
                .label("Calls Configuration ID")
                .description(
                    "The Infobip Calls configuration ID that the outbound call is placed under. Created in the " +
                        "Infobip portal or via the Calls API.")
                .required(true),
            string(APPLICATION_ID)
                .label("Application ID")
                .description("Optional Infobip application (subaccount) ID the call belongs to.")
                .required(false),
            string(SUB_WORKFLOW)
                .label("Real-Time Workflow")
                .description(
                    "The workflow ID to execute synchronously during the phone call. " +
                        "This workflow handles real-time audio processing and AI responses via media streaming.")
                .required(true),
            integer(TIMEOUT)
                .label("Ring Timeout")
                .description(
                    "Maximum time in seconds to wait for the call to be answered. " +
                        "If not answered within this time, the call fails.")
                .defaultValue(30)
                .minValue(5)
                .maxValue(600)
                .required(false),
            integer(MAX_DURATION)
                .label("Max Call Duration")
                .description(
                    "Maximum duration in minutes to wait for the call to complete. " +
                        "After this time, the action returns with a timeout status.")
                .defaultValue(30)
                .minValue(1)
                .maxValue(120)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("callId").description("Unique identifier for the call"),
                        string("state").description("Final call state (FINISHED, timeout)"),
                        integer("duration").description("Call duration in seconds"),
                        string("direction").description("Call direction (OUTBOUND)"))))
        .perform(InfobipMakeCallAction::perform);

    @Override
    public String getSubWorkflowIdProperty() {
        return SUB_WORKFLOW;
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        String to = inputParameters.getRequiredString(TO);
        String from = inputParameters.getRequiredString(FROM);
        String callsConfigurationId = inputParameters.getRequiredString(CALLS_CONFIGURATION_ID);
        String applicationId = inputParameters.getString(APPLICATION_ID);
        String subWorkflowId = inputParameters.getString(SUB_WORKFLOW);

        ActionContextAware contextAware = (ActionContextAware) context;
        String publicUrl = contextAware.getPublicUrl();

        if (publicUrl == null || publicUrl.isBlank()) {
            log.error("Public URL not configured. Cannot make outbound call.");

            return outcome("", "failed", 0, "Public URL not configured");
        }

        // Generate a unique call reference the platform WebSocket bridge uses to resolve the sub-workflow pipeline.
        String callReference = UUID.randomUUID()
            .toString();

        String workflowExecutionId = buildWorkflowExecutionId(contextAware);
        String webSocketUrl = buildWebSocketUrl(publicUrl, workflowExecutionId, callReference, subWorkflowId);

        if (log.isDebugEnabled()) {
            log.debug(
                "Making outbound Infobip call: to={}, from={}, callRef={}, wsUrl={}", to, from, callReference,
                webSocketUrl);
        }

        try {
            // Register a media-stream configuration pointing at the platform WebSocket bridge. Infobip streams raw PCM
            // frames (preceded by a JSON metadata frame) to this URL; the existing binary bridge already handles that
            // format. Endpoint/fields are inferred from Infobip's Calls media-streaming conventions.
            String mediaStreamConfigId = createMediaStreamConfig(context, callReference, webSocketUrl);

            String callId = createCall(context, to, from, callsConfigurationId, applicationId);

            if (callId == null || callId.isBlank()) {
                return outcome("", "failed", 0, "Call was not created");
            }

            log.info("Outbound Infobip call initiated: callId={}, callRef={}", callId, callReference);

            // Block until the call reaches a terminal state (or the max duration elapses). Once established, media
            // streaming toward the bridge is started so the sub-workflow can process audio in real time.
            return awaitCallCompletion(
                context, callId, mediaStreamConfigId, inputParameters.getInteger(MAX_DURATION, 30));
        } catch (Exception exception) {
            log.error("Failed to make outbound Infobip call: to={}, from={}", to, from, exception);

            return outcome("", "failed", 0, exception.getMessage());
        }
    }

    /**
     * Creates a media-stream configuration pointing at the WebSocket bridge and returns its id. Inferred endpoint:
     * {@code POST /calls/1/media-stream-configs} with body {@code {name, url}} returning {@code {id}}.
     */
    private static String createMediaStreamConfig(ActionContext context, String name, String webSocketUrl) {
        Map<String, Object> body = new LinkedHashMap<>();

        body.put("name", "bytechef-" + name);
        body.put("url", webSocketUrl);

        Map<String, Object> response = context
            .http(http -> http.post("/calls/1/media-stream-configs"))
            .body(Http.Body.of(body))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<Map<String, Object>>() {});

        Object id = response.get("id");

        return id == null ? null : String.valueOf(id);
    }

    /**
     * Places the outbound call and returns its id. Inferred endpoint: {@code POST /calls/1/calls} with body
     * {@code {endpoint: {type: "PHONE", phoneNumber}, from, callsConfigurationId, platform: {applicationId}}} returning
     * {@code {id, state, direction}}.
     */
    private static String createCall(
        ActionContext context, String to, String from, String callsConfigurationId, String applicationId) {

        Map<String, Object> endpoint = new LinkedHashMap<>();

        endpoint.put("type", "PHONE");
        endpoint.put("phoneNumber", to);

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("endpoint", endpoint);
        body.put("from", from);
        body.put("callsConfigurationId", callsConfigurationId);

        if (applicationId != null && !applicationId.isBlank()) {
            body.put("platform", Map.of("applicationId", applicationId));
        }

        Map<String, Object> response = context
            .http(http -> http.post("/calls/1/calls"))
            .body(Http.Body.of(body))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<Map<String, Object>>() {});

        Object id = response.get("id");

        return id == null ? null : String.valueOf(id);
    }

    /**
     * Starts bidirectional media streaming for an established call. Inferred endpoint:
     * {@code POST /calls/1/calls/{callId}/start-media-stream} with body
     * {@code {mediaStream: {audioProperties: {mediaStreamConfigId}}}}.
     */
    private static void startMediaStream(ActionContext context, String callId, String mediaStreamConfigId) {
        if (mediaStreamConfigId == null || mediaStreamConfigId.isBlank()) {
            return;
        }

        Map<String, Object> body = Map.of(
            "mediaStream", Map.of("audioProperties", Map.of("mediaStreamConfigId", mediaStreamConfigId)));

        context
            .http(http -> http.post("/calls/1/calls/" + callId + "/start-media-stream"))
            .body(Http.Body.of(body))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();
    }

    /**
     * Blocks until the outbound call reaches a terminal state or the maximum duration elapses, by polling the call
     * resource. Media streaming is started once the call becomes established.
     */
    private static Map<String, Object> awaitCallCompletion(
        ActionContext context, String callId, String mediaStreamConfigId, int maxDurationMinutes) {

        long deadline = System.currentTimeMillis() + maxDurationMinutes * 60_000L;
        String state = "CALLING";
        int duration = 0;
        boolean streamStarted = false;

        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(POLL_INTERVAL_MILLIS);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread()
                    .interrupt();

                break;
            }

            // Inferred endpoint: GET /calls/1/calls/{callId} returning {state, duration, direction}.
            Map<String, Object> call = context
                .http(http -> http.get("/calls/1/calls/" + callId))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<Map<String, Object>>() {});

            Object stateValue = call.get("state");

            if (stateValue != null) {
                state = String.valueOf(stateValue);
            }

            Object durationValue = call.get("duration");

            if (durationValue != null) {
                duration = parseDuration(String.valueOf(durationValue));
            }

            if (!streamStarted && STATE_ESTABLISHED.equalsIgnoreCase(state)) {
                startMediaStream(context, callId, mediaStreamConfigId);

                streamStarted = true;
            }

            if (isTerminalState(state)) {
                break;
            }
        }

        return Map.of(
            "callId", callId,
            "state", isTerminalState(state) ? state : "timeout",
            "duration", duration,
            "direction", "OUTBOUND");
    }

    private static String buildWorkflowExecutionId(ActionContextAware context) {
        Long principalId = context.getJobPrincipalId();
        String workflowId = context.getWorkflowId();
        String actionName = context.getActionName();

        if (principalId != null && workflowId != null) {
            return "automation_" + principalId + "_" + workflowId + "_" + actionName;
        }

        Long jobId = context.getJobId();

        return jobId != null ? "job_" + jobId : "unknown";
    }

    private static String buildWebSocketUrl(
        String publicUrl, String workflowExecutionId, String callReference, String subWorkflowId) {

        String base = publicUrl.replace("https://", "wss://")
            .replace("http://", "ws://");

        return base + "/webhooks/" + workflowExecutionId + "/wss?callSid=" + callReference +
            "&subWorkflowId=" + (subWorkflowId != null ? subWorkflowId : "");
    }

    private static Map<String, Object> outcome(String callId, String state, int duration, String error) {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("callId", callId);
        result.put("state", state);
        result.put("duration", duration);
        result.put("direction", "OUTBOUND");

        if (error != null) {
            result.put("error", error);
        }

        return result;
    }

    static boolean isTerminalState(String state) {
        return state != null && TERMINAL_STATES.contains(state.toUpperCase());
    }

    private static int parseDuration(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException numberFormatException) {
            return 0;
        }
    }
}
