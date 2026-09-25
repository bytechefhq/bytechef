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

package com.bytechef.component.ai.agent.chat.memory.session.tool;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.tool.SessionEventTools;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * The {@code conversation_search} recall tool, scoped by an {@link EventFilter}. It mirrors the library's
 * {@link SessionEventTools} but merges the scope into every search, so an agent configured with an agent branch never
 * reads a sibling branch's events. Archived events stay searchable.
 *
 * @author Ivica Cardic
 */
public class SessionConversationSearchTools {

    private static final String DEFAULT_SESSION_ID = "default";
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
        .build();

    private final int pageSize;
    private final EventFilter scopeFilter;
    private final SessionService sessionService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SessionConversationSearchTools(SessionService sessionService, int pageSize, EventFilter scopeFilter) {
        this.pageSize = pageSize;
        this.scopeFilter = scopeFilter;
        this.sessionService = sessionService;
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    @Tool(
        name = "conversation_search",
        description = "Search the full prior conversation history using case-insensitive keyword matching. " +
            "Returns paginated results ordered chronologically.")
    public String conversationSearch(
        @ToolParam(description = "Deep inner monologue private to you only.") String innerThought,
        @ToolParam(description = "Keyword to search for in the conversation history.") String query,
        @ToolParam(
            description = "Page of results to retrieve (0-indexed). Omit or use 0 for the first page.",
            required = false) Integer page,
        ToolContext toolContext) {

        int pageNumber = page == null ? 0 : Math.max(0, page);

        Map<String, Object> context = toolContext.getContext();

        Object sessionIdValue = context.get(SessionEventTools.SESSION_ID_CONTEXT_KEY);

        String sessionId = sessionIdValue instanceof String value && !value.isBlank() ? value : DEFAULT_SESSION_ID;

        EventFilter keywordFilter = EventFilter.keywordSearch(query, pageNumber, pageSize);

        List<SessionEvent> events = sessionService.getEvents(sessionId, keywordFilter.merge(scopeFilter));

        List<Map<String, String>> results = events.stream()
            .filter(event -> StringUtils.hasText(getText(event)))
            .map(event -> Map.of(
                "timestamp", String.valueOf(event.getTimestamp()),
                "type", getMessageTypeValue(event),
                "text", getText(event)))
            .toList();

        if (results.isEmpty()) {
            return "No results found.";
        }

        return JSON_MAPPER.writeValueAsString(results);
    }

    private static String getMessageTypeValue(SessionEvent event) {
        MessageType messageType = event.getMessageType();

        return messageType.getValue();
    }

    private static String getText(SessionEvent event) {
        Message message = event.getMessage();

        return message.getText();
    }
}
