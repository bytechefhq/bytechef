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

package com.bytechef.component.claude.code.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.claude.code.config.ClaudeCodeIntTestConfiguration;
import com.bytechef.component.claude.code.skill.ClaudeCodeSkillWriter;
import com.bytechef.component.claude.code.workspace.ClaudeCodeWorkspaceResolver;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.facade.AiSkillFacade;
import com.bytechef.platform.ai.skill.repository.AiSkillRepository;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@Import(PostgreSQLContainerConfiguration.class)
@SpringBootTest(classes = ClaudeCodeIntTestConfiguration.class)
class ClaudeCodeChatIntTest {

    @Autowired
    private AiSkillFacade aiSkillFacade;

    @Autowired
    private AiSkillRepository aiSkillRepository;

    @TempDir
    private Path basedir;

    @AfterEach
    void afterEach() {
        aiSkillRepository.deleteAll();
    }

    @Test
    void testPerformWritesOnlyTheSelectedSkillsOfTheSkillLibraryIntoTheWorkspace() throws Exception {
        AiSkill releaseNotes = aiSkillFacade.createAiSkill(
            "Release Notes", "Writes release notes.", "release-notes.zip",
            zip(Map.of(
                "release-notes/SKILL.md", "Group the changes by type.",
                "release-notes/scripts/collect.py", "print('changes')")));

        aiSkillFacade.createAiSkill(
            "Code Review", "Reviews code.", "code-review.zip", zip(Map.of("SKILL.md", "Check the tests.")));

        Map<String, String> skillFiles = new TreeMap<>();

        ClaudeCodeChat claudeCodeChat = new ClaudeCodeChat(
            mock(ClusterElementDefinitionService.class), new ClaudeCodeSkillWriter(aiSkillFacade),
            new ClaudeCodeWorkspaceResolver(basedir.toString(), 0),
            (prompt, workingDirectory, mcpServerConfigs, hookRegistry) -> {
                skillFiles.putAll(readFiles(workingDirectory.resolve(".claude/skills")));

                return "reply to " + prompt;
            });

        String result = claudeCodeChat.perform(
            ParametersFactory.create(Map.of("script", "hello", "skills", List.of(releaseNotes.getId()))), Map.of(),
            ParametersFactory.create(Map.of()), testRunContext());

        assertThat(result).isEqualTo("reply to hello");
        assertThat(skillFiles).containsExactlyEntriesOf(new TreeMap<>(Map.of(
            "release-notes/SKILL.md", "Group the changes by type.",
            "release-notes/scripts/collect.py", "print('changes')")));
    }

    @Test
    void testPerformRunsWithoutSkillsWhenNoneAreSelected() throws Exception {
        Map<String, String> skillFiles = new TreeMap<>();

        ClaudeCodeChat claudeCodeChat = new ClaudeCodeChat(
            mock(ClusterElementDefinitionService.class), new ClaudeCodeSkillWriter(aiSkillFacade),
            new ClaudeCodeWorkspaceResolver(basedir.toString(), 0),
            (prompt, workingDirectory, mcpServerConfigs, hookRegistry) -> {
                skillFiles.putAll(readFiles(workingDirectory.resolve(".claude/skills")));

                return prompt;
            });

        String result = claudeCodeChat.perform(
            ParametersFactory.create(Map.of("script", "hello")), Map.of(), ParametersFactory.create(Map.of()),
            testRunContext());

        assertThat(result).isEqualTo("hello");
        assertThat(skillFiles).isEmpty();
    }

    @Test
    void testGetSkillOptionsListsTheSkillLibrary() throws IOException {
        AiSkill releaseNotes = aiSkillFacade.createAiSkill(
            "Release Notes", null, "release-notes.zip", zip(Map.of("SKILL.md", "Group the changes by type.")));
        AiSkill codeReview = aiSkillFacade.createAiSkill(
            "Code Review", null, "code-review.zip", zip(Map.of("SKILL.md", "Check the tests.")));

        ClaudeCodeSkillWriter claudeCodeSkillWriter = new ClaudeCodeSkillWriter(aiSkillFacade);

        List<Option<Long>> options = claudeCodeSkillWriter.getSkillOptions(
            null, null, Map.of(), null, mock(ActionContext.class));

        assertThat(options)
            .extracting(Option::getLabel, Option::getValue)
            .containsExactlyInAnyOrder(
                tuple(releaseNotes.getName(), releaseNotes.getId()), tuple(codeReview.getName(), codeReview.getId()));
    }

    private static Map<String, String> readFiles(Path directory) throws IOException {
        Map<String, String> files = new TreeMap<>();

        if (!Files.isDirectory(directory)) {
            return files;
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            List<Path> regularFiles = paths.filter(Files::isRegularFile)
                .toList();

            for (Path file : regularFiles) {
                Path relativePath = directory.relativize(file);

                files.put(relativePath.toString(), Files.readString(file));
            }
        }

        return files;
    }

    private static ActionContextAware testRunContext() {
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        when(actionContextAware.getJobId()).thenReturn(7L);

        return actionContextAware;
    }

    private static byte[] zip(Map<String, String> entries) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                String content = entry.getValue();

                zipOutputStream.putNextEntry(new ZipEntry(entry.getKey()));
                zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
                zipOutputStream.closeEntry();
            }
        }

        return byteArrayOutputStream.toByteArray();
    }
}
