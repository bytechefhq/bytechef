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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.webhook.web.websocket.CallSessionRegistry.CallMetadata;
import com.bytechef.platform.webhook.web.websocket.CallSessionRegistry.CallSession;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

/**
 * @author Ivica Cardic
 */
class CallStatusHandlerTest {

    private static final String CALL_SID = "CA0001";
    private static final long SUB_JOB_ID = 7L;
    private static final String WORKFLOW_EXECUTION_ID = "workflow-execution-id";

    private final JobFacade jobFacade = mock(JobFacade.class);
    private final List<CallStatusHandler> nodeCallStatusHandlers = new CopyOnWriteArrayList<>();
    private final WorkflowContinuationHelper workflowContinuationHelper = mock(WorkflowContinuationHelper.class);

    private final MessageBroker messageBroker = (route, message) -> {
        for (CallStatusHandler nodeCallStatusHandler : nodeCallStatusHandlers) {
            nodeCallStatusHandler.onCallStatusEvent((CallStatusEvent) message);
        }
    };

    private CallSessionRegistry owningNodeCallSessionRegistry;
    private CallStatusHandler owningNodeCallStatusHandler;
    private CallStatusHandler otherNodeCallStatusHandler;

    @BeforeEach
    void beforeEach() {
        owningNodeCallSessionRegistry = new CallSessionRegistry();

        owningNodeCallStatusHandler = new CallStatusHandler(
            owningNodeCallSessionRegistry, jobFacade, messageBroker, workflowContinuationHelper);
        otherNodeCallStatusHandler = new CallStatusHandler(
            new CallSessionRegistry(), jobFacade, messageBroker, workflowContinuationHelper);

        nodeCallStatusHandlers.add(otherNodeCallStatusHandler);
        nodeCallStatusHandlers.add(owningNodeCallStatusHandler);

        WebSocketSession webSocketSession = mock(WebSocketSession.class);

        when(webSocketSession.getId()).thenReturn("web-socket-session-id");

        owningNodeCallSessionRegistry.registerSession(
            CALL_SID, webSocketSession, new CallMetadata("+1000", "+2000", "inbound", "AC0001"));

        CallSession callSession = owningNodeCallSessionRegistry.getSessionByCallSid(CALL_SID)
            .orElseThrow();

        callSession.setSubJobId(SUB_JOB_ID);
        callSession.setWorkflowExecutionId(WORKFLOW_EXECUTION_ID);
    }

    @Test
    void testTerminalCallbackOnTheOwningNodeStopsTheSubWorkflowAndContinuesTheWorkflow() {
        owningNodeCallStatusHandler.handleStatusCallback(terminalCallbackParameters());

        verify(jobFacade).stopJob(SUB_JOB_ID);
        verify(workflowContinuationHelper).createContinuationJob(eq(WORKFLOW_EXECUTION_ID), any());
    }

    @Test
    void testTerminalCallbackOnAnotherNodeReachesTheOwningNode() {
        otherNodeCallStatusHandler.handleStatusCallback(terminalCallbackParameters());

        verify(jobFacade).stopJob(SUB_JOB_ID);
        verify(workflowContinuationHelper).createContinuationJob(eq(WORKFLOW_EXECUTION_ID), any());

        CallSession callSession = owningNodeCallSessionRegistry.getSessionByCallSid(CALL_SID)
            .orElseThrow();

        assertThat(callSession.getCallStatus()).isEqualTo("completed");
        assertThat(callSession.getCallDuration()).isEqualTo(42);
    }

    @Test
    void testCallbackForAnUnknownCallChangesNothing() {
        otherNodeCallStatusHandler.handleStatusCallback(
            Map.of("CallSid", "CA9999", "CallStatus", "completed"));

        verify(jobFacade, never()).stopJob(anyLong());
        verify(workflowContinuationHelper, never()).createContinuationJob(any(), any());
    }

    private static Map<String, String> terminalCallbackParameters() {
        return Map.of("CallDuration", "42", "CallSid", CALL_SID, "CallStatus", "completed");
    }
}
