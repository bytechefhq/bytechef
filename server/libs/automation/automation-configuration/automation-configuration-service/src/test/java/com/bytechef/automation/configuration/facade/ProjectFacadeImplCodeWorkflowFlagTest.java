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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.automation.configuration.security.ProjectVisibilityPolicy;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.PreBuiltTemplateService;
import com.bytechef.automation.configuration.service.ProjectCodeWorkflowInfoSupplier;
import com.bytechef.automation.configuration.service.ProjectCodeWorkflowInfoSupplier.CodeWorkflowInfo;
import com.bytechef.automation.configuration.service.ProjectCodeWorkflowInfoSupplier.CodeWorkflowSource;
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
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class ProjectFacadeImplCodeWorkflowFlagTest {

    private static final long PROJECT_ID = 42L;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private ComponentDefinitionHelper componentDefinitionHelper;

    @Mock
    private PreBuiltTemplateService preBuiltTemplateService;

    @Mock
    private Project project;

    @Mock
    private ObjectProvider<ProjectCodeWorkflowInfoSupplier> projectCodeWorkflowInfoSupplierProvider;

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private TagService tagService;

    private ProjectFacadeImpl projectFacade;

    @BeforeEach
    void setUp() {
        projectFacade = new ProjectFacadeImpl(
            "CE", applicationProperties, mock(CategoryService.class), componentDefinitionHelper,
            mock(ErrorWorkflowConfigurationValidator.class), mock(PermissionService.class),
            preBuiltTemplateService,
            projectCodeWorkflowInfoSupplierProvider, projectWorkflowService, mock(ProjectDeploymentService.class),
            projectService, mock(ProjectVisibilityFilter.class),
            new ResourceVisibilityPolicyRegistry(List.of(new ProjectVisibilityPolicy())),
            mock(ProjectDeploymentFacade.class), mock(ProjectWorkflowFacade.class),
            mock(SharedTemplateFileStorage.class), mock(SharedTemplateService.class), tagService,
            mock(WorkflowService.class), mock(WorkflowTestConfigurationService.class),
            mock(WorkflowNodeTestOutputService.class), List.of());

        when(project.getId()).thenReturn(PROJECT_ID);
        when(projectService.getProject(PROJECT_ID)).thenReturn(project);
        when(projectWorkflowService.getProjectProjectWorkflowIds(anyLong(), anyInt())).thenReturn(List.of());
        when(tagService.getTags(any())).thenReturn(List.of());
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testDuplicateProjectDeploysTheSourceInsteadOfCopyingWorkflowIds() {
        ProjectCodeWorkflowInfoSupplier supplier = mock(ProjectCodeWorkflowInfoSupplier.class);

        Project duplicatedProject = mock(Project.class);

        when(duplicatedProject.getId()).thenReturn(43L);
        when(projectCodeWorkflowInfoSupplierProvider.getIfAvailable()).thenReturn(supplier);
        when(supplier.fetchCodeWorkflowSource(PROJECT_ID))
            .thenReturn(Optional.of(new CodeWorkflowSource("JAVASCRIPT", "({name: 'p'})")));
        when(projectService.create(any(Project.class))).thenReturn(duplicatedProject);
        when(projectService.getProject(43L)).thenReturn(duplicatedProject);

        projectFacade.duplicateProject(PROJECT_ID);

        verify(supplier).deployCodeWorkflowSource(43L, "JAVASCRIPT", "({name: 'p'})");
        verify(projectWorkflowService, never()).addWorkflow(anyLong(), anyInt(), any());
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testImportProjectDeploysACodeBackedExportsSource() throws Exception {
        ProjectCodeWorkflowInfoSupplier supplier = mock(ProjectCodeWorkflowInfoSupplier.class);

        Project importedProject = mock(Project.class);

        when(importedProject.getId()).thenReturn(44L);
        when(projectCodeWorkflowInfoSupplierProvider.getIfAvailable()).thenReturn(supplier);
        when(projectService.create(any(Project.class))).thenReturn(importedProject);

        byte[] projectData = codeWorkflowExport();

        long projectId = projectFacade.importProject(projectData, 1L);

        assertThat(projectId).isEqualTo(44L);

        // A code-backed export carries no workflow-*.json entries, so the import must not fall through to the
        // definition-adding path.
        verify(supplier).deployCodeWorkflowSource(44L, "JAVASCRIPT", "({name: 'imported'})");
    }

    /**
     * A minimal export of a code-backed project: project.json plus the source entry, no workflow definitions.
     */
    private static byte[] codeWorkflowExport() throws Exception {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            zipOutputStream.putNextEntry(new ZipEntry("project.json"));
            zipOutputStream.write("{\"name\":\"imported\"}".getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("code-workflow.JAVASCRIPT"));
            zipOutputStream.write("({name: 'imported'})".getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.finish();

            return byteArrayOutputStream.toByteArray();
        }
    }

    @Test
    void testGetProjectWithoutSupplierBeanReturnsFalse() {
        when(projectCodeWorkflowInfoSupplierProvider.getIfAvailable()).thenReturn(null);

        ProjectDTO projectDTO = projectFacade.getProject(PROJECT_ID);

        assertThat(projectDTO.codeWorkflow()).isFalse();
        assertThat(projectDTO.codeWorkflowLanguage()).isNull();
    }

    @Test
    void testGetProjectWithSupplierBeanReturnsLanguage() {
        ProjectCodeWorkflowInfoSupplier supplier = mock(ProjectCodeWorkflowInfoSupplier.class);

        when(projectCodeWorkflowInfoSupplierProvider.getIfAvailable()).thenReturn(supplier);
        when(supplier.fetchCodeWorkflowInfo(PROJECT_ID)).thenReturn(Optional.of(new CodeWorkflowInfo("JAVA")));

        ProjectDTO projectDTO = projectFacade.getProject(PROJECT_ID);

        assertThat(projectDTO.codeWorkflow()).isTrue();
        assertThat(projectDTO.codeWorkflowLanguage()).isEqualTo("JAVA");
    }
}
