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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ai.agent.skill.facade.AiAgentSkillFacade;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AiAgentUtilsSkillsToolTest {

    @Mock
    private AiAgentSkillFacade aiAgentSkillFacade;

    @Mock
    private Context context;

    @Mock
    private Parameters connectionParameters;

    @Mock
    private Parameters inputParameters;

    private AiAgentUtilsSkillsTool agentUtilsSkillsTool;

    @BeforeEach
    void setUp() {
        agentUtilsSkillsTool = new AiAgentUtilsSkillsTool(aiAgentSkillFacade);
    }

    @Test
    void testApplyWithEmptySkillsListThrows() {
        when(inputParameters.getList("skills", Object.class, List.of())).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, this::invokeApply);
        verifyNoInteractions(aiAgentSkillFacade);
    }

    @Test
    void testApplyWithValidSkillFetchesZipBytes() throws Exception {
        byte[] zipBytes = createZipWithMdFile("skill1/SKILL.md", createSkillMd("Test Skill", "Do something useful."));

        when(inputParameters.getList("skills", Object.class, List.of()))
            .thenReturn(List.of(Map.of("skillId", "42")));
        when(aiAgentSkillFacade.getAiAgentSkillDownload(42L)).thenReturn(zipBytes);

        ToolCallbackProvider toolCallbackProvider = invokeApply();

        assertNotNull(toolCallbackProvider);
        verify(aiAgentSkillFacade).getAiAgentSkillDownload(42L);
    }

    @Test
    void testApplyWithNonMapSkillItemThrows() {
        when(inputParameters.getList("skills", Object.class, List.of()))
            .thenReturn(List.of("not-a-map"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, this::invokeApply);

        assertTrue(exception.getMessage()
            .contains("Failed to load 1 of 1"));
        assertTrue(exception.getMessage()
            .contains("Unexpected skill item type"));
    }

    @Test
    void testApplyWithMissingSkillIdKeyThrows() {
        when(inputParameters.getList("skills", Object.class, List.of()))
            .thenReturn(List.of(Map.of("wrongKey", "1")));

        IllegalStateException exception = assertThrows(IllegalStateException.class, this::invokeApply);

        assertTrue(exception.getMessage()
            .contains("missing 'skillId' key"));
    }

    @Test
    void testApplyWithInvalidSkillIdThrows() {
        when(inputParameters.getList("skills", Object.class, List.of()))
            .thenReturn(List.of(Map.of("skillId", "not-a-number")));

        IllegalStateException exception = assertThrows(IllegalStateException.class, this::invokeApply);

        assertTrue(exception.getMessage()
            .contains("Invalid skill ID value"));
    }

    @Test
    void testApplyAccumulatesMultipleErrors() {
        when(inputParameters.getList("skills", Object.class, List.of()))
            .thenReturn(List.of(
                "not-a-map",
                Map.of("wrongKey", "value"),
                Map.of("skillId", "invalid")));

        IllegalStateException exception = assertThrows(IllegalStateException.class, this::invokeApply);

        assertTrue(exception.getMessage()
            .contains("Failed to load 3 of 3"));
    }

    @Test
    void testApplyWithSkillLoadFailureCollectsError() {
        when(inputParameters.getList("skills", Object.class, List.of()))
            .thenReturn(List.of(Map.of("skillId", "99")));
        when(aiAgentSkillFacade.getAiAgentSkillDownload(99L))
            .thenThrow(new IllegalArgumentException("AiAgentSkill not found with id: 99"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, this::invokeApply);

        assertTrue(exception.getMessage()
            .contains("Failed to load 1 of 1"));
        assertTrue(exception.getMessage()
            .contains("Failed to load skill ID 99"));
    }

    @Test
    void testApplyWithMultipleValidSkillsFetchesAll() throws Exception {
        byte[] zipBytes1 = createZipWithMdFile("skill1/SKILL.md", createSkillMd("Skill 1", "First skill."));
        byte[] zipBytes2 = createZipWithMdFile("skill2/SKILL.md", createSkillMd("Skill 2", "Second skill."));

        when(inputParameters.getList("skills", Object.class, List.of()))
            .thenReturn(List.of(Map.of("skillId", "1"), Map.of("skillId", "2")));
        when(aiAgentSkillFacade.getAiAgentSkillDownload(1L)).thenReturn(zipBytes1);
        when(aiAgentSkillFacade.getAiAgentSkillDownload(2L)).thenReturn(zipBytes2);

        ToolCallbackProvider toolCallbackProvider = invokeApply();

        assertNotNull(toolCallbackProvider);
        verify(aiAgentSkillFacade).getAiAgentSkillDownload(1L);
        verify(aiAgentSkillFacade).getAiAgentSkillDownload(2L);
    }

    @Test
    void testClusterElementDefinitionNotNull() {
        assertNotNull(agentUtilsSkillsTool.clusterElementDefinition);
    }

    private ToolCallbackProvider invokeApply() throws Exception {
        Method applyMethod = AiAgentUtilsSkillsTool.class.getDeclaredMethod(
            "apply", Parameters.class, Parameters.class, Context.class);

        applyMethod.setAccessible(true);

        try {
            return (ToolCallbackProvider) applyMethod.invoke(
                agentUtilsSkillsTool, inputParameters, connectionParameters, context);
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

    private String createSkillMd(String name, String description) {
        return "---\nname: " + name + "\ndescription: " + description + "\n---\n\n" + description;
    }

    private byte[] createZipWithMdFile(String entryName, String content) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            zipOutputStream.putNextEntry(new ZipEntry(entryName));
            zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
            zipOutputStream.finish();

            return byteArrayOutputStream.toByteArray();
        } catch (IOException ioException) {
            throw new RuntimeException("Failed to create test zip", ioException);
        }
    }
}
