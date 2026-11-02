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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.data.storage.jdbc.config.JdbcDataStorageConfiguration;
import com.bytechef.platform.data.storage.jdbc.repository.DataStorageRepository;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.config.PlatformWorkflowExecutionIntTestConfiguration;
import com.bytechef.platform.workflow.execution.domain.StateValueReference;
import com.bytechef.platform.workflow.execution.domain.TaskState;
import com.bytechef.platform.workflow.execution.domain.TriggerState;
import com.bytechef.platform.workflow.execution.repository.TaskStateRepository;
import com.bytechef.platform.workflow.execution.repository.TriggerStateRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Ivica Cardic
 */
@Import(JdbcDataStorageConfiguration.class)
@SpringBootTest(
    classes = PlatformWorkflowExecutionIntTestConfiguration.class,
    properties = "bytechef.data-storage.provider=jdbc")
class StateValueDataStorageIntTest {

    private static final long ENVIRONMENT_ID = 2L;
    private static final long JOB_ID = 41L;
    private static final long JOB_PRINCIPAL_ID = 7L;
    private static final JobResumeId JOB_RESUME_ID = JobResumeId.of(JOB_ID);
    private static final WorkflowExecutionId WORKFLOW_EXECUTION_ID = WorkflowExecutionId.of(
        PlatformType.AUTOMATION, JOB_PRINCIPAL_ID, "workflow-uuid", "trigger-a");

    @Autowired
    private DataStorage dataStorage;

    @Autowired
    private DataStorageRepository dataStorageRepository;

    @MockitoBean
    private JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;

    @MockitoBean
    private PrincipalJobService principalJobService;

    @Autowired
    private TaskStateRepository taskStateRepository;

    @Autowired
    private TaskStateService taskStateService;

    @Autowired
    private TriggerStateRepository triggerStateRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private TriggerStateService triggerStateService;

    @BeforeEach
    void beforeEach() {
        JobPrincipalAccessor jobPrincipalAccessor = mock(JobPrincipalAccessor.class);

        when(jobPrincipalAccessor.getEnvironmentId(JOB_PRINCIPAL_ID)).thenReturn(ENVIRONMENT_ID);
        when(jobPrincipalAccessorRegistry.fetchJobPrincipalAccessor(PlatformType.AUTOMATION))
            .thenReturn(Optional.of(jobPrincipalAccessor));
        when(principalJobService.fetchJobPrincipalId(JOB_ID, PlatformType.AUTOMATION))
            .thenReturn(Optional.of(JOB_PRINCIPAL_ID));
        when(principalJobService.fetchJobPrincipalId(JOB_ID, PlatformType.EMBEDDED))
            .thenReturn(Optional.empty());
    }

    @AfterEach
    void afterEach() {
        dataStorageRepository.deleteAll();
        taskStateRepository.deleteAll();
        triggerStateRepository.deleteAll();
    }

    @Test
    void testSaveTaskStateStoresValueInDataStorage() {
        taskStateService.save(JOB_RESUME_ID, Map.of("approved", false));

        TaskState taskState = taskStateRepository.findByJobResumeId(JOB_RESUME_ID.toString())
            .orElseThrow();

        assertThat(taskState.getValue())
            .isEqualTo(new StateValueReference(ENVIRONMENT_ID, PlatformType.AUTOMATION));
        assertThat(dataStorage.<Map<String, Object>>fetch(
            StateValueDataStorage.TASK_STATE_COMPONENT_NAME, DataStorageScope.CURRENT_EXECUTION,
            String.valueOf(JOB_ID), JOB_RESUME_ID.toString(), ENVIRONMENT_ID, PlatformType.AUTOMATION))
                .hasValue(Map.of("approved", false));
        assertThat(taskStateService.<Map<String, Object>>fetchValue(JOB_RESUME_ID))
            .hasValue(Map.of("approved", false));
        assertThat(taskStateService.<Map<String, Object>>fetchUnlockedValue(JOB_RESUME_ID))
            .hasValue(Map.of("approved", false));
    }

    @Test
    void testFetchUnlockedTaskStateInReadOnlyTransaction() {
        taskStateService.save(JOB_RESUME_ID, "value");

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.setReadOnly(true);

        Optional<String> valueOptional = transactionTemplate.execute(
            status -> taskStateService.fetchUnlockedValue(JOB_RESUME_ID));

        assertThat(valueOptional).hasValue("value");
    }

    @Test
    void testSaveTaskStateOverwritesDataStorageValue() {
        taskStateService.save(JOB_RESUME_ID, "first");
        taskStateService.save(JOB_RESUME_ID, "second");

        assertThat(taskStateRepository.count()).isEqualTo(1);
        assertThat(dataStorageRepository.count()).isEqualTo(1);
        assertThat(taskStateService.<String>fetchValue(JOB_RESUME_ID)).hasValue("second");
    }

    @Test
    void testDeleteTaskStateDeletesDataStorageValue() {
        taskStateService.save(JOB_RESUME_ID, "value");

        taskStateService.delete(JOB_RESUME_ID);

        assertThat(taskStateRepository.count()).isZero();
        assertThat(dataStorageRepository.count()).isZero();
    }

    @Test
    void testSaveTaskStateWithoutPrincipalKeepsValueInline() {
        JobResumeId jobResumeId = JobResumeId.of(99L);

        when(principalJobService.fetchJobPrincipalId(99L, PlatformType.AUTOMATION)).thenReturn(Optional.empty());
        when(principalJobService.fetchJobPrincipalId(99L, PlatformType.EMBEDDED)).thenReturn(Optional.empty());

        taskStateService.save(jobResumeId, "inline");

        TaskState taskState = taskStateRepository.findByJobResumeId(jobResumeId.toString())
            .orElseThrow();

        assertThat(taskState.getValue()).isEqualTo("inline");
        assertThat(dataStorageRepository.count()).isZero();
        assertThat(taskStateService.<String>fetchValue(jobResumeId)).hasValue("inline");
    }

    @Test
    void testFetchTaskStateReadsInlineValueWrittenBeforeDataStorage() {
        taskStateRepository.save(new TaskState(JOB_RESUME_ID, "legacy"));

        assertThat(taskStateService.<String>fetchValue(JOB_RESUME_ID)).hasValue("legacy");

        taskStateService.delete(JOB_RESUME_ID);

        assertThat(taskStateRepository.count()).isZero();
    }

    @Test
    void testSaveTriggerStateStoresValueInDataStorage() {
        triggerStateService.save(WORKFLOW_EXECUTION_ID, Map.of("cursor", "page-2"));

        TriggerState triggerState = triggerStateRepository
            .findByWorkflowExecutionId(WORKFLOW_EXECUTION_ID.toString())
            .orElseThrow();

        assertThat(triggerState.getValue())
            .isEqualTo(new StateValueReference(ENVIRONMENT_ID, PlatformType.AUTOMATION));
        assertThat(dataStorage.<Map<String, Object>>fetch(
            StateValueDataStorage.TRIGGER_STATE_COMPONENT_NAME, DataStorageScope.WORKFLOW, "workflow-uuid",
            WORKFLOW_EXECUTION_ID.toString(), ENVIRONMENT_ID, PlatformType.AUTOMATION))
                .hasValue(Map.of("cursor", "page-2"));
        assertThat(triggerStateService.<Map<String, Object>>fetchValue(WORKFLOW_EXECUTION_ID))
            .hasValue(Map.of("cursor", "page-2"));
    }

    @Test
    void testDeleteTriggerStateDeletesDataStorageValue() {
        triggerStateService.save(WORKFLOW_EXECUTION_ID, "value");

        triggerStateService.delete(WORKFLOW_EXECUTION_ID);

        assertThat(triggerStateRepository.count()).isZero();
        assertThat(dataStorageRepository.count()).isZero();
    }
}
