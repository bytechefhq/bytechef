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

package com.bytechef.component.ai.agent.utils.workflow.connection;

import static com.bytechef.ee.platform.ai.skill.SkillArchiveConstants.MAX_ZIP_ENTRIES;
import static com.bytechef.ee.platform.ai.skill.SkillArchiveConstants.MAX_ZIP_ENTRY_SIZE;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.ai.agent.utils.util.AiAgentUtilsUtils;
import com.bytechef.ee.platform.ai.skill.facade.AiSkillFacade;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.workflow.connection.ClusterElementConnectionFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Discovers {@link ComponentConnection} instances for the {@code skillsTool} cluster element by analysing the skill
 * scripts referenced in the cluster element's {@code skills} parameter.
 *
 * <p>
 * For each skill, the factory downloads the skill archive, scans script files for
 * {@code context.component.{name}.{action}(...)} calls, and creates a connection for every component that exists in the
 * registry. Unknown components are silently skipped.
 *
 * @author Ivica Cardic
 */
@Component
@SuppressFBWarnings("EI")
public class SkillComponentConnectionFactory implements ClusterElementConnectionFactory {

    private static final Logger log = LoggerFactory.getLogger(SkillComponentConnectionFactory.class);

    private static final String AI_AGENT_UTILS = "aiAgentUtils";
    private static final Pattern COMPONENT_CALL_PATTERN = Pattern.compile(
        "context\\.component\\.([A-Za-z][A-Za-z0-9_-]*)\\.([A-Za-z][A-Za-z0-9_]*)\\(([^)]*)\\)");
    private static final String SKILLS_TOOL = "skillsTool";
    private static final String SKILLS = "skills";

    private final AiSkillFacade aiSkillFacade;
    private final ComponentDefinitionService componentDefinitionService;

    public SkillComponentConnectionFactory(
        AiSkillFacade aiSkillFacade, ComponentDefinitionService componentDefinitionService) {

        this.aiSkillFacade = aiSkillFacade;
        this.componentDefinitionService = componentDefinitionService;
    }

    @Override
    public List<ComponentConnection> create(String workflowNodeName, Map<String, ?> parameters) {
        List<Long> skillIds = MapUtils.getList(parameters, SKILLS, Long.class, List.of());

        if (skillIds.isEmpty()) {
            return List.of();
        }

        Set<String> uniqueComponentNames = new LinkedHashSet<>();

        for (Long skillId : skillIds) {
            if (skillId == null) {
                continue;
            }

            try {
                byte[] zipBytes = aiSkillFacade.getAiSkillDownload(skillId);

                Map<String, List<ComponentCall>> scriptAnalysis =
                    getSkillScripts(zipBytes);

                for (List<ComponentCall> calls : scriptAnalysis.values()) {
                    for (ComponentCall call : calls) {
                        uniqueComponentNames.add(call.componentName());
                    }
                }
            } catch (RuntimeException runtimeException) {
                log.warn("Failed to analyze scripts for skill {}: {}", skillId, runtimeException.getMessage());
            }
        }

        List<ComponentConnection> connections = new ArrayList<>();

        for (String componentName : uniqueComponentNames) {
            try {
                ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
                    componentName, null);

                connections.add(ComponentConnection.of(
                    workflowNodeName, componentName,
                    componentDefinition.getVersion(), componentDefinition.isConnectionRequired()));
            } catch (RuntimeException runtimeException) {
                log.debug(
                    "Component '{}' referenced in skill scripts not found in registry, skipping", componentName);
            }
        }

        return connections;
    }

    @Override
    public boolean supports(String componentName, String clusterElementName) {
        return AI_AGENT_UTILS.equals(componentName) && SKILLS_TOOL.equals(clusterElementName);
    }

    static List<ComponentCall> extractComponentCalls(String content) {
        List<ComponentCall> calls = new ArrayList<>();

        Matcher matcher = COMPONENT_CALL_PATTERN.matcher(content);

        while (matcher.find()) {
            calls.add(new ComponentCall(matcher.group(1), matcher.group(2), StringUtils.trim(matcher.group(3))));
        }

        return calls;
    }

    /**
     * Scans all script files inside the skill zip archive for component calls of the form
     * {@code context.component.{componentName}.{actionName}(...)}. Only files under the {@code scripts/} directory that
     * contain a {@code perform} function are analysed.
     *
     * @return a map from script entry path to the list of component calls found in that script, ordered by appearance
     *         in the archive; empty if no matching scripts exist
     */
    static Map<String, List<ComponentCall>> getSkillScripts(byte[] zipBytes) {
        Map<String, List<ComponentCall>> result = new LinkedHashMap<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry zipEntry;
            int entryCount = 0;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (++entryCount > MAX_ZIP_ENTRIES) {
                    throw new IllegalArgumentException(
                        "Skill archive exceeds maximum allowed number of entries (" + MAX_ZIP_ENTRIES + ")");
                }

                if (zipEntry.isDirectory() || !AiAgentUtilsUtils.isScriptEntry(zipEntry.getName())) {
                    continue;
                }

                byte[] bytes = zipInputStream.readNBytes(MAX_ZIP_ENTRY_SIZE + 1);

                if (bytes.length > MAX_ZIP_ENTRY_SIZE) {
                    log.warn("Script '{}' exceeds maximum allowed size, skipping", zipEntry.getName());

                    continue;
                }

                String content = new String(bytes, StandardCharsets.UTF_8);

                if (!AiAgentUtilsUtils.hasPerformFunction(content)) {
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

    public record ComponentCall(String componentName, String actionName, String rawParameters) {
    }
}
