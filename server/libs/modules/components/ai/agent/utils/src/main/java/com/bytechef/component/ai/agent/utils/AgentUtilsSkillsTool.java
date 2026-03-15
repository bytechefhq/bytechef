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

package com.bytechef.component.ai.agent.utils;

import static com.bytechef.ai.agent.skill.SkillArchiveConstants.MAX_ZIP_ENTRIES;
import static com.bytechef.ai.agent.skill.SkillArchiveConstants.MAX_ZIP_ENTRY_SIZE;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;

import com.bytechef.ai.agent.skill.domain.AgentSkill;
import com.bytechef.ai.agent.skill.facade.AgentSkillFacade;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * Provides a TOOLS cluster element that loads selected AgentSkill zip archives, extracts .md files, and passes them to
 * Spring AI Community's SkillsTool as skill resources.
 *
 * @author Ivica Cardic
 */
public class AgentUtilsSkillsTool {

    private static final Logger logger = LoggerFactory.getLogger(AgentUtilsSkillsTool.class);

    private static final String SKILLS = "skills";
    private static final String SKILL_ID = "skillId";

    private final AgentSkillFacade agentSkillFacade;

    public final ClusterElementDefinition<ToolCallbackProviderFunction> clusterElementDefinition;

    @SuppressFBWarnings("EI")
    public AgentUtilsSkillsTool(AgentSkillFacade agentSkillFacade) {
        this.agentSkillFacade = agentSkillFacade;

        this.clusterElementDefinition =
            ComponentDsl.<ToolCallbackProviderFunction>clusterElement("skillsTool")
                .title("Skills Tool")
                .description(
                    "Extend AI agent capabilities with reusable, composable knowledge modules "
                        + "defined in Markdown with YAML front-matter.")
                .type(TOOLS)
                .properties(
                    array(SKILLS)
                        .label("Skills")
                        .description("Select skills to make available to the agent.")
                        .items(
                            object()
                                .properties(
                                    string(SKILL_ID)
                                        .label("Skill")
                                        .description("Choose a skill.")
                                        .options(
                                            (ClusterElementDefinition.OptionsFunction<String>) this::getSkillOptions)
                                        .required(true))))
                .object(() -> this::apply);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private ToolCallbackProvider apply(
        Parameters inputParameters, Parameters connectionParameters,
        com.bytechef.component.definition.Context context) throws Exception {

        List<Resource> skillResources = new ArrayList<>();
        List<String> skippedSkillReasons = new ArrayList<>();

        List<?> skillItems = inputParameters.getList(SKILLS, Object.class, List.of());

        if (skillItems.isEmpty()) {
            throw new IllegalArgumentException("At least one skill must be configured");
        }

        for (Object skillItem : skillItems) {
            if (!(skillItem instanceof Map<?, ?> skillMap)) {
                String reason = "Unexpected skill item type: " + skillItem.getClass()
                    .getName();

                logger.warn("{}, skipping", reason);
                skippedSkillReasons.add(reason);

                continue;
            }

            Object skillIdObj = skillMap.get(SKILL_ID);

            if (skillIdObj == null) {
                String reason = "Skill item missing '" + SKILL_ID + "' key, available keys: " + skillMap.keySet();

                logger.warn("{}, skipping", reason);
                skippedSkillReasons.add(reason);

                continue;
            }

            long skillId;

            try {
                skillId = Long.parseLong(skillIdObj.toString());
            } catch (NumberFormatException numberFormatException) {
                String reason = "Invalid skill ID value: '" + skillIdObj + "'";

                logger.warn("{}, skipping", reason);
                skippedSkillReasons.add(reason);

                continue;
            }

            try {
                byte[] zipBytes = agentSkillFacade.getAgentSkillDownload(skillId);

                List<Resource> extractedResources = extractSkillResources(zipBytes);

                skillResources.addAll(extractedResources);
            } catch (RuntimeException runtimeException) {
                String reason = "Failed to load skill ID " + skillId + ": " + runtimeException.getMessage();

                logger.warn(reason, runtimeException);
                skippedSkillReasons.add(reason);
            }
        }

        if (!skippedSkillReasons.isEmpty()) {
            throw new IllegalStateException(
                "Failed to load " + skippedSkillReasons.size() + " of " + skillItems.size() +
                    " configured skills: " + String.join("; ", skippedSkillReasons));
        }

        SkillsTool.Builder builder = SkillsTool.builder();

        if (!skillResources.isEmpty()) {
            builder.addSkillsResources(skillResources);
        }

        ToolCallback skillsToolCallback = builder.build();

        return ToolCallbackProvider.from(ToolCallbacks.from(skillsToolCallback));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private List<Option<String>> getSkillOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, ClusterElementContext context) {

        List<Option<String>> options = new ArrayList<>();

        List<AgentSkill> skills = agentSkillFacade.getAgentSkills();

        for (AgentSkill skill : skills) {
            options.add(option(skill.getName(), String.valueOf(skill.getId())));
        }

        return options;
    }

    private List<Resource> extractSkillResources(byte[] zipBytes) {
        List<Resource> resources = new ArrayList<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry zipEntry;
            int entryCount = 0;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (++entryCount > MAX_ZIP_ENTRIES) {
                    throw new IllegalArgumentException(
                        "Skill archive exceeds maximum allowed number of entries (" + MAX_ZIP_ENTRIES + ")");
                }

                if (!zipEntry.isDirectory() && zipEntry.getName()
                    .toLowerCase()
                    .endsWith(".md")) {

                    byte[] content = zipInputStream.readNBytes(MAX_ZIP_ENTRY_SIZE + 1);

                    if (content.length > MAX_ZIP_ENTRY_SIZE) {
                        throw new IllegalArgumentException(
                            "Skill file '" + zipEntry.getName() + "' exceeds the maximum allowed size of " +
                                (MAX_ZIP_ENTRY_SIZE / 1024 / 1024) + " MB");
                    }

                    resources.add(new ByteArrayResource(content, zipEntry.getName()));
                }
            }
        } catch (IOException ioException) {
            throw new UncheckedIOException("Failed to extract skill resources from zip archive", ioException);
        }

        return resources;
    }
}
