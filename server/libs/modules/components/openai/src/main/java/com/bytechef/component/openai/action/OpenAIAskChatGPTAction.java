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
import static com.bytechef.component.openai.constant.OpenAIConstants.LOGIT_BIAS;
import static com.bytechef.component.openai.constant.OpenAIConstants.MAX_TOKENS;
import static com.bytechef.component.openai.constant.OpenAIConstants.MESSAGES;
import static com.bytechef.component.openai.constant.OpenAIConstants.MODEL;
import static com.bytechef.component.openai.constant.OpenAIConstants.N;
import static com.bytechef.component.openai.constant.OpenAIConstants.NAME;
import static com.bytechef.component.openai.constant.OpenAIConstants.PRESENCE_PENALTY;
import static com.bytechef.component.openai.constant.OpenAIConstants.ROLE;
import static com.bytechef.component.openai.constant.OpenAIConstants.STOP;
import static com.bytechef.component.openai.constant.OpenAIConstants.STREAM;
import static com.bytechef.component.openai.constant.OpenAIConstants.TEMPERATURE;
import static com.bytechef.component.openai.constant.OpenAIConstants.TOP_P;
import static com.bytechef.component.openai.constant.OpenAIConstants.USER;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.openai.util.OpenAIUtils;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.service.OpenAiService;
import io.reactivex.Flowable;
import java.time.Duration;

/**
 * @author Monika Domiter
 */
public class OpenAIAskChatGPTAction extends AbstractChatCompletionAction {

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
            MODEL_PROPERTY,
            FREQUENCY_PENALTY_PROPERTY,
            LOGIT_BIAS_PROPERTY,
            MAX_TOKENS_PROPERTY,
            n,
            PRESENCE_PENALTY_PROPERTY,
            RESPONSE_FORMAT_PROPERTY,
            SEED_PROPERTY,
            STOP_PROPERTY,
            STREAM_PROPERTY,
            TEMPERATURE_PROPERTY,
            TOP_P_PROPERTY,
            TOOLS_PROPERTY,
            TOOL_CHOICE_PROPERTY,
            USER_PROPERTY)
        .output(OpenAIUtils::getOutput)
        .perform(OpenAIAskChatGPTAction::perform);

    private OpenAIAskChatGPTAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        OpenAiService openAiService = new OpenAiService((String) connectionParameters.get(TOKEN), Duration.ZERO);

        ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest();

        if (inputParameters.getRequiredBoolean(STREAM)) {
            setChatCompletionRequestValues(inputParameters, chatCompletionRequest);
            chatCompletionRequest.setStream(true);

            Flowable<ChatCompletionChunk> chatCompletionChunkFlowable =
                openAiService.streamChatCompletion(chatCompletionRequest);

            return chatCompletionChunkFlowable.toList();
        } else {
            setChatCompletionRequestValues(inputParameters, chatCompletionRequest);
            chatCompletionRequest.setStream(false);

            return openAiService.createChatCompletion(chatCompletionRequest);
        }
    }

    private static void setChatCompletionRequestValues(
        Parameters inputParameters, ChatCompletionRequest chatCompletionRequest) {

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
    }
}
