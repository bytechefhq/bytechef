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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.configuration.domain.ProjectCodeWorkflow;
import com.bytechef.ee.automation.configuration.exception.CodeWorkflowErrorType;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade.CodeWorkflowReconciliation;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tag.service.TagService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.CacheManager;

/**
 * Verifies {@link ProjectCodeWorkflowFacadeImpl#getCodeWorkflowSource} and
 * {@link ProjectCodeWorkflowFacadeImpl#updateCodeWorkflowSource}.
 *
 * <p>
 * {@code getCodeWorkflowSource} returns the stored script for a polyglot-backed container and rejects Java containers
 * since compiled jars have no editable source. {@code updateCodeWorkflowSource} compile-gates the incoming content
 * through the real JavaScript loader (an unparseable script fails the deploy before anything is persisted) and
 * name-locks it against the owning project (editing source cannot rename the project).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectCodeWorkflowFacadeSourceTest {

    @Test
    void testGetCodeWorkflowSourceReturnsSourceForNonJavaLanguage() {
        FileEntry workflowsFile = new FileEntry("my-code-project.js", "file:///my-code-project.js");

        ProjectCodeWorkflow projectCodeWorkflow = mock(ProjectCodeWorkflow.class);

        when(projectCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(5L);

        CodeWorkflowContainer codeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(codeWorkflowContainer.getLanguage()).thenReturn(Language.JAVASCRIPT);
        when(codeWorkflowContainer.getWorkflows()).thenReturn(workflowsFile);

        ProjectCodeWorkflowService projectCodeWorkflowService = mock(ProjectCodeWorkflowService.class);

        when(projectCodeWorkflowService.getProjectCodeWorkflow(42L)).thenReturn(projectCodeWorkflow);

        CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);

        when(codeWorkflowContainerService.getCodeWorkflowContainer(5L)).thenReturn(codeWorkflowContainer);

        CodeWorkflowFileStorage codeWorkflowFileStorage = mock(CodeWorkflowFileStorage.class);

        when(codeWorkflowFileStorage.readCodeWorkflowFileContent(workflowsFile))
            .thenReturn("({name:'my-code-project'})");

        ProjectCodeWorkflowFacadeImpl projectCodeWorkflowFacade = newFacade(
            mock(ProjectService.class), mock(ProjectWorkflowService.class), mock(CodeWorkflowContainerFacade.class),
            projectCodeWorkflowService, codeWorkflowContainerService, codeWorkflowFileStorage,
            mock(WorkflowService.class));

        String source = projectCodeWorkflowFacade.getCodeWorkflowSource(42L);

        assertThat(source).isEqualTo("({name:'my-code-project'})");
    }

    @Test
    void testGetCodeWorkflowSourceThrowsForJavaLanguage() {
        ProjectCodeWorkflow projectCodeWorkflow = mock(ProjectCodeWorkflow.class);

        when(projectCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(5L);

        CodeWorkflowContainer codeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(codeWorkflowContainer.getLanguage()).thenReturn(Language.JAVA);

        ProjectCodeWorkflowService projectCodeWorkflowService = mock(ProjectCodeWorkflowService.class);

        when(projectCodeWorkflowService.getProjectCodeWorkflow(42L)).thenReturn(projectCodeWorkflow);

        CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);

        when(codeWorkflowContainerService.getCodeWorkflowContainer(5L)).thenReturn(codeWorkflowContainer);

        CodeWorkflowFileStorage codeWorkflowFileStorage = mock(CodeWorkflowFileStorage.class);

        ProjectCodeWorkflowFacadeImpl projectCodeWorkflowFacade = newFacade(
            mock(ProjectService.class), mock(ProjectWorkflowService.class), mock(CodeWorkflowContainerFacade.class),
            projectCodeWorkflowService, codeWorkflowContainerService, codeWorkflowFileStorage,
            mock(WorkflowService.class));

        assertThatThrownBy(() -> projectCodeWorkflowFacade.getCodeWorkflowSource(42L))
            .isInstanceOf(ConfigurationException.class)
            .extracting(thrown -> ((ConfigurationException) thrown).getErrorKey())
            .isEqualTo(CodeWorkflowErrorType.LANGUAGE_NOT_SUPPORTED.getErrorKey());

        verifyNoInteractions(codeWorkflowFileStorage);
    }

    @Test
    void testUpdateCodeWorkflowSourceWithSameNameRedeploys() {
        ProjectCodeWorkflow projectCodeWorkflow = mock(ProjectCodeWorkflow.class);

        when(projectCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(5L);
        when(projectCodeWorkflow.getProjectVersion()).thenReturn(1);

        CodeWorkflowContainer existingCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(existingCodeWorkflowContainer.getLanguage()).thenReturn(Language.JAVASCRIPT);

        ProjectCodeWorkflowService projectCodeWorkflowService = mock(ProjectCodeWorkflowService.class);

        when(projectCodeWorkflowService.getProjectCodeWorkflow(1L)).thenReturn(projectCodeWorkflow);
        when(projectCodeWorkflowService.fetchProjectCodeWorkflow(1L)).thenReturn(Optional.of(projectCodeWorkflow));

        CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);

        when(codeWorkflowContainerService.getCodeWorkflowContainer(5L)).thenReturn(existingCodeWorkflowContainer);

        Project project = new Project();

        project.setId(1L);
        project.setName("my-code-project");

        ProjectService projectService = mock(ProjectService.class);

        when(projectService.getProject(1L)).thenReturn(project);

        ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);

        CodeWorkflowContainer redeployedCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        CodeWorkflowReconciliation reconciliation = new CodeWorkflowReconciliation(
            redeployedCodeWorkflowContainer, Map.of(), Map.of());

        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);

        when(codeWorkflowContainerFacade.update(
            eq(existingCodeWorkflowContainer), any(), any(), any(), eq(PlatformType.AUTOMATION)))
                .thenReturn(reconciliation);

        CodeWorkflowFileStorage codeWorkflowFileStorage = mock(CodeWorkflowFileStorage.class);

        ProjectCodeWorkflowFacadeImpl projectCodeWorkflowFacade = newFacade(
            projectService, projectWorkflowService, codeWorkflowContainerFacade, projectCodeWorkflowService,
            codeWorkflowContainerService, codeWorkflowFileStorage, mock(WorkflowService.class));

        projectCodeWorkflowFacade.updateCodeWorkflowSource(1L, jsScript("my-code-project"));

        ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);

        verify(codeWorkflowContainerFacade).update(
            eq(existingCodeWorkflowContainer), eq("1"), any(), bytesCaptor.capture(), eq(PlatformType.AUTOMATION));

        assertThat(new String(bytesCaptor.getValue(), StandardCharsets.UTF_8))
            .isEqualTo(jsScript("my-code-project"));

        verify(codeWorkflowContainerFacade, never()).create(any(), any(), any(), any(), any(), any());
        verify(codeWorkflowContainerFacade, never()).create(any(), any(), any(), any(), any(), any(), any());
        verify(projectCodeWorkflowService, never()).create(any(), any());
        verify(projectService, never()).publishProject(anyLong(), any(), anyBoolean());
    }

    @Test
    void testUpdateCodeWorkflowSourceWithUnparseableScriptThrowsSourceLoadFailed() {
        ProjectCodeWorkflow projectCodeWorkflow = mock(ProjectCodeWorkflow.class);

        when(projectCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(5L);

        CodeWorkflowContainer existingCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(existingCodeWorkflowContainer.getLanguage()).thenReturn(Language.JAVASCRIPT);

        ProjectCodeWorkflowService projectCodeWorkflowService = mock(ProjectCodeWorkflowService.class);

        when(projectCodeWorkflowService.getProjectCodeWorkflow(1L)).thenReturn(projectCodeWorkflow);

        CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);

        when(codeWorkflowContainerService.getCodeWorkflowContainer(5L)).thenReturn(existingCodeWorkflowContainer);

        Project project = new Project();

        project.setId(1L);
        project.setName("my-code-project");

        ProjectService projectService = mock(ProjectService.class);

        when(projectService.getProject(1L)).thenReturn(project);

        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);

        ProjectCodeWorkflowFacadeImpl projectCodeWorkflowFacade = newFacade(
            projectService, mock(ProjectWorkflowService.class), codeWorkflowContainerFacade,
            projectCodeWorkflowService, codeWorkflowContainerService, mock(CodeWorkflowFileStorage.class),
            mock(WorkflowService.class));

        assertThatThrownBy(
            () -> projectCodeWorkflowFacade.updateCodeWorkflowSource(1L, "this is not { valid javascript ("))
                .isInstanceOf(ConfigurationException.class)
                .extracting(thrown -> ((ConfigurationException) thrown).getErrorKey())
                .isEqualTo(CodeWorkflowErrorType.SOURCE_LOAD_FAILED.getErrorKey());

        verifyNoInteractions(codeWorkflowContainerFacade);
    }

    @Test
    void testUpdateCodeWorkflowSourceOnEmbeddedBridgeProjectIsRejected() {
        ProjectCodeWorkflow projectCodeWorkflow = mock(ProjectCodeWorkflow.class);

        when(projectCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(5L);

        CodeWorkflowContainer existingCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(existingCodeWorkflowContainer.getLanguage()).thenReturn(Language.JAVASCRIPT);

        ProjectCodeWorkflowService projectCodeWorkflowService = mock(ProjectCodeWorkflowService.class);

        when(projectCodeWorkflowService.getProjectCodeWorkflow(1L)).thenReturn(projectCodeWorkflow);

        CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);

        when(codeWorkflowContainerService.getCodeWorkflowContainer(5L)).thenReturn(existingCodeWorkflowContainer);

        Project project = new Project();

        project.setId(1L);
        project.setName("__EMBEDDED_AUTOMATION__catalog-project");

        ProjectService projectService = mock(ProjectService.class);

        when(projectService.getProject(1L)).thenReturn(project);

        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);

        ProjectCodeWorkflowFacadeImpl projectCodeWorkflowFacade = newFacade(
            projectService, mock(ProjectWorkflowService.class), codeWorkflowContainerFacade,
            projectCodeWorkflowService, codeWorkflowContainerService, mock(CodeWorkflowFileStorage.class),
            mock(WorkflowService.class));

        assertThatThrownBy(() -> projectCodeWorkflowFacade.updateCodeWorkflowSource(
            1L, jsScript("catalog-project")))
                .isInstanceOf(ConfigurationException.class)
                .extracting(thrown -> ((ConfigurationException) thrown).getErrorKey())
                .isEqualTo(CodeWorkflowErrorType.EMBEDDED_BRIDGE_PROJECT_NOT_EDITABLE.getErrorKey());

        verifyNoInteractions(codeWorkflowContainerFacade);
    }

    @Test
    void testUpdateCodeWorkflowSourceWithDifferentNameThrowsCodeWorkflowNameMismatch() {
        ProjectCodeWorkflow projectCodeWorkflow = mock(ProjectCodeWorkflow.class);

        when(projectCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(5L);

        CodeWorkflowContainer existingCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(existingCodeWorkflowContainer.getLanguage()).thenReturn(Language.JAVASCRIPT);

        ProjectCodeWorkflowService projectCodeWorkflowService = mock(ProjectCodeWorkflowService.class);

        when(projectCodeWorkflowService.getProjectCodeWorkflow(1L)).thenReturn(projectCodeWorkflow);

        CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);

        when(codeWorkflowContainerService.getCodeWorkflowContainer(5L)).thenReturn(existingCodeWorkflowContainer);

        Project project = new Project();

        project.setId(1L);
        project.setName("my-code-project");

        ProjectService projectService = mock(ProjectService.class);

        when(projectService.getProject(1L)).thenReturn(project);

        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);

        ProjectCodeWorkflowFacadeImpl projectCodeWorkflowFacade = newFacade(
            projectService, mock(ProjectWorkflowService.class), codeWorkflowContainerFacade,
            projectCodeWorkflowService, codeWorkflowContainerService, mock(CodeWorkflowFileStorage.class),
            mock(WorkflowService.class));

        assertThatThrownBy(() -> projectCodeWorkflowFacade.updateCodeWorkflowSource(
            1L, jsScript("a-completely-different-name")))
                .isInstanceOf(ConfigurationException.class)
                .extracting(thrown -> ((ConfigurationException) thrown).getErrorKey())
                .isEqualTo(CodeWorkflowErrorType.CODE_WORKFLOW_NAME_MISMATCH.getErrorKey());

        verifyNoInteractions(codeWorkflowContainerFacade);
    }

    @Test
    void testDeployCodeWorkflowSourceRewritesTheDeclaredNameToTheProjectName() {
        ProjectCodeWorkflowService projectCodeWorkflowService = mock(ProjectCodeWorkflowService.class);

        when(projectCodeWorkflowService.fetchProjectCodeWorkflow(2L)).thenReturn(Optional.empty());

        Project project = new Project();

        project.setId(2L);
        project.setName("my-code-project (2)");

        ProjectService projectService = mock(ProjectService.class);

        when(projectService.getProject(2L)).thenReturn(project);

        CodeWorkflowContainer newCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        CodeWorkflowReconciliation reconciliation = new CodeWorkflowReconciliation(
            newCodeWorkflowContainer, Map.of(), Map.of());

        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);

        when(codeWorkflowContainerFacade.create(
            any(), any(), any(), any(), any(), eq(PlatformType.AUTOMATION), any()))
                .thenReturn(reconciliation);

        ProjectCodeWorkflowFacadeImpl projectCodeWorkflowFacade = newFacade(
            projectService, mock(ProjectWorkflowService.class), codeWorkflowContainerFacade, projectCodeWorkflowService,
            mock(CodeWorkflowContainerService.class), mock(CodeWorkflowFileStorage.class), mock(WorkflowService.class));

        projectCodeWorkflowFacade.deployCodeWorkflowSource(
            2L, Language.JAVASCRIPT, jsScript("my-code-project"));

        ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);

        verify(codeWorkflowContainerFacade).create(
            eq("my-code-project (2)"), eq("1"), any(), eq(Language.JAVASCRIPT), bytesCaptor.capture(),
            eq(PlatformType.AUTOMATION), any());

        // The copy must declare its own name: an upload resolves the target project by the declared name, so a copy
        // still declaring the original's name would redeploy over the original.
        assertThat(new String(bytesCaptor.getValue(), StandardCharsets.UTF_8))
            .isEqualTo(jsScript("my-code-project (2)"));
    }

    @Test
    void testDeployCodeWorkflowSourceThrowsWhenTheNameIsNotAStringLiteral() {
        Project project = new Project();

        project.setId(2L);
        project.setName("renamed");

        ProjectService projectService = mock(ProjectService.class);

        when(projectService.getProject(2L)).thenReturn(project);

        ProjectCodeWorkflowFacadeImpl projectCodeWorkflowFacade = newFacade(
            projectService, mock(ProjectWorkflowService.class), mock(CodeWorkflowContainerFacade.class),
            mock(ProjectCodeWorkflowService.class), mock(CodeWorkflowContainerService.class),
            mock(CodeWorkflowFileStorage.class), mock(WorkflowService.class));

        String computedNameScript = "({name: [\"my\", \"code\"].join(\"-\"), version: \"1\", workflows: []})";

        assertThatThrownBy(
            () -> projectCodeWorkflowFacade.deployCodeWorkflowSource(2L, Language.JAVASCRIPT, computedNameScript))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("plain string literal");
    }

    private static String jsScript(String name) {
        return "({name: \"" + name + "\", version: \"1\", workflows: [{name: \"my-workflow\", "
            + "label: \"My Workflow\", tasks: [{name: \"my-task\", label: \"My Task\", "
            + "perform: function() { return \"hello\"; }}]}]})";
    }

    private static ProjectCodeWorkflowFacadeImpl newFacade(
        ProjectService projectService, ProjectWorkflowService projectWorkflowService,
        CodeWorkflowContainerFacade codeWorkflowContainerFacade,
        ProjectCodeWorkflowService projectCodeWorkflowService,
        CodeWorkflowContainerService codeWorkflowContainerService, CodeWorkflowFileStorage codeWorkflowFileStorage,
        WorkflowService workflowService) {

        return new ProjectCodeWorkflowFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), projectService, projectWorkflowService,
            codeWorkflowContainerFacade, projectCodeWorkflowService, codeWorkflowContainerService,
            codeWorkflowFileStorage, mock(TagService.class), workflowService, List.of());
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
