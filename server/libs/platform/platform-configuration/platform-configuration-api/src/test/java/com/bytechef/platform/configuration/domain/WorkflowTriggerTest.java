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

package com.bytechef.platform.configuration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowTriggerTest {

    @Test
    void testParsesDisabledFlag() {
        WorkflowTrigger workflowTrigger = new WorkflowTrigger(
            Map.of("name", "trigger_1", "type", "webhook/v1/autoRespondWithHTTP200", "disabled", true));

        assertThat(workflowTrigger.isDisabled()).isTrue();
        assertThat(workflowTrigger.getExtensions()).doesNotContainKey("disabled");
    }

    @Test
    void testToMapWritesDisabledOnlyWhenTrue() {
        WorkflowTrigger disabledWorkflowTrigger = new WorkflowTrigger(
            Map.of("name", "trigger_1", "type", "webhook/v1/autoRespondWithHTTP200", "disabled", true));
        WorkflowTrigger enabledWorkflowTrigger = new WorkflowTrigger(
            Map.of("name", "trigger_1", "type", "webhook/v1/autoRespondWithHTTP200"));

        assertThat(disabledWorkflowTrigger.toMap()).containsEntry("disabled", true);
        assertThat(enabledWorkflowTrigger.toMap()).doesNotContainKey("disabled");
    }

    @Test
    void testIsEnabledManualTriggerIgnoresDisabledFlag() {
        WorkflowTrigger workflowTrigger = new WorkflowTrigger(
            Map.of("name", "trigger_1", "type", "manual/v1/manual", "disabled", true));

        assertThat(workflowTrigger.isEnabled()).isTrue();
    }

    @Test
    void testIsEnabledFalseForDisabledNonManualTrigger() {
        WorkflowTrigger workflowTrigger = new WorkflowTrigger(
            Map.of("name", "trigger_1", "type", "webhook/v1/autoRespondWithHTTP200", "disabled", true));

        assertThat(workflowTrigger.isEnabled()).isFalse();
    }

    @Test
    void testOfEnabledFiltersDisabledTriggers() {
        WorkflowTrigger disabledWorkflowTrigger = new WorkflowTrigger(
            Map.of("name", "trigger_1", "type", "webhook/v1/autoRespondWithHTTP200", "disabled", true));
        WorkflowTrigger enabledWorkflowTrigger = new WorkflowTrigger(
            Map.of("name", "trigger_2", "type", "schedule/v1/cron"));

        Workflow workflow = Mockito.mock(Workflow.class);

        Mockito.when(workflow.getExtensions(WorkflowExtConstants.TRIGGERS, WorkflowTrigger.class, List.of()))
            .thenReturn(List.of(disabledWorkflowTrigger, enabledWorkflowTrigger));

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.ofEnabled(workflow);

        assertThat(workflowTriggers).extracting(WorkflowTrigger::getName)
            .containsExactly("trigger_2");
    }

    @Test
    void testEqualsIgnoresDisabled() {
        WorkflowTrigger disabledWorkflowTrigger = new WorkflowTrigger(
            Map.of("name", "trigger_1", "type", "webhook/v1/autoRespondWithHTTP200", "disabled", true));
        WorkflowTrigger enabledWorkflowTrigger = new WorkflowTrigger(
            Map.of("name", "trigger_1", "type", "webhook/v1/autoRespondWithHTTP200"));

        assertThat(disabledWorkflowTrigger).isEqualTo(enabledWorkflowTrigger);
    }
}
