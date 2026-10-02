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

package com.bytechef.component.ai.agent.chat.memory.session.cluster;

import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.COMPACTION_STRATEGY;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.CONVERSATION_ID;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.DEFAULT_USER_ID;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.DEFAULT_USER_ID_VALUE;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.ENABLE_CONVERSATION_SEARCH;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.MAX_EVENTS;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.MAX_EVENTS_TO_KEEP;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.MAX_TOKENS;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.MAX_TURNS;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.NONE;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.OVERLAP_SIZE;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.RECURSIVE_SUMMARIZATION;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.SEARCH_PAGE_SIZE;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.SLIDING_WINDOW;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.TOKEN_COUNT;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.TURN_WINDOW;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction.CHAT_MEMORY;
import static com.bytechef.platform.component.definition.ai.agent.ModelFunction.MODEL;
import static com.bytechef.platform.component.definition.ai.agent.SessionRepositoryFunction.SESSION_REPOSITORY;

import com.bytechef.component.ai.agent.chat.memory.session.compaction.EventCountTrigger;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.definition.ai.agent.SessionRepositoryFunction;
import com.bytechef.platform.component.definition.ai.agent.SessionServiceChatMemory;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import java.util.Map;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.session.DefaultSessionService;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.session.compaction.CompactionStrategy;
import org.springframework.ai.session.compaction.CompactionTrigger;
import org.springframework.ai.session.compaction.RecursiveSummarizationCompactionStrategy;
import org.springframework.ai.session.compaction.SlidingWindowCompactionStrategy;
import org.springframework.ai.session.compaction.TokenCountCompactionStrategy;
import org.springframework.ai.session.compaction.TokenCountTrigger;
import org.springframework.ai.session.compaction.TurnCountTrigger;
import org.springframework.ai.session.compaction.TurnWindowCompactionStrategy;
import org.springframework.ai.session.tool.SessionEventTools;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

/**
 * @author Ivica Cardic
 */
public class SessionChatMemory {

    private static final int DEFAULT_MAX_EVENTS = 20;
    private static final int DEFAULT_MAX_TURNS = 10;
    private static final int DEFAULT_MAX_TOKENS = 4000;
    private static final int DEFAULT_MAX_EVENTS_TO_KEEP = 10;
    private static final int DEFAULT_OVERLAP_SIZE = 2;
    private static final int DEFAULT_SEARCH_PAGE_SIZE = 10;

    private final ClusterElementDefinitionService clusterElementDefinitionService;

    public static ClusterElementDefinition<ChatMemoryFunction> of(
        ClusterElementDefinitionService clusterElementDefinitionService) {

        return new SessionChatMemory(clusterElementDefinitionService).build();
    }

    private SessionChatMemory(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.clusterElementDefinitionService = clusterElementDefinitionService;
    }

    private ClusterElementDefinition<ChatMemoryFunction> build() {
        return ComponentDsl.<ChatMemoryFunction>clusterElement("chatMemory")
            .title("Session Chat Memory")
            .description("Event-sourced session memory; prior messages are recalled per conversation session.")
            .properties(
                string(CONVERSATION_ID)
                    .label("Conversation ID")
                    .description("The unique identifier for the conversation session.")
                    .required(true),
                string(DEFAULT_USER_ID)
                    .label("Default User ID")
                    .description("User id assigned to new sessions.")
                    .defaultValue(DEFAULT_USER_ID_VALUE)
                    .required(false),
                string(COMPACTION_STRATEGY)
                    .label("Compaction Strategy")
                    .description("How to shrink history when it grows. Older events are archived, not deleted.")
                    .options(
                        option("None", NONE),
                        option("Sliding window (by events)", SLIDING_WINDOW),
                        option("Turn window (by turns)", TURN_WINDOW),
                        option("Token count", TOKEN_COUNT),
                        option("Recursive summarization (LLM)", RECURSIVE_SUMMARIZATION))
                    .defaultValue(NONE)
                    .required(false),
                integer(MAX_EVENTS)
                    .label("Max events")
                    .description(
                        "Compaction runs once the history has more events than this. For recursive summarization " +
                            "it must be greater than Max events to keep.")
                    .defaultValue(DEFAULT_MAX_EVENTS)
                    .displayCondition(
                        "%s == '%s' || %s == '%s'".formatted(
                            COMPACTION_STRATEGY, SLIDING_WINDOW, COMPACTION_STRATEGY, RECURSIVE_SUMMARIZATION))
                    .required(true),
                integer(MAX_TURNS)
                    .label("Max turns")
                    .description("The number of most recent turns kept in the history.")
                    .defaultValue(DEFAULT_MAX_TURNS)
                    .displayCondition("%s == '%s'".formatted(COMPACTION_STRATEGY, TURN_WINDOW))
                    .required(true),
                integer(MAX_TOKENS)
                    .label("Max tokens")
                    .description("The estimated token budget of the history.")
                    .defaultValue(DEFAULT_MAX_TOKENS)
                    .displayCondition("%s == '%s'".formatted(COMPACTION_STRATEGY, TOKEN_COUNT))
                    .required(true),
                integer(MAX_EVENTS_TO_KEEP)
                    .label("Max events to keep")
                    .description("The number of most recent events kept in full; older events are summarized.")
                    .defaultValue(DEFAULT_MAX_EVENTS_TO_KEEP)
                    .displayCondition("%s == '%s'".formatted(COMPACTION_STRATEGY, RECURSIVE_SUMMARIZATION))
                    .required(true),
                integer(OVERLAP_SIZE)
                    .label("Overlap size")
                    .description(
                        "The number of kept events also shown to the summarizer for continuity. It must be less " +
                            "than Max events to keep.")
                    .defaultValue(DEFAULT_OVERLAP_SIZE)
                    .displayCondition("%s == '%s'".formatted(COMPACTION_STRATEGY, RECURSIVE_SUMMARIZATION))
                    .required(true),
                bool(ENABLE_CONVERSATION_SEARCH)
                    .label("Enable conversation search tool")
                    .description("Exposes a keyword search tool over the full archived event log (recall storage).")
                    .defaultValue(false)
                    .required(false),
                integer(SEARCH_PAGE_SIZE)
                    .label("Search page size")
                    .description("The number of results the conversation search tool returns per page.")
                    .defaultValue(DEFAULT_SEARCH_PAGE_SIZE)
                    .displayCondition("%s == true".formatted(ENABLE_CONVERSATION_SEARCH))
                    .required(false))
            .type(CHAT_MEMORY)
            .object(() -> this::apply);
    }

    protected ChatMemoryFunction.Result apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) throws Exception {

        SessionService sessionService = DefaultSessionService.builder()
            .sessionRepository(resolveSessionRepository(extensions, componentConnections))
            .build();

        SessionMemoryAdvisor.Builder builder = SessionMemoryAdvisor
            .builder(sessionService)
            .defaultUserId(inputParameters.getString(DEFAULT_USER_ID, DEFAULT_USER_ID_VALUE))
            .order(ChatMemoryFunction.TOOL_MESSAGE_PERSISTENCE_ADVISOR_ORDER);

        Compaction compaction = resolveCompaction(
            inputParameters, () -> resolveSummarizerChatClient(extensions, componentConnections));

        if (compaction != null) {
            builder.compactionStrategy(compaction.strategy())
                .compactionTrigger(compaction.trigger());
        }

        BaseAdvisor advisor = builder.build();

        ToolCallback[] toolCallbacks = resolveRecallToolCallbacks(inputParameters, sessionService);

        return new ChatMemoryFunction.Result(
            advisor, new SessionServiceChatMemory(sessionService), toolCallbacks, true);
    }

    private SessionRepository resolveSessionRepository(
        Parameters extensions, Map<String, ComponentConnection> componentConnections) throws Exception {

        ClusterElement clusterElement = ClusterElementMap.of(extensions)
            .getClusterElement(SESSION_REPOSITORY);

        SessionRepositoryFunction sessionRepositoryFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        ComponentConnection componentConnection = componentConnections.get(clusterElement.getWorkflowNodeName());

        return sessionRepositoryFunction.apply(
            ParametersFactory.create(clusterElement.getParameters()),
            ParametersFactory.create(componentConnection),
            ParametersFactory.create(clusterElement.getExtensions()), componentConnections);
    }

    static @Nullable Compaction resolveCompaction(
        Parameters inputParameters, Callable<ChatClient> summarizerChatClientResolver) throws Exception {

        String compactionStrategy = inputParameters.getString(COMPACTION_STRATEGY, NONE);

        return switch (compactionStrategy) {
            case NONE -> null;
            case SLIDING_WINDOW -> {
                int maxEvents = getPositiveInteger(inputParameters, MAX_EVENTS, DEFAULT_MAX_EVENTS, "Max events");

                yield new Compaction(
                    SlidingWindowCompactionStrategy.builder()
                        .maxEvents(maxEvents)
                        .build(),
                    new EventCountTrigger(maxEvents));
            }
            case TURN_WINDOW -> {
                int maxTurns = getPositiveInteger(inputParameters, MAX_TURNS, DEFAULT_MAX_TURNS, "Max turns");

                yield new Compaction(
                    TurnWindowCompactionStrategy.builder()
                        .maxTurns(maxTurns)
                        .build(),
                    new TurnCountTrigger(maxTurns));
            }
            case TOKEN_COUNT -> {
                int maxTokens = getPositiveInteger(inputParameters, MAX_TOKENS, DEFAULT_MAX_TOKENS, "Max tokens");

                yield new Compaction(
                    TokenCountCompactionStrategy.builder()
                        .maxTokens(maxTokens)
                        .build(),
                    TokenCountTrigger.builder()
                        .threshold(maxTokens)
                        .build());
            }
            case RECURSIVE_SUMMARIZATION -> {
                int maxEvents = inputParameters.getInteger(MAX_EVENTS, DEFAULT_MAX_EVENTS);
                int maxEventsToKeep = getPositiveInteger(
                    inputParameters, MAX_EVENTS_TO_KEEP, DEFAULT_MAX_EVENTS_TO_KEEP, "Max events to keep");
                int overlapSize = inputParameters.getInteger(OVERLAP_SIZE, DEFAULT_OVERLAP_SIZE);

                if (overlapSize < 0 || overlapSize >= maxEventsToKeep) {
                    throw new IllegalArgumentException(
                        "Overlap size must be at least 0 and less than max events to keep (" + maxEventsToKeep +
                            ") for recursive summarization, but was " + overlapSize);
                }

                if (maxEvents <= maxEventsToKeep) {
                    throw new IllegalArgumentException(
                        "Max events (" + maxEvents + ") must be greater than max events to keep (" +
                            maxEventsToKeep + ") for recursive summarization");
                }

                yield new Compaction(
                    RecursiveSummarizationCompactionStrategy.builder(summarizerChatClientResolver.call())
                        .maxEventsToKeep(maxEventsToKeep)
                        .overlapSize(overlapSize)
                        .build(),
                    new EventCountTrigger(maxEvents));
            }
            default -> throw new IllegalArgumentException("Unknown compaction strategy: " + compactionStrategy);
        };
    }

    private static int getPositiveInteger(Parameters inputParameters, String name, int defaultValue, String label) {
        int value = inputParameters.getInteger(name, defaultValue);

        if (value <= 0) {
            throw new IllegalArgumentException(label + " must be greater than 0, but was " + value);
        }

        return value;
    }

    private ChatClient resolveSummarizerChatClient(
        Parameters extensions, Map<String, ComponentConnection> componentConnections) throws Exception {

        ClusterElement clusterElement = ClusterElementMap.of(extensions)
            .fetchClusterElement(MODEL)
            .orElseThrow(() -> new IllegalStateException(
                "Recursive summarization requires a Model child to be configured on Session Chat Memory."));

        ModelFunction modelFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        ComponentConnection componentConnection = componentConnections.get(clusterElement.getWorkflowNodeName());

        ChatModel chatModel = (ChatModel) modelFunction.apply(
            ParametersFactory.create(clusterElement.getParameters()),
            ParametersFactory.create(componentConnection),
            false);

        return ChatClient.builder(chatModel)
            .build();
    }

    private @Nullable ToolCallback[] resolveRecallToolCallbacks(
        Parameters inputParameters, SessionService sessionService) {

        if (!Boolean.TRUE.equals(inputParameters.getBoolean(ENABLE_CONVERSATION_SEARCH, false))) {
            return null;
        }

        SessionEventTools sessionEventTools = SessionEventTools.builder(sessionService)
            .pageSize(
                getPositiveInteger(inputParameters, SEARCH_PAGE_SIZE, DEFAULT_SEARCH_PAGE_SIZE, "Search page size"))
            .build();

        return ToolCallbacks.from(sessionEventTools);
    }

    record Compaction(CompactionStrategy strategy, CompactionTrigger trigger) {
    }
}
