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

package com.bytechef.platform.webhook.voice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.trigger.dispatcher.TriggerDispatcherPreSendProcessor;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class VoiceSessionConnectionResolverTest {

    private static final WorkflowTrigger WORKFLOW_TRIGGER = new WorkflowTrigger(
        Map.of("name", "trigger_1", "type", "browser/v1/voiceSession", "parameters", Map.of()));

    @Test
    void testDeployedPathReadsConnectionIdsStampedByThePreSendProcessor() {
        VoiceSessionConnectionResolver resolver = new VoiceSessionConnectionResolver(
            List.of(new StampingProcessor(Map.of("voiceAgent_1", 12L))), mock(WorkflowTestConfigurationService.class));

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 7L, "wf-uuid", "trigger_1");

        assertThat(resolver.resolveDeployed(workflowExecutionId, WORKFLOW_TRIGGER))
            .containsExactlyEntriesOf(Map.of("voiceAgent_1", 12L));
    }

    @Test
    void testDeployedPathUsesOnlyTheFirstProcessorThatCanProcess() {
        AtomicBoolean secondCalled = new AtomicBoolean();

        TriggerDispatcherPreSendProcessor secondProcessor = new StampingProcessor(Map.of("voiceAgent_1", 99L)) {

            @Override
            public TriggerExecution process(TriggerExecution triggerExecution) {
                secondCalled.set(true);

                return super.process(triggerExecution);
            }
        };

        VoiceSessionConnectionResolver resolver = new VoiceSessionConnectionResolver(
            List.of(new StampingProcessor(Map.of("voiceAgent_1", 12L)), secondProcessor),
            mock(WorkflowTestConfigurationService.class));

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 7L, "wf-uuid", "trigger_1");

        assertThat(resolver.resolveDeployed(workflowExecutionId, WORKFLOW_TRIGGER))
            .containsExactlyEntriesOf(Map.of("voiceAgent_1", 12L));
        assertThat(secondCalled).isFalse();
    }

    @Test
    void testDeployedPathWithoutAMatchingProcessorResolvesNoConnections() {
        VoiceSessionConnectionResolver resolver = new VoiceSessionConnectionResolver(
            List.of(), mock(WorkflowTestConfigurationService.class));

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 7L, "wf-uuid", "trigger_1");

        assertThat(resolver.resolveDeployed(workflowExecutionId, WORKFLOW_TRIGGER)).isEmpty();
    }

    @Test
    void testTestPathReadsTheWorkflowTestConfiguration() {
        WorkflowTestConfigurationService service = mock(WorkflowTestConfigurationService.class);
        WorkflowTestConfigurationConnection connection = mock(WorkflowTestConfigurationConnection.class);

        when(connection.getWorkflowConnectionKey()).thenReturn("voiceAgent_1");
        when(connection.getConnectionId()).thenReturn(12L);
        when(service.getWorkflowTestConfigurationConnections("wf-1", "trigger_1", 1L)).thenReturn(List.of(connection));

        VoiceSessionConnectionResolver resolver = new VoiceSessionConnectionResolver(List.of(), service);

        assertThat(resolver.resolveTest("wf-1", "trigger_1", 1L)).containsExactlyEntriesOf(Map.of("voiceAgent_1", 12L));
    }

    private static class StampingProcessor implements TriggerDispatcherPreSendProcessor {

        private final Map<String, Long> connectionIds;

        StampingProcessor(Map<String, Long> connectionIds) {
            this.connectionIds = connectionIds;
        }

        @Override
        public TriggerExecution process(TriggerExecution triggerExecution) {
            return triggerExecution.putMetadata(MetadataConstants.CONNECTION_IDS, connectionIds);
        }

        @Override
        public boolean canProcess(TriggerExecution triggerExecution) {
            return true;
        }
    }
}
