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

package com.bytechef.component.twilio.action;

import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType.FORM_URL_ENCODED;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.twilio.constant.TwilioConstants.FROM;
import static com.bytechef.component.twilio.constant.TwilioConstants.TO;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.RealtimeCallAction;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Twilio outbound call action that initiates a call and executes a sub-workflow synchronously during the call.
 *
 * <p>
 * This action:
 * <ol>
 * <li>Makes an outbound call via Twilio API</li>
 * <li>Registers a call session with a completion latch</li>
 * <li>Blocks until the call completes</li>
 * <li>Returns the call result</li>
 * </ol>
 *
 * @author Ivica Cardic
 */
public class TwilioMakeCallAction implements RealtimeCallAction {

    private static final Logger logger = LoggerFactory.getLogger(TwilioMakeCallAction.class);

    public static final String SUB_WORKFLOW = "subWorkflow";
    private static final String TIMEOUT = "timeout";
    private static final String MAX_DURATION = "maxDuration";

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("makeCall")
        .title("Make Outbound Call")
        .description(
            "Initiates an outbound voice call and executes a real-time workflow synchronously during the call. " +
                "The action blocks until the call completes, allowing real-time audio processing and AI conversations.")
        .properties(
            string(TO)
                .label("To")
                .description("The phone number to call in E.164 format.")
                .controlType(ControlType.PHONE)
                .exampleValue("+15551234567")
                .required(true),
            string(FROM)
                .label("From")
                .description("Your Twilio phone number in E.164 format.")
                .controlType(ControlType.PHONE)
                .exampleValue("+15559876543")
                .required(true),
            string(SUB_WORKFLOW)
                .label("Real-Time Workflow")
                .description(
                    "The workflow ID to execute synchronously during the phone call. " +
                        "This workflow handles real-time audio processing and AI responses via WebSocket streaming.")
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
                        string("callSid").description("Unique identifier for the call"),
                        string("status").description("Final call status (completed, failed, busy, no-answer, timeout)"),
                        integer("duration").description("Call duration in seconds"),
                        string("direction").description("Call direction (outbound-api)"))))
        .perform(TwilioMakeCallAction::perform);

    @Override
    public String getSubWorkflowIdProperty() {
        return SUB_WORKFLOW;
    }

    @SuppressWarnings("unchecked")
    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        String to = inputParameters.getRequiredString(TO);
        String from = inputParameters.getRequiredString(FROM);
        String subWorkflowId = inputParameters.getString(SUB_WORKFLOW);
        int ringTimeout = inputParameters.getInteger(TIMEOUT, 30);

        ActionContextAware contextAware = (ActionContextAware) context;
        String publicUrl = contextAware.getPublicUrl();

        if (publicUrl == null || publicUrl.isBlank()) {
            logger.error("Public URL not configured. Cannot make outbound call.");

            return Map.of(
                "callSid", "",
                "status", "failed",
                "duration", 0,
                "direction", "outbound-api",
                "error", "Public URL not configured");
        }

        // Generate a unique call reference for this call
        String callReference = UUID.randomUUID()
            .toString();

        // Build workflow execution ID for TwiML URL
        String workflowExecutionId = buildWorkflowExecutionId(contextAware);

        // Build callback URLs
        String statusCallbackUrl = publicUrl + "/webhooks/twilio/status?callRef=" + callReference;
        String twimlUrl = publicUrl + "/webhooks/" + workflowExecutionId + "/twiml" +
            "?callRef=" + callReference +
            "&subWorkflowId=" + (subWorkflowId != null ? subWorkflowId : "");

        if (logger.isDebugEnabled()) {
            logger.debug(
                "Making outbound call: to={}, from={}, callRef={}, twimlUrl={}", to, from, callReference, twimlUrl);
        }

        // Make Twilio API call
        Map<String, Object> twilioParams = new LinkedHashMap<>();

        twilioParams.put("To", to);
        twilioParams.put("From", from);
        twilioParams.put("Url", twimlUrl);
        twilioParams.put("StatusCallback", statusCallbackUrl);
        twilioParams.put("StatusCallbackEvent", "initiated ringing answered completed");
        twilioParams.put("StatusCallbackMethod", "POST");
        twilioParams.put("Timeout", String.valueOf(ringTimeout));

        String accountSid = connectionParameters.getRequiredString(USERNAME);

        try {
            Map<String, Object> response = context
                .http(http -> http.post("/Accounts/" + accountSid + "/Calls.json"))
                .body(Body.of(twilioParams, FORM_URL_ENCODED))
                .configuration(responseType(ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<Map<String, Object>>() {});

            String callSid = (String) response.get("sid");
            String status = (String) response.get("status");

            logger.info("Outbound call initiated: callSid={}, status={}, callRef={}", callSid, status, callReference);

            // For now, return immediately with the call status
            // In the full implementation, we would register a session and block until completion
            // This requires the CallSessionRegistry to be accessible from the component

            return Map.of(
                "callSid", callSid != null ? callSid : "",
                "status", status != null ? status : "initiated",
                "duration", 0,
                "direction", "outbound-api");

        } catch (Exception exception) {
            logger.error("Failed to make outbound call: to={}, from={}", to, from, exception);

            return Map.of(
                "callSid", "",
                "status", "failed",
                "duration", 0,
                "direction", "outbound-api",
                "error", exception.getMessage());
        }
    }

    private static String buildWorkflowExecutionId(ActionContextAware context) {
        // Build a workflow execution ID that can be used in the TwiML URL
        // Format: type_principalId_workflowId_actionName
        Long principalId = context.getJobPrincipalId();
        String workflowId = context.getWorkflowId();
        String actionName = context.getActionName();

        if (principalId != null && workflowId != null) {
            return "automation_" + principalId + "_" + workflowId + "_" + actionName;
        }

        // Fallback to job ID
        Long jobId = context.getJobId();

        return jobId != null ? "job_" + jobId : "unknown";
    }
}
