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

package com.bytechef.component.ai.agent.chat.memory.builtin.util;

import static com.bytechef.component.ai.agent.chat.memory.builtin.constant.ChatMemoryConstants.DEFAULT_USER_ID;
import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;

/**
 * @author Ivica Cardic
 */
public class ChatMemoryUtils {

    private ChatMemoryUtils() {
    }

    /**
     * Lists the built-in user's session ids as conversation options, using each session's first event text as the
     * option description.
     */
    public static ActionDefinition.OptionsFunction<String> getFirstMessages(SessionRepository sessionRepository) {
        return (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> {
            if (sessionRepository == null) {
                return List.of();
            }

            List<ComponentDsl.ModifiableOption<String>> options = new ArrayList<>();

            for (Session session : sessionRepository.findByUserId(DEFAULT_USER_ID)) {
                List<SessionEvent> events = sessionRepository.findEvents(session.id(), EventFilter.all());

                String description = events.isEmpty()
                    ? null
                    : events.get(0)
                        .getMessage()
                        .getText();

                options.add(option(session.id(), session.id(), description));
            }

            return options;
        };
    }

    /**
     * Returns the session ids of every conversation the built-in chat memory owns (sessions created under
     * {@link com.bytechef.component.ai.agent.chat.memory.builtin.constant.ChatMemoryConstants#DEFAULT_USER_ID}).
     */
    public static List<String> findConversationIds(SessionRepository sessionRepository) {
        List<String> conversationIds = new ArrayList<>();

        for (Session session : sessionRepository.findByUserId(DEFAULT_USER_ID)) {
            conversationIds.add(session.id());
        }

        return conversationIds;
    }
}
