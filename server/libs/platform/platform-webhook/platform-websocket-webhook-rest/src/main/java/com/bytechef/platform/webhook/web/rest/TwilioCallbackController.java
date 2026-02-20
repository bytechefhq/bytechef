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

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.platform.webhook.web.websocket.CallSessionRegistry;
import com.bytechef.platform.webhook.web.websocket.CallSessionRegistry.CallSession;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(TwilioCallbackController.class);

    private final CallSessionRegistry callSessionRegistry;
    private final JobFacade jobFacade;

    @SuppressFBWarnings("EI")
    TwilioCallbackController(CallSessionRegistry callSessionRegistry, JobFacade jobFacade) {
        this.callSessionRegistry = callSessionRegistry;
        this.jobFacade = jobFacade;
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
    public ResponseEntity<String> handleStatusCallback(@RequestParam Map<String, String> params) {
        String callSid = params.get("CallSid");
        String callStatus = params.get("CallStatus");
        String from = params.get("From");
        String to = params.get("To");
        String direction = params.get("Direction");
        String accountSid = params.get("AccountSid");

        if (logger.isDebugEnabled()) {
            logger.debug(
                "Twilio status callback: callSid={}, status={}, from={}, to={}, direction={}, accountSid={}",
                callSid, callStatus, from, to, direction, accountSid);
        }

        if (callSid == null || callStatus == null) {
            logger.warn("Missing required parameters in Twilio callback: callSid={}, callStatus={}", callSid,
                callStatus);

            return ResponseEntity.badRequest()
                .body("Missing required parameters: CallSid and CallStatus");
        }

        callSessionRegistry.updateCallStatus(callSid, callStatus);

        Optional<CallSession> sessionOptional = callSessionRegistry.getSessionByCallSid(callSid);

        if (sessionOptional.isPresent()) {
            CallSession session = sessionOptional.get();

            logger.info("Updated call status for active session: callSid={}, status={}, sessionId={}",
                callSid, callStatus, session.getWebSocketSessionId());

            if (isTerminalStatus(callStatus)) {
                Long subJobId = session.getSubJobId();

                if (subJobId != null) {
                    logger.info("Stopping sub-workflow for terminated call: callSid={}, subJobId={}", callSid,
                        subJobId);

                    try {
                        jobFacade.stopJob(subJobId);
                    } catch (Exception exception) {
                        logger.warn("Failed to stop sub-workflow: callSid={}, subJobId={}", callSid, subJobId,
                            exception);
                    }
                }

                // Extract call duration if available
                String durationStr = params.get("CallDuration");

                if (durationStr != null) {
                    try {
                        session.setCallDuration(Integer.parseInt(durationStr));
                    } catch (NumberFormatException exception) {
                        logger.warn("Failed to parse call duration: {}", durationStr);
                    }
                }

                // Signal completion to unblock any waiting action
                session.signalCompletion();

                logger.info("Signaled completion for call: callSid={}, actionJobId={}", callSid,
                    session.getActionJobId());
            }
        } else {
            logger.info("Call status update for inactive session: callSid={}, status={}", callSid, callStatus);
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

        if (logger.isDebugEnabled()) {
            logger.debug(
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

        if (logger.isDebugEnabled()) {
            logger.debug("Twilio media stream callback: callSid={}, streamSid={}, event={}", callSid, streamSid,
                event);
        }

        return ResponseEntity.ok("OK");
    }
}
