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

package com.bytechef.component.ai.llm.util;

import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Option;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeTypeUtils;

/**
 * @author Monika Kušter
 * @author Marko Kriskovic
 */
public class LLMUtils {
    private LLMUtils() {
    }

    public static Message createMessage(ChatModel.Message message, ActionContext actionContext) {

        return switch (message.role()) {
            case "system" -> new SystemMessage(message.content());
            case "user" -> {
                FileEntry fileEntry = message.image();

                if (fileEntry == null) {
                    yield new UserMessage(message.content());
                } else {
                    String mimeType = fileEntry.getMimeType();
                    byte[] encodedImageBytes = actionContext.file(file -> file.readAllBytes(fileEntry));

                    yield new UserMessage(
                        message.content(), new Media(
                            MimeTypeUtils.parseMimeType(mimeType), new ByteArrayResource(encodedImageBytes)));
                }
            }
            case "assistant" -> new AssistantMessage(message.content());
            case "tool" -> new ToolResponseMessage(new ArrayList<>());
            default -> null;
        };
    }

    @SuppressWarnings("unchecked")
    public static <R> List<Option<R>> getEnumOptions(Map<String, R> map) {
        return map.entrySet()
            .stream()
            .map(entry -> (Option<R>) option(entry.getKey(), entry.getValue()))
            .toList();
    }
}
