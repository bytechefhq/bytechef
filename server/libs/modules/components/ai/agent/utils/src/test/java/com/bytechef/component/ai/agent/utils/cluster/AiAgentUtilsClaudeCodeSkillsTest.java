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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.facade.AiSkillFacade;
import com.bytechef.platform.ai.skill.facade.AiSkillFacade.AiSkillDownload;
import com.bytechef.platform.component.definition.ai.claudecode.ClaudeCodeSkillsFunction;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
class AiAgentUtilsClaudeCodeSkillsTest {

    private static final String SKILL_MD = """
        ---
        name: release-notes
        description: Writes release notes.
        ---
        Group the changes by type.
        """;

    private final AiSkillFacade aiSkillFacade = mock(AiSkillFacade.class);
    private final Context context = mock(Context.class);

    @TempDir
    private Path skillsDirectory;

    @Test
    void testApplyWritesEveryFileOfEachSkillIntoItsOwnDirectory() throws Exception {
        mockSkill(1L, "Release Notes", zip(Map.of(
            "SKILL.md", SKILL_MD, "scripts/collect.py", "print('changes')", "templates/notes.txt", "## Changes")));
        mockSkill(2L, "Code Review", zip(Map.of("code-review/SKILL.md", SKILL_MD, "code-review/rules.md", "rules")));

        apply(List.of(1L, 2L));

        assertThat(skillsDirectory.resolve("release-notes/SKILL.md")).hasContent(SKILL_MD);
        assertThat(skillsDirectory.resolve("release-notes/scripts/collect.py")).hasContent("print('changes')");
        assertThat(skillsDirectory.resolve("release-notes/templates/notes.txt")).hasContent("## Changes");
        assertThat(skillsDirectory.resolve("code-review/SKILL.md")).hasContent(SKILL_MD);
        assertThat(skillsDirectory.resolve("code-review/rules.md")).hasContent("rules");
        assertThat(skillsDirectory.resolve("code-review/code-review")).doesNotExist();
    }

    @Test
    void testApplyGivesSkillsWithTheSameDirectoryNameDistinctDirectories() throws Exception {
        mockSkill(1L, "Notes", zip(Map.of("SKILL.md", SKILL_MD)));
        mockSkill(2L, "notes", zip(Map.of("SKILL.md", SKILL_MD)));

        apply(List.of(1L, 2L));

        assertThat(skillsDirectory.resolve("notes/SKILL.md")).exists();
        assertThat(skillsDirectory.resolve("notes-2/SKILL.md")).exists();
    }

    @Test
    void testApplyRejectsAnArchiveWithoutSkillMd() throws Exception {
        mockSkill(1L, "Broken", zip(Map.of("README.md", "no skill here")));

        assertThatThrownBy(() -> apply(List.of(1L)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("has no SKILL.md");
    }

    @Test
    void testApplyRejectsPathTraversal() throws Exception {
        Map<String, String> entries = new LinkedHashMap<>();

        entries.put("SKILL.md", SKILL_MD);
        entries.put("../escaped.txt", "outside");

        mockSkill(1L, "Evil", zip(entries));

        assertThatThrownBy(() -> apply(List.of(1L)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("path traversal");

        assertThat(skillsDirectory.resolve("../escaped.txt")).doesNotExist();
    }

    @Test
    void testApplyRejectsAnEmptySelection() {
        assertThatThrownBy(() -> apply(List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("At least one skill");
    }

    private void apply(List<Long> skillIds) throws Exception {
        AiAgentUtilsClaudeCodeSkills aiAgentUtilsClaudeCodeSkills = new AiAgentUtilsClaudeCodeSkills(aiSkillFacade);

        ClaudeCodeSkillsFunction claudeCodeSkillsFunction =
            aiAgentUtilsClaudeCodeSkills.clusterElementDefinition.getElement();

        claudeCodeSkillsFunction.apply(
            MockParametersFactory.create(Map.of("skills", skillIds)), MockParametersFactory.create(Map.of()),
            skillsDirectory, context);
    }

    private void mockSkill(long id, String name, byte[] bytes) {
        AiSkill aiSkill = new AiSkill();

        aiSkill.setId(id);
        aiSkill.setName(name);

        when(aiSkillFacade.getAiSkillWithDownload(id)).thenReturn(new AiSkillDownload(aiSkill, bytes));
    }

    private static byte[] zip(Map<String, String> entries) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zipOutputStream.putNextEntry(new ZipEntry(entry.getKey()));
                zipOutputStream.write(entry.getValue()
                    .getBytes(StandardCharsets.UTF_8));
                zipOutputStream.closeEntry();
            }
        }

        return byteArrayOutputStream.toByteArray();
    }
}
