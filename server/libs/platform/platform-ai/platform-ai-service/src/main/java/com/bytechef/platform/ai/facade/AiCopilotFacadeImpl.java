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

package com.bytechef.platform.ai.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.ai.facade.dto.ContextDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
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
    private final ChatClient chatClientComponent;
    private final ChatClient chatClientScript;
    private final WorkflowService workflowService;
    private final OrchestratorWorkers orchestratorWorkers;

    @SuppressFBWarnings("EI")
    public AiCopilotFacadeImpl(
        ChatClient.Builder chatClientBuilder, VectorStore vectorStore, WorkflowService workflowService) {

        this.workflowService = workflowService;

        MessageChatMemoryAdvisor messageChatMemoryAdvisor = new MessageChatMemoryAdvisor(new InMemoryChatMemory());

        SearchRequest.Builder searchRequestBuilder = SearchRequest.builder();

        SearchRequest.Builder componentsBuilder = searchRequestBuilder.filterExpression("category == 'components'");

        QuestionAnswerAdvisor questionAnswerAdvisorComponents = new QuestionAnswerAdvisor(
            vectorStore, componentsBuilder.build());

        SearchRequest.Builder workflowsBuilder = searchRequestBuilder.filterExpression("category == 'workflows'");

        QuestionAnswerAdvisor questionAnswerAdvisorWorkflow = new QuestionAnswerAdvisor(
            vectorStore, workflowsBuilder.build());

        this.chatClientComponent = chatClientBuilder.clone()
            // TODO add multiuser, multitenant history
            .defaultAdvisors(
                messageChatMemoryAdvisor,
                questionAnswerAdvisorComponents)
            .build();

        this.chatClientWorkflow = chatClientBuilder.clone()
            // TODO add multiuser, multitenant history
            .defaultAdvisors(
                messageChatMemoryAdvisor,
                questionAnswerAdvisorWorkflow
//                , questionAnswerAdvisorComponents
            )
            .build();

        this.chatClientScript = chatClientBuilder.clone()
            // TODO add multiuser, multitenant history
            .defaultAdvisors(
                messageChatMemoryAdvisor,
                questionAnswerAdvisorComponents
            // add script advisor
            )
            .build();

        this.orchestratorWorkers = new OrchestratorWorkers(this.chatClientComponent,
            "BAnalyze this task and break it down into subtasks so that the first subtask is a single Trigger, and each subsequent subtasks are either a single Action or a single Flow (example for-loop, if-statement ...).",
            "Search the context for an existing Component or Flow and Trigger or Action that best match the described subtask. Only return Triggers, Actions or Flows that exist in the context with correct Parameters. Return them in the described JSON format.");
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
            case WORKFLOW_EDITOR, WORKFLOW_EDITOR_COMPONENTS_POPOVER_MENU -> {
                OrchestratorWorkers.FinalResponse process = orchestratorWorkers.process(message);

                yield chatClientWorkflow.prompt()
//                    .system(
//                        """
//                            Answer only with a json in a format similar to the json objects in the context of the specific Component.
//                            Only use the Actions, Triggers and Parameters which you know exist; look for JSON Example
//                            for the action or trigger.
//                            """)
                    .user(user -> user.text("Merge all the subtasks into a json workflow similar to the ones in the context.")
                        .param("task_analysis", process.analysis())
                        .param("task_list", process.workerResponses()))
                    .advisors(advisor -> advisor.param(
                        AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId))
                    .stream()
                    .content()
                    .map(content -> Map.of("text", content));
            }
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
