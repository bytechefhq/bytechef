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

package com.bytechef.component.ai.agent.utils.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.ai.agent.utils.test.util.AiAgentUtilsTestUtils;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.script.engine.PolyglotEngine;
import com.bytechef.platform.ai.skill.facade.AiSkillFacade;
import com.bytechef.platform.component.definition.ClusterElementContextAware;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class AiAgentUtilsSkillsToolTest {

    @Mock
    private AiSkillFacade aiSkillFacade;

    @Mock
    private ClusterElementContextAware context;

    @Mock
    private Parameters connectionParameters;

    @Mock
    private Parameters extensions;

    @Mock
    private Parameters inputParameters;

    @Mock
    private PolyglotEngine polyglotEngine;

    private AiAgentUtilsSkillsTool agentUtilsSkillsTool;

    @BeforeEach
    void setUp() {
        agentUtilsSkillsTool = new AiAgentUtilsSkillsTool(aiSkillFacade, polyglotEngine);
    }

    @Test
    void testApplyWithEmptySkillsListThrows() {
        when(inputParameters.getList("skills", Long.class, List.of())).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, this::invokeApply);
        verifyNoInteractions(aiSkillFacade);
    }

    @Test
    void testApplyWithValidSkillFetchesZipBytes() throws Exception {
        byte[] zipBytes = AiAgentUtilsTestUtils.createZipWithMdFile("skill1/SKILL.md",
            AiAgentUtilsTestUtils.createSkillMd("Test Skill", "Do something useful."));

        when(inputParameters.getList("skills", Long.class, List.of())).thenReturn(List.of(42L));
        when(aiSkillFacade.getAiSkillDownload(42L)).thenReturn(zipBytes);

        ToolCallbackProvider toolCallbackProvider = invokeApply();

        assertNotNull(toolCallbackProvider);
        verify(aiSkillFacade).getAiSkillDownload(42L);
    }

    @Test
    void testApplyWithNullSkillItemCollectsError() {
        when(inputParameters.getList("skills", Long.class, List.of()))
            .thenReturn(Collections.singletonList(null));

        IllegalStateException exception = assertThrows(IllegalStateException.class, this::invokeApply);

        assertTrue(exception.getMessage()
            .contains("Failed to load 1 of 1"));
        assertTrue(exception.getMessage()
            .contains("Unexpected null skill item"));
    }

    @Test
    void testApplyWithSkillLoadFailureCollectsError() {
        when(inputParameters.getList("skills", Long.class, List.of())).thenReturn(List.of(99L));
        when(aiSkillFacade.getAiSkillDownload(99L))
            .thenThrow(new IllegalArgumentException("AiSkill not found with id: 99"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, this::invokeApply);

        assertTrue(exception.getMessage()
            .contains("Failed to load 1 of 1"));
        assertTrue(exception.getMessage()
            .contains("Failed to load skill ID 99"));
    }

    @Test
    void testApplyAccumulatesMultipleErrors() {
        when(inputParameters.getList("skills", Long.class, List.of()))
            .thenReturn(Arrays.asList(null, 77L));
        when(aiSkillFacade.getAiSkillDownload(77L))
            .thenThrow(new IllegalArgumentException("AiSkill not found with id: 77"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, this::invokeApply);

        assertTrue(exception.getMessage()
            .contains("Failed to load 2 of 2"));
    }

    @Test
    void testApplyWithMultipleValidSkillsFetchesAll() throws Exception {
        byte[] zipBytes1 = AiAgentUtilsTestUtils.createZipWithMdFile("skill1/SKILL.md",
            AiAgentUtilsTestUtils.createSkillMd("Skill 1", "First skill."));
        byte[] zipBytes2 = AiAgentUtilsTestUtils.createZipWithMdFile("skill2/SKILL.md",
            AiAgentUtilsTestUtils.createSkillMd("Skill 2", "Second skill."));

        when(inputParameters.getList("skills", Long.class, List.of())).thenReturn(List.of(1L, 2L));
        when(aiSkillFacade.getAiSkillDownload(1L)).thenReturn(zipBytes1);
        when(aiSkillFacade.getAiSkillDownload(2L)).thenReturn(zipBytes2);

        ToolCallbackProvider toolCallbackProvider = invokeApply();

        assertNotNull(toolCallbackProvider);
        verify(aiSkillFacade).getAiSkillDownload(1L);
        verify(aiSkillFacade).getAiSkillDownload(2L);
    }

    @Test
    void testApplyScriptToolDeclaresInputAndPassesItToScript() throws Exception {
        String script = "function perform(input, context) {\n    return input.commits;\n}\n";

        byte[] zipBytes = AiAgentUtilsTestUtils.createZipWithEntries(
            Map.of(
                "release-notes/SKILL.md",
                AiAgentUtilsTestUtils.createSkillMd("release-notes", "Turns commits into release notes."),
                "release-notes/scripts/group_commits.js", script));

        when(inputParameters.getList("skills", Long.class, List.of())).thenReturn(List.of(1L));
        when(aiSkillFacade.getAiSkillDownload(1L)).thenReturn(zipBytes);
        when(polyglotEngine.execute(eq("js"), any(Parameters.class), eq(Map.of()), eq(context)))
            .thenReturn(Map.of());

        ToolCallback scriptToolCallback = Arrays.stream(invokeApply().getToolCallbacks())
            .filter(toolCallback -> "release_notes_group_commits".equals(toolCallback.getToolDefinition()
                .name()))
            .findFirst()
            .orElseThrow();

        ToolDefinition toolDefinition = scriptToolCallback.getToolDefinition();

        Map<String, ?> inputSchema = JsonUtils.readMap(toolDefinition.inputSchema());

        assertThat(inputSchema.get("required")).isEqualTo(List.of("input"));
        assertThat(inputSchema).extractingByKey("properties", InstanceOfAssertFactories.MAP)
            .containsKey("input");
        assertThat(toolDefinition.description()).contains("scripts/group_commits.js", "release-notes");

        scriptToolCallback.call("{\"input\": {\"commits\": [\"feat: add skills tool\"]}}");

        ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);

        verify(polyglotEngine).execute(eq("js"), parametersArgumentCaptor.capture(), eq(Map.of()), eq(context));

        Parameters scriptParameters = parametersArgumentCaptor.getValue();

        assertThat(scriptParameters.getRequiredString("script")).isEqualTo(script);
        assertThat(scriptParameters.getMap("input")).isEqualTo(Map.of("commits", List.of("feat: add skills tool")));
    }

    @Test
    void testClusterElementDefinitionNotNull() {
        assertNotNull(agentUtilsSkillsTool.getClusterElementDefinition());
    }

    private ToolCallbackProvider invokeApply() throws Exception {
        Method applyMethod = AiAgentUtilsSkillsTool.class.getDeclaredMethod(
            "apply", Parameters.class, Parameters.class, Parameters.class, Map.class,
            com.bytechef.component.definition.Context.class);

        applyMethod.setAccessible(true);

        try {
            return (ToolCallbackProvider) applyMethod.invoke(
                agentUtilsSkillsTool, inputParameters, connectionParameters, extensions, Map.of(), context);
        } catch (java.lang.reflect.InvocationTargetException invocationTargetException) {
            Throwable cause = invocationTargetException.getCause();

            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            if (cause instanceof Exception exception) {
                throw exception;
            }

            throw new RuntimeException(cause);
        }
    }

}
