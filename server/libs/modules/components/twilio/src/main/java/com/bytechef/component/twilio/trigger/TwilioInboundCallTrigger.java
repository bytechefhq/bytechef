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

package com.bytechef.component.twilio.trigger;

import static com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Twilio inbound voice call trigger that returns TwiML with WebSocket streaming configuration.
 *
 * <p>
 * This trigger handles incoming Twilio voice calls and responds with TwiML that instructs Twilio to connect via
 * WebSocket for real-time audio streaming. The sub-workflow is then executed synchronously during the call via the
 * WebSocket handler.
 * </p>
 *
 * @author Ivica Cardic
 */
public class TwilioInboundCallTrigger {

    public static final String SUB_WORKFLOW = "subWorkflow";

    // Platform header names - must match AbstractWebhookTriggerController constants
    private static final String HEADER_WORKFLOW_EXECUTION_ID = "X-ByteChef-Workflow-Execution-Id";
    private static final String HEADER_PUBLIC_URL = "X-ByteChef-Public-Url";

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("inboundCall")
        .title("Inbound Voice Call")
        .description(
            "Triggers when an inbound voice call is received. Returns TwiML that establishes " +
                "a WebSocket connection for real-time audio streaming and AI conversation.")
        .type(TriggerType.STATIC_WEBHOOK)
        .properties(
            string(SUB_WORKFLOW)
                .label("Real-Time Workflow")
                .description(
                    "The workflow ID to execute synchronously during the phone call. " +
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
                        string("callStatus").description("Call status"))))
        .webhookValidate(TwilioInboundCallTrigger::webhookValidate)
        .webhookRequest(TwilioInboundCallTrigger::webhookRequest);

    private TwilioInboundCallTrigger() {
    }

    /**
     * Validates the incoming Twilio webhook and returns TwiML response with WebSocket stream configuration.
     */
    protected static WebhookValidateResponse webhookValidate(
        Parameters inputParameters, HttpHeaders headers, HttpParameters parameters, WebhookBody body,
        WebhookMethod method, TriggerContext context) {

        Map<String, Object> bodyContent = extractBodyContent(body);

        String callSid = (String) bodyContent.get("CallSid");

        if (callSid == null || callSid.isBlank()) {
            return WebhookValidateResponse.badRequest();
        }

        // Extract platform-provided headers
        String workflowExecutionId = getFirstHeaderValue(headers, HEADER_WORKFLOW_EXECUTION_ID);
        String publicUrl = getFirstHeaderValue(headers, HEADER_PUBLIC_URL);

        if (workflowExecutionId == null || publicUrl == null) {
            // Platform headers not available - cannot generate proper TwiML
            return WebhookValidateResponse.badRequest();
        }

        String subWorkflowId = inputParameters.getString(SUB_WORKFLOW);

        // Store call context for WebSocket handler
        storeCallContext(context, callSid, subWorkflowId, bodyContent);

        // Build TwiML response with WebSocket stream
        String twiml = buildTwimlResponse(publicUrl, workflowExecutionId, callSid, subWorkflowId);

        return new WebhookValidateResponse(
            twiml,
            Map.of("Content-Type", List.of("application/xml")),
            200);
    }

    /**
     * Processes the webhook request and returns the call data as trigger output.
     */
    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters webhookEnableOutput, TriggerContext context) {

        Map<String, Object> bodyContent = extractBodyContent(body);

        Map<String, Object> output = new LinkedHashMap<>();

        output.put("callSid", bodyContent.get("CallSid"));
        output.put("from", bodyContent.get("From"));
        output.put("to", bodyContent.get("To"));
        output.put("direction", bodyContent.get("Direction"));
        output.put("accountSid", bodyContent.get("AccountSid"));
        output.put("callStatus", bodyContent.get("CallStatus"));

        return output;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractBodyContent(WebhookBody body) {
        Object content = body.getContent();

        if (content instanceof Map) {
            return (Map<String, Object>) content;
        }

        return Map.of();
    }

    private static String getFirstHeaderValue(HttpHeaders headers, String headerName) {
        return headers.firstValue(headerName)
            .orElse(
                headers.firstValue(headerName.toLowerCase())
                    .orElse(null));
    }

    private static void storeCallContext(
        TriggerContext context, String callSid, String subWorkflowId, Map<String, Object> bodyContent) {

        Map<String, Object> callContext = new LinkedHashMap<>();

        callContext.put("callSid", callSid);
        callContext.put("subWorkflowId", subWorkflowId);
        callContext.put("from", bodyContent.get("From"));
        callContext.put("to", bodyContent.get("To"));
        callContext.put("direction", bodyContent.get("Direction"));
        callContext.put("accountSid", bodyContent.get("AccountSid"));

        context.data(data -> data.put(
            TriggerContext.Data.Scope.WORKFLOW,
            "callSession:" + callSid,
            callContext));
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private static String buildTwimlResponse(
        String publicUrl, String workflowExecutionId, String callSid, String subWorkflowId) {

        String wsUrl = publicUrl.replace("https://", "wss://")
            .replace("http://", "ws://")
            + "/webhooks/" + workflowExecutionId + "/wss?callSid=" + callSid + "&subWorkflowId=" + subWorkflowId;

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
}
