/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.project.ProjectHandler;
import com.bytechef.automation.project.definition.ProjectDefinition;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.configuration.exception.CodeWorkflowErrorType;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade.CodeWorkflowReconciliation;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.codeworkflow.loader.automation.ProjectHandlerLoader;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.CacheManager;

/**
 * Verifies the starter templates and {@link ProjectCodeWorkflowFacadeImpl#createEmptyCodeWorkflow}.
 *
 * <p>
 * The correctness gate is that each rendered starter template actually loads through the real polyglot
 * {@link ProjectHandlerLoader} into a {@link ProjectDefinition} with the substituted name and at least one workflow.
 * Python and Ruby are guarded by an availability assumption so CI without graalpy / truffleruby skips rather than
 * fails. JavaScript is always available.
 *
 * <p>
 * The facade tests exercise the JavaScript deploy path (services mocked, real loader) and the validation branches.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
class ProjectCodeWorkflowFacadeCreateEmptyTest {

    @Test
    void testJavaScriptStarterLoadsThroughLoader() throws IOException {
        assertStarterLoads(Language.JAVASCRIPT);
    }

    @Test
    void testPythonStarterLoadsThroughLoader() throws IOException {
        assumePolyglotAvailable("python");

        assertStarterLoads(Language.PYTHON);
    }

    @Test
    void testRubyStarterLoadsThroughLoader() throws IOException {
        assumePolyglotAvailable("ruby");

        assertStarterLoads(Language.RUBY);
    }

    @Test
    void testCreateEmptyCodeWorkflowDeploysLoadableJavaScriptProject() {
        ProjectService projectService = mock(ProjectService.class);
        ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);
        ProjectCodeWorkflowService projectCodeWorkflowService = mock(ProjectCodeWorkflowService.class);

        Project createdProject = new Project();

        createdProject.setId(1L);
        createdProject.setName("my-code-project");

        when(projectService.fetchProject("my-code-project")).thenReturn(Optional.empty());
        when(projectService.create(any()))
            .thenReturn(createdProject);

        when(projectCodeWorkflowService.fetchProjectCodeWorkflow(1L)).thenReturn(Optional.empty());

        CodeWorkflowContainer codeWorkflowContainer = mock(CodeWorkflowContainer.class);

        CodeWorkflowReconciliation reconciliation = new CodeWorkflowReconciliation(
            codeWorkflowContainer, Map.of(), Map.of());

        when(codeWorkflowContainerFacade.create(any(), any(), any(), eq(Language.JAVASCRIPT), any(), any(), any()))
            .thenReturn(reconciliation);

        ProjectCodeWorkflowFacadeImpl projectCodeWorkflowFacade = new ProjectCodeWorkflowFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), projectService, projectWorkflowService,
            codeWorkflowContainerFacade, projectCodeWorkflowService, mock(CodeWorkflowContainerService.class),
            mock(CodeWorkflowFileStorage.class), mock(TagService.class), mock(WorkflowService.class), List.of());

        Project project = projectCodeWorkflowFacade.createEmptyCodeWorkflow(1L, "my-code-project", Language.JAVASCRIPT);

        assertThat(project.getId()).isEqualTo(1L);
        assertThat(project.getName()).isEqualTo("my-code-project");

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);

        verify(projectService).create(projectCaptor.capture());

        assertThat(projectCaptor.getValue()
            .getName()).isEqualTo("my-code-project");

        verify(projectCodeWorkflowService).create(codeWorkflowContainer, createdProject);
        verify(projectService, never()).publishProject(anyLong(), any(), anyBoolean());
    }

    // RUBY-DISABLED: RUBY joins JAVA in the rejected set — org.graalvm.polyglot:ruby is published only up to 25.0.0
    // and crashes on the Truffle 25.2.4 this repo pins, so create-empty rejects it with LANGUAGE_NOT_SUPPORTED rather
    // than rendering a starter it cannot load. Drop RUBY from names below once a polyglot ruby jar built on Truffle
    // 25.2+ is published (or GraalVM is downgraded). Grep RUBY-DISABLED.
    @ParameterizedTest
    @EnumSource(value = Language.class, names = {
        "JAVA", "RUBY"
    })
    void testCreateEmptyCodeWorkflowRejectsUnsupportedLanguage(Language language) {
        ProjectService projectService = mock(ProjectService.class);
        ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);
        ProjectCodeWorkflowService projectCodeWorkflowService = mock(ProjectCodeWorkflowService.class);

        ProjectCodeWorkflowFacadeImpl projectCodeWorkflowFacade = new ProjectCodeWorkflowFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), projectService, projectWorkflowService,
            codeWorkflowContainerFacade, projectCodeWorkflowService, mock(CodeWorkflowContainerService.class),
            mock(CodeWorkflowFileStorage.class), mock(TagService.class), mock(WorkflowService.class), List.of());

        assertThatThrownBy(() -> projectCodeWorkflowFacade.createEmptyCodeWorkflow(1L, "my-code-project", language))
            .isInstanceOf(ConfigurationException.class)
            .extracting(thrown -> ((ConfigurationException) thrown).getErrorKey())
            .isEqualTo(CodeWorkflowErrorType.LANGUAGE_NOT_SUPPORTED.getErrorKey());

        verify(projectService, never()).create(any());
    }

    @Test
    void testCreateEmptyCodeWorkflowRejectsExistingProjectName() {
        ProjectService projectService = mock(ProjectService.class);
        ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);
        ProjectCodeWorkflowService projectCodeWorkflowService = mock(ProjectCodeWorkflowService.class);

        Project existingProject = new Project();

        existingProject.setId(2L);
        existingProject.setName("my-code-project");

        when(projectService.fetchProject("my-code-project"))
            .thenReturn(Optional.of(existingProject));

        ProjectCodeWorkflowFacadeImpl projectCodeWorkflowFacade = new ProjectCodeWorkflowFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), projectService, projectWorkflowService,
            codeWorkflowContainerFacade, projectCodeWorkflowService, mock(CodeWorkflowContainerService.class),
            mock(CodeWorkflowFileStorage.class), mock(TagService.class), mock(WorkflowService.class), List.of());

        assertThatThrownBy(() -> projectCodeWorkflowFacade.createEmptyCodeWorkflow(
            1L, "my-code-project", Language.JAVASCRIPT))
                .isInstanceOf(ConfigurationException.class)
                .extracting(thrown -> ((ConfigurationException) thrown).getErrorKey())
                .isEqualTo(CodeWorkflowErrorType.CODE_WORKFLOW_ALREADY_EXISTS.getErrorKey());

        verify(projectService, never()).create(any());
        verify(codeWorkflowContainerFacade, never()).create(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testCreateEmptyCodeWorkflowRejectsBlankName() {
        assertNameRejected("   ");
    }

    @Test
    void testCreateEmptyCodeWorkflowRejectsNullName() {
        assertNameRejected(null);
    }

    @Test
    void testCreateEmptyCodeWorkflowRejectsNameContainingCarriageReturn() {
        assertNameRejected("acme\rbad");
    }

    @Test
    void testCreateEmptyCodeWorkflowRejectsNameContainingQuote() {
        assertNameRejected("acme\", version: 9, x: (");
    }

    @Test
    void testCreateEmptyCodeWorkflowRejectsNameContainingBackslash() {
        assertNameRejected("acme\\bad");
    }

    @Test
    void testCreateEmptyCodeWorkflowRejectsNameContainingNewline() {
        assertNameRejected("acme\nbad");
    }

    private void assertNameRejected(String name) {
        ProjectService projectService = mock(ProjectService.class);
        ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);
        ProjectCodeWorkflowService projectCodeWorkflowService = mock(ProjectCodeWorkflowService.class);

        ProjectCodeWorkflowFacadeImpl projectCodeWorkflowFacade = new ProjectCodeWorkflowFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), projectService, projectWorkflowService,
            codeWorkflowContainerFacade, projectCodeWorkflowService, mock(CodeWorkflowContainerService.class),
            mock(CodeWorkflowFileStorage.class), mock(TagService.class), mock(WorkflowService.class), List.of());

        assertThatThrownBy(() -> projectCodeWorkflowFacade.createEmptyCodeWorkflow(1L, name, Language.JAVASCRIPT))
            .isInstanceOf(ConfigurationException.class)
            .extracting(thrown -> ((ConfigurationException) thrown).getErrorKey())
            .isEqualTo(CodeWorkflowErrorType.INVALID_CODE_WORKFLOW_NAME.getErrorKey());

        verify(projectService, never()).create(any());
        verify(projectService, never()).fetchProject(any());
    }

    private static void assertStarterLoads(Language language) throws IOException {
        String template = readTemplate(language).replace("__NAME__", "my-code-project");

        Path path = Files.createTempFile("code_workflow_starter", "." + language.getExtension());

        Files.write(path, template.getBytes(StandardCharsets.UTF_8));

        URI uri = path.toUri();

        try {
            ProjectHandler projectHandler = ProjectHandlerLoader.loadProjectHandler(
                uri.toURL(), language, ProjectHandlerLoader.JavaLoader.CLASS_LOADER,
                uri + UUID.randomUUID()
                    .toString(),
                mock(CacheManager.class));

            ProjectDefinition projectDefinition = projectHandler.getDefinition();

            assertThat(projectDefinition.getName()).isEqualTo("my-code-project");
            assertThat(projectDefinition.getWorkflows()).isNotEmpty();
            assertThat(projectDefinition.getWorkflows()
                .getFirst()
                .getName()).isEqualTo("my-workflow");
        } finally {
            Files.delete(path);
        }
    }

    private static void assumePolyglotAvailable(String languageId) {
        try (Context context = Context.create()) {
            context.eval(languageId, "1");
        } catch (RuntimeException e) {
            Assumptions.assumeTrue(false, "Polyglot language '" + languageId + "' is not available: " + e.getMessage());
        }
    }

    private static String readTemplate(Language language) throws IOException {
        String resource = "code-workflow-templates/starter." + language.getExtension();

        try (InputStream inputStream = ProjectCodeWorkflowFacadeCreateEmptyTest.class.getClassLoader()
            .getResourceAsStream(resource)) {

            if (inputStream == null) {
                throw new IllegalStateException("Missing starter template: " + resource);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static ApplicationProperties applicationProperties(boolean javaEnabled) {
        ApplicationProperties.Workflow workflow = new ApplicationProperties.Workflow();

        workflow.getCodeWorkflow()
            .setJavaEnabled(javaEnabled);

        ApplicationProperties applicationProperties = mock(ApplicationProperties.class);

        when(applicationProperties.getWorkflow()).thenReturn(workflow);

        return applicationProperties;
    }
}
