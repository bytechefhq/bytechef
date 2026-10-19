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

import static org.assertj.core.api.Assertions.assertThat;
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
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.component.definition.ActionDefinition.ResumePerformFunction;
import com.bytechef.component.definition.ActionDefinition.ResumePerformFunction.ResumeResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ActionContextAware.ApprovalLinks;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
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

    private static final ApprovalLinks APPROVAL_LINKS = new ApprovalLinks(
        "https://example.com/api", "abc", "https://example.com/api/job/resume/abc",
        "https://example.com/api/resume/abc");

    @Test
    void testPerformRoutesApprovalChannelThroughTokenRefreshAwareService() throws Exception {
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);

        ModifiableActionDefinition actionDefinition = ApprovalRequestApprovalAction.of(clusterElementDefinitionService);

        MultipleConnectionsPerformFunction performFunction = (MultipleConnectionsPerformFunction) actionDefinition
            .getPerform()
            .orElseThrow();

        ActionContextAware context = mock(ActionContextAware.class);

        when(context.isEditorEnvironment()).thenReturn(false);
        when(context.getApprovalLinks(any())).thenReturn(APPROVAL_LINKS);

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
    void testResumePerformAlwaysOutputsACommentAndAnApprover() throws Exception {
        ModifiableActionDefinition actionDefinition = ApprovalRequestApprovalAction.of(
            mock(ClusterElementDefinitionService.class));

        ResumePerformFunction resumePerformFunction = (ResumePerformFunction) actionDefinition.getResumePerform()
            .orElseThrow();

        Parameters emptyParameters = ParametersFactory.create(Map.of());

        ResumeResponse withoutComment = resumePerformFunction.apply(
            emptyParameters, emptyParameters, emptyParameters, ParametersFactory.create(Map.of("approved", true)),
            mock(ActionContext.class));

        assertThat(withoutComment)
            .containsEntry("approved", true)
            .containsEntry("approvedBy", "")
            .containsEntry("comment", "");

        ResumeResponse withComment = resumePerformFunction.apply(
            emptyParameters, emptyParameters, emptyParameters,
            ParametersFactory.create(Map.of("approved", false, "comment", "Too expensive")),
            mock(ActionContext.class));

        assertThat(withComment)
            .containsEntry("approved", false)
            .containsEntry("comment", "Too expensive");

        ResumeResponse withApprover = resumePerformFunction.apply(
            emptyParameters, emptyParameters, emptyParameters,
            ParametersFactory.create(Map.of("approved", true, "approvedBy", "@jane")), mock(ActionContext.class));

        assertThat(withApprover).containsEntry("approvedBy", "@jane");

        Map<String, Object> nullApproverData = new HashMap<>();

        nullApproverData.put("approved", true);
        nullApproverData.put("approvedBy", null);

        ResumeResponse withNullApprover = resumePerformFunction.apply(
            emptyParameters, emptyParameters, emptyParameters, ParametersFactory.create(nullApproverData),
            mock(ActionContext.class));

        assertThat(withNullApprover).containsEntry("approvedBy", "");
    }

    @Test
    void testResumePerformOutputsEveryFormInputWithItsDeclaredType() throws Exception {
        ModifiableActionDefinition actionDefinition = ApprovalRequestApprovalAction.of(
            mock(ClusterElementDefinitionService.class));

        ResumePerformFunction resumePerformFunction = (ResumePerformFunction) actionDefinition.getResumePerform()
            .orElseThrow();

        Parameters emptyParameters = ParametersFactory.create(Map.of());
        Parameters inputParameters = ParametersFactory.create(
            Map.of(
                "inputs",
                List.of(
                    Map.of("fieldName", "amount", "fieldType", 9, "required", true),
                    Map.of("fieldName", "reason", "fieldType", 6),
                    Map.of("fieldName", "confirmed", "fieldType", 1),
                    Map.of("fieldName", "dueDate", "fieldType", 2))));

        ResumeResponse resumeResponse = resumePerformFunction.apply(
            inputParameters, emptyParameters, emptyParameters,
            ParametersFactory.create(Map.of("approved", true, "amount", "7")), mock(ActionContext.class));

        assertThat(resumeResponse)
            .containsEntry("amount", 7L)
            .containsEntry("reason", "")
            .containsEntry("confirmed", false)
            .containsEntry("dueDate", null);
    }

    @Test
    void testPerformMarksTheSuspendAsAnApproval() throws Exception {
        ModifiableActionDefinition actionDefinition = ApprovalRequestApprovalAction.of(
            mock(ClusterElementDefinitionService.class));

        MultipleConnectionsPerformFunction performFunction = (MultipleConnectionsPerformFunction) actionDefinition
            .getPerform()
            .orElseThrow();

        ActionContextAware context = mock(ActionContextAware.class);

        when(context.isEditorEnvironment()).thenReturn(false);
        when(context.getApprovalLinks(any())).thenReturn(APPROVAL_LINKS);

        performFunction.apply(
            ParametersFactory.create(Map.of()), Map.of(), ParametersFactory.create(Map.of()), context);

        ArgumentCaptor<Suspend> suspendArgumentCaptor = ArgumentCaptor.forClass(Suspend.class);

        verify(context).suspend(suspendArgumentCaptor.capture());

        Suspend suspend = suspendArgumentCaptor.getValue();

        Map<String, ?> continueParameters = suspend.continueParameters();

        assertThat((Boolean) continueParameters.get(MetadataConstants.APPROVAL_RESUME)).isTrue();
        assertThat(continueParameters.get(MetadataConstants.APPROVAL_RAISED_AT)).isNotNull();
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
        when(context.getApprovalLinks(any())).thenReturn(APPROVAL_LINKS);

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

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> channelInputParametersArgumentCaptor = ArgumentCaptor.forClass(Map.class);

        verify(clusterElementDefinitionService).executeApprovalChannel(
            eq("approvalTask"), anyInt(), anyString(), channelInputParametersArgumentCaptor.capture(),
            eq(APPROVAL_LINKS.formUrl()), any(), any());
        verify(context).suspend(any(ActionContext.Suspend.class));

        assertThat(channelInputParametersArgumentCaptor.getValue())
            .containsEntry("resumeId", APPROVAL_LINKS.resumeId())
            .containsEntry("resumeUrl", APPROVAL_LINKS.resumeUrl());
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
        when(context.getApprovalLinks(any())).thenReturn(APPROVAL_LINKS);

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
                        Map.of("name", "slack_1", "parameters", Map.of(), "type", "slack/v1/approvalChannel"),
                        Map.of(
                            "name", "discord_1", "parameters", Map.of(), "type", "discord/v1/approvalChannel")))));

        IllegalStateException illegalStateException = assertThrows(
            IllegalStateException.class,
            () -> performFunction.apply(inputParameters, Map.of(), extensions, context));

        assertThat(illegalStateException.getCause()).isNull();
        assertThat(illegalStateException.getMessage())
            .startsWith("None of the 2 configured approval channels could deliver the approval request.")
            .contains("slack/approvalChannel: channel_not_found")
            .contains("discord/approvalChannel: channel_not_found");
        assertThat(illegalStateException.getSuppressed()).hasSize(2);

        verify(clusterElementDefinitionService).executeApprovalChannel(
            eq("slack"), anyInt(), anyString(), any(), anyString(), any(), any());
        verify(clusterElementDefinitionService).executeApprovalChannel(
            eq("discord"), anyInt(), anyString(), any(), anyString(), any(), any());
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
        when(context.getApprovalLinks(any())).thenReturn(APPROVAL_LINKS);

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
    void testPerformPassesTheMintedShortResumeIdToChannels() throws Exception {
        Map<String, ?> channelInputParameters = deliverToTelegram(
            new ApprovalLinks(
                "https://example.com/api", "abc", "https://example.com/api/job/resume/abc",
                "https://example.com/api/resume/abc", "short-1"));

        assertThat(channelInputParameters.get("shortResumeId")).isEqualTo("short-1");
    }

    @Test
    void testPerformDropsAChannelSuppliedShortResumeIdWhenNoneWasMinted() throws Exception {
        Map<String, ?> channelInputParameters = deliverToTelegram(APPROVAL_LINKS);

        assertThat(channelInputParameters.containsKey("shortResumeId")).isFalse();
    }

    private static Map<String, ?> deliverToTelegram(ApprovalLinks approvalLinks) throws Exception {
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);

        ModifiableActionDefinition actionDefinition = ApprovalRequestApprovalAction.of(clusterElementDefinitionService);

        MultipleConnectionsPerformFunction performFunction = (MultipleConnectionsPerformFunction) actionDefinition
            .getPerform()
            .orElseThrow();

        ActionContextAware context = mock(ActionContextAware.class);

        when(context.isEditorEnvironment()).thenReturn(false);
        when(context.getApprovalLinks(any())).thenReturn(approvalLinks);

        Parameters extensions = ParametersFactory.create(
            Map.of(
                "clusterElements",
                Map.of(
                    "approvalChannels",
                    List.of(
                        Map.of(
                            "name", "telegram_1", "parameters", Map.of("chat_id", "7", "shortResumeId", "spoofed"),
                            "type", "telegram/v1/approvalChannel")))));

        performFunction.apply(ParametersFactory.create(Map.of()), Map.of(), extensions, context);

        ArgumentCaptor<Map<String, ?>> channelInputParametersArgumentCaptor = ArgumentCaptor.captor();

        verify(clusterElementDefinitionService).executeApprovalChannel(
            eq("telegram"), anyInt(), anyString(), channelInputParametersArgumentCaptor.capture(), anyString(), any(),
            any());

        return channelInputParametersArgumentCaptor.getValue();
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
        when(context.getApprovalLinks(any())).thenReturn(APPROVAL_LINKS);

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
        when(context.getApprovalLinks(any())).thenReturn(APPROVAL_LINKS);

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
        when(context.getApprovalLinks(any())).thenReturn(APPROVAL_LINKS);

        Parameters inputParameters = ParametersFactory.create(Map.of());
        Parameters extensions = ParametersFactory.create(Map.of());

        Object result = performFunction.apply(inputParameters, Map.of(), extensions, context);

        assertNull(result);

        verify(context).suspend(any(ActionContext.Suspend.class));

        verify(clusterElementDefinitionService, never()).executeApprovalChannel(
            anyString(), anyInt(), anyString(), any(), anyString(), any(), any());
    }
}
