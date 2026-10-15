/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.SystemProjects;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.project.definition.ProjectDefinition;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.configuration.domain.ProjectCodeWorkflow;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowDraftService;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowService;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectCodeWorkflowSourceDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectCodeWorkflowStateDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.exception.CodeWorkflowErrorType;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AutomationWorkflowProjectCodeWorkflowFacadeTest {

    @Mock
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @Mock
    private CodeWorkflowContainerService codeWorkflowContainerService;

    @Mock
    private CodeWorkflowFileStorage codeWorkflowFileStorage;

    @Mock
    private ProjectCodeWorkflowDraftService projectCodeWorkflowDraftService;

    @Mock
    private ProjectCodeWorkflowService projectCodeWorkflowService;

    @Mock
    private ProjectService projectService;

    @Test
    void testSaveWritesTheDraftThenPublishesThroughTheCatalog() {
        when(automationWorkflowProjectFacade.fetchProjectIdByName("my-project")).thenReturn(Optional.of(7L));

        Project project = new Project();

        project.setId(7L);

        when(projectService.getProject(7L)).thenReturn(project);

        List<String> warnings = facade().save(jsProject("my-project"), Language.JAVASCRIPT);

        InOrder inOrder = inOrder(projectCodeWorkflowDraftService, automationWorkflowProjectFacade);

        inOrder.verify(projectCodeWorkflowDraftService)
            .saveDraft(eq(project), any(ProjectDefinition.class), any(), eq(Language.JAVASCRIPT));
        inOrder.verify(automationWorkflowProjectFacade)
            .publishProject(7L);

        assertThat(warnings).isNotEmpty();
    }

    @Test
    void testFirstDeployCreatesTheCatalogProject() {
        when(automationWorkflowProjectFacade.fetchProjectIdByName("acme-billing")).thenReturn(Optional.empty());
        when(automationWorkflowProjectFacade.createProject(
            eq("acme-billing"), any(), isNull(), eq(List.of()), isNull())).thenReturn(100L);

        Project project = new Project();

        project.setId(100L);

        when(projectService.getProject(100L)).thenReturn(project);

        facade().save(jsProject("acme-billing"), Language.JAVASCRIPT);

        verify(projectCodeWorkflowDraftService)
            .saveDraft(eq(project), any(ProjectDefinition.class), any(), eq(Language.JAVASCRIPT));
        verify(automationWorkflowProjectFacade).publishProject(100L);
        verify(projectService, never()).publishProject(anyLong(), any(), anyBoolean());
    }

    @Test
    void testSaveReturnsWarningForWorkflowWithNoTrigger() {
        List<String> warnings = saveWithTriggers("");

        assertThat(warnings).hasSize(1);
        assertThat(warnings.getFirst()).contains("has no trigger that can start it");
    }

    @Test
    void testSaveReturnsWarningForWorkflowWithOnlyManualTrigger() {
        List<String> warnings = saveWithTriggers("{name: 'manual', type: 'manual/v1/manual'}");

        assertThat(warnings).hasSize(1);
        assertThat(warnings.getFirst()).contains("has no trigger that can start it");
    }

    @Test
    void testSaveReturnsNoWarningForWorkflowWithPollingTrigger() {
        assertThat(saveWithTriggers("{name: 'newEmail', type: 'googleMail/v1/newEmailPolling'}")).isEmpty();
    }

    @Test
    void testSaveReturnsNoWarningForWorkflowWithRequestTrigger() {
        assertThat(saveWithTriggers("{name: 'request', type: 'request/v1/request'}")).isEmpty();
    }

    @Test
    void testCreateEmptyCodeWorkflowCreatesAnUnpublishedCatalogDraft() {
        when(automationWorkflowProjectFacade.fetchProjectIdByName("orders")).thenReturn(Optional.empty());
        when(automationWorkflowProjectFacade.createProject(
            eq("orders"), any(), eq("Sales"), eq(List.of("crm")), eq("true"))).thenReturn(7L);

        Project project = new Project();

        project.setId(7L);

        when(projectService.getProject(7L)).thenReturn(project);

        long id = facade().createEmptyCodeWorkflow(
            "orders", Language.JAVASCRIPT, null, "Sales", List.of("crm"), "true");

        assertThat(id).isEqualTo(7L);

        verify(projectCodeWorkflowDraftService)
            .saveDraft(eq(project), any(ProjectDefinition.class), any(), eq(Language.JAVASCRIPT));
        verify(automationWorkflowProjectFacade, never()).publishProject(anyLong());
    }

    @Test
    void testCreateEmptyCodeWorkflowRejectsAnExistingName() {
        when(automationWorkflowProjectFacade.fetchProjectIdByName("orders")).thenReturn(Optional.of(7L));

        assertErrorKey(
            () -> facade().createEmptyCodeWorkflow("orders", Language.JAVASCRIPT, null, null, null, null),
            CodeWorkflowErrorType.CODE_WORKFLOW_ALREADY_EXISTS);
    }

    @Test
    void testCreateEmptyCodeWorkflowRejectsTheReservedPrefix() {
        assertErrorKey(
            () -> facade().createEmptyCodeWorkflow(
                "__EMBEDDED_AUTOMATION__x", Language.JAVASCRIPT, null, null, null, null),
            CodeWorkflowErrorType.INVALID_CODE_WORKFLOW_NAME);
    }

    @Test
    void testCreateEmptyCodeWorkflowRejectsJava() {
        assertErrorKey(
            () -> facade().createEmptyCodeWorkflow("orders", Language.JAVA, null, null, null, null),
            CodeWorkflowErrorType.LANGUAGE_NOT_SUPPORTED);
    }

    @Test
    void testUpdateCodeWorkflowSourceSavesADraftWithoutPublishing() {
        stubCatalogCodeProject(7L, "orders", Language.JAVASCRIPT);

        facade().updateCodeWorkflowSource(7L, jsSource("orders"));

        verify(projectCodeWorkflowDraftService)
            .saveDraft(any(Project.class), any(ProjectDefinition.class), any(), eq(Language.JAVASCRIPT));
        verify(automationWorkflowProjectFacade, never()).publishProject(anyLong());
    }

    @Test
    void testUpdateCodeWorkflowSourceRejectsARename() {
        stubCatalogCodeProject(7L, "orders", Language.JAVASCRIPT);

        assertErrorKey(
            () -> facade().updateCodeWorkflowSource(7L, jsSource("renamed")),
            CodeWorkflowErrorType.CODE_WORKFLOW_NAME_MISMATCH);

        verify(projectCodeWorkflowDraftService, never()).saveDraft(any(), any(), any(), any());
    }

    @Test
    void testUpdateCodeWorkflowSourceRejectsAnUnloadableSource() {
        stubCatalogCodeProject(7L, "orders", Language.JAVASCRIPT);

        assertErrorKey(
            () -> facade().updateCodeWorkflowSource(7L, "this is not javascript ("),
            CodeWorkflowErrorType.SOURCE_LOAD_FAILED);
    }

    @Test
    void testCodeWorkflowSourceOfAVisualCatalogProjectFailsCleanly() {
        when(automationWorkflowProjectFacade.getProject(8L)).thenReturn(mock(AutomationWorkflowProjectDTO.class));
        when(projectCodeWorkflowService.fetchProjectCodeWorkflow(8L)).thenReturn(Optional.empty());

        assertErrorKey(
            () -> facade().getCodeWorkflowSource(8L), CodeWorkflowErrorType.LANGUAGE_NOT_SUPPORTED);
        assertErrorKey(
            () -> facade().updateCodeWorkflowSource(8L, jsSource("x")), CodeWorkflowErrorType.LANGUAGE_NOT_SUPPORTED);
    }

    @Test
    void testGetCodeWorkflowSourceReturnsContentAndLanguage() {
        stubCatalogCodeProject(7L, "orders", Language.PYTHON);

        when(codeWorkflowFileStorage.readCodeWorkflowFileContent(any())).thenReturn("source");

        assertThat(facade().getCodeWorkflowSource(7L))
            .isEqualTo(new AutomationWorkflowProjectCodeWorkflowSourceDTO("source", Language.PYTHON));
    }

    @Test
    void testGetCodeWorkflowStateIsPublishableWhenTheDraftHasItsOwnSource() {
        stubCatalogCodeProject(7L, "orders", Language.PYTHON, 3, 3);

        assertThat(facade().getCodeWorkflowState(7L))
            .contains(new AutomationWorkflowProjectCodeWorkflowStateDTO(Language.PYTHON, true));
    }

    @Test
    void testGetCodeWorkflowStateIsNotPublishableWhenTheDraftHasNoSourceOfItsOwn() {
        stubCatalogCodeProject(7L, "orders", Language.JAVASCRIPT, 2, 3);

        assertThat(facade().getCodeWorkflowState(7L))
            .contains(new AutomationWorkflowProjectCodeWorkflowStateDTO(Language.JAVASCRIPT, false));
    }

    @Test
    void testGetCodeWorkflowStateIsEmptyForAVisualProject() {
        Project project = new Project();

        project.setId(8L);
        project.setProjectVersions(List.of(new ProjectVersion(1)));

        when(projectService.getProject(8L)).thenReturn(project);
        when(projectCodeWorkflowService.fetchProjectCodeWorkflow(8L)).thenReturn(Optional.empty());

        assertThat(facade().getCodeWorkflowState(8L)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "createEmptyCodeWorkflow", "getCodeWorkflowSource", "getCodeWorkflowState", "updateCodeWorkflowSource",
        "save"
    })
    void testCodeWorkflowMethodsAreAdminOnly(String methodName) {
        Method method = Arrays.stream(AutomationWorkflowProjectCodeWorkflowFacadeImpl.class.getDeclaredMethods())
            .filter(declaredMethod -> declaredMethod.getName()
                .equals(methodName))
            .findFirst()
            .orElseThrow();

        assertThat(method.getAnnotation(PreAuthorize.class))
            .extracting(PreAuthorize::value)
            .isEqualTo("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")");
    }

    private void stubCatalogCodeProject(long projectId, String displayName, Language language) {
        stubCatalogCodeProject(projectId, displayName, language, null, 1);
    }

    private void stubCatalogCodeProject(
        long projectId, String displayName, Language language, Integer codeRowVersion, int draftVersion) {

        lenient().when(automationWorkflowProjectFacade.getProject(projectId))
            .thenReturn(mock(AutomationWorkflowProjectDTO.class));

        Project project = new Project();

        project.setId(projectId);
        project.setName(SystemProjects.EMBEDDED_AUTOMATION_NAME_PREFIX + displayName);
        project.setProjectVersions(List.of(new ProjectVersion(draftVersion)));

        lenient().when(projectService.getProject(projectId))
            .thenReturn(project);

        ProjectCodeWorkflow projectCodeWorkflow = mock(ProjectCodeWorkflow.class);

        when(projectCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(50L);

        if (codeRowVersion != null) {
            when(projectCodeWorkflow.getProjectVersion()).thenReturn(codeRowVersion);
        }

        when(projectCodeWorkflowService.fetchProjectCodeWorkflow(projectId))
            .thenReturn(Optional.of(projectCodeWorkflow));

        CodeWorkflowContainer codeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(codeWorkflowContainer.getLanguage()).thenReturn(language);
        when(codeWorkflowContainerService.getCodeWorkflowContainer(50L)).thenReturn(codeWorkflowContainer);
    }

    private static void assertErrorKey(ThrowableAssert.ThrowingCallable callable, CodeWorkflowErrorType errorType) {
        assertThatThrownBy(callable)
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(errorType.getErrorKey()));
    }

    private static String jsSource(String name) {
        return "({name: \"" + name + "\", version: \"1\", workflows: [{name: \"my-workflow\", "
            + "label: \"My Workflow\", tasks: [{name: \"my-task\", label: \"My Task\", "
            + "perform: function() { return \"hello\"; }}]}]})";
    }

    private AutomationWorkflowProjectCodeWorkflowFacadeImpl facade() {
        return new AutomationWorkflowProjectCodeWorkflowFacadeImpl(
            new ApplicationProperties(), automationWorkflowProjectFacade, mock(CacheManager.class),
            codeWorkflowContainerService, codeWorkflowFileStorage, projectCodeWorkflowDraftService,
            projectCodeWorkflowService, projectService);
    }

    private List<String> saveWithTriggers(String triggers) {
        when(automationWorkflowProjectFacade.fetchProjectIdByName("acme-billing")).thenReturn(Optional.of(100L));

        Project project = new Project();

        project.setId(100L);

        when(projectService.getProject(100L)).thenReturn(project);

        return facade().save(jsProject("acme-billing", triggers), Language.JAVASCRIPT);
    }

    private static byte[] jsProject(String name) {
        return jsProject(name, "");
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private static byte[] jsProject(String name, String triggers) {
        String source = """
            ({
                name: "%s",
                version: "1",
                description: "A code workflow.",
                workflows: [
                    {
                        name: "charge",
                        label: "Charge",
                        triggers: [%s],
                        tasks: [
                            {
                                name: "my-task",
                                label: "My Task",
                                perform: function () {
                                    return "hello";
                                }
                            }
                        ]
                    }
                ]
            })
            """.formatted(name, triggers);

        return source.getBytes(StandardCharsets.UTF_8);
    }
}
