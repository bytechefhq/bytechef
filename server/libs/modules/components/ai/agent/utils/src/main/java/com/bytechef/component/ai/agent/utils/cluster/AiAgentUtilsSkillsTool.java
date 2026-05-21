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
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;
import static com.bytechef.ee.platform.ai.skill.SkillArchiveConstants.MAX_ZIP_ENTRIES;
import static com.bytechef.ee.platform.ai.skill.SkillArchiveConstants.MAX_ZIP_ENTRY_SIZE;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.ee.platform.ai.skill.domain.AiSkill;
import com.bytechef.ee.platform.ai.skill.facade.AiSkillFacade;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * Provides a TOOLS cluster element that loads selected AiSkill zip archives, extracts .md files, and passes them to
 * Spring AI Community's SkillsTool as skill resources.
 *
 * @author Ivica Cardic
 */
public class AiAgentUtilsSkillsTool {

    private static final Logger log = LoggerFactory.getLogger(AiAgentUtilsSkillsTool.class);

    private static final String SKILLS = "skills";
    private static final String SKILL_ID = "skillId";
    private static final String SCRIPTS_DIRECTORY = "scripts/";

    /**
     * Matches calls of the form: context.component.{componentName}.{actionName}({rawParameters})
     * Parameters are captured as a raw string for later processing.
     */
    private static final Pattern COMPONENT_CALL_PATTERN = Pattern.compile(
        "context\\.component\\.([A-Za-z][A-Za-z0-9_-]*)\\.([A-Za-z][A-Za-z0-9_]*)\\(([^)]*)\\)");

    /**
     * Detects a perform function declaration across the supported script languages:
     * JavaScript: function perform(
     * Python/Ruby: def perform(
     * R:           perform <- function(
     * Java:        ) perform(Map
     */
    private static final Pattern PERFORM_FUNCTION_PATTERN = Pattern.compile(
        "(?:function\\s+perform\\s*\\(|def\\s+perform\\s*[\\s(]|perform\\s*<-\\s*function\\s*\\(|\\bperform\\s*\\(Map)");

    private final AiSkillFacade aiSkillFacade;
    private final ComponentDefinitionService componentDefinitionService;

    public final ClusterElementDefinition<ToolCallbackProviderFunction> clusterElementDefinition;

    @SuppressFBWarnings("EI")
    public AiAgentUtilsSkillsTool(AiSkillFacade aiSkillFacade, ComponentDefinitionService componentDefinitionService) {
        this.aiSkillFacade = aiSkillFacade;
        this.componentDefinitionService = componentDefinitionService;

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
                        .placeholder("Choose a skill...")
                        .items(
                            integer(SKILL_ID)
                                .label("Skill")
                                .options(
                                    (ClusterElementDefinition.OptionsFunction<Long>) this::getSkillOptions)
                                .required(true)))
                .object(() -> this::apply);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private ToolCallbackProvider apply(
        Parameters inputParameters, Parameters connectionParameters,
        com.bytechef.component.definition.Context context) throws Exception {

        List<Path> skillDirectories = new ArrayList<>();
        List<String> skippedSkillReasons = new ArrayList<>();

        List<Long> skillIds = inputParameters.getList(SKILLS, Long.class, List.of());

        if (skillIds.isEmpty()) {
            throw new IllegalArgumentException("At least one skill must be configured");
        }

        for (Long skillId : skillIds) {
            if (skillId == null) {
                String reason = "Unexpected null skill item in configuration";

                log.warn("{}, skipping", reason);
                skippedSkillReasons.add(reason);

                continue;
            }

            try {
                byte[] zipBytes = aiSkillFacade.getAiSkillDownload(skillId);

                Path skillDirectory = extractSkillToTempDirectory(zipBytes, skillId);

                skillDirectories.add(skillDirectory);
            } catch (RuntimeException runtimeException) {
                String reason = "Failed to load skill ID " + skillId + ": " + runtimeException.getMessage();

                log.warn(reason, runtimeException);
                skippedSkillReasons.add(reason);
            }
        }

        if (!skippedSkillReasons.isEmpty()) {
            throw new IllegalStateException(
                "Failed to load " + skippedSkillReasons.size() + " of " + skillIds.size() +
                    " configured skills: " + String.join("; ", skippedSkillReasons));
        }

        SkillsTool.Builder builder = SkillsTool.builder();

        for (Path skillDirectory : skillDirectories) {
            builder.addSkillsDirectory(skillDirectory.toString());
        }

        ToolCallback skillsToolCallback = builder.build();

        return ToolCallbackProvider.from(skillsToolCallback);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private List<Option<Long>> getSkillOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, ClusterElementContext context) {

        List<Option<Long>> options = new ArrayList<>();

        List<AiSkill> skills = aiSkillFacade.getAiSkills();

        for (AiSkill skill : skills) {
            options.add(option(skill.getName(), skill.getId()
                .longValue()));
        }

        return options;
    }

    /**
     * Scans all script files inside the skill zip archive for component calls of the form
     * {@code context.component.{componentName}.{actionName}(...)}.
     * Only files under the {@code scripts/} directory that contain a {@code perform} function are analysed.
     *
     * @return a map from script entry path to the list of component calls found in that script,
     *         ordered by appearance in the archive; empty if no matching scripts exist
     */
    public static Map<String, List<ComponentCall>> analyzeSkillScripts(byte[] zipBytes) {
        Map<String, List<ComponentCall>> result = new LinkedHashMap<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry zipEntry;
            int entryCount = 0;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (++entryCount > MAX_ZIP_ENTRIES) {
                    throw new IllegalArgumentException(
                        "Skill archive exceeds maximum allowed number of entries (" + MAX_ZIP_ENTRIES + ")");
                }

                if (zipEntry.isDirectory() || !isScriptEntry(zipEntry.getName())) {
                    continue;
                }

                byte[] bytes = zipInputStream.readNBytes(MAX_ZIP_ENTRY_SIZE + 1);

                if (bytes.length > MAX_ZIP_ENTRY_SIZE) {
                    log.warn("Script '{}' exceeds maximum allowed size, skipping", zipEntry.getName());

                    continue;
                }

                String content = new String(bytes, StandardCharsets.UTF_8);

                if (!hasPerformFunction(content)) {
                    continue;
                }

                List<ComponentCall> calls = extractComponentCalls(content);

                if (!calls.isEmpty()) {
                    result.put(zipEntry.getName(), calls);
                }
            }
        } catch (IOException ioException) {
            throw new UncheckedIOException("Failed to analyse skill archive", ioException);
        }

        return result;
    }

    /**
     * Returns true if the entry path falls inside a {@code scripts/} directory, handling both
     * top-level ({@code scripts/foo.js}) and nested ({@code skill-name/scripts/foo.js}) layouts.
     */
    public static boolean isScriptEntry(String entryName) {
        String normalized = entryName.replace('\\', '/');

        return normalized.startsWith(SCRIPTS_DIRECTORY) || normalized.contains("/" + SCRIPTS_DIRECTORY);
    }

    public static boolean hasPerformFunction(String content) {
        return PERFORM_FUNCTION_PATTERN.matcher(content)
            .find();
    }

    public static List<ComponentCall> extractComponentCalls(String content) {
        List<ComponentCall> calls = new ArrayList<>();

        Matcher matcher = COMPONENT_CALL_PATTERN.matcher(content);

        while (matcher.find()) {
            calls.add(new ComponentCall(matcher.group(1), matcher.group(2), matcher.group(3).trim()));
        }

        return calls;
    }

    /** Represents a single {@code context.component.X.Y(...)} call found inside a skill script. */
    public record ComponentCall(String componentName, String actionName, String rawParameters) {}

    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    private Path extractSkillToTempDirectory(byte[] zipBytes, long skillId) {
        try {
            Path tempDirectory = Files.createTempDirectory("bytechef-skill-" + skillId + "-");

            try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                ZipEntry zipEntry;
                int entryCount = 0;

                while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                    if (++entryCount > MAX_ZIP_ENTRIES) {
                        throw new IllegalArgumentException(
                            "Skill archive exceeds maximum allowed number of entries (" + MAX_ZIP_ENTRIES + ")");
                    }

                    if (zipEntry.isDirectory()) {
                        continue;
                    }

                    if (!zipEntry.getName()
                        .toLowerCase()
                        .endsWith(".md")) {

                        continue;
                    }

                    byte[] content = zipInputStream.readNBytes(MAX_ZIP_ENTRY_SIZE + 1);

                    if (content.length > MAX_ZIP_ENTRY_SIZE) {
                        throw new IllegalArgumentException(
                            "Skill file '" + zipEntry.getName() + "' exceeds the maximum allowed size of " +
                                (MAX_ZIP_ENTRY_SIZE / 1024 / 1024) + " MB");
                    }

                    Path targetPath = tempDirectory.resolve(zipEntry.getName())
                        .normalize();

                    if (!targetPath.startsWith(tempDirectory)) {
                        throw new IllegalArgumentException(
                            "Zip entry path traversal detected: " + zipEntry.getName());
                    }

                    Path parentPath = targetPath.getParent();

                    if (parentPath != null) {
                        Files.createDirectories(parentPath);
                    }

                    Files.write(targetPath, content);
                }
            }

            return tempDirectory;
        } catch (IOException ioException) {
            throw new UncheckedIOException("Failed to extract skill resources from zip archive", ioException);
        }
    }
}
