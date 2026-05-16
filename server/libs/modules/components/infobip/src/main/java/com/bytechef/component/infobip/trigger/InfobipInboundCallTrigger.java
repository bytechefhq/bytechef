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

package com.bytechef.component.infobip.trigger;

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
import com.bytechef.component.infobip.util.InfobipSignatureValidator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Infobip inbound voice call trigger that acknowledges the incoming call event and hands the call to the platform
 * WebSocket bridge for real-time media streaming.
 *
 * <p>
 * Unlike Twilio (which returns TwiML with an inline {@code <Stream>} element), Infobip drives inbound media streaming
 * through a Calls configuration whose media-stream config already points at the platform WebSocket bridge. This trigger
 * therefore receives the inbound-call webhook event, records the call context so the bridge can resolve the
 * sub-workflow pipeline, and returns the call data as trigger output.
 * </p>
 *
 * <p>
 * The Infobip inbound-call event field names below follow Infobip's publicly documented Calls webhook conventions and
 * are marked as <em>inferred</em> where they could not be verified against the live API reference.
 * </p>
 *
 * @author Ivica Cardic
 */
public class InfobipInboundCallTrigger {

    public static final String SUB_WORKFLOW = "subWorkflow";
    public static final String SIGNATURE_SECRET = "signatureSecret";

    // Header carrying the HMAC signature on signed Infobip webhooks. The exact name could not be verified against
    // Infobip's live API reference and should be confirmed against the account's webhook configuration.
    private static final String SIGNATURE_HEADER = "X-Signature";

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("inboundCall")
        .title("Inbound Voice Call")
        .description(
            "Triggers when an inbound voice call is received via Infobip. The call is streamed to a real-time " +
                "workflow for audio processing and AI conversation over media streaming.")
        .type(TriggerType.STATIC_WEBHOOK)
        .properties(
            string(SUB_WORKFLOW)
                .label("Real-Time Workflow")
                .description(
                    "The workflow ID to execute synchronously during the phone call. " +
                        "This workflow handles real-time audio processing and AI responses.")
                .required(true),
            string(SIGNATURE_SECRET)
                .label("Webhook Signature Secret")
                .description(
                    "Optional shared secret used to verify the HMAC signature Infobip sends on signed webhooks. " +
                        "When set, requests with a missing or invalid signature are rejected. Leave empty to skip " +
                        "signature verification.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("callId").description("Unique identifier for the call"),
                        string("from").description("Caller phone number"),
                        string("to").description("Called phone number"),
                        string("direction").description("Call direction (INBOUND)"),
                        string("state").description("Call state"))))
        .webhookValidate(InfobipInboundCallTrigger::webhookValidate)
        .webhookRequest(InfobipInboundCallTrigger::webhookRequest);

    private InfobipInboundCallTrigger() {
    }

    /**
     * Acknowledges the inbound-call event with a 200 response. Infobip retries webhook deliveries that are not
     * acknowledged, so a plain 200 is returned once the call context has been recorded.
     */
    protected static WebhookValidateResponse webhookValidate(
        Parameters inputParameters, HttpHeaders headers, HttpParameters parameters, WebhookBody body,
        WebhookMethod method, TriggerContext context) {

        String signatureSecret = inputParameters.getString(SIGNATURE_SECRET);

        // Opt-in signature verification: only enforced when a signing secret is configured, mirroring the Twilio
        // status-callback validation. Reject requests whose HMAC signature does not match the raw body.
        if (signatureSecret != null && !signatureSecret.isBlank()) {
            String signature = headers.firstValue(SIGNATURE_HEADER)
                .orElse(null);

            if (!InfobipSignatureValidator.isValid(signatureSecret, body.getRawContent(), signature)) {
                return new WebhookValidateResponse("", Map.of(), 403);
            }
        }

        Map<String, Object> bodyContent = extractBodyContent(body);

        // Inferred field name: Infobip Calls webhook events carry the call id under "callId".
        String callId = (String) bodyContent.get("callId");

        if (callId == null || callId.isBlank()) {
            return WebhookValidateResponse.badRequest();
        }

        String subWorkflowId = inputParameters.getString(SUB_WORKFLOW);

        storeCallContext(context, callId, subWorkflowId, bodyContent);

        return new WebhookValidateResponse("", Map.of("Content-Type", List.of("application/json")), 200);
    }

    /**
     * Processes the webhook request and returns the call data as trigger output.
     */
    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters webhookEnableOutput, TriggerContext context) {

        Map<String, Object> bodyContent = extractBodyContent(body);

        Map<String, Object> output = new LinkedHashMap<>();

        output.put("callId", bodyContent.get("callId"));
        output.put("from", bodyContent.get("from"));
        output.put("to", bodyContent.get("to"));
        output.put("direction", bodyContent.get("direction"));
        output.put("state", bodyContent.get("state"));

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

    private static void storeCallContext(
        TriggerContext context, String callId, String subWorkflowId, Map<String, Object> bodyContent) {

        Map<String, Object> callContext = new LinkedHashMap<>();

        callContext.put("callId", callId);
        callContext.put("subWorkflowId", subWorkflowId);
        callContext.put("from", bodyContent.get("from"));
        callContext.put("to", bodyContent.get("to"));
        callContext.put("direction", bodyContent.get("direction"));

        context.data(data -> data.put(
            TriggerContext.Data.Scope.WORKFLOW,
            "callSession:" + callId,
            callContext));
    }
}
