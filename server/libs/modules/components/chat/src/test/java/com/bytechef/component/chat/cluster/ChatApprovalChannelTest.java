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

package com.bytechef.component.chat.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.component.definition.Parameters;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.definition.ClusterElementContextAware;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import com.bytechef.platform.webhook.message.route.SseStreamMessageRoute;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ChatApprovalChannelTest {

    private static final String FORM_URL = "https://example.com/resume/abc123";
    private static final String RESUME_URL = "https://example.com/job/resume/abc123";
    private static final Map<String, String> RESUME_PARAMETERS = Map.of("resumeId", "abc123", "resumeUrl", RESUME_URL);

    private final JobService jobService = mock(JobService.class);
    private final MessageBroker messageBroker = mock(MessageBroker.class);

    private void givenJob(long jobId, Map<String, ?> metadata) {
        Job job = new Job();

        job.setMetadata(metadata);

        when(jobService.getJob(jobId)).thenReturn(job);
    }

    @Test
    void testPerformPublishesApprovalRequestEventOnJobStream() {
        ClusterElementContextAware context = mock(ClusterElementContextAware.class);

        when(context.getJobId()).thenReturn(17L);

        givenJob(17L, Map.of(MetadataConstants.STREAMED_EXECUTION, true));

        Parameters inputParameters = ParametersFactory.create(
            Map.of(
                "formTitle", "Expense approval",
                "formDescription", "Approve the expense report.",
                "inputs", List.of(Map.of("fieldName", "amount", "fieldType", 8)),
                "resumeId", "abc123",
                "resumeUrl", RESUME_URL));

        ChatApprovalChannel.perform(inputParameters, FORM_URL, context, messageBroker, jobService);

        ArgumentCaptor<SseStreamEvent> eventCaptor = ArgumentCaptor.forClass(SseStreamEvent.class);

        verify(messageBroker).send(eq(SseStreamMessageRoute.SSE_STREAM_EVENTS), eventCaptor.capture());

        SseStreamEvent sseStreamEvent = eventCaptor.getValue();

        assertThat(sseStreamEvent.getJobId()).isEqualTo(17L);
        assertThat(sseStreamEvent.getEventType()).isEqualTo(SseStreamEvent.EVENT_TYPE_DATA);
        assertThat(sseStreamEvent.getMetadata(TenantContext.CURRENT_TENANT_ID)).isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, Object> eventData = (Map<String, Object>) sseStreamEvent.getPayload();

        assertThat(eventData)
            .containsEntry("__eventType", "approval_request")
            .containsEntry("resumeId", "abc123")
            .containsEntry("resumeUrl", RESUME_URL)
            .containsEntry("formUrl", FORM_URL)
            .containsEntry("formTitle", "Expense approval")
            .containsEntry("formDescription", "Approve the expense report.")
            .containsKey("inputs");
    }

    @Test
    void testPerformOmitsAbsentFormMetadataAndDefaultsInputs() {
        ClusterElementContextAware context = mock(ClusterElementContextAware.class);

        when(context.getJobId()).thenReturn(17L);

        givenJob(17L, Map.of(MetadataConstants.STREAMED_EXECUTION, true));

        ChatApprovalChannel.perform(ParametersFactory.create(RESUME_PARAMETERS), FORM_URL, context, messageBroker,
            jobService);

        ArgumentCaptor<SseStreamEvent> eventCaptor = ArgumentCaptor.forClass(SseStreamEvent.class);

        verify(messageBroker).send(eq(SseStreamMessageRoute.SSE_STREAM_EVENTS), eventCaptor.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> eventData = (Map<String, Object>) eventCaptor.getValue()
            .getPayload();

        assertThat(eventData)
            .doesNotContainKey("formTitle")
            .doesNotContainKey("formDescription")
            .containsEntry("inputs", List.of());
    }

    @Test
    void testPerformWithoutJobIdFailsLoudly() {
        ClusterElementContextAware context = mock(ClusterElementContextAware.class);

        when(context.getJobId()).thenReturn(null);

        assertThatThrownBy(
            () -> ChatApprovalChannel.perform(ParametersFactory.create(Map.of()), FORM_URL, context, messageBroker,
                jobService))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("chat origin");

        verifyNoInteractions(messageBroker);
    }

    @Test
    void testPerformFailsForRunNotStartedFromChat() {
        ClusterElementContextAware context = mock(ClusterElementContextAware.class);

        when(context.getJobId()).thenReturn(17L);

        givenJob(17L, Map.of());

        assertThatThrownBy(
            () -> ChatApprovalChannel.perform(
                ParametersFactory.create(RESUME_PARAMETERS), FORM_URL, context, messageBroker, jobService))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("chat origin");

        verifyNoInteractions(messageBroker);
    }
}
