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

package com.bytechef.component.ai.llm.open.router.action;

import static com.bytechef.component.ai.llm.ChatModel.Format.SIMPLE;
import static com.bytechef.component.ai.llm.ChatModel.ResponseFormat.JSON;
import static com.bytechef.component.ai.llm.constant.LLMConstants.ASK;
import static com.bytechef.component.ai.llm.constant.LLMConstants.ATTACHMENTS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.ATTACHMENTS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FORMAT_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FREQUENCY_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FREQUENCY_PENALTY_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LOGIT_BIAS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LOGIT_BIAS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MESSAGES;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MESSAGES_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PRESENCE_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PRESENCE_PENALTY_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PROMPT_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.REASONING_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_SCHEMA;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SEED;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SEED_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SYSTEM_PROMPT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SYSTEM_PROMPT_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_K_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER_PROMPT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.VERBOSITY_PROPERTY;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.LOGPROBS;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.LOGPROBS_PROPERTY;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.MAX_COMPLETION_TOKENS;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.MAX_COMPLETION_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.SUPPORTED_PARAMETERS;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.TOP_LOGPROBS;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.TOP_LOGPROBS_PROPERTY;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.getOpenRouterModels;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.Body;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.ChatModel.ResponseFormat;
import com.bytechef.component.ai.llm.ChatModel.Role;
import com.bytechef.component.ai.llm.converter.JsonSchemaStructuredOutputConverter;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Marko Kriskovic
 */
public class OpenRouterChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(ASK)
        .title("Ask")
        .description("Ask anything you want.")
        .properties(
            array(SUPPORTED_PARAMETERS)
                .label("Supported parameters")
                .description("Filter models by supported parameter")
                .items(string())
                .options(getSupportedParametersOptions())
                .defaultValue("response_format")
                .required(true),
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .options(getOpenRouterModels())
                .optionsLookupDependsOn(SUPPORTED_PARAMETERS)
                .required(true),
            PROMPT_PROPERTY,
            FORMAT_PROPERTY,
            SYSTEM_PROMPT_PROPERTY,
            ATTACHMENTS_PROPERTY,
            MESSAGES_PROPERTY,
            RESPONSE_PROPERTY,
//                .displayCondition("contains({'response_format','structured_outputs'}, %s)".formatted(SUPPORTED_PARAMETERS))
            FREQUENCY_PENALTY_PROPERTY
                .displayCondition("contains(%s, 'frequency_penalty')".formatted(SUPPORTED_PARAMETERS)),
            LOGIT_BIAS_PROPERTY
                .displayCondition("contains(%s, 'logit_bias')".formatted(SUPPORTED_PARAMETERS)),
            LOGPROBS_PROPERTY
                .displayCondition("contains(%s, 'logprobs')".formatted(SUPPORTED_PARAMETERS)),
            MAX_COMPLETION_TOKENS_PROPERTY
                .displayCondition("contains(%s, 'max_completion_tokens')".formatted(SUPPORTED_PARAMETERS)),
            MAX_TOKENS_PROPERTY
                .displayCondition("contains(%s, 'max_tokens')".formatted(SUPPORTED_PARAMETERS)),
            PRESENCE_PENALTY_PROPERTY
                .displayCondition("contains(%s, 'presence_penalty')".formatted(SUPPORTED_PARAMETERS)),
            REASONING_PROPERTY
                .displayCondition("contains(%s, 'reasoning')".formatted(SUPPORTED_PARAMETERS)),
            SEED_PROPERTY
                .displayCondition("contains(%s, 'seed')".formatted(SUPPORTED_PARAMETERS)),
            STOP_PROPERTY
                .displayCondition("contains(%s, 'stop')".formatted(SUPPORTED_PARAMETERS)),
            TEMPERATURE_PROPERTY
                .displayCondition("contains(%s, 'temperature')".formatted(SUPPORTED_PARAMETERS)),
            TOP_LOGPROBS_PROPERTY
                .displayCondition("contains(%s, 'top_logprobs')".formatted(SUPPORTED_PARAMETERS)),
            TOP_K_PROPERTY
                .displayCondition("contains(%s, 'top_k')".formatted(SUPPORTED_PARAMETERS)),
            TOP_P_PROPERTY
                .displayCondition("contains(%s, 'top_p')".formatted(SUPPORTED_PARAMETERS)),
            VERBOSITY_PROPERTY
                .displayCondition("contains(%s, 'verbosity')".formatted(SUPPORTED_PARAMETERS)),
            USER_PROPERTY)
        .output(ModelUtils::output)
        .perform(OpenRouterChatAction::perform);

    private static List<Option<String>> getSupportedParametersOptions() {
        return Arrays.stream(getSupportedParametersString())
            .map(param -> option(param, param))
            .collect(Collectors.toList());
    }

    private static String[] getSupportedParametersString() {
        return new String[] {
            "frequency_penalty",
            "include_reasoning",
            "logit_bias", "logprobs",
            "max_completion_tokens", "max_tokens", "min_p",
            "parallel_tool_calls", "presence_penalty",
            "reasoning", "reasoning_effort", "response_format", "repetition_penalty",
            "seed", "stop", "structured_outputs",
            "temperature", "tools", "tool_choice", "top_a", "top_k", "top_p", "top_logprobs",
            "verbosity",
            "web_search_options"
        };
    }

    private OpenRouterChatAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        List<Map<String, Object>> messages = buildMessages(inputParameters, context);
        Map<String, Object> body = buildRequestBody(inputParameters, messages);

        Map<String, Object> response = context.http(http -> http.post("/chat/completions"))
            .configuration(responseType(Context.Http.ResponseType.JSON))
            .body(Body.of(body))
            .execute()
            .getBody(new TypeReference<>() {});

        String content = extractContent(response);

        ResponseFormat responseFormat = inputParameters.getRequiredFromPath(
            RESPONSE + "." + RESPONSE_FORMAT, ResponseFormat.class);

        if (responseFormat == JSON) {
            JsonSchemaStructuredOutputConverter converter = new JsonSchemaStructuredOutputConverter(
                inputParameters.getFromPath(RESPONSE + "." + RESPONSE_SCHEMA, String.class), context);

            return converter.convert(content);
        }

        return content;
    }

    @SuppressWarnings("unchecked")
    private static String extractContent(Map<String, Object> response) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.getFirst().get("message");

        return (String) message.get("content");
    }

    private static List<Map<String, Object>> buildMessages(Parameters inputParameters, ActionContext context) {
        String format = inputParameters.getRequiredString(FORMAT);
        List<Map<String, Object>> messages = new ArrayList<>();

        if (format.equals(SIMPLE.name())) {
            String systemPrompt = inputParameters.getString(SYSTEM_PROMPT);

            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                messages.add(Map.of("role", "system", "content", systemPrompt));
            }

            String userPrompt = inputParameters.getRequiredString(USER_PROMPT);
            List<FileEntry> attachments = inputParameters.getList(ATTACHMENTS, FileEntry.class);

            messages.add(buildUserMessage(userPrompt, attachments, context));
        } else {
            List<ChatModel.Message> chatModelMessages = inputParameters.getList(MESSAGES, new TypeReference<>() {});

            for (ChatModel.Message chatModelMessage : chatModelMessages) {
                if (chatModelMessage.role() == Role.USER) {
                    messages.add(buildUserMessage(chatModelMessage.content(), chatModelMessage.attachments(), context));
                } else {
                    messages.add(Map.of(
                        "role", chatModelMessage.role().name().toLowerCase(),
                        "content", chatModelMessage.content()));
                }
            }
        }

        return messages;
    }

    private static Map<String, Object> buildUserMessage(
        String text, List<FileEntry> attachments, ActionContext context) {

        if (attachments == null || attachments.isEmpty()) {
            return Map.of("role", "user", "content", text);
        }

        List<Map<String, Object>> contentParts = new ArrayList<>();
        StringBuilder textBuilder = new StringBuilder(text);

        for (FileEntry attachment : attachments) {
            String mimeType = attachment.getMimeType();

            if (mimeType.startsWith("text/")) {
                String fileText = context.file(file -> file.readToString(attachment));

                textBuilder.append("\n").append(fileText);
            } else if (mimeType.startsWith("image/")) {
                byte[] bytes = context.file(file -> file.readAllBytes(attachment));
                String base64 = Base64.getEncoder().encodeToString(bytes);

                contentParts.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", "data:" + mimeType + ";base64," + base64)));
            }
        }

        contentParts.addFirst(Map.of("type", "text", "text", textBuilder.toString()));

        return Map.of("role", "user", "content", contentParts);
    }

    private static Map<String, Object> buildRequestBody(
        Parameters inputParameters, List<Map<String, Object>> messages) {

        Map<String, Object> body = new HashMap<>();

        body.put("model", inputParameters.getRequiredString(MODEL));
        body.put("messages", messages);

        Double frequencyPenalty = inputParameters.getDouble(FREQUENCY_PENALTY);

        if (frequencyPenalty != null) {
            body.put("frequency_penalty", frequencyPenalty);
        }

        Map<String, Double> logitBias = inputParameters.getMap(LOGIT_BIAS, new TypeReference<>() {});

        if (logitBias != null) {
            body.put("logit_bias", logitBias);
        }

        Boolean logprobs = inputParameters.getBoolean(LOGPROBS);

        if (logprobs != null) {
            body.put("logprobs", logprobs);
        }

        Integer maxCompletionTokens = inputParameters.getInteger(MAX_COMPLETION_TOKENS);

        if (maxCompletionTokens != null) {
            body.put("max_completion_tokens", maxCompletionTokens);
        }

        Integer maxTokens = inputParameters.getInteger(MAX_TOKENS);

        if (maxTokens != null) {
            body.put("max_tokens", maxTokens);
        }

        Double presencePenalty = inputParameters.getDouble(PRESENCE_PENALTY);

        if (presencePenalty != null) {
            body.put("presence_penalty", presencePenalty);
        }

        Integer seed = inputParameters.getInteger(SEED);

        if (seed != null) {
            body.put("seed", seed);
        }

        List<String> stop = inputParameters.getList(STOP, new TypeReference<>() {});

        if (stop != null && !stop.isEmpty()) {
            body.put("stop", stop);
        }

        Double temperature = inputParameters.getDouble(TEMPERATURE);

        if (temperature != null) {
            body.put("temperature", temperature);
        }

        Double topP = inputParameters.getDouble(TOP_P);

        if (topP != null) {
            body.put("top_p", topP);
        }

        Integer topLogprobs = inputParameters.getInteger(TOP_LOGPROBS);

        if (topLogprobs != null) {
            body.put("top_logprobs", topLogprobs);
        }

        String user = inputParameters.getString(USER);

        if (user != null) {
            body.put("user", user);
        }

        ResponseFormat responseFormat = inputParameters.getRequiredFromPath(
            RESPONSE + "." + RESPONSE_FORMAT, ResponseFormat.class);

        body.put("response_format", Map.of("type", responseFormat == JSON ? "json_object" : "text"));

        return body;
    }
}
