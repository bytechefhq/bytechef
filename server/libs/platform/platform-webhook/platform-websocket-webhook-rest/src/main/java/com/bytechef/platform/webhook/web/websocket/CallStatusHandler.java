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

import static com.bytechef.tenant.constant.TenantConstants.CURRENT_TENANT_ID;

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.webhook.annotation.ConditionalOnWebhook;
import com.bytechef.platform.webhook.web.websocket.CallSessionRegistry.CallSession;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnWebhook
public class CallStatusHandler {

    private static final Logger log = LoggerFactory.getLogger(CallStatusHandler.class);

    private final CallSessionRegistry callSessionRegistry;
    private final JobFacade jobFacade;
    private final MessageBroker messageBroker;
    private final WorkflowContinuationHelper workflowContinuationHelper;

    @SuppressFBWarnings("EI")
    public CallStatusHandler(
        CallSessionRegistry callSessionRegistry, JobFacade jobFacade, MessageBroker messageBroker,
        WorkflowContinuationHelper workflowContinuationHelper) {

        this.callSessionRegistry = callSessionRegistry;
        this.jobFacade = jobFacade;
        this.messageBroker = messageBroker;
        this.workflowContinuationHelper = workflowContinuationHelper;
    }

    @SuppressFBWarnings("CRLF_INJECTION_LOGS")
    public void handleStatusCallback(Map<String, String> parameters) {
        String callSid = parameters.get("CallSid");

        if (callSessionRegistry.hasSession(callSid)) {
            applyStatusCallback(parameters);

            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Forwarding the status callback of call {} to the node that holds it", callSid);
        }

        CallStatusEvent callStatusEvent = new CallStatusEvent(parameters);

        callStatusEvent.putMetadata(CURRENT_TENANT_ID, TenantContext.getCurrentTenantId());

        messageBroker.send(CallStatusMessageRoute.CALL_STATUS_EVENTS, callStatusEvent);
    }

    @SuppressFBWarnings("CRLF_INJECTION_LOGS")
    public void onCallStatusEvent(CallStatusEvent callStatusEvent) {
        Map<String, String> parameters = callStatusEvent.getParameters();

        if (!callSessionRegistry.hasSession(parameters.get("CallSid"))) {
            return;
        }

        TenantContext.runWithTenantId(
            (String) callStatusEvent.getMetadata(CURRENT_TENANT_ID), () -> applyStatusCallback(parameters));
    }

    @SuppressFBWarnings("CRLF_INJECTION_LOGS")
    private void applyStatusCallback(Map<String, String> parameters) {
        String callSid = parameters.get("CallSid");
        String callStatus = parameters.get("CallStatus");

        callSessionRegistry.updateCallStatus(callSid, callStatus);

        Optional<CallSession> sessionOptional = callSessionRegistry.getSessionByCallSid(callSid);

        if (sessionOptional.isEmpty()) {
            log.info("Call status update for inactive session: callSid={}, status={}", callSid, callStatus);

            return;
        }

        CallSession session = sessionOptional.get();

        log.info(
            "Updated call status for active session: callSid={}, status={}, sessionId={}", callSid, callStatus,
            session.getWebSocketSessionId());

        if (!isTerminalStatus(callStatus)) {
            return;
        }

        Long subJobId = session.getSubJobId();

        if (subJobId != null) {
            log.info("Stopping sub-workflow for terminated call: callSid={}, subJobId={}", callSid, subJobId);

            try {
                jobFacade.stopJob(subJobId);
            } catch (Exception exception) {
                log.warn("Failed to stop sub-workflow: callSid={}, subJobId={}", callSid, subJobId, exception);
            }
        }

        String durationString = parameters.get("CallDuration");

        if (durationString != null) {
            try {
                session.setCallDuration(Integer.parseInt(durationString));
            } catch (NumberFormatException exception) {
                log.warn("Failed to parse call duration: {}", durationString);
            }
        }

        session.signalCompletion();

        String workflowExecutionId = session.getWorkflowExecutionId();

        if (workflowExecutionId != null) {
            Map<String, Object> afterCallData = new LinkedHashMap<>();

            afterCallData.put("callSid", callSid);
            afterCallData.put("callStatus", callStatus);
            afterCallData.put("from", parameters.get("From"));
            afterCallData.put("to", parameters.get("To"));
            afterCallData.put("direction", parameters.get("Direction"));
            afterCallData.put("accountSid", parameters.get("AccountSid"));

            if (session.getCallDuration() != null) {
                afterCallData.put("callDuration", session.getCallDuration());
            }

            workflowContinuationHelper.createContinuationJob(workflowExecutionId, afterCallData);
        }

        log.info("Signaled completion for call: callSid={}, actionJobId={}", callSid, session.getActionJobId());
    }

    private static boolean isTerminalStatus(String status) {
        return "completed".equalsIgnoreCase(status) || "failed".equalsIgnoreCase(status) ||
            "busy".equalsIgnoreCase(status) || "no-answer".equalsIgnoreCase(status) ||
            "canceled".equalsIgnoreCase(status);
    }
}
