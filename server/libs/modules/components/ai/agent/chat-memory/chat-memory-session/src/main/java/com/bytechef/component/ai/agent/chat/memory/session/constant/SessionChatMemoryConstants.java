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

package com.bytechef.component.ai.agent.chat.memory.session.constant;

/**
 * @author Ivica Cardic
 */
public class SessionChatMemoryConstants {

    public static final String SESSION_CHAT_MEMORY = "sessionChatMemory";

    public static final String CONVERSATION_ID = "conversationId";
    public static final String DEFAULT_USER_ID = "defaultUserId";
    public static final String COMPACTION_STRATEGY = "compactionStrategy";
    public static final String MAX_EVENTS = "maxEvents";
    public static final String MAX_TURNS = "maxTurns";
    public static final String MAX_TOKENS = "maxTokens";
    public static final String MAX_EVENTS_TO_KEEP = "maxEventsToKeep";
    public static final String OVERLAP_SIZE = "overlapSize";
    public static final String ENABLE_CONVERSATION_SEARCH = "enableConversationSearch";
    public static final String SEARCH_PAGE_SIZE = "searchPageSize";
    public static final String AGENT_BRANCH = "agentBranch";

    // compactionStrategy option values
    public static final String NONE = "NONE";
    public static final String SLIDING_WINDOW = "SLIDING_WINDOW";
    public static final String TURN_WINDOW = "TURN_WINDOW";
    public static final String TOKEN_COUNT = "TOKEN_COUNT";
    public static final String RECURSIVE_SUMMARIZATION = "RECURSIVE_SUMMARIZATION";

    public static final String DEFAULT_USER_ID_VALUE = "bytechef";

    private SessionChatMemoryConstants() {
    }
}
