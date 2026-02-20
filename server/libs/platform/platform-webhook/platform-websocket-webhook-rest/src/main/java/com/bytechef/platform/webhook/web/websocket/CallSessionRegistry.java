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

package com.bytechef.platform.webhook.web.websocket;

import com.bytechef.commons.util.StringUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * Registry for managing Twilio call sessions. Provides centralized session tracking using callSid as the primary key
 * for complete lifecycle management. Separates WebSocketSession storage from CallSession for easier scaling.
 *
 * @author Ivica Cardic
 */
@Component
public class CallSessionRegistry {

    private static final Logger logger = LoggerFactory.getLogger(CallSessionRegistry.class);

    private final Cache<String, CallSession> callSessions;
    private final Cache<String, WebSocketSession> webSocketSessions;

    public CallSessionRegistry() {
        this.callSessions = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

        this.webSocketSessions = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();
    }

    /**
     * Register a pending outbound call session before the WebSocket connection is established. Used by outbound call
     * actions to create a session that will block until the call completes.
     *
     * @param callReference a unique reference for the call (UUID, used before we have the real callSid)
     * @param subWorkflowId the ID of the sub-workflow to execute during the call
     * @param actionJobId   the job ID of the action waiting for call completion
     * @return the created CallSession with completion latch initialized
     */
    public CallSession registerPendingCall(String callReference, String subWorkflowId, Long actionJobId) {
        if (callReference == null) {
            logger.warn("Cannot register pending call with null callReference");

            return null;
        }

        CallSession callSession = new CallSession(callReference, null, null);

        callSession.setSubWorkflowId(subWorkflowId);
        callSession.setActionJobId(actionJobId);
        callSession.setCallStatus("pending");
        callSession.initCompletionLatch();

        callSessions.put(callReference, callSession);

        if (logger.isDebugEnabled()) {
            logger.debug(
                "Registered pending call session: callReference={}, subWorkflowId={}, actionJobId={}",
                StringUtils.sanitize(callReference), subWorkflowId, actionJobId);
        }

        return callSession;
    }

    /**
     * Update a pending call session with the real Twilio call SID.
     *
     * @param callReference the original call reference
     * @param callSid       the real Twilio call SID
     */
    public void updateCallSid(String callReference, String callSid) {
        if (callReference == null || callSid == null) {
            return;
        }

        CallSession callSession = callSessions.getIfPresent(callReference);

        if (callSession != null) {
            // Re-register with the real callSid
            callSessions.invalidate(callReference);
            callSessions.put(callSid, callSession);

            if (logger.isDebugEnabled()) {
                logger.debug(
                    "Updated call session with real callSid: callReference={}, callSid={}",
                    StringUtils.sanitize(callReference), StringUtils.sanitize(callSid));
            }
        }
    }

    /**
     * Register a new call session with the given callSid and WebSocket session.
     *
     * @param callSid  the Twilio call SID
     * @param session  the WebSocket session
     * @param metadata additional metadata for the call (optional)
     */
    public void registerSession(String callSid, WebSocketSession session, CallMetadata metadata) {
        if (callSid == null || session == null) {
            logger.warn("Cannot register session with null callSid or session");

            return;
        }

        CallSession callSession = new CallSession(callSid, session.getId(), metadata);

        callSessions.put(callSid, callSession);
        webSocketSessions.put(session.getId(), session);

        if (logger.isDebugEnabled()) {
            logger.debug(
                "Registered call session: callSid={}, sessionId={}", StringUtils.sanitize(callSid),
                session.getId());
        }
    }

    /**
     * Get the call session by callSid.
     *
     * @param callSid the Twilio call SID
     * @return Optional containing the CallSession if found, empty otherwise
     */
    public Optional<CallSession> getSessionByCallSid(String callSid) {
        if (callSid == null) {
            return Optional.empty();
        }

        CallSession callSession = callSessions.getIfPresent(callSid);

        return Optional.ofNullable(callSession);
    }

    /**
     * Get the WebSocket session by callSid.
     *
     * @param callSid the Twilio call SID
     * @return Optional containing the WebSocketSession if found, empty otherwise
     */
    public Optional<WebSocketSession> getWebSocketSession(String callSid) {
        return getSessionByCallSid(callSid)
            .map(CallSession::getWebSocketSessionId)
            .map(webSocketSessions::getIfPresent);
    }

    /**
     * Get the WebSocket session by session ID.
     *
     * @param sessionId the WebSocket session ID
     * @return Optional containing the WebSocketSession if found, empty otherwise
     */
    public Optional<WebSocketSession> getWebSocketSessionById(String sessionId) {
        if (sessionId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(webSocketSessions.getIfPresent(sessionId));
    }

    /**
     * Update the call status for the given callSid.
     *
     * @param callSid    the Twilio call SID
     * @param callStatus the new call status
     */
    public void updateCallStatus(String callSid, String callStatus) {
        if (callSid == null) {
            return;
        }

        CallSession callSession = callSessions.getIfPresent(callSid);

        if (callSession != null) {
            callSession.setCallStatus(callStatus);

            if (logger.isDebugEnabled()) {
                logger.debug(
                    "Updated call status: callSid={}, status={}", StringUtils.sanitize(callSid),
                    StringUtils.sanitize(callStatus));
            }

            if (logger.isDebugEnabled() &&
                ("completed".equalsIgnoreCase(callStatus) || "failed".equalsIgnoreCase(callStatus) ||
                    "busy".equalsIgnoreCase(callStatus) || "no-answer".equalsIgnoreCase(callStatus))) {

                logger.debug(
                    "Call ended, will remove session after expiry: callSid={}", StringUtils.sanitize(callSid));
            }
        } else {
            logger.warn("Call session not found for status update: callSid={}", StringUtils.sanitize(callSid));
        }
    }

    /**
     * Remove the call session by callSid.
     *
     * @param callSid the Twilio call SID
     */
    public void removeSessionByCallSid(String callSid) {
        if (callSid == null) {
            return;
        }

        CallSession callSession = callSessions.getIfPresent(callSid);

        if (callSession != null) {
            webSocketSessions.invalidate(callSession.getWebSocketSessionId());
        }

        callSessions.invalidate(callSid);

        if (logger.isDebugEnabled()) {
            logger.debug("Removed call session: callSid={}", StringUtils.sanitize(callSid));
        }
    }

    /**
     * Remove the WebSocket session by session ID.
     *
     * @param sessionId the WebSocket session ID
     */
    public void removeWebSocketSession(String sessionId) {
        if (sessionId == null) {
            return;
        }

        webSocketSessions.invalidate(sessionId);

        if (logger.isDebugEnabled()) {
            logger.debug("Removed WebSocket session: sessionId={}", sessionId);
        }
    }

    /**
     * Check if a session exists for the given callSid.
     *
     * @param callSid the Twilio call SID
     * @return true if session exists, false otherwise
     */
    public boolean hasSession(String callSid) {
        if (callSid == null) {
            return false;
        }

        return callSessions.getIfPresent(callSid) != null;
    }

    /**
     * Check if a call is still active (not in terminal state).
     *
     * @param callSid the Twilio call SID
     * @return true if call is active, false if completed/failed/not found
     */
    public boolean isCallActive(String callSid) {
        if (callSid == null) {
            return false;
        }

        CallSession callSession = callSessions.getIfPresent(callSid);

        if (callSession == null) {
            return false;
        }

        String status = callSession.getCallStatus();

        return status != null && !isTerminalStatus(status);
    }

    private static boolean isTerminalStatus(String status) {
        return "completed".equalsIgnoreCase(status) || "failed".equalsIgnoreCase(status) ||
            "busy".equalsIgnoreCase(status) || "no-answer".equalsIgnoreCase(status) ||
            "canceled".equalsIgnoreCase(status);
    }

    /**
     * Represents a call session with metadata. WebSocketSession is stored separately for easier scaling.
     */
    public static class CallSession {

        private final String callSid;
        private final String webSocketSessionId;
        private final CallMetadata metadata;
        private final long createdAt;

        private String callStatus;
        private String subWorkflowId;
        private Long mainJobId;
        private Long subJobId;
        private String workflowExecutionId;
        private Long actionJobId;
        private Integer callDuration;
        private CountDownLatch completionLatch;

        public CallSession(String callSid, String webSocketSessionId, CallMetadata metadata) {
            this.callSid = callSid;
            this.webSocketSessionId = webSocketSessionId;
            this.metadata = metadata;
            this.callStatus = "initiated";
            this.createdAt = System.currentTimeMillis();
        }

        public String getCallSid() {
            return callSid;
        }

        public String getWebSocketSessionId() {
            return webSocketSessionId;
        }

        public CallMetadata getMetadata() {
            return metadata;
        }

        public String getCallStatus() {
            return callStatus;
        }

        public void setCallStatus(String callStatus) {
            this.callStatus = callStatus;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public String getSubWorkflowId() {
            return subWorkflowId;
        }

        public void setSubWorkflowId(String subWorkflowId) {
            this.subWorkflowId = subWorkflowId;
        }

        public Long getMainJobId() {
            return mainJobId;
        }

        public void setMainJobId(Long mainJobId) {
            this.mainJobId = mainJobId;
        }

        public Long getSubJobId() {
            return subJobId;
        }

        public void setSubJobId(Long subJobId) {
            this.subJobId = subJobId;
        }

        public String getWorkflowExecutionId() {
            return workflowExecutionId;
        }

        public void setWorkflowExecutionId(String workflowExecutionId) {
            this.workflowExecutionId = workflowExecutionId;
        }

        public Long getActionJobId() {
            return actionJobId;
        }

        public void setActionJobId(Long actionJobId) {
            this.actionJobId = actionJobId;
        }

        public Integer getCallDuration() {
            return callDuration;
        }

        public void setCallDuration(Integer callDuration) {
            this.callDuration = callDuration;
        }

        /**
         * Initialize the completion latch for blocking until call completion.
         */
        public void initCompletionLatch() {
            this.completionLatch = new CountDownLatch(1);
        }

        /**
         * Block until the call completes or the timeout expires.
         *
         * @param timeout the maximum time to wait
         * @param unit    the time unit
         * @return true if the call completed, false if timeout occurred
         * @throws InterruptedException if the thread is interrupted while waiting
         */
        public boolean awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException {
            if (completionLatch == null) {
                return true;
            }

            return completionLatch.await(timeout, unit);
        }

        /**
         * Signal that the call has completed. This unblocks any thread waiting on awaitCompletion().
         */
        public void signalCompletion() {
            if (completionLatch != null) {
                completionLatch.countDown();
            }
        }
    }

    /**
     * Metadata associated with a call session.
     */
    public static class CallMetadata {

        private final String from;
        private final String to;
        private final String direction;
        private final String accountSid;

        public CallMetadata(String from, String to, String direction, String accountSid) {
            this.from = from;
            this.to = to;
            this.direction = direction;
            this.accountSid = accountSid;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        public String getDirection() {
            return direction;
        }

        public String getAccountSid() {
            return accountSid;
        }
    }
}
