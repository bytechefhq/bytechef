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

import static org.mockito.Mockito.mock;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.automation.configuration.security.ProjectVisibilityPolicy;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.PreBuiltTemplateService;
import com.bytechef.automation.configuration.service.ProjectCodeWorkflowInfoSupplier;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.SharedTemplateService;
import com.bytechef.automation.configuration.util.ComponentDefinitionHelper;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.file.storage.SharedTemplateFileStorage;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ProjectErrorWorkflowFacadeTest {

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private ErrorWorkflowConfigurationValidator errorWorkflowConfigurationValidator;

    @Mock
    private ProjectService projectService;

    private ProjectFacadeImpl projectFacade;

    /**
     * Constructed explicitly rather than through {@code @InjectMocks}: the facade's first constructor parameter is the
     * {@code bytechef.edition} string, which Mockito would supply as {@code null} and the constructor rightly rejects.
     */
    @BeforeEach
    void setUp() {
        @SuppressWarnings("unchecked")
        ObjectProvider<ProjectCodeWorkflowInfoSupplier> projectCodeWorkflowInfoSupplierProvider =
            mock(ObjectProvider.class);

        projectFacade = new ProjectFacadeImpl(
            "CE", applicationProperties, mock(CategoryService.class), mock(ComponentDefinitionHelper.class),
            errorWorkflowConfigurationValidator, mock(PermissionService.class),
            mock(PreBuiltTemplateService.class),
            projectCodeWorkflowInfoSupplierProvider, mock(ProjectWorkflowService.class),
            mock(ProjectDeploymentService.class),
            projectService, mock(ProjectVisibilityFilter.class),
            new ResourceVisibilityPolicyRegistry(List.of(new ProjectVisibilityPolicy())),
            mock(ProjectDeploymentFacade.class), mock(ProjectWorkflowFacade.class),
            mock(SharedTemplateFileStorage.class), mock(SharedTemplateService.class), mock(TagService.class),
            mock(WorkflowService.class), mock(WorkflowTestConfigurationService.class),
            mock(WorkflowNodeTestOutputService.class), List.of());
    }

    @Test
    void testValidatesBeforeSaving() {
        projectFacade.updateProjectErrorWorkflow(1L, 5L);

        Mockito.verify(errorWorkflowConfigurationValidator)
            .validate(1L, 5L, null);
        Mockito.verify(projectService)
            .updateErrorWorkflow(1L, 5L);
    }

    @Test
    void testClearingSkipsValidation() {
        projectFacade.updateProjectErrorWorkflow(1L, null);

        Mockito.verifyNoInteractions(errorWorkflowConfigurationValidator);
        Mockito.verify(projectService)
            .updateErrorWorkflow(1L, null);
    }

    @Test
    void testRejectedReferenceIsNotSaved() {
        Mockito.doThrow(new IllegalArgumentException("nope"))
            .when(errorWorkflowConfigurationValidator)
            .validate(1L, 5L, null);

        Assertions.assertThrows(
            IllegalArgumentException.class, () -> projectFacade.updateProjectErrorWorkflow(1L, 5L));
        Mockito.verify(projectService, Mockito.never())
            .updateErrorWorkflow(ArgumentMatchers.anyLong(), ArgumentMatchers.any());
    }
}
