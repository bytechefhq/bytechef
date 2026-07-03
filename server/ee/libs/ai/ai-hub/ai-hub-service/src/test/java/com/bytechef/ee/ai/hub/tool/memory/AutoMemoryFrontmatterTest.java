/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool.memory;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.ai.auto.memory.AutoMemoryFrontmatter;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AutoMemoryFrontmatterTest {

    @Test
    void testParseExtractsFieldsAndBody() {
        String text = """
            ---
            name: user_profile
            title: User Profile
            description: who the user is
            type: USER
            ---
            The user prefers tabs.""";

        AutoMemoryFrontmatter.Parsed parsed = AutoMemoryFrontmatter.parse(text);

        assertThat(parsed.title()).isEqualTo("User Profile");
        assertThat(parsed.description()).isEqualTo("who the user is");
        assertThat(parsed.memoryType()).isEqualTo(AiAutoMemoryType.USER);
        assertThat(parsed.content()).isEqualTo("The user prefers tabs.");
    }

    @Test
    void testParseToleratesMissingFrontmatter() {
        AutoMemoryFrontmatter.Parsed parsed = AutoMemoryFrontmatter.parse("just a body, no frontmatter");

        assertThat(parsed.title()).isNull();
        assertThat(parsed.description()).isNull();
        assertThat(parsed.memoryType()).isNull();
        assertThat(parsed.content()).isEqualTo("just a body, no frontmatter");
    }

    @Test
    void testRenderRoundTripsThroughParse() {
        String rendered = AutoMemoryFrontmatter.render(
            "user_profile", "User Profile", "who the user is", AiAutoMemoryType.USER, "The user prefers tabs.");

        AutoMemoryFrontmatter.Parsed parsed = AutoMemoryFrontmatter.parse(rendered);

        assertThat(parsed.title()).isEqualTo("User Profile");
        assertThat(parsed.description()).isEqualTo("who the user is");
        assertThat(parsed.memoryType()).isEqualTo(AiAutoMemoryType.USER);
        assertThat(parsed.content()).isEqualTo("The user prefers tabs.");
    }

    @Test
    void testRenderOmitsBlankDescription() {
        String rendered = AutoMemoryFrontmatter.render(
            "x", "X", null, AiAutoMemoryType.PROJECT, "body");

        assertThat(rendered).doesNotContain("description:");
    }
}
