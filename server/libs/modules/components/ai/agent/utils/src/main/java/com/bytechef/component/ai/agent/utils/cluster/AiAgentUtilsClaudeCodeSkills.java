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

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.platform.ai.skill.SkillArchiveConstants.MAX_ZIP_ENTRIES;
import static com.bytechef.platform.ai.skill.SkillArchiveConstants.MAX_ZIP_ENTRY_SIZE;
import static com.bytechef.platform.component.definition.ai.claudecode.ClaudeCodeSkillsFunction.SKILLS;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.facade.AiSkillFacade;
import com.bytechef.platform.ai.skill.facade.AiSkillFacade.AiSkillDownload;
import com.bytechef.platform.component.definition.ai.claudecode.ClaudeCodeSkillsFunction;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Ivica Cardic
 */
public class AiAgentUtilsClaudeCodeSkills {

    static final long MAX_SKILL_SIZE = 100L * 1024 * 1024;

    private static final String SKILL_ID = "skillId";
    private static final String SKILL_MD_FILENAME = "SKILL.md";
    private static final String SKILLS_PROPERTY = "skills";

    public final ClusterElementDefinition<ClaudeCodeSkillsFunction> clusterElementDefinition;

    private final AiSkillFacade aiSkillFacade;

    @SuppressFBWarnings("EI")
    public AiAgentUtilsClaudeCodeSkills(AiSkillFacade aiSkillFacade) {
        this.aiSkillFacade = aiSkillFacade;

        this.clusterElementDefinition = ComponentDsl.<ClaudeCodeSkillsFunction>clusterElement("claudeCodeSkills")
            .title("Skills")
            .description("Gives Claude Code skills from the ByteChef skill library.")
            .type(SKILLS)
            .properties(
                array(SKILLS_PROPERTY)
                    .label("Skills")
                    .description("Select the skills Claude Code can use.")
                    .placeholder("Choose a skill...")
                    .expressionEnabled(false)
                    .items(
                        integer(SKILL_ID)
                            .label("Skill")
                            .options((ClusterElementDefinition.OptionsFunction<Long>) this::getSkillOptions)
                            .expressionEnabled(false)
                            .required(true))
                    .required(true))
            .object(() -> this::apply);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private void apply(
        Parameters inputParameters, Parameters connectionParameters, Path skillsDirectory, Context context)
        throws IOException {

        List<Long> skillIds = inputParameters.getList(SKILLS_PROPERTY, Long.class, List.of());

        if (skillIds.isEmpty()) {
            throw new IllegalArgumentException("At least one skill must be configured");
        }

        for (Long skillId : skillIds) {
            if (skillId == null) {
                throw new IllegalArgumentException("Unexpected null skill item in configuration");
            }

            AiSkillDownload aiSkillDownload = aiSkillFacade.getAiSkillWithDownload(skillId);

            AiSkill aiSkill = aiSkillDownload.aiSkill();

            Path skillDirectory = getSkillDirectory(skillsDirectory, aiSkill.getName(), skillId);

            extractSkill(aiSkillDownload.bytes(), skillDirectory, aiSkill.getName());
        }
    }

    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    static void extractSkill(byte[] zipBytes, Path skillDirectory, String skillName) throws IOException {
        String skillRoot = getSkillRoot(zipBytes);

        if (skillRoot == null) {
            throw new IllegalArgumentException(
                "Skill '%s' has no %s file in its archive".formatted(skillName, SKILL_MD_FILENAME));
        }

        Files.createDirectories(skillDirectory);

        long totalSize = 0;

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry zipEntry;
            int entryCount = 0;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (++entryCount > MAX_ZIP_ENTRIES) {
                    throw new IllegalArgumentException(
                        "Skill archive exceeds maximum allowed number of entries (" + MAX_ZIP_ENTRIES + ")");
                }

                String entryName = zipEntry.getName();

                if (!entryName.startsWith(skillRoot) || entryName.equals(skillRoot)) {
                    continue;
                }

                Path targetPath = skillDirectory.resolve(entryName.substring(skillRoot.length()))
                    .normalize();

                if (!targetPath.startsWith(skillDirectory) || targetPath.equals(skillDirectory)) {
                    throw new IllegalArgumentException("Zip entry path traversal detected: " + entryName);
                }

                if (zipEntry.isDirectory()) {
                    Files.createDirectories(targetPath);

                    continue;
                }

                byte[] entryBytes = zipInputStream.readNBytes(MAX_ZIP_ENTRY_SIZE + 1);

                if (entryBytes.length > MAX_ZIP_ENTRY_SIZE) {
                    throw new IllegalArgumentException(
                        "Skill file '%s' exceeds the maximum allowed size of %d MB".formatted(
                            entryName, MAX_ZIP_ENTRY_SIZE / 1024 / 1024));
                }

                totalSize += entryBytes.length;

                if (totalSize > MAX_SKILL_SIZE) {
                    throw new IllegalArgumentException(
                        "Skill archive exceeds the maximum allowed size of %d MB".formatted(
                            MAX_SKILL_SIZE / 1024 / 1024));
                }

                Path parentPath = targetPath.getParent();

                if (parentPath != null) {
                    Files.createDirectories(parentPath);
                }

                Files.write(targetPath, entryBytes);
            }
        }
    }

    private static String getSkillRoot(byte[] zipBytes) throws IOException {
        String skillRoot = null;

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry zipEntry;
            int entryCount = 0;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (++entryCount > MAX_ZIP_ENTRIES) {
                    throw new IllegalArgumentException(
                        "Skill archive exceeds maximum allowed number of entries (" + MAX_ZIP_ENTRIES + ")");
                }

                String entryName = zipEntry.getName();

                if (zipEntry.isDirectory() || !(entryName.equals(SKILL_MD_FILENAME) ||
                    entryName.endsWith("/" + SKILL_MD_FILENAME))) {

                    continue;
                }

                String entryRoot = entryName.substring(0, entryName.length() - SKILL_MD_FILENAME.length());

                if (skillRoot == null || entryRoot.length() < skillRoot.length()) {
                    skillRoot = entryRoot;
                }
            }
        }

        return skillRoot;
    }

    static Path getSkillDirectory(Path skillsDirectory, String skillName, long skillId) {
        String directoryName = skillName == null ? "" : skillName.strip()
            .toLowerCase()
            .replaceAll("[^a-z0-9_-]+", "-")
            .replaceAll("^-+|-+$", "");

        if (directoryName.isEmpty()) {
            directoryName = "skill-" + skillId;
        }

        Path skillDirectory = skillsDirectory.resolve(directoryName);

        if (Files.exists(skillDirectory)) {
            skillDirectory = skillsDirectory.resolve(directoryName + "-" + skillId);
        }

        return skillDirectory;
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private List<Option<Long>> getSkillOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, ClusterElementContext context) {

        List<Option<Long>> options = new ArrayList<>();

        for (AiSkill aiSkill : aiSkillFacade.getAiSkills()) {
            Long id = aiSkill.getId();

            options.add(option(aiSkill.getName(), id.longValue()));
        }

        return options;
    }
}
