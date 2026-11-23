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

package com.bytechef.automation.workflow.execution.tool.config;

import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.automation.configuration.tool.ProjectWorkflowTools;
import com.bytechef.automation.configuration.tool.ScriptTools;
import com.bytechef.automation.workflow.execution.tool.WorkflowExecutionTools;
import com.bytechef.platform.component.tool.TaskTools;
import com.bytechef.platform.workflow.tool.WorkflowInstructionTools;
import com.bytechef.platform.workflow.tool.WorkflowValidatorTools;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * @author Marko Kriskovic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class WorkflowExecutionAgentConfiguration {

    private final WorkflowValidatorTools workflowValidatorTools;
    private final WorkflowInstructionTools workflowInstructionTools;
    private final String workflowExecutionBuildSystemPrompt;

    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public WorkflowExecutionAgentConfiguration(
        @Value("classpath:prompt_workflow_execution_build.txt") Resource promptWorkflowExecutionBuildResource,
        WorkflowValidatorTools workflowValidatorTools,
        WorkflowInstructionTools workflowInstructionTools) {

        this.workflowValidatorTools = workflowValidatorTools;
        this.workflowInstructionTools = workflowInstructionTools;
        this.workflowExecutionBuildSystemPrompt = getSystemPrompt(promptWorkflowExecutionBuildResource);
    }

    @Bean
    public ChatClient workflowExecutionBuildSubAgentChatClient(
        ChatModel chatModel, WorkflowExecutionTools workflowExecutionTools, ProjectWorkflowTools projectWorkflowTools,
        ScriptTools scriptTools, TaskTools taskTools) {

        return buildWorkflowExecutionBuildSubAgentChatClient(
            chatModel, workflowExecutionTools, projectWorkflowTools, scriptTools, taskTools);
    }

    @Bean
    public IntelligentToolChatClientFactory workflowExecutionBuildSubAgentChatClientFactory(
        @Qualifier("workflowExecutionBuildSubAgentChatClient") ChatClient workflowExecutionBuildSubAgentChatClient,
        WorkflowExecutionTools workflowExecutionTools, ProjectWorkflowTools projectWorkflowTools,
        ScriptTools scriptTools, TaskTools taskTools) {

        return candidateChatModel -> candidateChatModel == null
            ? workflowExecutionBuildSubAgentChatClient
            : buildWorkflowExecutionBuildSubAgentChatClient(
                candidateChatModel, workflowExecutionTools, projectWorkflowTools, scriptTools, taskTools);
    }

    private ChatClient buildWorkflowExecutionBuildSubAgentChatClient(
        ChatModel chatModel, WorkflowExecutionTools workflowExecutionTools, ProjectWorkflowTools projectWorkflowTools,
        ScriptTools scriptTools, TaskTools taskTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(workflowExecutionBuildSystemPrompt)
            .defaultTools(
                workflowExecutionTools, projectWorkflowTools, scriptTools, taskTools, workflowValidatorTools,
                workflowInstructionTools)
            .build();
    }

    private String getSystemPrompt(Resource systemPromptResource) {
        try {
            InputStream inputStream = systemPromptResource.getInputStream();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read system prompt resource: " + systemPromptResource.getDescription(), exception);
        }
    }
}
