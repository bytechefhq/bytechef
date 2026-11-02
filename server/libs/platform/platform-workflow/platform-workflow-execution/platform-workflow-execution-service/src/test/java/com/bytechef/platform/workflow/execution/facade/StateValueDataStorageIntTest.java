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

package com.bytechef.platform.workflow.execution.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.file.storage.filesystem.service.FilesystemFileStorageService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.data.storage.file.storage.service.FileDataStorageService;
import com.bytechef.platform.data.storage.file.storage.service.FileDataStorageServiceImpl;
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
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
@Import(StateValueDataStorageIntTest.FileDataStorageTestConfiguration.class)
@SpringBootTest(classes = PlatformWorkflowExecutionIntTestConfiguration.class)
class StateValueDataStorageIntTest {

    private static final long ENVIRONMENT_ID = 2L;
    private static final long JOB_ID = 41L;
    private static final long JOB_PRINCIPAL_ID = 7L;
    private static final JobResumeId JOB_RESUME_ID = JobResumeId.of(JOB_ID);
    private static final WorkflowExecutionId WORKFLOW_EXECUTION_ID = WorkflowExecutionId.of(
        PlatformType.AUTOMATION, JOB_PRINCIPAL_ID, "workflow-uuid", "trigger-a");

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private DataStorage dataStorage;

    @MockitoBean
    private JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;

    @MockitoBean
    private PrincipalJobService principalJobService;

    @Autowired
    private TaskStateRepository taskStateRepository;

    @Autowired
    private TaskStateFacade taskStateFacade;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private TriggerStateRepository triggerStateRepository;

    @Autowired
    private TriggerStateFacade triggerStateFacade;

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

        setProvider(ApplicationProperties.DataStorage.Provider.FILESYSTEM);
    }

    @AfterEach
    void afterEach() {
        taskStateFacade.delete(JOB_RESUME_ID);
        triggerStateFacade.delete(WORKFLOW_EXECUTION_ID);

        taskStateRepository.deleteAll();
        triggerStateRepository.deleteAll();
    }

    @Test
    void testSaveTaskStateStoresValueInDataStorage() {
        taskStateFacade.save(JOB_RESUME_ID, Map.of("approved", false));

        TaskState taskState = taskStateRepository.findByJobResumeId(JOB_RESUME_ID.toString())
            .orElseThrow();

        assertThat(taskState.getValue())
            .isInstanceOfSatisfying(StateValueReference.class, reference -> {
                assertThat(reference.environmentId()).isEqualTo(ENVIRONMENT_ID);
                assertThat(reference.type()).isEqualTo(PlatformType.AUTOMATION);
            });
        assertThat(getTaskStateEntries()).containsOnly(Map.entry(
            ((StateValueReference) taskState.getValue()).key(), Map.of("approved", false)));
        assertThat(taskStateFacade.<Map<String, Object>>fetchValue(JOB_RESUME_ID))
            .hasValue(Map.of("approved", false));
        assertThat(taskStateFacade.<Map<String, Object>>fetchUnlockedValue(JOB_RESUME_ID))
            .hasValue(Map.of("approved", false));
    }

    @Test
    void testFetchUnlockedTaskStateInReadOnlyTransaction() {
        taskStateFacade.save(JOB_RESUME_ID, "value");

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.setReadOnly(true);

        Optional<String> valueOptional = transactionTemplate.execute(
            status -> taskStateFacade.fetchUnlockedValue(JOB_RESUME_ID));

        assertThat(valueOptional).hasValue("value");
    }

    @Test
    void testSaveTaskStateOverwritesDataStorageValue() {
        taskStateFacade.save(JOB_RESUME_ID, "first");
        taskStateFacade.save(JOB_RESUME_ID, "second");

        assertThat(taskStateRepository.count()).isEqualTo(1);
        assertThat(getTaskStateEntries()).containsOnlyKeys(getTaskStateReference().key());
        assertThat(getTaskStateEntries()).containsValue("second");
        assertThat(taskStateFacade.<String>fetchValue(JOB_RESUME_ID)).hasValue("second");
    }

    @Test
    void testSaveTaskStateOverwritesEntryLeftByFailedCleanup() {
        taskStateFacade.save(JOB_RESUME_ID, "initial");
        taskStateFacade.save(JOB_RESUME_ID, "first");

        String firstKey = getTaskStateReference().key();

        taskStateFacade.save(JOB_RESUME_ID, "second");

        putTaskStateEntry(firstKey, "first");

        assertThat(getTaskStateEntries()).hasSize(2);

        taskStateFacade.save(JOB_RESUME_ID, "third");

        assertThat(getTaskStateReference().key()).isEqualTo(firstKey);
        assertThat(getTaskStateEntries()).containsOnly(Map.entry(firstKey, "third"));
    }

    @Test
    void testDeleteTaskStateDeletesEntryLeftByFailedCleanup() {
        taskStateFacade.save(JOB_RESUME_ID, "initial");
        taskStateFacade.save(JOB_RESUME_ID, "first");

        String firstKey = getTaskStateReference().key();

        taskStateFacade.save(JOB_RESUME_ID, "second");

        putTaskStateEntry(firstKey, "first");

        taskStateFacade.delete(JOB_RESUME_ID);

        assertThat(getTaskStateEntries()).isEmpty();
    }

    @Test
    void testRolledBackTaskStateSaveKeepsCommittedValue() {
        taskStateFacade.save(JOB_RESUME_ID, "first");

        StateValueReference reference = getTaskStateReference();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            taskStateFacade.save(JOB_RESUME_ID, "second");

            status.setRollbackOnly();
        });

        assertThat(getTaskStateReference()).isEqualTo(reference);
        assertThat(getTaskStateEntries()).containsOnly(Map.entry(reference.key(), "first"));
        assertThat(taskStateFacade.<String>fetchValue(JOB_RESUME_ID)).hasValue("first");
    }

    @Test
    void testCommittedTaskStateCleanupKeepsSlotReusedByLaterSave() {
        taskStateFacade.save(JOB_RESUME_ID, "initial");
        taskStateFacade.save(JOB_RESUME_ID, "first");

        String firstKey = getTaskStateReference().key();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            saveTaskStateAfterCompletion("third");

            taskStateFacade.save(JOB_RESUME_ID, "second");
        });

        assertThat(getTaskStateReference().key()).isEqualTo(firstKey);
        assertThat(getTaskStateEntries()).containsOnly(Map.entry(firstKey, "third"));
        assertThat(taskStateFacade.<String>fetchValue(JOB_RESUME_ID)).hasValue("third");
    }

    @Test
    void testRolledBackTaskStateCleanupKeepsSlotReusedByLaterSave() {
        taskStateFacade.save(JOB_RESUME_ID, "initial");
        taskStateFacade.save(JOB_RESUME_ID, "first");

        String firstKey = getTaskStateReference().key();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            saveTaskStateAfterCompletion("third");

            taskStateFacade.save(JOB_RESUME_ID, "second");

            status.setRollbackOnly();
        });

        String thirdKey = getTaskStateReference().key();

        assertThat(thirdKey).isNotEqualTo(firstKey);
        assertThat(getTaskStateEntries()).containsOnly(Map.entry(thirdKey, "third"));
        assertThat(taskStateFacade.<String>fetchValue(JOB_RESUME_ID)).hasValue("third");
    }

    @Test
    void testRolledBackConcurrentFirstTaskStateSaveKeepsCommittedValue() throws Exception {
        CountDownLatch firstSavedLatch = new CountDownLatch(1);
        CountDownLatch secondSavedLatch = new CountDownLatch(1);
        CountDownLatch firstCommittedLatch = new CountDownLatch(1);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
            Future<?> firstFuture = executorService.submit(() -> {
                transactionTemplate.executeWithoutResult(status -> {
                    taskStateFacade.save(JOB_RESUME_ID, "first");

                    firstSavedLatch.countDown();

                    awaitLatch(secondSavedLatch);
                });

                firstCommittedLatch.countDown();
            });

            Future<?> secondFuture = executorService.submit(() -> {
                awaitLatch(firstSavedLatch);

                transactionTemplate.executeWithoutResult(status -> {
                    taskStateFacade.save(JOB_RESUME_ID, "second");

                    secondSavedLatch.countDown();

                    awaitLatch(firstCommittedLatch);

                    status.setRollbackOnly();
                });
            });

            firstFuture.get();
            secondFuture.get();
        }

        assertThat(taskStateRepository.count()).isEqualTo(1);
        assertThat(getTaskStateEntries()).containsOnly(Map.entry(getTaskStateReference().key(), "first"));
        assertThat(taskStateFacade.<String>fetchValue(JOB_RESUME_ID)).hasValue("first");
    }

    @Test
    void testRolledBackTaskStateDeleteKeepsValue() {
        taskStateFacade.save(JOB_RESUME_ID, "value");

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            taskStateFacade.delete(JOB_RESUME_ID);

            status.setRollbackOnly();
        });

        assertThat(taskStateRepository.count()).isEqualTo(1);
        assertThat(taskStateFacade.<String>fetchValue(JOB_RESUME_ID)).hasValue("value");
    }

    @Test
    void testDeleteTaskStateDeletesDataStorageValue() {
        taskStateFacade.save(JOB_RESUME_ID, "value");

        taskStateFacade.delete(JOB_RESUME_ID);

        assertThat(taskStateRepository.count()).isZero();
        assertThat(getTaskStateEntries()).isEmpty();
    }

    @Test
    void testSaveTaskStateWithJdbcProviderKeepsValueInline() {
        setProvider(ApplicationProperties.DataStorage.Provider.JDBC);

        taskStateFacade.save(JOB_RESUME_ID, "inline");

        TaskState taskState = taskStateRepository.findByJobResumeId(JOB_RESUME_ID.toString())
            .orElseThrow();

        assertThat(taskState.getValue()).isEqualTo("inline");
        assertThat(getTaskStateEntries()).isEmpty();
        assertThat(taskStateFacade.<String>fetchValue(JOB_RESUME_ID)).hasValue("inline");
    }

    @Test
    void testSaveTaskStateWithoutPrincipalKeepsValueInline() {
        JobResumeId jobResumeId = JobResumeId.of(99L);

        when(principalJobService.fetchJobPrincipalId(99L, PlatformType.AUTOMATION)).thenReturn(Optional.empty());
        when(principalJobService.fetchJobPrincipalId(99L, PlatformType.EMBEDDED)).thenReturn(Optional.empty());

        taskStateFacade.save(jobResumeId, "inline");

        TaskState taskState = taskStateRepository.findByJobResumeId(jobResumeId.toString())
            .orElseThrow();

        assertThat(taskState.getValue()).isEqualTo("inline");
        assertThat(taskStateFacade.<String>fetchValue(jobResumeId)).hasValue("inline");
    }

    @Test
    void testFetchTaskStateReadsInlineValueWrittenBeforeDataStorage() {
        taskStateRepository.save(new TaskState(JOB_RESUME_ID, "legacy"));

        assertThat(taskStateFacade.<String>fetchValue(JOB_RESUME_ID)).hasValue("legacy");

        taskStateFacade.delete(JOB_RESUME_ID);

        assertThat(taskStateRepository.count()).isZero();
    }

    @Test
    void testSaveTriggerStateStoresValueInDataStorage() {
        triggerStateFacade.save(WORKFLOW_EXECUTION_ID, Map.of("cursor", "page-2"));

        TriggerState triggerState = triggerStateRepository
            .findByWorkflowExecutionId(WORKFLOW_EXECUTION_ID.toString())
            .orElseThrow();

        assertThat(triggerState.getValue())
            .isInstanceOfSatisfying(StateValueReference.class, reference -> {
                assertThat(reference.environmentId()).isEqualTo(ENVIRONMENT_ID);
                assertThat(reference.type()).isEqualTo(PlatformType.AUTOMATION);
            });
        assertThat(getTriggerStateEntries()).containsOnly(Map.entry(
            ((StateValueReference) triggerState.getValue()).key(), Map.of("cursor", "page-2")));
        assertThat(triggerStateFacade.<Map<String, Object>>fetchValue(WORKFLOW_EXECUTION_ID))
            .hasValue(Map.of("cursor", "page-2"));
    }

    @Test
    void testDeleteTriggerStateDeletesDataStorageValue() {
        triggerStateFacade.save(WORKFLOW_EXECUTION_ID, "value");

        triggerStateFacade.delete(WORKFLOW_EXECUTION_ID);

        assertThat(triggerStateRepository.count()).isZero();
        assertThat(getTriggerStateEntries()).isEmpty();
    }

    private StateValueReference getTaskStateReference() {
        TaskState taskState = taskStateRepository.findByJobResumeId(JOB_RESUME_ID.toString())
            .orElseThrow();

        return (StateValueReference) taskState.getValue();
    }

    private static void awaitLatch(CountDownLatch countDownLatch) {
        try {
            if (!countDownLatch.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for the other transaction");
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            throw new IllegalStateException(interruptedException);
        }
    }

    private void saveTaskStateAfterCompletion(String value) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCompletion(int status) {
                try (ExecutorService executorService = Executors.newSingleThreadExecutor()) {
                    executorService.submit(() -> taskStateFacade.save(JOB_RESUME_ID, value))
                        .get();
                } catch (ExecutionException | InterruptedException exception) {
                    throw new IllegalStateException(exception);
                }
            }
        });
    }

    private Map<String, Object> getTaskStateEntries() {
        return dataStorage.getAll(
            StateValueDataStorage.TASK_STATE_COMPONENT_NAME, DataStorageScope.CURRENT_EXECUTION,
            String.valueOf(JOB_ID), ENVIRONMENT_ID, PlatformType.AUTOMATION);
    }

    private void putTaskStateEntry(String key, Object value) {
        dataStorage.put(
            StateValueDataStorage.TASK_STATE_COMPONENT_NAME, DataStorageScope.CURRENT_EXECUTION,
            String.valueOf(JOB_ID), key, value, ENVIRONMENT_ID, PlatformType.AUTOMATION);
    }

    private Map<String, Object> getTriggerStateEntries() {
        return dataStorage.getAll(
            StateValueDataStorage.TRIGGER_STATE_COMPONENT_NAME, DataStorageScope.WORKFLOW, "workflow-uuid",
            ENVIRONMENT_ID, PlatformType.AUTOMATION);
    }

    private void setProvider(ApplicationProperties.DataStorage.Provider provider) {
        ApplicationProperties.DataStorage dataStorageProperties = new ApplicationProperties.DataStorage();

        dataStorageProperties.setProvider(provider);

        applicationProperties.setDataStorage(dataStorageProperties);
    }

    @TestConfiguration
    static class FileDataStorageTestConfiguration {

        @Bean
        DataStorage dataStorage(ObjectMapper objectMapper) throws IOException {
            Path baseDir = Files.createTempDirectory("state_value_data_storage");

            return new FileDataStorage(
                new FileDataStorageServiceImpl(new FilesystemFileStorageService(baseDir.toString()), objectMapper));
        }
    }

    private record FileDataStorage(FileDataStorageService fileDataStorageService) implements DataStorage {

        @Override
        public void delete(
            String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
            PlatformType type) {

            fileDataStorageService.delete(componentName, scope, scopeId, key, environmentId, type);
        }

        @Override
        public void deleteScopeData(DataStorageScope scope, String scopeId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> Optional<T> fetch(
            String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
            PlatformType type) {

            return fileDataStorageService.fetch(componentName, scope, scopeId, key, environmentId, type);
        }

        @Override
        public <T> T get(
            String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
            PlatformType type) {

            return fileDataStorageService.get(componentName, scope, scopeId, key, environmentId, type);
        }

        @Override
        public <T> Map<String, T> getAll(
            String componentName, DataStorageScope scope, String scopeId, long environmentId, PlatformType type) {

            return fileDataStorageService.getAll(componentName, scope, scopeId, environmentId, type);
        }

        @Override
        public void put(
            String componentName, DataStorageScope scope, String scopeId, String key, Object value,
            long environmentId, PlatformType type) {

            fileDataStorageService.put(componentName, scope, scopeId, key, value, environmentId, type);
        }
    }
}
