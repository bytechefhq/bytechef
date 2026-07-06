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
import static com.bytechef.platform.ai.skill.SkillArchiveConstants.MAX_ZIP_ENTRIES;
import static com.bytechef.platform.ai.skill.SkillArchiveConstants.MAX_ZIP_ENTRY_SIZE;

import com.bytechef.component.ai.agent.utils.AiAgentUtilsClusterElementContributor;
import com.bytechef.component.ai.agent.utils.util.AiAgentUtilsUtils;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.script.constant.ScriptConstants;
import com.bytechef.component.script.engine.PolyglotEngine;
import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.facade.AiSkillFacade;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.JobContextAware;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolCallbackProviderFunction;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

/**
 * Provides a TOOLS cluster element that loads selected AiSkill zip archives, extracts .md files, and passes them to
 * Spring AI Community's SkillsTool as skill resources. Additionally, any executable scripts found in the
 * {@code scripts/} directory are registered as individual {@link ToolCallback} instances backed by
 * {@link PolyglotEngine}.
 *
 * @author Ivica Cardic
 */
@Component
public class AiAgentUtilsSkillsTool implements AiAgentUtilsClusterElementContributor {

    private static final Map<String, String> EXTENSION_TO_LANGUAGE_ID = Map.of(
        "js", "js",
        "py", "python",
        "rb", "ruby",
        "r", "R",
        "java", "java");
    private static final Pattern FRONTMATTER_NAME_PATTERN = Pattern.compile(
        "^---\\s*\\n.*?^name:\\s*(.+?)\\s*$", Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern FRONTMATTER_DESCRIPTION_PATTERN = Pattern.compile(
        "^---\\s*\\n.*?^description:\\s*(.+?)\\s*$", Pattern.MULTILINE | Pattern.DOTALL);
    private static final String SKILLS = "skills";
    private static final String SKILL_ID = "skillId";
    private static final String SKILL_MD_FILENAME = "SKILL.md";

    private final AiSkillFacade aiSkillFacade;
    private final PolyglotEngine polyglotEngine;

    private final ClusterElementDefinition<MultipleConnectionsToolCallbackProviderFunction> clusterElementDefinition;

    @SuppressFBWarnings("EI")
    public AiAgentUtilsSkillsTool(AiSkillFacade aiSkillFacade, PolyglotEngine polyglotEngine) {
        this.aiSkillFacade = aiSkillFacade;
        this.polyglotEngine = polyglotEngine;

        this.clusterElementDefinition =
            ComponentDsl.<MultipleConnectionsToolCallbackProviderFunction>clusterElement("skillsTool")
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
                        .expressionEnabled(false)
                        .items(
                            integer(SKILL_ID)
                                .label("Skill")
                                .options(
                                    (ClusterElementDefinition.OptionsFunction<Long>) this::getSkillOptions)
                                .expressionEnabled(false)
                                .required(true)))
                .object(() -> this::apply);
    }

    @Override
    public ClusterElementDefinition<?> getClusterElementDefinition() {
        return clusterElementDefinition;
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private ToolCallbackProvider apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections, Context context) {

        List<Path> skillDirectories = new ArrayList<>();
        List<SkillScripts> skillScriptsList = new ArrayList<>();
        List<String> skippedSkillReasons = new ArrayList<>();

        List<Long> skillIds = inputParameters.getList(SKILLS, Long.class, List.of());

        if (skillIds.isEmpty()) {
            throw new IllegalArgumentException("At least one skill must be configured");
        }

        for (Long skillId : skillIds) {
            if (skillId == null) {
                String reason = "Unexpected null skill item in configuration";

                context.log(log -> log.warn("{}, skipping", reason));

                skippedSkillReasons.add(reason);

                continue;
            }

            try {
                byte[] zipBytes = aiSkillFacade.getAiSkillDownload(skillId);

                SkillExtraction extraction = extractSkillResources(zipBytes, skillId, context);

                skillDirectories.add(extraction.skillDirectory());

                SkillScripts skillScripts = extraction.skillScripts();

                List<ScriptEntry> scriptEntries = skillScripts.scripts();

                if (!scriptEntries.isEmpty()) {
                    skillScriptsList.add(skillScripts);
                }
            } catch (RuntimeException runtimeException) {
                String reason = "Failed to load skill ID " + skillId + ": " + runtimeException.getMessage();

                context.log(log -> log.warn(reason, runtimeException));

                skippedSkillReasons.add(reason);
            }
        }

        if (!skippedSkillReasons.isEmpty()) {
            throw new IllegalStateException(
                "Failed to load %d of %d configured skills: %s".formatted(
                    skippedSkillReasons.size(), skillIds.size(), String.join("; ", skippedSkillReasons)));
        }

        SkillsTool.Builder builder = SkillsTool.builder();

        for (Path skillDirectory : skillDirectories) {
            builder.addSkillsDirectory(skillDirectory.toString());
        }

        ToolCallback skillsToolCallback = builder.build();

        ToolCallbackProvider toolCallbackProvider = ToolCallbackProvider.from(skillsToolCallback);

        List<ToolCallback> allToolCallbacks = new ArrayList<>(Arrays.asList(toolCallbackProvider.getToolCallbacks()));

        JobContextAware jobContextAware = (JobContextAware) context;

        for (SkillScripts skillScripts : skillScriptsList) {
            for (ScriptEntry scriptEntry : skillScripts.scripts()) {
                ToolCallback scriptToolCallback = createScriptToolCallback(
                    skillScripts.name(), skillScripts.description(), scriptEntry, componentConnections,
                    jobContextAware);

                allToolCallbacks.add(scriptToolCallback);
            }
        }

        return ToolCallbackProvider.from(allToolCallbacks.toArray(ToolCallback[]::new));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private List<Option<Long>> getSkillOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, ClusterElementContext context) {

        List<Option<Long>> options = new ArrayList<>();

        List<AiSkill> aiSkills = aiSkillFacade.getAiSkills();

        for (AiSkill skill : aiSkills) {
            options.add(option(skill.getName(), skill.getId()
                .longValue()));
        }

        return options;
    }

    private ToolCallback createScriptToolCallback(
        String skillName, String skillDescription, ScriptEntry scriptEntry,
        Map<String, ComponentConnection> componentConnections, JobContextAware jobContextAware) {

        String toolName = sanitizeToolName(skillName + "_" + stripExtension(scriptEntry.fileName()));
        String scriptContent = scriptEntry.content();
        String languageId = scriptEntry.languageId();

        return FunctionToolCallback.builder(
            toolName,
            (Map<String, Object> toolInput) -> {
                Parameters scriptParams = ParametersFactory.create(
                    Map.of(
                        "script", scriptContent,
                        ScriptConstants.INPUT, toolInput != null ? toolInput : Map.of()));

                return polyglotEngine.execute(languageId, scriptParams, componentConnections, jobContextAware);
            })
            .inputType(Map.class)
            .description(skillDescription)
            .build();
    }

    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    private static SkillExtraction extractSkillResources(byte[] zipBytes, long skillId, Context context) {
        String skillName = "";
        String skillDescription = "";
        List<ScriptEntry> scriptEntries = new ArrayList<>();

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

                    String entryName = zipEntry.getName();
                    boolean isMarkdown = entryName.toLowerCase()
                        .endsWith(".md");
                    boolean isScript = !isMarkdown && AiAgentUtilsUtils.isScriptEntry(entryName);

                    if (!isMarkdown && !isScript) {
                        continue;
                    }

                    byte[] entryBytes = zipInputStream.readNBytes(MAX_ZIP_ENTRY_SIZE + 1);

                    if (entryBytes.length > MAX_ZIP_ENTRY_SIZE) {
                        if (isMarkdown) {
                            throw new IllegalArgumentException(
                                "Skill file '%s' exceeds the maximum allowed size of %d MB".formatted(
                                    entryName, MAX_ZIP_ENTRY_SIZE / 1024 / 1024));
                        }

                        context.log(
                            log -> log.warn("Entry '{}' exceeds maximum allowed size, skipping", entryName));

                        continue;
                    }

                    if (isMarkdown) {
                        Path targetPath = tempDirectory.resolve(entryName)
                            .normalize();

                        if (!targetPath.startsWith(tempDirectory)) {
                            throw new IllegalArgumentException("Zip entry path traversal detected: " + entryName);
                        }

                        Path parentPath = targetPath.getParent();

                        if (parentPath != null) {
                            Files.createDirectories(parentPath);
                        }

                        Files.write(targetPath, entryBytes);

                        if (entryName.endsWith(SKILL_MD_FILENAME)) {
                            String content = new String(entryBytes, StandardCharsets.UTF_8);

                            skillName = extractFrontmatterField(content, FRONTMATTER_NAME_PATTERN);
                            skillDescription = extractFrontmatterField(content, FRONTMATTER_DESCRIPTION_PATTERN);
                        }
                    } else {
                        String content = new String(entryBytes, StandardCharsets.UTF_8);

                        if (!AiAgentUtilsUtils.hasPerformFunction(content)) {
                            continue;
                        }

                        String fileName = entryName.substring(entryName.lastIndexOf('/') + 1);
                        String extension = getExtension(fileName);
                        String languageId = EXTENSION_TO_LANGUAGE_ID.get(extension.toLowerCase());

                        if (languageId != null) {
                            scriptEntries.add(new ScriptEntry(fileName, languageId, content));
                        } else {
                            context.log(log -> log.warn(
                                "Unsupported script extension '{}' in '{}', skipping", extension, entryName));
                        }
                    }
                }
            }

            return new SkillExtraction(tempDirectory, new SkillScripts(skillName, skillDescription, scriptEntries));
        } catch (IOException ioException) {
            throw new UncheckedIOException("Failed to extract skill resources from zip archive", ioException);
        }
    }

    private static String extractFrontmatterField(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);

        return matcher.find() ? matcher.group(1)
            .trim() : "";
    }

    private static String sanitizeToolName(String name) {
        return name.replaceAll("[^A-Za-z0-9_]", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_|_$", "");
    }

    private static String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');

        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    private static String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');

        return dotIndex > 0 ? fileName.substring(dotIndex + 1) : "";
    }

    private record SkillExtraction(Path skillDirectory, SkillScripts skillScripts) {
    }

    private record SkillScripts(String name, String description, List<ScriptEntry> scripts) {
    }

    private record ScriptEntry(String fileName, String languageId, String content) {
    }
}
