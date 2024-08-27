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

package com.bytechef.component.llm.util;

import static com.bytechef.component.definition.ComponentDSL.option;

import com.bytechef.component.definition.ComponentDSL.ModifiableOption;
import java.util.ArrayList;
import java.util.Map;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * @author Monika Domiter
 */
public class LLMUtils {
    private LLMUtils() {
    }

    public static Message createMessage(String role, String content) {
        return switch (role) {
            case "system" -> new SystemMessage(content);
            case "user" -> new UserMessage(content);
            case "assistant" -> new AssistantMessage(content);
            case "tool" -> new ToolResponseMessage(new ArrayList<>());
            default -> null;
        };
    }

    public static <R> ModifiableOption[] getEnumOptions(Map<String, R> map) {
        return map.entrySet()
            .stream()
            .map(entry -> option(entry.getKey(), entry.getValue()))
            .toArray(ModifiableOption[]::new);
    }
}
