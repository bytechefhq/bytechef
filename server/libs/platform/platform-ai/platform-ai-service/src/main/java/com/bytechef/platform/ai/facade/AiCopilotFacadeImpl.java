/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.ai.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.ai.facade.dto.ContextDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * @author Ivica Cardic
 */
@Service
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class AiCopilotFacadeImpl implements AiCopilotFacade {

    private final ChatClient chatClientWorkflow;
    private final ChatClient chatClientDocs;
    private final ChatClient chatClientScript;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public AiCopilotFacadeImpl(
        ChatClient.Builder chatClientBuilder, VectorStore vectorStore, WorkflowService workflowService) {

        this.workflowService = workflowService;

        MessageChatMemoryAdvisor messageChatMemoryAdvisor = new MessageChatMemoryAdvisor(new InMemoryChatMemory());

        SearchRequest.Builder searchRequestBuilder = SearchRequest.builder();

        SearchRequest.Builder documentationBuilder =
            searchRequestBuilder.filterExpression("category == 'documentation'");

        QuestionAnswerAdvisor questionAnswerAdvisorDocs = new QuestionAnswerAdvisor(
            vectorStore, documentationBuilder.build());

        SearchRequest.Builder componentsBuilder = searchRequestBuilder.filterExpression("category == 'components'");

        QuestionAnswerAdvisor questionAnswerAdvisorComponents = new QuestionAnswerAdvisor(
            vectorStore, componentsBuilder.build());

        SearchRequest.Builder workflowsBuilder = searchRequestBuilder.filterExpression("category == 'workflows'");

        QuestionAnswerAdvisor questionAnswerAdvisorWorkflow = new QuestionAnswerAdvisor(
            vectorStore, workflowsBuilder.build());

        this.chatClientDocs = chatClientBuilder.clone()
            // TODO add multiuser, multitenant history
            .defaultAdvisors(
                messageChatMemoryAdvisor,
                questionAnswerAdvisorDocs,
                questionAnswerAdvisorComponents)
            .build();

        this.chatClientWorkflow = chatClientBuilder.clone()
            // TODO add multiuser, multitenant history
            .defaultAdvisors(
                messageChatMemoryAdvisor,
                questionAnswerAdvisorWorkflow,
                questionAnswerAdvisorComponents)
            .build();

        this.chatClientScript = chatClientBuilder.clone()
            // TODO add multiuser, multitenant history
            .defaultAdvisors(
                messageChatMemoryAdvisor,
                questionAnswerAdvisorComponents
            // add script advisor
            )
            .build();
    }

    @Override
    public Flux<Map<String, ?>> chat(String message, ContextDTO contextDTO, String conversationId) {
        Workflow workflow = workflowService.getWorkflow(contextDTO.workflowId());

        final String userPrompt = """
              Current workflow:
              {workflow}
              Instructions:
              {message}
            """;
        final String workflowString = "workflow";
        final String messageString = "message";

        return switch (contextDTO.source()) {
            case WORKFLOW_EDITOR -> {
                ChatClient.ChatClientRequestSpec advisors = chatClientDocs.prompt()
                    .system(
                        """
                            You are a ByteChef assistant. You answer questions about ByteChef and help users with
                            problems. If a user asks you about generating a workflow: answer only with a json in a
                            format similar to the json objects in the vector database. Only use the actions, triggers
                            and parameters which you know exist.
                            """)
                    .user(message)
                    .advisors(advisor -> advisor
                        .param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId));
                yield advisors.stream()
                    .content()
                    .map(content -> Map.of("text", content));
            }
            case WORKFLOW_EDITOR_COMPONENTS_POPOVER_MENU ->
                chatClientWorkflow.prompt()
                    .system(
                        """
                            Answer only with a json in a format similar to the json objects in the vector database.
                            Only use the actions, triggers and parameters which you know exist; look for JSON Example
                            for the action or trigger. If a parameter is required, you must use it.""")
                    .user(user -> user.text(userPrompt)
                        .param(workflowString, workflow.getDefinition())
                        .param(messageString, message))
                    .advisors(advisor -> advisor.param(
                        AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId))
                    .stream()
                    .content()
                    .map(content -> Map.of("text", content));
            case CODE_EDITOR -> {
                Map<String, ?> parameters = contextDTO.parameters();

                yield switch ((String) parameters.get("language")) {
                    case "javascript" -> chatClientScript.prompt()
                        .system("You are a javascript code generator, answer only with code.")
                        .user(user -> user.text(userPrompt)
                            .param(workflowString, workflow.getDefinition())
                            .param(messageString, message))
                        .advisors(advisor -> advisor
                            .param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId))
                        .stream()
                        .content()
                        .map(content -> Map.of("text", content));
                    case "python" -> chatClientScript.prompt()
                        .system("You are a python code generator, answer only with code.")
                        .user(user -> user.text(userPrompt)
                            .param(workflowString, workflow.getDefinition())
                            .param(messageString, message))
                        .advisors(advisor -> advisor.param(
                            AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId))
                        .stream()
                        .content()
                        .map(content -> Map.of("text", content));
                    case "ruby" -> chatClientScript.prompt()
                        .system("You are a ruby code generator, answer only with code.")
                        .user(user -> user.text(userPrompt)
                            .param(workflowString, workflow.getDefinition())
                            .param(messageString, message))
                        .advisors(advisor -> advisor
                            .param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId))
                        .stream()
                        .content()
                        .map(content -> Map.of("text", content));
                    default -> throw new IllegalStateException("Unexpected value: " + parameters.get("language"));
                };
            }
            case null, default -> throw new IllegalStateException("Unexpected value: " + contextDTO.source());
        };
    }
}
