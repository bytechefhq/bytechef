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

package com.bytechef.automation.configuration.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.dto.ProjectDeploymentWorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.util.ProjectDeploymentFacadeHelper;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.DataStorageScopeDeleter.ScopeData;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ProjectIntTestConfiguration.class,
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true"
    })
@Import({
    DataStorageProjectDeploymentDeleteEventListenerIntTest.RecordingDataStorageConfiguration.class,
    PostgreSQLContainerConfiguration.class
})
@ProjectIntTestConfigurationSharedMocks
class DataStorageProjectDeploymentDeleteEventListenerIntTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PrincipalJobService principalJobService;

    @Autowired
    private ProjectDeploymentFacade projectDeploymentFacade;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

    @Autowired
    private ProjectFacade projectFacade;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    private RecordingDataStorage recordingDataStorage;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private TriggerDefinitionService triggerDefinitionService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private ProjectDeploymentFacadeHelper projectDeploymentFacadeHelper;
    private ProjectDTO projectDTO;
    private ProjectDeploymentDTO projectDeploymentDTO;

    @BeforeEach
    void beforeEach() {
        recordingDataStorage.reset();

        Workspace workspace = workspaceRepository.save(new Workspace("test"));

        projectDeploymentFacadeHelper = new ProjectDeploymentFacadeHelper(
            categoryRepository, projectFacade, projectRepository, projectDeploymentFacade, projectWorkflowFacade,
            projectWorkflowRepository);

        when(principalJobService.getJobIds(any(), any(), any(), any(), any(), any(), anyBoolean(), anyInt()))
            .thenReturn(Page.empty());
        when(triggerDefinitionService.getTriggerDefinition(anyString(), anyInt(), anyString()))
            .thenThrow(new IllegalArgumentException("Trigger definition not found"));

        projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        projectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeployment(workspace.getId(), projectDTO);
    }

    @AfterEach
    void afterEach() {
        projectDeploymentWorkflowRepository.deleteAll();
        projectWorkflowRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
        categoryRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    void testDeletingADeploymentDeletesItsPrincipalAndWorkflowDataAfterCommit() {
        List<ScopeData> expectedScopeDataList = new ArrayList<>();

        expectedScopeDataList.add(
            new ScopeData(
                DataStorageScope.PRINCIPAL, String.valueOf(projectDeploymentDTO.id()), PlatformType.AUTOMATION));

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflowRepository
            .findAllByProjectDeploymentId(projectDeploymentDTO.id())) {
            expectedScopeDataList.add(
                new ScopeData(
                    DataStorageScope.WORKFLOW, String.valueOf(projectDeploymentWorkflow.getId()),
                    PlatformType.AUTOMATION));
        }

        assertThat(expectedScopeDataList).hasSize(2);

        projectDeploymentFacade.deleteProjectDeployment(projectDeploymentDTO.id());

        assertThat(recordingDataStorage.deletedScopeDataList).containsExactlyInAnyOrderElementsOf(
            expectedScopeDataList);
    }

    @Test
    void testRolledBackDeploymentDeleteKeepsData() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            projectDeploymentFacade.deleteProjectDeployment(projectDeploymentDTO.id());

            transactionStatus.setRollbackOnly();
        });

        assertThat(recordingDataStorage.deletedScopeDataList).isEmpty();
        assertThat(projectDeploymentRepository.findById(projectDeploymentDTO.id())).isPresent();
    }

    @Test
    void testFailingDataStorageDoesNotFailTheDeploymentDelete() {
        recordingDataStorage.failing = true;

        projectDeploymentFacade.deleteProjectDeployment(projectDeploymentDTO.id());

        assertThat(recordingDataStorage.attempts.get()).isPositive();
        assertThat(projectDeploymentRepository.findById(projectDeploymentDTO.id())).isEmpty();
    }

    @Test
    void testUpgradingADeploymentDeletesNothing() {
        projectFacade.publishProject(projectDTO.id(), "Published v2 for test", false);

        ProjectWorkflow v2ProjectWorkflow = projectWorkflowRepository.findAllByProjectIdAndProjectVersion(
            projectDTO.id(), 2)
            .getFirst();

        ProjectDeploymentWorkflowDTO projectDeploymentWorkflowDTO = new ProjectDeploymentWorkflowDTO(
            List.of(), null, null, Map.of(), false, null, null, null, null, null, null, null, 0,
            v2ProjectWorkflow.getWorkflowId(), v2ProjectWorkflow.getUuidAsString());

        ProjectDeploymentDTO currentProjectDeploymentDTO = projectDeploymentFacade.getProjectDeployment(
            projectDeploymentDTO.id());

        projectDeploymentFacade.updateProjectDeployment(
            ProjectDeploymentDTO.builder()
                .id(projectDeploymentDTO.id())
                .projectId(projectDTO.id())
                .name(projectDeploymentDTO.name())
                .enabled(false)
                .environment(projectDeploymentDTO.environment())
                .projectVersion(2)
                .projectDeploymentWorkflows(List.of(projectDeploymentWorkflowDTO))
                .version(currentProjectDeploymentDTO.version())
                .build());

        assertThat(recordingDataStorage.deletedScopeDataList).isEmpty();
        assertThat(recordingDataStorage.attempts.get()).isZero();
    }

    @TestConfiguration
    static class RecordingDataStorageConfiguration {

        @Bean
        RecordingDataStorage recordingDataStorage() {
            return new RecordingDataStorage();
        }
    }

    static final class RecordingDataStorage implements DataStorage {

        private final List<ScopeData> deletedScopeDataList = new CopyOnWriteArrayList<>();
        private final AtomicInteger attempts = new AtomicInteger();
        private volatile boolean failing;

        void reset() {
            deletedScopeDataList.clear();
            attempts.set(0);
            failing = false;
        }

        @Override
        public void deleteScopeData(DataStorageScope scope, String scopeId, PlatformType type) {
            attempts.incrementAndGet();

            if (failing) {
                throw new IllegalStateException("Data Storage is unavailable");
            }

            deletedScopeDataList.add(new ScopeData(scope, scopeId, type));
        }

        @Override
        public void delete(
            String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
            PlatformType type) {

            throw new UnsupportedOperationException();
        }

        @Override
        public <T> Optional<T> fetch(
            String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
            PlatformType type) {

            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T get(
            String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
            PlatformType type) {

            throw new UnsupportedOperationException();
        }

        @Override
        public <T> Map<String, T> getAll(
            String componentName, DataStorageScope scope, String scopeId, long environmentId, PlatformType type) {

            throw new UnsupportedOperationException();
        }

        @Override
        public void put(
            String componentName, DataStorageScope scope, String scopeId, String key, Object value,
            long environmentId, PlatformType type) {

            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T update(
            String componentName, DataStorageScope scope, String scopeId, String key, long environmentId,
            PlatformType type, Function<Optional<T>, T> updater) {

            throw new UnsupportedOperationException();
        }
    }
}
