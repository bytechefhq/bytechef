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

package com.bytechef.ai.copilot.config;

import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.ai.copilot.agent.ProjectSpringAIAgent;
import com.bytechef.ai.copilot.tool.RehydrateContextToolCallback;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolScope;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.copilot.util.Mode;
import com.bytechef.ai.copilot.util.Source;
import com.bytechef.automation.ai.tool.ProjectTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ReadProjectTools;
import com.bytechef.automation.ai.tool.ReadProjectWorkflowTools;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Registers the Project Copilot panel source agents ({@code project_ask}/{@code project_build}). Lives in CE alongside
 * {@code CopilotConfiguration} because {@link ProjectTools}, {@link ProjectWorkflowTools}, {@link ReadProjectTools},
 * and {@link ReadProjectWorkflowTools} are CE.
 *
 * <p>
 * The BUILD agent delegates workflow-content work to the {@code buildWorkflow} and {@code importWorkflow} subagents,
 * fetched from the {@link com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog} via
 * {@link com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog#getForPanel} scoped to
 * {@link com.bytechef.ai.copilot.tool.catalog.IntelligentToolScope#PROJECT}; the catalog silently omits either
 * definition whose underlying {@code ChatClient} bean is absent. It does NOT wire a code-workflow delegate:
 * {@code CodeWorkflowAgentToolCallback} lives in the EE module {@code automation-ai-copilot}
 * ({@code server/ee/libs/automation/automation-ai/automation-ai-copilot}), and this module is CE. Wiring it here would
 * invert the CE/EE dependency direction. The EE module should contribute that delegate itself (e.g. by decorating or
 * replacing this configuration's {@code project_build} agent) as a follow-up.
 *
 * <p>
 * Gated on {@code bytechef.ai.copilot.enabled} rather than an OR with {@code bytechef.ai.hub.enabled}: the
 * {@code buildWorkflow}/{@code importWorkflow} delegate {@code ChatClient} beans that
 * {@link com.bytechef.ai.copilot.config.CopilotIntelligentToolContributor} resolves are declared in
 * {@code CopilotConfiguration}, which is gated on {@code bytechef.ai.copilot.enabled} alone. An OR-gate here would let
 * this configuration register with {@code hub.enabled=true, copilot.enabled=false}, producing a {@code project_build}
 * agent whose prompt instructs it to delegate workflow-content work to those subagents while both are silently absent.
 * Matching the gate keeps the slice and its delegates present or absent together.
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class ProjectAgentConfiguration {

    @Value("classpath:prompt_project_ask.txt")
    private Resource promptProjectAskResource;

    @Value("classpath:prompt_project_build.txt")
    private Resource promptProjectBuildResource;

    private final State state = new State();

    @Bean
    ProjectSpringAIAgent projectAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.PROJECT.name() + "_" + Mode.ASK.name();

        return ProjectSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptProjectAskResource))
            .state(state)
            .toolCallbacks(askToolCallbacks(securityContextRehydrator, readProjectTools, readProjectWorkflowTools))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    ProjectSpringAIAgent projectBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ProjectTools projectTools,
        ProjectWorkflowTools projectWorkflowTools, SecurityContextRehydrator securityContextRehydrator,
        IntelligentToolCatalog intelligentToolCatalog,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.PROJECT.name() + "_" + Mode.BUILD.name();

        return ProjectSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptProjectBuildResource))
            .state(state)
            .toolCallbacks(
                buildToolCallbacks(
                    securityContextRehydrator, projectTools, projectWorkflowTools, intelligentToolCatalog))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    /**
     * Package-private so {@code ProjectAgentConfigurationTest} can assert on the resolved tool names directly —
     * {@link ProjectSpringAIAgent} does not expose its wrapped {@link ToolCallback} list.
     */
    List<ToolCallback> askToolCallbacks(
        SecurityContextRehydrator securityContextRehydrator, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools) {

        return wrapTools(securityContextRehydrator, List.of(readProjectTools, readProjectWorkflowTools));
    }

    /**
     * Package-private so {@code ProjectAgentConfigurationTest} can assert on the resolved tool names directly —
     * {@link ProjectSpringAIAgent} does not expose its wrapped {@link ToolCallback} list.
     */
    List<ToolCallback> buildToolCallbacks(
        SecurityContextRehydrator securityContextRehydrator, ProjectTools projectTools,
        ProjectWorkflowTools projectWorkflowTools, IntelligentToolCatalog intelligentToolCatalog) {

        List<ToolCallback> toolCallbacks =
            new ArrayList<>(wrapTools(securityContextRehydrator, List.of(projectTools, projectWorkflowTools)));

        // Both delegates are registered bare here, matching their pre-catalog registration: the panel's flat CRUD
        // tools (projectTools/projectWorkflowTools above) get RehydrateContextToolCallback via wrapTools, but these
        // two intelligent delegates never did and still don't.
        toolCallbacks.addAll(
            intelligentToolCatalog.getForPanel(
                IntelligentToolScope.PROJECT, IntelligentToolVariant.BUILD, (chatClient, definition) -> chatClient,
                (toolCallback, definition) -> toolCallback));

        return toolCallbacks;
    }

    private List<ToolCallback> wrapTools(SecurityContextRehydrator securityContextRehydrator, List<Object> tools) {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (Object tool : tools) {
            if (tool instanceof ToolCallback toolCallback) {
                toolCallbacks.add(RehydrateContextToolCallback.wrap(toolCallback, securityContextRehydrator));
            } else {
                for (ToolCallback toolCallback : ToolCallbacks.from(tool)) {
                    toolCallbacks.add(RehydrateContextToolCallback.wrap(toolCallback, securityContextRehydrator));
                }
            }
        }

        return toolCallbacks;
    }

    private String readPrompt(Resource resource) {
        try {
            InputStream inputStream = resource.getInputStream();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read project prompt resource: " + resource.getDescription(), exception);
        }
    }
}
