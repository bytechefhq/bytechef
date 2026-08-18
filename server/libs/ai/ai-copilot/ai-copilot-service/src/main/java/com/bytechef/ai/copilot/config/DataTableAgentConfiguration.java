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
import com.bytechef.ai.copilot.agent.DataTableSpringAIAgent;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.ai.copilot.tool.RehydrateContextToolCallback;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.util.Mode;
import com.bytechef.ai.copilot.util.Source;
import com.bytechef.automation.ai.tool.ToolArtifactRecorder;
import com.bytechef.automation.ai.tool.datatable.DataTableToolCallbacksFactory;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Registers the Data Table Copilot panel source agents ({@code data_table_ask}/{@code data_table_build}) and the shared
 * {@link DataTableToolCallbacksFactory} bean the ai_hub agents and the management MCP server register flat (ticket 732,
 * CRUD-delegate-unwind Task 5 — the former {@code data_table_agent} subagent, and the two {@code ChatClient} beans that
 * used to back it, were dissolved). Lives in CE alongside {@code CopilotConfiguration} because
 * {@link DataTableToolCallbacksFactory} and the data-table services/facades it wraps are CE; the optional
 * {@code ToolArtifactRecorder} hook is a CE SPI that the EE AI Hub recorder plugs into when present.
 *
 * <p>
 * Gated so the beans exist when either the Copilot panel or the AI Hub surface is enabled, since both consume the panel
 * agents and/or the factory.
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnExpression("${bytechef.ai.copilot.enabled:false} or ${bytechef.ai.hub.enabled:false}")
public class DataTableAgentConfiguration {

    @Value("classpath:prompt_data_table_ask.txt")
    private Resource promptDataTableAskResource;

    @Value("classpath:prompt_data_table_build.txt")
    private Resource promptDataTableBuildResource;

    private final State state = new State();

    @Bean
    DataTableToolCallbacksFactory dataTableToolCallbacksFactory(
        WorkspaceDataTableFacade workspaceDataTableFacade, DataTableService dataTableService,
        DataTableRowService dataTableRowService,
        ObjectProvider<ToolArtifactRecorder> toolArtifactRecorderProvider) {

        return new DataTableToolCallbacksFactory(
            workspaceDataTableFacade, dataTableService, dataTableRowService,
            toolArtifactRecorderProvider.getIfAvailable());
    }

    @Bean
    DataTableSpringAIAgent dataTableAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, DataTableToolCallbacksFactory dataTableToolCallbacksFactory,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.DATA_TABLE.name() + "_" + Mode.ASK.name();

        return DataTableSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptDataTableAskResource))
            .state(state)
            .toolCallbacks(
                wrapToolCallbacks(securityContextRehydrator, dataTableToolCallbacksFactory.readToolCallbacks()))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    DataTableSpringAIAgent dataTableBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, DataTableToolCallbacksFactory dataTableToolCallbacksFactory,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.DATA_TABLE.name() + "_" + Mode.BUILD.name();

        return DataTableSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptDataTableBuildResource))
            .state(state)
            .toolCallbacks(
                wrapToolCallbacks(securityContextRehydrator, dataTableToolCallbacksFactory.writeToolCallbacks()))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    private List<ToolCallback> wrapToolCallbacks(
        SecurityContextRehydrator securityContextRehydrator, List<ToolCallback> toolCallbacks) {

        List<ToolCallback> wrapped = new ArrayList<>(toolCallbacks.size());

        for (ToolCallback toolCallback : toolCallbacks) {
            wrapped.add(RehydrateContextToolCallback.wrap(toolCallback, securityContextRehydrator));
        }

        return wrapped;
    }

    private String readPrompt(Resource resource) {
        try {
            InputStream inputStream = resource.getInputStream();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read data table prompt resource: " + resource.getDescription(), exception);
        }
    }
}
