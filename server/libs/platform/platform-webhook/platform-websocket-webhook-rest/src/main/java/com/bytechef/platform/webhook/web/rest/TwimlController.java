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
import com.bytechef.platform.webhook.web.websocket.TwilioStreamToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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

    private static final Logger log = LoggerFactory.getLogger(TwimlController.class);

    private static final long STREAM_TOKEN_TTL_SECONDS = 3600L;

    private final CallSessionRegistry callSessionRegistry;
    private final String publicUrl;
    private final String twilioAuthToken;
    private final String streamTokenSecret;

    @SuppressFBWarnings("EI")
    TwimlController(
        CallSessionRegistry callSessionRegistry,
        @Value("${bytechef.webhook.url:}") String publicUrl,
        @Value("${bytechef.twilio.auth-token:}") String twilioAuthToken,
        @Value("${bytechef.twilio.stream-token.secret:}") String streamTokenSecret) {

        this.callSessionRegistry = callSessionRegistry;
        this.publicUrl = publicUrl;
        this.twilioAuthToken = twilioAuthToken;
        this.streamTokenSecret = streamTokenSecret;
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
        @RequestParam(required = false) String subWorkflowId,
        @RequestParam Map<String, String> allParams,
        HttpServletRequest request) {

        if (!isValidTwilioRequest(request, allParams)) {
            log.warn("Rejected TwiML request with invalid Twilio signature");

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .build();
        }

        if (log.isDebugEnabled()) {
            log.debug(
                "TwiML request: workflowExecutionId={}, callSid={}, callRef={}, subWorkflowId={}",
                workflowExecutionId, callSid, callRef, subWorkflowId);
        }

        if (publicUrl == null || publicUrl.isBlank()) {
            log.error("Public URL not configured. Cannot serve TwiML.");

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
            log.warn("Missing callSid and callRef in TwiML request");

            return ResponseEntity.badRequest()
                .body(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response><Say>Error: Missing call identifier</Say></Response>");
        }

        String twiml = buildTwimlResponse(
            publicUrl, workflowExecutionId, effectiveCallSid, subWorkflowId, streamTokenSecret);

        return ResponseEntity.ok(twiml);
    }

    /**
     * Opt-in Twilio request-signature validation, identical to the status-callback controller: when
     * {@code bytechef.twilio.auth-token} is configured, requests with a missing or invalid {@code X-Twilio-Signature}
     * are rejected; when it is unset, validation is skipped (unchanged behavior).
     */
    private boolean isValidTwilioRequest(HttpServletRequest request, Map<String, String> params) {
        if (twilioAuthToken == null || twilioAuthToken.isBlank()) {
            return true;
        }

        String queryString = request.getQueryString();
        String url = publicUrl + request.getRequestURI() + (queryString == null ? "" : "?" + queryString);

        return TwilioSignatureValidator.isValid(
            twilioAuthToken, url, params, request.getHeader("X-Twilio-Signature"));
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private static String buildTwimlResponse(
        String publicUrl, String workflowExecutionId, String callSid, String subWorkflowId,
        String streamTokenSecret) {

        // URL-encode every attacker-influenced value placed into a URL so it cannot inject extra path segments or query
        // parameters, then XML-encode the resulting attribute values so they cannot break out of the TwiML markup.
        String wsUrl = publicUrl.replace("https://", "wss://")
            .replace("http://", "ws://")
            + "/webhooks/" + urlEncode(workflowExecutionId) + "/wss?callSid=" + urlEncode(callSid);

        if (subWorkflowId != null) {
            wsUrl += "&subWorkflowId=" + urlEncode(subWorkflowId);
        }

        // When configured, bind the media-stream WebSocket to this callSid with a signed, expiring token — a WebSocket
        // upgrade cannot carry X-Twilio-Signature, so the token is what the handler verifies on connect.
        if (streamTokenSecret != null && !streamTokenSecret.isBlank()) {
            String streamToken = TwilioStreamToken.mint(
                streamTokenSecret, callSid, STREAM_TOKEN_TTL_SECONDS,
                Instant.now()
                    .getEpochSecond());

            wsUrl += "&streamToken=" + urlEncode(streamToken);
        }

        String statusCallbackUrl = publicUrl + "/webhooks/twilio/status?callSid=" + urlEncode(callSid);

        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <Response>
                <Connect action="%s">
                    <Stream url="%s" statusCallback="%s">
                        <Parameter name="callSid" value="%s"/>
                    </Stream>
                </Connect>
            </Response>
            """.formatted(
            escapeXml(statusCallbackUrl), escapeXml(wsUrl), escapeXml(statusCallbackUrl), escapeXml(callSid));
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}
