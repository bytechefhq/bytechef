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

package com.bytechef.platform.ai.auto.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AutoMemoryFrontmatterTest {

    @Test
    void testRenderThenParseRoundTrips() {
        String content = "line one\n\n---\nline after a rule: with a colon\n";

        String rendered = AutoMemoryFrontmatter.render(
            "user_profile", "Profile: role and tone", "What the user prefers", AiAutoMemoryType.FEEDBACK, content);

        AutoMemoryFrontmatter.Parsed parsed = AutoMemoryFrontmatter.parse(rendered);

        assertThat(parsed.name()).isEqualTo("user_profile");
        assertThat(parsed.title()).isEqualTo("Profile: role and tone");
        assertThat(parsed.description()).isEqualTo("What the user prefers");
        assertThat(parsed.memoryType()).isEqualTo(AiAutoMemoryType.FEEDBACK);
        assertThat(parsed.content()).isEqualTo(content);
    }

    /**
     * A title or description stored before the single-line rule may hold a line break. Rendered as-is, a description of
     * {@code "a\n---\nb"} would close the frontmatter early and push the type line into the body on the next edit.
     */
    @Test
    void testRenderPutsAMultiLineTitleAndDescriptionOnOneLine() {
        String rendered = AutoMemoryFrontmatter.render(
            "legacy", "first\r\nsecond", "a\n---\nb", AiAutoMemoryType.USER, "body");

        AutoMemoryFrontmatter.Parsed parsed = AutoMemoryFrontmatter.parse(rendered);

        assertThat(parsed.title()).isEqualTo("first second");
        assertThat(parsed.description()).isEqualTo("a --- b");
        assertThat(parsed.memoryType()).isEqualTo(AiAutoMemoryType.USER);
        assertThat(parsed.content()).isEqualTo("body");
    }

    @Test
    void testRenderOmitsBlankDescription() {
        String rendered = AutoMemoryFrontmatter.render("note", "Note", " ", AiAutoMemoryType.PROJECT, "body");

        assertThat(rendered).doesNotContain("description:");
        assertThat(AutoMemoryFrontmatter.parse(rendered)
            .description()).isNull();
    }

    @Test
    void testParseReturnsNullForOmittedFields() {
        AutoMemoryFrontmatter.Parsed parsed = AutoMemoryFrontmatter.parse("---\ntitle: Only a title\n---\nbody");

        assertThat(parsed.title()).isEqualTo("Only a title");
        assertThat(parsed.description()).isNull();
        assertThat(parsed.memoryType()).isNull();
        assertThat(parsed.content()).isEqualTo("body");
    }

    @Test
    void testParseAcceptsLowerCaseTypeAndWindowsLineEndings() {
        AutoMemoryFrontmatter.Parsed parsed = AutoMemoryFrontmatter.parse("---\r\ntype: reference\r\n---\r\nbody");

        assertThat(parsed.memoryType()).isEqualTo(AiAutoMemoryType.REFERENCE);
        assertThat(parsed.content()).isEqualTo("body");
    }

    @Test
    void testParseKeepsTheBodyLineEndingsUnchanged() {
        String content = "first line\r\nsecond line\nthird line\r\n";

        AutoMemoryFrontmatter.Parsed parsed = AutoMemoryFrontmatter.parse(
            AutoMemoryFrontmatter.render("crlf", "Title", "Description", AiAutoMemoryType.USER, content));

        assertThat(parsed.title()).isEqualTo("Title");
        assertThat(parsed.description()).isEqualTo("Description");
        assertThat(parsed.content()).isEqualTo(content);
    }

    @Test
    void testParseRejectsTextWithoutFrontmatter() {
        assertThatThrownBy(() -> AutoMemoryFrontmatter.parse("just a body"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no frontmatter block");
    }

    @Test
    void testParseRejectsTextInsertedAboveTheFrontmatter() {
        assertThatThrownBy(() -> AutoMemoryFrontmatter.parse("inserted\n---\ntitle: T\ntype: USER\n---\nbody"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no frontmatter block");
    }

    @Test
    void testParseRejectsAnUnclosedFrontmatterBlock() {
        assertThatThrownBy(() -> AutoMemoryFrontmatter.parse("---\ntitle: T\nbody"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not closed");
    }

    @Test
    void testParseRejectsALineWithoutAKey() {
        assertThatThrownBy(() -> AutoMemoryFrontmatter.parse("---\ntitle: T\nstray text\n---\nbody"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("stray text");
    }

    @Test
    void testParseRejectsAnUnknownKey() {
        assertThatThrownBy(() -> AutoMemoryFrontmatter.parse("---\ntitel: T\n---\nbody"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("titel");
    }

    @Test
    void testParseRejectsAnUnknownType() {
        assertThatThrownBy(() -> AutoMemoryFrontmatter.parse("---\ntype: preference\n---\nbody"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("preference")
            .hasMessageContaining("USER|FEEDBACK|PROJECT|REFERENCE");
    }

    @Test
    void testParseRejectsARepeatedKey() {
        assertThatThrownBy(() -> AutoMemoryFrontmatter.parse("---\ntitle: One\ntitle: Two\ntype: USER\n---\nbody"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("'title' appears more than once");
    }
}
