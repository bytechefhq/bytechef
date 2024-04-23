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

package com.bytechef.component.openai.action;

import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.openai.constant.OpenAIConstants.ASK_CHAT_GPT;
import static com.bytechef.component.openai.constant.OpenAIConstants.CONTENT;
import static com.bytechef.component.openai.constant.OpenAIConstants.FREQUENCY_PENALTY;
import static com.bytechef.component.openai.constant.OpenAIConstants.FREQUENCY_PENALTY_PROPERTY;
import static com.bytechef.component.openai.constant.OpenAIConstants.LOGIT_BIAS;
import static com.bytechef.component.openai.constant.OpenAIConstants.LOGIT_BIAS_PROPERTY;
import static com.bytechef.component.openai.constant.OpenAIConstants.MAX_TOKENS;
import static com.bytechef.component.openai.constant.OpenAIConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.openai.constant.OpenAIConstants.MESSAGES;
import static com.bytechef.component.openai.constant.OpenAIConstants.MODEL;
import static com.bytechef.component.openai.constant.OpenAIConstants.MODEL_PROPERTY;
import static com.bytechef.component.openai.constant.OpenAIConstants.N;
import static com.bytechef.component.openai.constant.OpenAIConstants.NAME;
import static com.bytechef.component.openai.constant.OpenAIConstants.N_PROPERTY;
import static com.bytechef.component.openai.constant.OpenAIConstants.PRESENCE_PENALTY;
import static com.bytechef.component.openai.constant.OpenAIConstants.PRESENCE_PENALTY_PROPERTY;
import static com.bytechef.component.openai.constant.OpenAIConstants.ROLE;
import static com.bytechef.component.openai.constant.OpenAIConstants.STOP;
import static com.bytechef.component.openai.constant.OpenAIConstants.STOP_PROPERTY;
import static com.bytechef.component.openai.constant.OpenAIConstants.TEMPERATURE;
import static com.bytechef.component.openai.constant.OpenAIConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.openai.constant.OpenAIConstants.TOP_P;
import static com.bytechef.component.openai.constant.OpenAIConstants.TOP_P_PROPERTY;
import static com.bytechef.component.openai.constant.OpenAIConstants.USER;
import static com.bytechef.component.openai.constant.OpenAIConstants.USER_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.openai.util.OpenAIUtils;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import java.time.Duration;
import java.util.List;

/**
 * @author Monika Domiter
 */
public class OpenAIAskChatGPTAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(ASK_CHAT_GPT)
        .title("Ask ChatGPT")
        .description("Ask ChatGPT anything you want.")
        .properties(
            array(MESSAGES)
                .label("Messages")
                .description("A list of messages comprising the conversation so far.")
                .items(
                    object().properties(
                        string(CONTENT)
                            .label("Content")
                            .description("The contents of the message.")
                            .required(true),
                        string(ROLE)
                            .label("Role")
                            .description("The role of the messages author")
                            .options(
                                option("system", "system"),
                                option("user", "user"),
                                option("assistant", "assistant"),
                                option("tool", "tool"))
                            .required(true),
                        string(NAME)
                            .label("Name")
                            .description(
                                "An optional name for the participant. Provides the model information to " +
                                    "differentiate between participants of the same role.")
                            .required(false)))
                .required(true),
            MODEL_PROPERTY
                .options((ActionOptionsFunction<String>) OpenAIUtils::getModelOptions),
            FREQUENCY_PENALTY_PROPERTY,
            LOGIT_BIAS_PROPERTY,
            MAX_TOKENS_PROPERTY,
            N_PROPERTY,
            PRESENCE_PENALTY_PROPERTY,
            STOP_PROPERTY,
            TEMPERATURE_PROPERTY,
            TOP_P_PROPERTY,
            USER_PROPERTY)
        .outputSchema(OpenAIUtils.OUTPUT_SCHEMA_RESPONSE)
        .perform(OpenAIAskChatGPTAction::perform);

    private OpenAIAskChatGPTAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        ChatMessage chatMessage = null;

        OpenAiService openAiService = new OpenAiService((String) connectionParameters.get(TOKEN), Duration.ZERO);

        ChatCompletionRequest chatCompletionRequest = createChatCompletionRequest(inputParameters);

        ChatCompletionResult chatCompletionResult = openAiService.createChatCompletion(chatCompletionRequest);

        List<ChatCompletionChoice> chatCompletionChoices = chatCompletionResult.getChoices();

        if (!chatCompletionChoices.isEmpty()) {
            ChatCompletionChoice chatCompletionChoice = chatCompletionChoices.getFirst();

            chatMessage = chatCompletionChoice.getMessage();
        }

        return chatMessage;
    }

    private static ChatCompletionRequest createChatCompletionRequest(Parameters inputParameters) {
        ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest();

        chatCompletionRequest.setMessages(inputParameters.getList(MESSAGES, new TypeReference<>() {}));
        chatCompletionRequest.setModel(inputParameters.getRequiredString(MODEL));
        chatCompletionRequest.setFrequencyPenalty(inputParameters.getDouble(FREQUENCY_PENALTY));
        chatCompletionRequest.setLogitBias(inputParameters.getMap(LOGIT_BIAS, new TypeReference<>() {}));
        chatCompletionRequest.setMaxTokens(inputParameters.getInteger(MAX_TOKENS));
        chatCompletionRequest.setN(inputParameters.getInteger(N));
        chatCompletionRequest.setPresencePenalty(inputParameters.getDouble(PRESENCE_PENALTY));
        chatCompletionRequest.setStop(inputParameters.getList(STOP, new TypeReference<>() {}));
        chatCompletionRequest.setTemperature(inputParameters.getDouble(TEMPERATURE));
        chatCompletionRequest.setTopP(inputParameters.getDouble(TOP_P));
        chatCompletionRequest.setUser(inputParameters.getString(USER));

        return chatCompletionRequest;
    }
}
