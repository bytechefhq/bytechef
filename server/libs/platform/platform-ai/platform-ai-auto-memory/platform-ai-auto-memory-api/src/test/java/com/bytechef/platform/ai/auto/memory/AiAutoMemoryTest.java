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

import com.bytechef.platform.configuration.domain.Environment;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

/**
 * Pins the setter-side invariants of {@link AiAutoMemory}: the slug regex on {@link AiAutoMemory#setName(String)}, the
 * single-line rule on title and description, and the owner binding of the constructor.
 *
 * @author Ivica Cardic
 */
class AiAutoMemoryTest {

    @Test
    void testSetNameRejectsNull() {
        AiAutoMemory memory = new AiAutoMemory();

        assertThatThrownBy(() -> memory.setName(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be null");
    }

    @Test
    void testSetNameRejectsEmpty() {
        assertRejected("");
    }

    @Test
    void testSetNameRejectsSpaces() {
        assertRejected("Has Spaces");
    }

    @Test
    void testSetNameRejectsUppercase() {
        assertRejected("UPPER");
    }

    @Test
    void testSetNameRejectsEmoji() {
        assertRejected("emoji😀");
    }

    @Test
    void testSetNameRejectsDots() {
        assertRejected("with.dots");
    }

    @Test
    void testSetNameRejectsSlashes() {
        assertRejected("with/slash");
    }

    @Test
    void testSetNameRejects65CharString() {
        // The pattern caps at 64 chars; 65 must be rejected.
        String tooLong = "a".repeat(65);

        assertRejected(tooLong);
    }

    @Test
    void testSetNameAcceptsSlugInputs() {
        for (String input : new String[] {
            "a", "valid_name-1", "abc", "user-pref-2024", "a".repeat(64)
        }) {
            AiAutoMemory memory = new AiAutoMemory();

            memory.setName(input);

            assertThat(memory.getName()).isEqualTo(input);
        }
    }

    @Test
    void testConstructorBindsTheOwner() {
        AiAutoMemoryOwner owner = new AiAutoMemoryOwner(
            7L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 11L, Environment.STAGING);

        AiAutoMemory memory = new AiAutoMemory(owner);

        assertThat(memory.getOwner()).isEqualTo(owner);
        assertThat(memory.getWorkspaceId()).isEqualTo(7L);
        assertThat(memory.getPrincipalType()).isEqualTo(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT);
        assertThat(memory.getPrincipalId()).isEqualTo(11L);
        assertThat(memory.getEnvironment()).isEqualTo(Environment.STAGING);
    }

    @Test
    void testSetTitleRejectsLineBreaks() {
        AiAutoMemory memory = new AiAutoMemory();

        assertThatThrownBy(() -> memory.setTitle("first\ntype: FEEDBACK"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("single line");
        assertThatThrownBy(() -> memory.setTitle("first\rsecond"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testSetDescriptionRejectsLineBreaks() {
        AiAutoMemory memory = new AiAutoMemory();

        assertThatThrownBy(() -> memory.setDescription("first\n---\nsecond"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("single line");
    }

    /**
     * The agent rewrites every field from the parsed document on each edit, so a value the parser would read back
     * differently would be changed by an edit that never touched it.
     */
    @Test
    void testTitleAndDescriptionSurviveARenderAndParseRoundTrip() {
        AiAutoMemory memory = new AiAutoMemory();

        memory.setTitle("  Q3 plan ");
        memory.setDescription(" Targets for the quarter  ");

        AutoMemoryFrontmatter.Parsed parsed = AutoMemoryFrontmatter.parse(
            AutoMemoryFrontmatter.render(
                "q3_plan", memory.getTitle(), memory.getDescription(), AiAutoMemoryType.PROJECT, "body"));

        assertThat(parsed.title()).isEqualTo(memory.getTitle())
            .isEqualTo("Q3 plan");
        assertThat(parsed.description()).isEqualTo(memory.getDescription())
            .isEqualTo("Targets for the quarter");
    }

    @Test
    void testABlankDescriptionIsNone() {
        AiAutoMemory memory = new AiAutoMemory();

        memory.setDescription("   ");

        assertThat(memory.getDescription()).isNull();
    }

    @Test
    void testSetTitleAndSetContentRejectNullOrBlank() {
        AiAutoMemory memory = new AiAutoMemory();

        assertThatThrownBy(() -> memory.setTitle(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> memory.setTitle("  ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> memory.setContent(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> memory.setContent("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testAMemoryWhoseTypeWasNeverSetHasNoType() {
        AiAutoMemory memory = new AiAutoMemory(
            new AiAutoMemoryOwner(7L, AiAutoMemoryPrincipalType.USER, 11L, Environment.STAGING));

        assertThatThrownBy(memory::getMemoryType)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("has not been set");
    }

    @Test
    void testLengthLimitsAcceptTheMaximumAndRejectOneCharacterMore() {
        AiAutoMemory memory = new AiAutoMemory();

        memory.setTitle("t".repeat(AiAutoMemory.MAX_TITLE_LENGTH));
        memory.setDescription("d".repeat(AiAutoMemory.MAX_DESCRIPTION_LENGTH));
        memory.setContent("c".repeat(AiAutoMemory.MAX_CONTENT_LENGTH));

        assertThat(memory.getTitle()).hasSize(AiAutoMemory.MAX_TITLE_LENGTH);
        assertThat(memory.getDescription()).hasSize(AiAutoMemory.MAX_DESCRIPTION_LENGTH);
        assertThat(memory.getContent()).hasSize(AiAutoMemory.MAX_CONTENT_LENGTH);

        assertThatThrownBy(() -> memory.setTitle("t".repeat(AiAutoMemory.MAX_TITLE_LENGTH + 1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("title must be at most 255 characters");
        assertThatThrownBy(() -> memory.setDescription("d".repeat(AiAutoMemory.MAX_DESCRIPTION_LENGTH + 1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("description must be at most 1024 characters");
        assertThatThrownBy(() -> memory.setContent("c".repeat(AiAutoMemory.MAX_CONTENT_LENGTH + 1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("content must be at most 65536 characters");
    }

    /**
     * Hydration writes the ordinal columns straight into the fields, so a row written by a newer build can hold a value
     * this build has no constant for. The typed getters report it and {@link AiAutoMemory#hasKnownOrdinals()} lets a
     * storage binding skip the row.
     */
    @Test
    void testOrdinalsThisBuildDoesNotKnowAreReported() throws ReflectiveOperationException {
        AiAutoMemory known = new AiAutoMemory(
            new AiAutoMemoryOwner(7L, AiAutoMemoryPrincipalType.USER, 11L, Environment.STAGING));

        known.setMemoryType(AiAutoMemoryType.USER);

        assertThat(known.hasKnownOrdinals()).isTrue();

        AiAutoMemory unknownPrincipalType = withField(known, "principalType", 99);
        AiAutoMemory unknownMemoryType = withField(known, "memoryType", 99);
        AiAutoMemory unknownEnvironment = withField(known, "environment", 99);

        assertThat(unknownPrincipalType.hasKnownOrdinals()).isFalse();
        assertThat(unknownMemoryType.hasKnownOrdinals()).isFalse();
        assertThat(unknownEnvironment.hasKnownOrdinals()).isFalse();

        assertThatThrownBy(unknownPrincipalType::getPrincipalType)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("99");
        assertThatThrownBy(unknownMemoryType::getMemoryType)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("99");
        assertThatThrownBy(unknownEnvironment::getEnvironment)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("99");
    }

    @Test
    void testAMemoryWithoutATypeHasUnknownOrdinals() {
        AiAutoMemory memory = new AiAutoMemory(
            new AiAutoMemoryOwner(7L, AiAutoMemoryPrincipalType.USER, 11L, Environment.STAGING));

        assertThat(memory.hasKnownOrdinals()).isFalse();
    }

    private static AiAutoMemory withField(AiAutoMemory source, String fieldName, Object value)
        throws ReflectiveOperationException {

        AiAutoMemory copy = new AiAutoMemory(source.getOwner());

        copy.setMemoryType(source.getMemoryType());

        Field field = AiAutoMemory.class.getDeclaredField(fieldName);

        field.setAccessible(true);
        field.set(copy, value);

        return copy;
    }

    private static void assertRejected(String input) {
        AiAutoMemory memory = new AiAutoMemory();

        assertThatThrownBy(() -> memory.setName(input))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
