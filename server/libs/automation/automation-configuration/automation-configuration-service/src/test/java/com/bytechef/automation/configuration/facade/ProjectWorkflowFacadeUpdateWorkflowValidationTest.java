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

package com.bytechef.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.PreBuiltTemplateService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.SharedTemplateService;
import com.bytechef.automation.configuration.util.ComponentDefinitionHelper;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.file.storage.SharedTemplateFileStorage;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Proves that {@link ProjectWorkflowFacadeImpl#updateWorkflow} validates the INCOMING definition only, never whatever
 * was previously persisted for the same workflow id. This matters because
 * {@code workflowValidatorFacade.validateNoDuplicateNodeNames} (and the sibling reserved-name guards added alongside
 * it) run on every update: if they consulted the stored definition instead of the incoming one, a workflow already
 * containing a violation (e.g. saved before the guard existed) could never be fixed. It cannot:
 * {@link ProjectWorkflowFacadeImpl#updateWorkflow} passes {@code definition} -- the new value the caller is trying to
 * save -- directly to each guard, and {@code WorkflowServiceImpl#update} unconditionally overwrites the stored
 * definition with it (no merge), so the only definition ever inspected is the one being saved right now.
 */
@ExtendWith(MockitoExtension.class)
class ProjectWorkflowFacadeUpdateWorkflowValidationTest {

    @Mock
    private ComponentDefinitionHelper componentDefinitionHelper;

    @Mock
    private PreBuiltTemplateService preBuiltTemplateService;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private ErrorWorkflowConfigurationValidator errorWorkflowConfigurationValidator;

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Mock
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private SharedTemplateFileStorage sharedTemplateFileStorage;

    @Mock
    private SharedTemplateService sharedTemplateService;

    @Mock
    private WorkflowCacheManager workflowCacheManager;

    @Mock
    private WorkflowFacade workflowFacade;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    @Mock
    private WorkflowDTO workflowDTO;

    private ProjectWorkflowFacadeImpl projectWorkflowFacade;

    @BeforeEach
    void setUp() {
        projectWorkflowFacade = new ProjectWorkflowFacadeImpl(
            componentDefinitionHelper, preBuiltTemplateService, applicationProperties, environmentService,
            errorWorkflowConfigurationValidator, projectDeploymentService, projectDeploymentWorkflowService,
            projectService, projectWorkflowService, sharedTemplateFileStorage, sharedTemplateService,
            workflowCacheManager, workflowFacade, List.of(), workflowService, workflowTestConfigurationService,
            flatDuplicateNameAwareFacade());
    }

    @Test
    void updateWorkflowRejectsIncomingDefinitionThatStillHasDuplicateNodeNames() {
        String definitionWithDuplicates = """
            {"label":"t","triggers":[],"tasks":[
                {"name":"task_1","type":"logger/v1/info"},
                {"name":"task_1","type":"logger/v1/info"}
            ]}
            """;

        assertThatThrownBy(() -> projectWorkflowFacade.updateWorkflow("wf-1", definitionWithDuplicates, 0))
            .isInstanceOf(ConfigurationException.class);

        verifyNoInteractions(workflowFacade);
    }

    @Test
    void updateWorkflowAcceptsIncomingDefinitionThatResolvesPreviouslyDuplicateNames() {
        // Simulates fixing a workflow that was persisted with duplicate node names before this guard existed (or
        // otherwise slipped past it): the fix itself -- the incoming definition passed to THIS call -- has no
        // duplicates, so it must be accepted regardless of what is stored for "wf-1".
        String fixedDefinition = """
            {"label":"t","triggers":[],"tasks":[
                {"name":"task_1","type":"logger/v1/info"},
                {"name":"task_2","type":"logger/v1/info"}
            ]}
            """;

        ProjectWorkflow projectWorkflow = new ProjectWorkflow(1L, 1, "wf-1");

        ReflectionTestUtils.setField(projectWorkflow, "id", 10L);

        when(environmentService.getEnvironments()).thenReturn(List.of());
        when(projectWorkflowService.getWorkflowProjectWorkflow("wf-1")).thenReturn(projectWorkflow);
        when(workflowFacade.getWorkflow("wf-1")).thenReturn(workflowDTO);

        assertThatCode(() -> projectWorkflowFacade.updateWorkflow("wf-1", fixedDefinition, 0))
            .doesNotThrowAnyException();

        verify(workflowFacade).update("wf-1", fixedDefinition, 0);
    }

    /**
     * A {@link WorkflowValidatorFacade} whose duplicate-name detection is a minimal flat {@code tasks[].name} scan --
     * enough to exercise {@code ProjectWorkflowFacadeImpl}'s call-site behavior without pulling in the full recursive
     * node-name traversal, which lives in the validator service module (outside this module's dependency graph). That
     * traversal's own correctness is already covered exhaustively by {@code WorkflowValidatorDuplicateNodeNamesTest};
     * this fake only needs to agree with it on flat, non-nested task lists, which is all these tests use.
     */
    private static WorkflowValidatorFacade flatDuplicateNameAwareFacade() {
        return new WorkflowValidatorFacade() {

            @Override
            public WorkflowValidationResult validateWorkflow(String workflow) {
                return new WorkflowValidationResult(List.of(), List.of());
            }

            @Override
            public WorkflowValidationResult validateWorkflowById(String workflowId) {
                return new WorkflowValidationResult(List.of(), List.of());
            }

            @Override
            public List<String> getDuplicateNodeNames(String workflow) {
                List<String> taskNames = new ArrayList<>();

                try {
                    JsonMapper jsonMapper = JsonMapper.builder()
                        .build();

                    JsonNode workflowJsonNode = jsonMapper.readTree(workflow);
                    JsonNode tasksJsonNode = workflowJsonNode.get("tasks");

                    if (tasksJsonNode != null && tasksJsonNode.isArray()) {
                        for (JsonNode taskJsonNode : tasksJsonNode) {
                            JsonNode nameJsonNode = taskJsonNode.get("name");

                            if (nameJsonNode != null && nameJsonNode.isString()) {
                                taskNames.add(nameJsonNode.asString());
                            }
                        }
                    }
                } catch (Exception e) {
                    return List.of();
                }

                Set<String> seenNames = new HashSet<>();
                List<String> duplicateNames = new ArrayList<>();

                for (String taskName : taskNames) {
                    if (!seenNames.add(taskName)) {
                        duplicateNames.add(taskName);
                    }
                }

                return duplicateNames;
            }
        };
    }
}
