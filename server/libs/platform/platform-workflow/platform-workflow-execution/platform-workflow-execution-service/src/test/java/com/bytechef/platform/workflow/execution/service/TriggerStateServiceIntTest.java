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

package com.bytechef.platform.workflow.execution.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.config.PlatformWorkflowExecutionIntTestConfiguration;
import com.bytechef.platform.workflow.execution.repository.TriggerStateRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PlatformWorkflowExecutionIntTestConfiguration.class)
class TriggerStateServiceIntTest {

    private static final WorkflowExecutionId WORKFLOW_EXECUTION_ID = WorkflowExecutionId.of(
        PlatformType.AUTOMATION, 9L, "workflow-uuid", "trigger-a");
    private static final WorkflowExecutionId OTHER_WORKFLOW_EXECUTION_ID = WorkflowExecutionId.of(
        PlatformType.AUTOMATION, 9L, "workflow-uuid", "trigger-b");

    @Autowired
    private TriggerStateRepository triggerStateRepository;

    @Autowired
    private TriggerStateService triggerStateService;

    @AfterEach
    void afterEach() {
        triggerStateRepository.deleteAll();
    }

    @Test
    void testFetchValue() {
        HashMap<String, Object> value = new HashMap<>();

        value.put("cursor", "page-2");
        value.put("count", 3);

        triggerStateService.save(WORKFLOW_EXECUTION_ID, value);

        Optional<Map<String, Object>> fetchedValue = triggerStateService.fetchValue(WORKFLOW_EXECUTION_ID);
        Optional<Object> missingValue = triggerStateService.fetchValue(OTHER_WORKFLOW_EXECUTION_ID);

        assertThat(fetchedValue).hasValue(Map.of("cursor", "page-2", "count", 3));
        assertThat(missingValue).isEmpty();
    }

    @Test
    void testSave() {
        triggerStateService.save(WORKFLOW_EXECUTION_ID, "first");
        triggerStateService.save(OTHER_WORKFLOW_EXECUTION_ID, "other");
        triggerStateService.save(WORKFLOW_EXECUTION_ID, "second");

        assertThat(triggerStateRepository.count()).isEqualTo(2);
        assertThat(triggerStateService.<String>fetchValue(WORKFLOW_EXECUTION_ID)).hasValue("second");
        assertThat(triggerStateService.<String>fetchValue(OTHER_WORKFLOW_EXECUTION_ID)).hasValue("other");
    }

    @Test
    void testDelete() {
        triggerStateService.save(WORKFLOW_EXECUTION_ID, "first");
        triggerStateService.save(OTHER_WORKFLOW_EXECUTION_ID, "other");

        triggerStateService.delete(WORKFLOW_EXECUTION_ID);

        assertThat(triggerStateService.fetchValue(WORKFLOW_EXECUTION_ID)).isEmpty();
        assertThat(triggerStateService.<String>fetchValue(OTHER_WORKFLOW_EXECUTION_ID)).hasValue("other");
    }
}
