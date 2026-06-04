/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.ai.agent.utils.workflow.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.ee.component.ai.agent.utils.test.util.AiAgentUtilsTestUtils;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Marko Kriskovic
 * @version ee
 */
public class SkillComponentConnectionFactoryTest {

    @Test
    void testExtractComponentCallsParsesMultipleCalls() {
        String content = """
            context.component.slack.sendMessage({'channel': 'dev'})
            context.component.gmail.sendEmail({'to': 'a@b.com', 'subject': 'hi'})
            """;

        List<SkillComponentConnectionFactory.ComponentCall> calls =
            SkillComponentConnectionFactory.extractComponentCalls(content);

        assertEquals(2, calls.size());

        SkillComponentConnectionFactory.ComponentCall componentCall1 = calls.getFirst();

        assertEquals("slack", componentCall1.componentName());
        assertEquals("sendMessage", componentCall1.actionName());

        SkillComponentConnectionFactory.ComponentCall componentCall2 = calls.get(1);

        assertEquals("gmail", componentCall2.componentName());
        assertEquals("sendEmail", componentCall2.actionName());
    }

    @Test
    void testExtractComponentCallsReturnsEmptyWhenNonePresent() {
        assertTrue(SkillComponentConnectionFactory.extractComponentCalls("return null;")
            .isEmpty());
    }

    @Test
    void testGetSkillScriptsFindsComponentCallsInScriptsFolder() {
        String script = """
            function perform(input, context) {
                context.component.slack.sendMessage({'channel': 'general', 'text': 'hello'})
                context.component.logger.info({'text': 'done'})
                return null;
            }
            """;

        byte[] zipBytes = AiAgentUtilsTestUtils.createZipWithEntries(
            Map.of(
                "my-skill/SKILL.md", AiAgentUtilsTestUtils.createSkillMd("My Skill", "desc"),
                "my-skill/scripts/main.js", script));

        Map<String, List<SkillComponentConnectionFactory.ComponentCall>> result =
            SkillComponentConnectionFactory.getSkillScripts(zipBytes);

        assertFalse(result.isEmpty());

        List<SkillComponentConnectionFactory.ComponentCall> calls = result.get("my-skill/scripts/main.js");

        assertNotNull(calls);
        assertEquals(2, calls.size());

        SkillComponentConnectionFactory.ComponentCall componentCall1 = calls.getFirst();

        assertEquals("slack", componentCall1.componentName());
        assertEquals("sendMessage", componentCall1.actionName());

        SkillComponentConnectionFactory.ComponentCall componentCall2 = calls.get(1);

        assertEquals("logger", componentCall2.componentName());
        assertEquals("info", componentCall2.actionName());
    }

    @Test
    void testGetSkillScriptsIgnoresFilesOutsideScriptsFolder() {
        String script = """
            function perform(input, context) {
                context.component.slack.sendMessage({'text': 'hi'})
                return null;
            }
            """;

        byte[] zipBytes = AiAgentUtilsTestUtils.createZipWithEntries(
            Map.of(
                "my-skill/SKILL.md", AiAgentUtilsTestUtils.createSkillMd("My Skill", "desc"),
                "my-skill/references/notes.js", script));

        Map<String, List<SkillComponentConnectionFactory.ComponentCall>> result =
            SkillComponentConnectionFactory.getSkillScripts(zipBytes);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetSkillScriptsSkipsScriptWithoutPerformFunction() {
        String script = "context.component.slack.sendMessage({'text': 'hi'})";

        byte[] zipBytes = AiAgentUtilsTestUtils.createZipWithEntries(Map.of("my-skill/scripts/helper.js", script));

        Map<String, List<SkillComponentConnectionFactory.ComponentCall>> result =
            SkillComponentConnectionFactory.getSkillScripts(zipBytes);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetSkillScriptsReturnsEmptyWhenPerformHasNoComponentCalls() {
        String script = """
            function perform(input, context) {
                return null;
            }
            """;

        byte[] zipBytes = AiAgentUtilsTestUtils.createZipWithEntries(Map.of("my-skill/scripts/main.js", script));

        Map<String, List<SkillComponentConnectionFactory.ComponentCall>> result =
            SkillComponentConnectionFactory.getSkillScripts(zipBytes);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetSkillScriptsHandlesEmptyParameterList() {
        String script = """
            function perform(input, context) {
                context.component.http.get()
                return null;
            }
            """;

        byte[] zipBytes = AiAgentUtilsTestUtils.createZipWithEntries(Map.of("skill/scripts/main.js", script));

        Map<String, List<SkillComponentConnectionFactory.ComponentCall>> result =
            SkillComponentConnectionFactory.getSkillScripts(zipBytes);

        List<SkillComponentConnectionFactory.ComponentCall> calls = result.get("skill/scripts/main.js");

        assertNotNull(calls);
        assertEquals(1, calls.size());

        SkillComponentConnectionFactory.ComponentCall componentCall = calls.getFirst();

        assertEquals("", componentCall.rawParameters());
    }
}
