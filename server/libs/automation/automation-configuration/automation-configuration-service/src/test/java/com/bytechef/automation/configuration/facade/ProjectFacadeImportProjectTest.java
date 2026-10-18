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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.exception.ProjectErrorType;
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.automation.configuration.security.ProjectVisibilityPolicy;
import com.bytechef.automation.configuration.service.PreBuiltTemplateService;
import com.bytechef.automation.configuration.service.ProjectCodeWorkflowInfoSupplier;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectOwnershipService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.SharedTemplateService;
import com.bytechef.automation.configuration.util.ComponentDefinitionHelper;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.file.storage.SharedTemplateFileStorage;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.assertj.core.api.ThrowableAssertAlternative;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class ProjectFacadeImportProjectTest {

    @Mock
    private ObjectProvider<ProjectCodeWorkflowInfoSupplier> projectCodeWorkflowInfoSupplierProvider;

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectWorkflowFacade projectWorkflowFacade;

    private ProjectFacadeImpl projectFacade;

    @BeforeEach
    void setUp() {
        projectFacade = new ProjectFacadeImpl(
            "CE", mock(ApplicationProperties.class), mock(CategoryService.class), mock(ComponentDefinitionHelper.class),
            mock(PreBuiltTemplateService.class), projectCodeWorkflowInfoSupplierProvider,
            mock(ProjectWorkflowService.class), mock(ProjectDeploymentService.class),
            mock(ProjectOwnershipService.class), projectService, mock(ProjectVersionWorkflowChangeFacade.class),
            mock(ProjectVisibilityFilter.class),
            new ResourceVisibilityPolicyRegistry(List.of(new ProjectVisibilityPolicy())),
            mock(ProjectDeploymentFacade.class), projectWorkflowFacade, mock(SharedTemplateFileStorage.class),
            mock(SharedTemplateService.class), mock(TagService.class), mock(WorkflowService.class),
            mock(WorkflowTestConfigurationService.class), mock(WorkflowNodeTestOutputService.class), List.of());
    }

    @Test
    void testImportProjectImportsAnExportedEmptyProject() throws IOException {
        Project importedProject = mock(Project.class);

        when(importedProject.getId()).thenReturn(44L);
        when(projectService.create(any(Project.class))).thenReturn(importedProject);

        long projectId = projectFacade.importProject(
            projectArchive(Map.of("project.json", "{\"name\":\"empty\"}")), 1L);

        assertThat(projectId).isEqualTo(44L);

        verify(projectWorkflowFacade, never()).addWorkflow(anyLong(), any());
    }

    @Test
    void testImportProjectRejectsAFileThatIsNotAZipArchive() {
        assertImportRejected("not a zip file".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void testImportProjectRejectsAnArchiveWithoutProjectJson() throws IOException {
        assertImportRejected(projectArchive(Map.of("workflow-1.json", "\"{}\"")));
    }

    @Test
    void testImportProjectRejectsAProjectJsonThatIsNotValidJson() throws IOException {
        assertImportRejected(projectArchive(Map.of("project.json", "{name")))
            .withMessageContaining("project.json");
    }

    @Test
    void testImportProjectRejectsAProjectJsonWithoutName() throws IOException {
        assertImportRejected(projectArchive(Map.of("project.json", "{\"description\":\"no name\"}")));
    }

    private ThrowableAssertAlternative<ConfigurationException> assertImportRejected(byte[] projectData) {
        ThrowableAssertAlternative<ConfigurationException> throwableAssert =
            assertThatExceptionOfType(ConfigurationException.class)
                .isThrownBy(() -> projectFacade.importProject(projectData, 1L));

        throwableAssert.satisfies(configurationException -> {
            assertThat(configurationException.getErrorKey())
                .isEqualTo(ProjectErrorType.INVALID_PROJECT_FILE.getErrorKey());
            assertThat(configurationException.getCause()).isNull();
        });

        verify(projectService, never()).create(any(Project.class));

        return throwableAssert;
    }

    private static byte[] projectArchive(Map<String, String> entries) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zipOutputStream.putNextEntry(new ZipEntry(entry.getKey()));
                zipOutputStream.write(entry.getValue()
                    .getBytes(StandardCharsets.UTF_8));
                zipOutputStream.closeEntry();
            }

            zipOutputStream.finish();

            return byteArrayOutputStream.toByteArray();
        }
    }
}
