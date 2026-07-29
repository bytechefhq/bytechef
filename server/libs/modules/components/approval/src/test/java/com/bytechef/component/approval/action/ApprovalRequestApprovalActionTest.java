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

package com.bytechef.component.approval.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler.SseEmitter;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.ai.constant.AiAgentSseEventType;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.SuspendAwareSseEmitterHandler;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ApprovalRequestApprovalActionTest {

    @Test
    void testPerformRoutesApprovalChannelThroughTokenRefreshAwareService() throws Exception {
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);

        ModifiableActionDefinition actionDefinition = ApprovalRequestApprovalAction.of(clusterElementDefinitionService);

        MultipleConnectionsPerformFunction performFunction = (MultipleConnectionsPerformFunction) actionDefinition
            .getPerform()
            .orElseThrow();

        ActionContextAware context = mock(ActionContextAware.class);

        when(context.isEditorEnvironment()).thenReturn(false);
        when(context.getResumeUrl()).thenReturn("https://example.com/api/job/resume/abc");

        ComponentConnection componentConnection = new ComponentConnection(
            "googleMail", 1, 5L, Map.of("access_token", "expired"), AuthorizationType.OAUTH2_AUTHORIZATION_CODE);

        Parameters inputParameters = ParametersFactory.create(Map.of());
        Parameters extensions = ParametersFactory.create(
            Map.of(
                "clusterElements",
                Map.of(
                    "approvalChannels",
                    List.of(
                        Map.of(
                            "name", "googleMail_1",
                            "type", "googleMail/v1/googleMail",
                            "parameters", Map.of("subject", "Please approve"))))));

        performFunction.apply(inputParameters, Map.of("googleMail_1", componentConnection), extensions, context);

        // The channel must run through the aspect-wrapped service method, with the form URL derived from the resume URL
        // and the channel's own connection threaded in so a token refresh can retry against fresh credentials.
        verify(clusterElementDefinitionService, times(1)).executeApprovalChannel(
            eq("googleMail"), eq(1), eq("googleMail"), any(), eq("https://example.com/api/resume/abc"),
            eq(componentConnection), eq(context));
    }

    @Test
    void testPerformDeliversRemainingChannelsWhenOneFails() throws Exception {
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);

        ModifiableActionDefinition actionDefinition = ApprovalRequestApprovalAction.of(clusterElementDefinitionService);

        MultipleConnectionsPerformFunction performFunction = (MultipleConnectionsPerformFunction) actionDefinition
            .getPerform()
            .orElseThrow();

        ActionContextAware context = mock(ActionContextAware.class);

        when(context.isEditorEnvironment()).thenReturn(false);
        when(context.getResumeUrl()).thenReturn("https://example.com/api/job/resume/abc");

        when(clusterElementDefinitionService.executeApprovalChannel(
            eq("slack"), anyInt(), anyString(), any(), anyString(), any(), any()))
                .thenThrow(new RuntimeException("channel_not_found"));

        Parameters inputParameters = ParametersFactory.create(Map.of());
        Parameters extensions = ParametersFactory.create(
            Map.of(
                "clusterElements",
                Map.of(
                    "approvalChannels",
                    List.of(
                        Map.of("name", "slack_1", "parameters", Map.of(), "type", "slack/v1/approvalChannel"),
                        Map.of(
                            "name", "approvalTask_1", "parameters", Map.of(),
                            "type", "approvalTask/v1/approvalTask")))));

        performFunction.apply(inputParameters, Map.of(), extensions, context);

        // The failing Slack channel must not prevent the fallback channel from delivering or the run from pausing.
        verify(clusterElementDefinitionService).executeApprovalChannel(
            eq("approvalTask"), anyInt(), anyString(), any(), anyString(), any(), any());
        verify(context).suspend(any(ActionContext.Suspend.class));
    }

    @Test
    void testPerformFailsWhenEveryChannelFails() throws Exception {
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);

        ModifiableActionDefinition actionDefinition = ApprovalRequestApprovalAction.of(clusterElementDefinitionService);

        MultipleConnectionsPerformFunction performFunction = (MultipleConnectionsPerformFunction) actionDefinition
            .getPerform()
            .orElseThrow();

        ActionContextAware context = mock(ActionContextAware.class);

        when(context.isEditorEnvironment()).thenReturn(false);
        when(context.getResumeUrl()).thenReturn("https://example.com/api/job/resume/abc");

        when(clusterElementDefinitionService.executeApprovalChannel(
            anyString(), anyInt(), anyString(), any(), anyString(), any(), any()))
                .thenThrow(new RuntimeException("channel_not_found"));

        Parameters inputParameters = ParametersFactory.create(Map.of());
        Parameters extensions = ParametersFactory.create(
            Map.of(
                "clusterElements",
                Map.of(
                    "approvalChannels",
                    List.of(
                        Map.of("name", "slack_1", "parameters", Map.of(), "type", "slack/v1/approvalChannel")))));

        assertThrows(
            IllegalStateException.class,
            () -> performFunction.apply(inputParameters, Map.of(), extensions, context));

        verify(context, never()).suspend(any(ActionContext.Suspend.class));
    }

    @Test
    void testPerformPublishesExpiresAtToChannels() throws Exception {
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);

        ModifiableActionDefinition actionDefinition = ApprovalRequestApprovalAction.of(clusterElementDefinitionService);

        MultipleConnectionsPerformFunction performFunction = (MultipleConnectionsPerformFunction) actionDefinition
            .getPerform()
            .orElseThrow();

        ActionContextAware context = mock(ActionContextAware.class);

        when(context.isEditorEnvironment()).thenReturn(false);
        when(context.getResumeUrl()).thenReturn("https://example.com/api/job/resume/abc");

        Parameters inputParameters = ParametersFactory.create(Map.of("expiresIn", 4, "expiresInUnit", "HOURS"));
        Parameters extensions = ParametersFactory.create(
            Map.of(
                "clusterElements",
                Map.of(
                    "approvalChannels",
                    List.of(
                        Map.of("name", "slack_1", "parameters", Map.of(), "type", "slack/v1/approvalChannel")))));

        performFunction.apply(inputParameters, Map.of(), extensions, context);

        ArgumentCaptor<Map<String, ?>> channelInputParametersArgumentCaptor = ArgumentCaptor.captor();

        verify(clusterElementDefinitionService).executeApprovalChannel(
            eq("slack"), anyInt(), anyString(), channelInputParametersArgumentCaptor.capture(), anyString(), any(),
            any());

        Map<String, ?> channelInputParameters = channelInputParametersArgumentCaptor.getValue();

        Instant expiresAt = Instant.parse((String) channelInputParameters.get("expiresAt"));

        assertTrue(expiresAt.isAfter(Instant.now()
            .plus(3, ChronoUnit.HOURS)));
        assertTrue(expiresAt.isBefore(Instant.now()
            .plus(5, ChronoUnit.HOURS)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPerformInEditorEnvironmentEmitsApprovalCardOnTestStream() throws Exception {
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);

        ModifiableActionDefinition actionDefinition = ApprovalRequestApprovalAction.of(clusterElementDefinitionService);

        MultipleConnectionsPerformFunction performFunction = (MultipleConnectionsPerformFunction) actionDefinition
            .getPerform()
            .orElseThrow();

        ActionContextAware context = mock(ActionContextAware.class);

        when(context.isEditorEnvironment()).thenReturn(true);
        when(context.getJobId()).thenReturn(42L);
        when(context.getResumeUrl()).thenReturn("https://example.com/api/job/resume/abc");

        Parameters inputParameters = ParametersFactory.create(Map.of("formTitle", "Approve this"));
        Parameters extensions = ParametersFactory.create(Map.of());

        Object result = performFunction.apply(inputParameters, Map.of(), extensions, context);

        // The editor run must deliver the approval card through the run's SSE stream instead of channels: the action
        // returns a suspend-aware emitter output that the in-process post-output processor drains into the test-run
        // stream bridges.
        SuspendAwareSseEmitterHandler emitterHandler = assertInstanceOf(SuspendAwareSseEmitterHandler.class, result);

        SseEmitter sseEmitter = mock(SseEmitter.class);

        emitterHandler.handle(sseEmitter);

        ArgumentCaptor<Object> payloadArgumentCaptor = ArgumentCaptor.forClass(Object.class);

        verify(sseEmitter).send(payloadArgumentCaptor.capture());

        Map<String, Object> eventData = (Map<String, Object>) payloadArgumentCaptor.getValue();

        assertEquals(AiAgentSseEventType.APPROVAL_REQUEST, eventData.get(AiAgentSseEventType.EVENT_TYPE));
        assertEquals("abc", eventData.get("resumeId"));
        assertEquals("https://example.com/api/resume/abc", eventData.get("formUrl"));
        assertEquals("Approve this", eventData.get("formTitle"));

        // The suspend happens inside the emitter handler (after the card event is sent), so the post-output
        // processor observes it once the stream is drained; suspending before returning would have made the
        // service layer swallow the emitter output.
        verify(context).suspend(any(ActionContext.Suspend.class));
        verify(sseEmitter).complete();

        verify(clusterElementDefinitionService, never()).executeApprovalChannel(
            anyString(), anyInt(), anyString(), any(), anyString(), any(), any());
    }

    @Test
    void testPerformUsesConfiguredExpiry() throws Exception {
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);

        ModifiableActionDefinition actionDefinition = ApprovalRequestApprovalAction.of(clusterElementDefinitionService);

        MultipleConnectionsPerformFunction performFunction = (MultipleConnectionsPerformFunction) actionDefinition
            .getPerform()
            .orElseThrow();

        ActionContextAware context = mock(ActionContextAware.class);

        when(context.isEditorEnvironment()).thenReturn(false);
        when(context.getResumeUrl()).thenReturn("https://example.com/api/job/resume/abc");

        Parameters inputParameters = ParametersFactory.create(Map.of("expiresIn", 4, "expiresInUnit", "HOURS"));
        Parameters extensions = ParametersFactory.create(Map.of());

        performFunction.apply(inputParameters, Map.of(), extensions, context);

        ArgumentCaptor<ActionContext.Suspend> suspendArgumentCaptor = ArgumentCaptor.forClass(
            ActionContext.Suspend.class);

        verify(context).suspend(suspendArgumentCaptor.capture());

        ActionContext.Suspend suspend = suspendArgumentCaptor.getValue();

        Instant expiresAt = suspend.expiresAt();

        assertTrue(expiresAt.isAfter(Instant.now()
            .plus(3, ChronoUnit.HOURS)));
        assertTrue(expiresAt.isBefore(Instant.now()
            .plus(5, ChronoUnit.HOURS)));
    }

    @Test
    void testPerformDefaultsExpiryToSixtyDays() throws Exception {
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);

        ModifiableActionDefinition actionDefinition = ApprovalRequestApprovalAction.of(clusterElementDefinitionService);

        MultipleConnectionsPerformFunction performFunction = (MultipleConnectionsPerformFunction) actionDefinition
            .getPerform()
            .orElseThrow();

        ActionContextAware context = mock(ActionContextAware.class);

        when(context.isEditorEnvironment()).thenReturn(false);
        when(context.getResumeUrl()).thenReturn("https://example.com/api/job/resume/abc");

        Parameters inputParameters = ParametersFactory.create(Map.of());
        Parameters extensions = ParametersFactory.create(Map.of());

        performFunction.apply(inputParameters, Map.of(), extensions, context);

        ArgumentCaptor<ActionContext.Suspend> suspendArgumentCaptor = ArgumentCaptor.forClass(
            ActionContext.Suspend.class);

        verify(context).suspend(suspendArgumentCaptor.capture());

        ActionContext.Suspend suspend = suspendArgumentCaptor.getValue();

        Instant expiresAt = suspend.expiresAt();

        assertTrue(expiresAt.isAfter(Instant.now()
            .plus(59, ChronoUnit.DAYS)));
        assertTrue(expiresAt.isBefore(Instant.now()
            .plus(61, ChronoUnit.DAYS)));
    }

    @Test
    void testPerformInEditorEnvironmentWithoutJobIdSuspendsWithoutEmitting() throws Exception {
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);

        ModifiableActionDefinition actionDefinition = ApprovalRequestApprovalAction.of(clusterElementDefinitionService);

        MultipleConnectionsPerformFunction performFunction = (MultipleConnectionsPerformFunction) actionDefinition
            .getPerform()
            .orElseThrow();

        ActionContextAware context = mock(ActionContextAware.class);

        when(context.isEditorEnvironment()).thenReturn(true);
        when(context.getJobId()).thenReturn(null);
        when(context.getResumeUrl()).thenReturn("https://example.com/api/job/resume/abc");

        Parameters inputParameters = ParametersFactory.create(Map.of());
        Parameters extensions = ParametersFactory.create(Map.of());

        Object result = performFunction.apply(inputParameters, Map.of(), extensions, context);

        assertNull(result);

        verify(context).suspend(any(ActionContext.Suspend.class));

        verify(clusterElementDefinitionService, never()).executeApprovalChannel(
            anyString(), anyInt(), anyString(), any(), anyString(), any(), any());
    }
}
