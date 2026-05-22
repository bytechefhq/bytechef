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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.script.engine.PolyglotEngine;
import com.bytechef.ee.platform.ai.skill.facade.AiSkillFacade;
import com.bytechef.platform.component.definition.ClusterElementContextAware;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
        byte[] zipBytes = createZipWithMdFile("skill1/SKILL.md", createSkillMd("Test Skill", "Do something useful."));

        when(inputParameters.getList("skills", Long.class, List.of())).thenReturn(List.of(42L));
        when(aiSkillFacade.getAiSkillDownload(42L)).thenReturn(zipBytes);

        ToolCallbackProvider toolCallbackProvider = invokeApply();

        assertNotNull(toolCallbackProvider);
        verify(aiSkillFacade).getAiSkillDownload(42L);
    }

    @Test
    void testApplyWithNullSkillItemCollectsError() {
        when(inputParameters.getList("skills", Long.class, List.of()))
            .thenReturn(Arrays.asList((Long) null));

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
        byte[] zipBytes1 = createZipWithMdFile("skill1/SKILL.md", createSkillMd("Skill 1", "First skill."));
        byte[] zipBytes2 = createZipWithMdFile("skill2/SKILL.md", createSkillMd("Skill 2", "Second skill."));

        when(inputParameters.getList("skills", Long.class, List.of())).thenReturn(List.of(1L, 2L));
        when(aiSkillFacade.getAiSkillDownload(1L)).thenReturn(zipBytes1);
        when(aiSkillFacade.getAiSkillDownload(2L)).thenReturn(zipBytes2);

        ToolCallbackProvider toolCallbackProvider = invokeApply();

        assertNotNull(toolCallbackProvider);
        verify(aiSkillFacade).getAiSkillDownload(1L);
        verify(aiSkillFacade).getAiSkillDownload(2L);
    }

    @Test
    void testClusterElementDefinitionNotNull() {
        assertNotNull(agentUtilsSkillsTool.clusterElementDefinition);
    }

    // --- analyzeSkillScripts tests ---

    @Test
    void testAnalyzeSkillScriptsFindsComponentCallsInScriptsFolder() {
        String script = """
            function perform(input, context) {
                context.component.slack.sendMessage({'channel': 'general', 'text': 'hello'})
                context.component.logger.info({'text': 'done'})
                return null;
            }
            """;

        byte[] zipBytes = createZipWithEntries(
            Map.of("my-skill/SKILL.md", createSkillMd("My Skill", "desc"),
                "my-skill/scripts/main.js", script));

        Map<String, List<AiAgentUtilsSkillsTool.ComponentCall>> result =
            agentUtilsSkillsTool.analyzeSkillScripts(zipBytes);

        assertFalse(result.isEmpty());

        List<AiAgentUtilsSkillsTool.ComponentCall> calls = result.get("my-skill/scripts/main.js");

        assertNotNull(calls);
        assertEquals(2, calls.size());
        assertEquals("slack", calls.get(0)
            .componentName());
        assertEquals("sendMessage", calls.get(0)
            .actionName());
        assertEquals("logger", calls.get(1)
            .componentName());
        assertEquals("info", calls.get(1)
            .actionName());
    }

    @Test
    void testAnalyzeSkillScriptsIgnoresFilesOutsideScriptsFolder() {
        String script = """
            function perform(input, context) {
                context.component.slack.sendMessage({'text': 'hi'})
                return null;
            }
            """;

        byte[] zipBytes = createZipWithEntries(
            Map.of("my-skill/SKILL.md", createSkillMd("My Skill", "desc"),
                "my-skill/references/notes.js", script));

        Map<String, List<AiAgentUtilsSkillsTool.ComponentCall>> result =
            agentUtilsSkillsTool.analyzeSkillScripts(zipBytes);

        assertTrue(result.isEmpty());
    }

    @Test
    void testAnalyzeSkillScriptsSkipsScriptWithoutPerformFunction() {
        String script = "context.component.slack.sendMessage({'text': 'hi'})";

        byte[] zipBytes = createZipWithEntries(
            Map.of("my-skill/scripts/helper.js", script));

        Map<String, List<AiAgentUtilsSkillsTool.ComponentCall>> result =
            agentUtilsSkillsTool.analyzeSkillScripts(zipBytes);

        assertTrue(result.isEmpty());
    }

    @Test
    void testAnalyzeSkillScriptsReturnsEmptyWhenPerformHasNoComponentCalls() {
        String script = """
            function perform(input, context) {
                return null;
            }
            """;

        byte[] zipBytes = createZipWithEntries(Map.of("my-skill/scripts/main.js", script));

        Map<String, List<AiAgentUtilsSkillsTool.ComponentCall>> result =
            agentUtilsSkillsTool.analyzeSkillScripts(zipBytes);

        assertTrue(result.isEmpty());
    }

    @Test
    void testAnalyzeSkillScriptsHandlesEmptyParameterList() {
        String script = """
            function perform(input, context) {
                context.component.http.get()
                return null;
            }
            """;

        byte[] zipBytes = createZipWithEntries(Map.of("skill/scripts/main.js", script));

        Map<String, List<AiAgentUtilsSkillsTool.ComponentCall>> result =
            agentUtilsSkillsTool.analyzeSkillScripts(zipBytes);

        List<AiAgentUtilsSkillsTool.ComponentCall> calls = result.get("skill/scripts/main.js");

        assertNotNull(calls);
        assertEquals(1, calls.size());
        assertEquals("", calls.get(0)
            .rawParameters());
    }

    // --- isScriptEntry tests ---

    @Test
    void testIsScriptEntryTopLevel() {
        assertTrue(AiAgentUtilsSkillsTool.isScriptEntry("scripts/main.js"));
    }

    @Test
    void testIsScriptEntryNested() {
        assertTrue(AiAgentUtilsSkillsTool.isScriptEntry("skill-name/scripts/main.py"));
    }

    @Test
    void testIsScriptEntryReturnsFalseForMdFile() {
        assertFalse(AiAgentUtilsSkillsTool.isScriptEntry("skill-name/SKILL.md"));
    }

    @Test
    void testIsScriptEntryReturnsFalseForReferences() {
        assertFalse(AiAgentUtilsSkillsTool.isScriptEntry("skill-name/references/guide.md"));
    }

    // --- hasPerformFunction tests ---

    @Test
    void testHasPerformFunctionJavaScript() {
        assertTrue(AiAgentUtilsSkillsTool.hasPerformFunction("function perform(input, context) {\n  return null;\n}"));
    }

    @Test
    void testHasPerformFunctionPython() {
        assertTrue(AiAgentUtilsSkillsTool.hasPerformFunction("def perform(input, context):\n  return None"));
    }

    @Test
    void testHasPerformFunctionR() {
        assertTrue(
            AiAgentUtilsSkillsTool.hasPerformFunction("perform <- function(input, context) {\n  return(NULL)\n}"));
    }

    @Test
    void testHasPerformFunctionReturnsFalseWhenAbsent() {
        assertFalse(AiAgentUtilsSkillsTool.hasPerformFunction("function helper() {}"));
    }

    // --- extractComponentCalls tests ---

    @Test
    void testExtractComponentCallsParsesMultipleCalls() {
        String content = """
            context.component.slack.sendMessage({'channel': 'dev'})
            context.component.gmail.sendEmail({'to': 'a@b.com', 'subject': 'hi'})
            """;

        List<AiAgentUtilsSkillsTool.ComponentCall> calls =
            AiAgentUtilsSkillsTool.extractComponentCalls(content);

        assertEquals(2, calls.size());
        assertEquals("slack", calls.get(0)
            .componentName());
        assertEquals("sendMessage", calls.get(0)
            .actionName());
        assertEquals("gmail", calls.get(1)
            .componentName());
        assertEquals("sendEmail", calls.get(1)
            .actionName());
    }

    @Test
    void testExtractComponentCallsReturnsEmptyWhenNonePresent() {
        assertTrue(AiAgentUtilsSkillsTool.extractComponentCalls("return null;")
            .isEmpty());
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

    private String createSkillMd(String name, String description) {
        return "---\nname: " + name + "\ndescription: " + description + "\n---\n\n" + description;
    }

    private byte[] createZipWithMdFile(String entryName, String content) {
        return createZipWithEntries(Map.of(entryName, content));
    }

    private byte[] createZipWithEntries(Map<String, String> entries) {
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
        } catch (IOException ioException) {
            throw new RuntimeException("Failed to create test zip", ioException);
        }
    }
}
