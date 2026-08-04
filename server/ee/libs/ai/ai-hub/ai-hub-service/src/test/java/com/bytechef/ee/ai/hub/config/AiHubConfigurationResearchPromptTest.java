/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

/**
 * Pins the coherence between the ASK/BUILD system prompts and the conditionally registered {@code research} tool: when
 * the tool is absent (no Firecrawl key), every prompt section that documents it must be stripped — a documented but
 * unregistered tool makes the model call it and the turn dies with "No ToolCallback found for tool name: research".
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubConfigurationResearchPromptTest {

    @Test
    void testFilterKeepsSectionAndStripsMarkersWhenResearchAvailable() {
        String prompt = "before\n\n[[research:start]]\nResearch:\n- research({topic})\n[[research:end]]\n\nafter\n";

        String filtered = AiHubConfiguration.filterResearchSection(prompt, true);

        assertThat(filtered).isEqualTo("before\n\nResearch:\n- research({topic})\n\nafter\n");
    }

    @Test
    void testFilterRemovesSectionWhenResearchUnavailable() {
        String prompt = "before\n\n[[research:start]]\nResearch:\n- research({topic})\n[[research:end]]\n\nafter\n";

        String filtered = AiHubConfiguration.filterResearchSection(prompt, false);

        assertThat(filtered).isEqualTo("before\n\nafter\n");
    }

    @Test
    void testFilterRemovesEveryMarkedSectionWhenResearchUnavailable() {
        String prompt = "a\n\n[[research:start]]\nfirst\n[[research:end]]\n\nb\n\n"
            + "[[research:start]]\nsecond\n[[research:end]]\n\nc\n";

        String filtered = AiHubConfiguration.filterResearchSection(prompt, false);

        assertThat(filtered).isEqualTo("a\n\nb\n\nc\n");
    }

    @Test
    void testFilterLeavesUnmarkedPromptUnchanged() {
        String prompt = "no markers here\n";

        assertThat(AiHubConfiguration.filterResearchSection(prompt, false)).isEqualTo(prompt);
        assertThat(AiHubConfiguration.filterResearchSection(prompt, true)).isEqualTo(prompt);
    }

    @Test
    void testAskPromptResourceCarriesNoResearchMentionWhenUnavailable() {
        String filtered = AiHubConfiguration.filterResearchSection(readPrompt("prompt_ai_hub_ask.txt"), false);

        assertThat(filtered).doesNotContain("research(");
        assertThat(filtered).doesNotContain("`research`");
        assertThat(filtered).doesNotContain("[[research");
    }

    @Test
    void testBuildPromptResourceCarriesNoResearchMentionWhenUnavailable() {
        String filtered = AiHubConfiguration.filterResearchSection(readPrompt("prompt_ai_hub_build.txt"), false);

        assertThat(filtered).doesNotContain("research(");
        assertThat(filtered).doesNotContain("`research`");
        assertThat(filtered).doesNotContain("[[research");
    }

    @Test
    void testPromptResourcesCarryNoMarkersWhenAvailable() {
        assertThat(AiHubConfiguration.filterResearchSection(readPrompt("prompt_ai_hub_ask.txt"), true))
            .doesNotContain("[[research")
            .contains("research({topic})");
        assertThat(AiHubConfiguration.filterResearchSection(readPrompt("prompt_ai_hub_build.txt"), true))
            .doesNotContain("[[research")
            .contains("research({topic})");
    }

    private String readPrompt(String resourceName) {
        Resource resource = new DefaultResourceLoader().getResource("classpath:" + resourceName);

        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read prompt resource: " + resourceName, exception);
        }
    }
}
