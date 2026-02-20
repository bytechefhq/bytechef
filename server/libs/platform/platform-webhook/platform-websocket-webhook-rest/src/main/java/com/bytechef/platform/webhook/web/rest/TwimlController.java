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

package com.bytechef.platform.webhook.web.rest;

import com.bytechef.platform.webhook.web.websocket.CallSessionRegistry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for serving TwiML responses for outbound Twilio calls.
 *
 * <p>
 * This controller provides the TwiML endpoint that Twilio calls when an outbound call is answered. It returns TwiML
 * that instructs Twilio to connect via WebSocket for real-time audio streaming.
 * </p>
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("/webhooks")
class TwimlController {

    private static final Logger logger = LoggerFactory.getLogger(TwimlController.class);

    private final CallSessionRegistry callSessionRegistry;
    private final String publicUrl;

    @SuppressFBWarnings("EI")
    TwimlController(
        CallSessionRegistry callSessionRegistry,
        @Value("${bytechef.webhook.url:}") String publicUrl) {

        this.callSessionRegistry = callSessionRegistry;
        this.publicUrl = publicUrl;
    }

    /**
     * Serves TwiML for outbound calls when Twilio connects after the call is answered.
     *
     * <p>
     * <b>Security Note:</b> CSRF protection is intentionally disabled for this endpoint. Twilio webhook callbacks
     * cannot include CSRF tokens. Security is maintained through Twilio's request signature validation.
     *
     * @param workflowExecutionId the workflow execution ID
     * @param callSid             the Twilio call SID
     * @param callRef             the call reference for pending calls
     * @param subWorkflowId       the sub-workflow ID to execute during the call
     * @return TwiML response with WebSocket stream configuration
     */
    @SuppressFBWarnings({
        "CRLF_INJECTION_LOGS", "SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING"
    })
    @RequestMapping(
        value = "/{workflowExecutionId}/twiml",
        method = {
            RequestMethod.GET, RequestMethod.POST
        },
        produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> serveTwiml(
        @PathVariable String workflowExecutionId,
        @RequestParam(required = false) String callSid,
        @RequestParam(required = false) String callRef,
        @RequestParam(required = false) String subWorkflowId) {

        if (logger.isDebugEnabled()) {
            logger.debug(
                "TwiML request: workflowExecutionId={}, callSid={}, callRef={}, subWorkflowId={}",
                workflowExecutionId, callSid, callRef, subWorkflowId);
        }

        if (publicUrl == null || publicUrl.isBlank()) {
            logger.error("Public URL not configured. Cannot serve TwiML.");

            return ResponseEntity.internalServerError()
                .body(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response><Say>Error: Server configuration issue</Say></Response>");
        }

        // If callSid is provided by Twilio, update the pending session
        if (callSid != null && callRef != null) {
            callSessionRegistry.updateCallSid(callRef, callSid);
        }

        String effectiveCallSid = callSid != null ? callSid : callRef;

        if (effectiveCallSid == null) {
            logger.warn("Missing callSid and callRef in TwiML request");

            return ResponseEntity.badRequest()
                .body(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response><Say>Error: Missing call identifier</Say></Response>");
        }

        String twiml = buildTwimlResponse(publicUrl, workflowExecutionId, effectiveCallSid, subWorkflowId);

        return ResponseEntity.ok(twiml);
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private static String buildTwimlResponse(
        String publicUrl, String workflowExecutionId, String callSid, String subWorkflowId) {

        String wsUrl = publicUrl.replace("https://", "wss://")
            .replace("http://", "ws://")
            + "/webhooks/" + workflowExecutionId + "/wss?callSid=" + callSid;

        if (subWorkflowId != null) {
            wsUrl += "&subWorkflowId=" + subWorkflowId;
        }

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
