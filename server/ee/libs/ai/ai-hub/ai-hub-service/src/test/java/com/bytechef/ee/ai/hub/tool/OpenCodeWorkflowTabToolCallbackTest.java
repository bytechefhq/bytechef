/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.ee.automation.configuration.facade.ProjectCodeWorkflowFacade;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 * @author Ivica Cardic
 */
class OpenCodeWorkflowTabToolCallbackTest {

    private static final String TOOL_INPUT =
        "{\"projectId\":\"7\",\"language\":\"JAVASCRIPT\",\"name\":\"My Code Workflow\"}";

    private static final ToolContext TOOL_CONTEXT = new ToolContext(
        new AiHubToolInvocationContext(7L, 42L, null, null, 0L, "thread-9").toToolContext());

    private final AiHubChatArtifactRecorder artifactRecorder = mock(AiHubChatArtifactRecorder.class);
    private final JsonMapper jsonMapper = new JsonMapper();
    private final ProjectCodeWorkflowFacade projectCodeWorkflowFacade = mock(ProjectCodeWorkflowFacade.class);

    @Test
    void testToolDefinitionName() {
        OpenCodeWorkflowTabToolCallback callback = new OpenCodeWorkflowTabToolCallback(
            projectCodeWorkflowFacade, null);

        assertThat(callback.getToolDefinition()
            .name()).isEqualTo("openCodeWorkflowTab");
    }

    @Test
    void testCallOpensAndRecordsAVisibleCodeWorkflow() {
        when(projectCodeWorkflowFacade.getCodeWorkflowProjects()).thenReturn(List.of(project(3L), project(7L)));

        OpenCodeWorkflowTabToolCallback callback = new OpenCodeWorkflowTabToolCallback(
            projectCodeWorkflowFacade, artifactRecorder);

        JsonNode resultJsonNode = jsonMapper.readTree(callback.call(TOOL_INPUT, TOOL_CONTEXT));

        assertThat(resultJsonNode.get("opened")
            .asBoolean()).isTrue();
        assertThat(resultJsonNode.get("projectId")
            .asText()).isEqualTo("7");
        assertThat(resultJsonNode.get("language")
            .asText()).isEqualTo("JAVASCRIPT");
        assertThat(resultJsonNode.get("name")
            .asText()).isEqualTo("My Code Workflow");

        verify(artifactRecorder).recordReference(
            "thread-9", 42L, "CODE_WORKFLOW_REFERENCED", "7", "My Code Workflow");
    }

    @Test
    void testCallOpensAVisibleCodeWorkflowWithoutARecorder() {
        when(projectCodeWorkflowFacade.getCodeWorkflowProjects()).thenReturn(List.of(project(7L)));

        OpenCodeWorkflowTabToolCallback callback = new OpenCodeWorkflowTabToolCallback(
            projectCodeWorkflowFacade, null);

        JsonNode resultJsonNode = jsonMapper.readTree(callback.call(TOOL_INPUT, TOOL_CONTEXT));

        assertThat(resultJsonNode.get("opened")
            .asBoolean()).isTrue();
    }

    @Test
    void testCallReturnsErrorAndRecordsNothingWhenTheCallerMayNotSeeCodeWorkflows() {
        when(projectCodeWorkflowFacade.getCodeWorkflowProjects()).thenThrow(new AccessDeniedException("denied"));

        assertErrorWithoutRecord(TOOL_INPUT);
    }

    @Test
    void testCallReturnsErrorAndRecordsNothingForAProjectThatIsNotAVisibleCodeWorkflow() {
        when(projectCodeWorkflowFacade.getCodeWorkflowProjects()).thenReturn(List.of(project(3L)));

        assertErrorWithoutRecord(TOOL_INPUT);
    }

    @Test
    void testCallReturnsErrorForANonNumericProjectId() {
        assertErrorWithoutRecord("{\"projectId\":\"project-7\",\"language\":\"JAVASCRIPT\",\"name\":\"Bad\"}");

        verifyNoInteractions(projectCodeWorkflowFacade);
    }

    @Test
    void testCallReturnsErrorWhenRequiredFieldsAreMissing() {
        assertErrorWithoutRecord("{\"projectId\":\"\",\"language\":\"JAVASCRIPT\",\"name\":\"My Code Workflow\"}");
        assertErrorWithoutRecord("{\"projectId\":\"7\",\"language\":\"\",\"name\":\"My Code Workflow\"}");
        assertErrorWithoutRecord("{\"projectId\":\"7\",\"language\":\"JAVASCRIPT\",\"name\":\"\"}");

        verifyNoInteractions(projectCodeWorkflowFacade);
    }

    private void assertErrorWithoutRecord(String toolInput) {
        OpenCodeWorkflowTabToolCallback callback = new OpenCodeWorkflowTabToolCallback(
            projectCodeWorkflowFacade, artifactRecorder);

        JsonNode resultJsonNode = jsonMapper.readTree(callback.call(toolInput, TOOL_CONTEXT));

        assertThat(resultJsonNode.has("error")).isTrue();
        assertThat(resultJsonNode.has("opened")).isFalse();

        verify(artifactRecorder, never()).recordReference(anyString(), any(), anyString(), anyString(), anyString());
    }

    private static Project project(long id) {
        Project project = new Project();

        project.setId(id);

        return project;
    }
}
