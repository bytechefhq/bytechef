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
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.definition.BaseOutputDefinition;
import com.bytechef.definition.BaseProperty;
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
            case ASSISTANT -> new AssistantMessage(message.content());
            case SYSTEM -> new SystemMessage(message.content());
            case TOOL -> new ToolResponseMessage(new ArrayList<>());
            case USER -> {
                List<FileEntry> attachments = message.attachments();
                StringBuilder content = new StringBuilder(message.content());

                if (attachments == null || attachments.isEmpty()) {
                    yield new UserMessage(message.content());
                } else {
                    List<Media> media = new ArrayList<>();

                    for (FileEntry attachment : attachments) {
                        String mimeType = attachment.getMimeType();

                        if (mimeType.startsWith("text/")) {
                            content.append("\n");
                            content.append((String) actionContext.file(file -> file.readToString(attachment)));
                        } else if (mimeType.startsWith("image/")) {
                            byte[] attachmentBytes = actionContext.file(file -> file.readAllBytes(attachment));

                            media.add(
                                new Media(
                                    MimeTypeUtils.parseMimeType(mimeType), new ByteArrayResource(attachmentBytes)));
                        } else {
                            throw new IllegalArgumentException("Unsupported attachment type: " + mimeType);
                        }
                    }

                    yield new UserMessage(content.toString(), media);
                }
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static <R> List<Option<R>> getEnumOptions(Map<String, R> map) {
        return map.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> (Option<R>) option(entry.getKey(), entry.getValue()))
            .toList();
    }

    public static BaseOutputDefinition.OutputResponse output(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        BaseProperty.BaseValueProperty<?> outputSchemaProperty = string();

        if (inputParameters.getFromPath("response.responseFormat", Integer.class, 1) == 2) {
            String responseSchema = inputParameters.getRequiredFromPath("response.responseSchema", String.class);

            outputSchemaProperty = actionContext.outputSchema(
                outputSchema -> outputSchema.getOutputSchema(responseSchema));
        }

        return new BaseOutputDefinition.OutputResponse(outputSchemaProperty);
    }
}
