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

import com.bytechef.platform.webhook.voice.VoiceSessionEngine;
import com.bytechef.platform.webhook.web.websocket.CallSessionRegistry;
import com.bytechef.platform.webhook.web.websocket.CallSessionRegistry.CallSession;
import com.bytechef.platform.webhook.web.websocket.WorkflowContinuationHelper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling Twilio webhook callbacks related to call status updates.
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("/webhooks/twilio")
class TwilioCallbackController {

    private static final Logger log = LoggerFactory.getLogger(TwilioCallbackController.class);

    private final CallSessionRegistry callSessionRegistry;
    private final VoiceSessionEngine voiceSessionEngine;
    private final String publicUrl;
    private final String twilioAuthToken;
    private final WorkflowContinuationHelper workflowContinuationHelper;

    @SuppressFBWarnings("EI")
    TwilioCallbackController(
        CallSessionRegistry callSessionRegistry, VoiceSessionEngine voiceSessionEngine,
        WorkflowContinuationHelper workflowContinuationHelper,
        @Value("${bytechef.webhook.url:}") String publicUrl,
        @Value("${bytechef.twilio.auth-token:}") String twilioAuthToken) {

        this.callSessionRegistry = callSessionRegistry;
        this.voiceSessionEngine = voiceSessionEngine;
        this.publicUrl = publicUrl;
        this.twilioAuthToken = twilioAuthToken;
        this.workflowContinuationHelper = workflowContinuationHelper;
    }

    /**
     * Returns whether the request carries a valid {@code X-Twilio-Signature}. Validation is opt-in: when no
     * {@code bytechef.twilio.auth-token} is configured this returns {@code true} (unchanged behavior); when it is
     * configured, requests with a missing or invalid signature are rejected.
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

    /**
     * Handles Twilio status callback webhooks for call events.
     *
     * <p>
     * <b>Security Note:</b> CSRF protection is intentionally disabled for this endpoint. Twilio webhook callbacks
     * cannot include CSRF tokens. Security is maintained through Twilio's request signature validation. The CRLF
     * injection suppression is present because call status values are logged; however, Twilio status values are
     * validated against expected values before logging.
     *
     * @param params the Twilio webhook parameters
     * @return ResponseEntity with OK status
     */
    @SuppressFBWarnings({
        "CRLF_INJECTION_LOGS", "SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING"
    })
    @PostMapping("/status")
    public ResponseEntity<String> handleStatusCallback(
        @RequestParam Map<String, String> params, HttpServletRequest request) {

        if (!isValidTwilioRequest(request, params)) {
            log.warn("Rejected Twilio status callback with invalid signature");

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .build();
        }

        String callSid = params.get("CallSid");
        String callStatus = params.get("CallStatus");
        String from = params.get("From");
        String to = params.get("To");
        String direction = params.get("Direction");
        String accountSid = params.get("AccountSid");

        if (log.isDebugEnabled()) {
            log.debug(
                "Twilio status callback: callSid={}, status={}, from={}, to={}, direction={}, accountSid={}",
                callSid, callStatus, from, to, direction, accountSid);
        }

        if (callSid == null || callStatus == null) {
            log.warn("Missing required parameters in Twilio callback: callSid={}, callStatus={}", callSid,
                callStatus);

            return ResponseEntity.badRequest()
                .body("Missing required parameters: CallSid and CallStatus");
        }

        callSessionRegistry.updateCallStatus(callSid, callStatus);

        Optional<CallSession> sessionOptional = callSessionRegistry.getSessionByCallSid(callSid);

        if (sessionOptional.isPresent()) {
            CallSession session = sessionOptional.get();

            log.info("Updated call status for active session: callSid={}, status={}, sessionId={}",
                callSid, callStatus, session.getWebSocketSessionId());

            if (isTerminalStatus(callStatus)) {
                Long voiceSessionId = session.getVoiceSessionId();

                if (voiceSessionId != null) {
                    log.info("Stopping voice session for terminated call: callSid={}, voiceSessionId={}", callSid,
                        voiceSessionId);

                    try {
                        voiceSessionEngine.stop(voiceSessionId);
                    } catch (Exception exception) {
                        log.warn("Failed to stop voice session: callSid={}, voiceSessionId={}", callSid,
                            voiceSessionId, exception);
                    }
                }

                String durationStr = params.get("CallDuration");

                if (durationStr != null) {
                    try {
                        session.setCallDuration(Integer.parseInt(durationStr));
                    } catch (NumberFormatException exception) {
                        log.warn("Failed to parse call duration: {}", durationStr);
                    }
                }

                session.signalCompletion();

                String workflowExecutionId = session.getWorkflowExecutionId();

                if (workflowExecutionId != null) {
                    Map<String, Object> afterCallData = new LinkedHashMap<>();

                    afterCallData.put("callSid", callSid);
                    afterCallData.put("callStatus", callStatus);
                    afterCallData.put("from", from);
                    afterCallData.put("to", to);
                    afterCallData.put("direction", direction);
                    afterCallData.put("accountSid", accountSid);

                    if (session.getCallDuration() != null) {
                        afterCallData.put("callDuration", session.getCallDuration());
                    }

                    workflowContinuationHelper.createContinuationJob(workflowExecutionId, afterCallData);
                }

                log.info("Signaled completion for call: callSid={}, actionJobId={}", callSid,
                    session.getActionJobId());
            }
        } else {
            log.info("Call status update for inactive session: callSid={}, status={}", callSid, callStatus);
        }

        return ResponseEntity.ok("OK");
    }

    private static boolean isTerminalStatus(String status) {
        return "completed".equalsIgnoreCase(status) || "failed".equalsIgnoreCase(status) ||
            "busy".equalsIgnoreCase(status) || "no-answer".equalsIgnoreCase(status) ||
            "canceled".equalsIgnoreCase(status);
    }

    /**
     * Handles Twilio recording status callback webhooks.
     *
     * <p>
     * <b>Security Note:</b> CSRF protection is intentionally disabled for this endpoint. Twilio webhook callbacks
     * cannot include CSRF tokens. The CRLF injection suppression is present because recording values are logged.
     *
     * @param params the Twilio webhook parameters
     * @return ResponseEntity with OK status
     */
    @SuppressFBWarnings({
        "CRLF_INJECTION_LOGS", "SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING"
    })
    @PostMapping("/recording")
    public ResponseEntity<String> handleRecordingCallback(@RequestParam Map<String, String> params) {
        String callSid = params.get("CallSid");
        String recordingSid = params.get("RecordingSid");
        String recordingUrl = params.get("RecordingUrl");
        String recordingStatus = params.get("RecordingStatus");

        if (log.isDebugEnabled()) {
            log.debug(
                "Twilio recording callback: callSid={}, recordingSid={}, recordingUrl={}, recordingStatus={}",
                callSid, recordingSid, recordingUrl, recordingStatus);
        }

        return ResponseEntity.ok("OK");
    }

    /**
     * Handles Twilio media stream event callbacks.
     *
     * <p>
     * <b>Security Note:</b> CSRF protection is intentionally disabled for this endpoint. Twilio webhook callbacks
     * cannot include CSRF tokens. The CRLF injection suppression is present because stream event values are logged.
     *
     * @param params the Twilio webhook parameters
     * @return ResponseEntity with OK status
     */
    @SuppressFBWarnings({
        "CRLF_INJECTION_LOGS", "SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING"
    })
    @PostMapping("/media-stream")
    public ResponseEntity<String> handleMediaStreamCallback(@RequestParam Map<String, String> params) {
        String callSid = params.get("CallSid");
        String streamSid = params.get("StreamSid");
        String event = params.get("Event");

        if (log.isDebugEnabled()) {
            log.debug("Twilio media stream callback: callSid={}, streamSid={}, event={}", callSid, streamSid,
                event);
        }

        return ResponseEntity.ok("OK");
    }
}
