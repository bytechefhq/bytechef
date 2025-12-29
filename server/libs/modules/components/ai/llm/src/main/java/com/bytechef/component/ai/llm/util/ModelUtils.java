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

package com.bytechef.component.ai.llm.util;

import static com.bytechef.component.ai.llm.ChatModel.ResponseFormat.JSON;
import static com.bytechef.component.ai.llm.ChatModel.ResponseFormat.TEXT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.ATTACHMENTS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MESSAGES;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_SCHEMA;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SYSTEM_PROMPT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER_PROMPT;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.ChatModel.Format;
import com.bytechef.component.ai.llm.ChatModel.ResponseFormat;
import com.bytechef.component.ai.llm.ChatModel.Role;
import com.bytechef.component.ai.llm.converter.JsonSchemaStructuredOutputConverter;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.definition.BaseProperty;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.content.Media;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.RestClient;

/**
 * @author Monika Kušter
 * @author Marko Kriskovic
 */
public class ModelUtils {

    private ModelUtils() {
    }

    public static @Nullable Object getChatResponse(
        CallResponseSpec callResponseSpec, Parameters parameters, Context context) {

        return getChatResponse(callResponseSpec, parameters, true, context);
    }

    public static @Nullable Object getChatResponse(
        CallResponseSpec callResponseSpec, Parameters parameters, boolean responseFormatRequired, Context context) {

        Object response = null;
        ResponseFormat responseFormat = TEXT;

        if (responseFormatRequired) {
            responseFormat = parameters.getRequiredFromPath(RESPONSE + "." + RESPONSE_FORMAT, ResponseFormat.class);
        }

        if (responseFormat == TEXT) {
            try {
                ChatResponse chatResponse = callResponseSpec.chatResponse();

                if (chatResponse != null) {
                    response = chatResponse.getResult()
                        .getOutput()
                        .getText();
                }
            } catch (org.springframework.ai.retry.NonTransientAiException e) {
                String message = e.getMessage();

                String providerMessage = context.json(
                    json -> json.read(
                        message.substring(message.indexOf("{"), message.lastIndexOf("}") + 1), "error.message",
                        new TypeReference<>() {}));

                throw new ProviderException(providerMessage);
            }
        } else {
            response = callResponseSpec.entity(
                new JsonSchemaStructuredOutputConverter(
                    parameters.getFromPath(RESPONSE + "." + RESPONSE_SCHEMA, String.class), context));
        }

        return response;
    }

    @SuppressWarnings("unchecked")
    public static <R> List<Option<R>> getEnumOptions(Map<String, R> map) {
        return map.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> (Option<R>) option(entry.getKey(), entry.getValue()))
            .toList();
    }

    public static List<Message> getMessages(Parameters inputParameters, ActionContext actionContext) {
        return getMessages(inputParameters, actionContext, true);
    }

    public static List<Message> getMessages(
        Parameters inputParameters, ActionContext actionContext, boolean messageFormatRequired) {

        List<ChatModel.Message> chatModelMessages = new ArrayList<>();

        if (messageFormatRequired) {
            String format = inputParameters.getRequiredString(FORMAT);

            if (format.equals(Format.SIMPLE.name())) {
                String userPrompt = inputParameters.getRequiredString(USER_PROMPT);

                ChatModel.Message userMessage = new ChatModel.Message(
                    userPrompt, inputParameters.getList(ATTACHMENTS, FileEntry.class), Role.USER);

                chatModelMessages.add(userMessage);

                String systemPrompt = inputParameters.getString(SYSTEM_PROMPT);

                if (systemPrompt != null && !systemPrompt.isEmpty()) {
                    ChatModel.Message systeMMessage = new ChatModel.Message(systemPrompt, null, Role.SYSTEM);

                    chatModelMessages.add(systeMMessage);
                }
            } else {
                chatModelMessages = inputParameters.getList(MESSAGES, new TypeReference<>() {});
            }
        } else {
            chatModelMessages = inputParameters.getList(MESSAGES, new TypeReference<>() {});
        }

        return new ArrayList<>(
            chatModelMessages.stream()
                .map(chatModelMessage -> createMessage(chatModelMessage, actionContext))
                .toList());
    }

    public static RestClient.Builder getRestClientBuilder() {
        HttpClientSettings requestFactorySettings = new HttpClientSettings(
            null, Duration.of(1, ChronoUnit.MINUTES), Duration.of(5, ChronoUnit.MINUTES), SslBundle.of(null));

        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.jdk()
            .build(requestFactorySettings);

        return RestClient.builder()
            .requestFactory(requestFactory);
    }

    public static OutputResponse output(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        BaseProperty.BaseValueProperty<?> outputSchemaProperty = string();

        if (inputParameters.getFromPath(RESPONSE + "." + RESPONSE_FORMAT, ResponseFormat.class, TEXT) == JSON) {
            if (!inputParameters.containsPath(RESPONSE + "." + RESPONSE_SCHEMA)) {
                return null;
            }

            String responseSchema = inputParameters.getRequiredFromPath(RESPONSE + "." + RESPONSE_SCHEMA, String.class);

            outputSchemaProperty = actionContext.outputSchema(
                outputSchema -> outputSchema.getOutputSchema(responseSchema));
        }

        return OutputResponse.of(outputSchemaProperty);
    }

    private static Message createMessage(ChatModel.Message message, ActionContext actionContext) {
        String messageContent = processText(message.content());

        return switch (message.role()) {
            case ASSISTANT -> new AssistantMessage(messageContent);
            case SYSTEM -> new SystemMessage(messageContent);
//            case TOOL -> new ToolResponseMessage(new ArrayList<>());
            case USER -> {
                List<FileEntry> attachments = message.attachments();
                StringBuilder content = new StringBuilder(messageContent);

                if (attachments == null || attachments.isEmpty()) {
                    yield new UserMessage(messageContent);
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

                    yield UserMessage.builder()
                        .text(content.toString())
                        .media(media)
                        .build();
                }
            }
        };
    }

    private static String processText(String messageContent) {
        // Remove braces to avoid issues with prompt template
        return messageContent.replace("{", " ")
            .replace("}", " ");
    }
}
