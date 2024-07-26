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
import static com.bytechef.component.openai.constant.OpenAIConstants.CHAT;
import static com.bytechef.component.openai.constant.OpenAIConstants.CONTENT;
import static com.bytechef.component.openai.constant.OpenAIConstants.FREQUENCY_PENALTY;
import static com.bytechef.component.openai.constant.OpenAIConstants.FREQUENCY_PENALTY_PROPERTY;
import static com.bytechef.component.openai.constant.OpenAIConstants.LOGIT_BIAS;
import static com.bytechef.component.openai.constant.OpenAIConstants.LOGIT_BIAS_PROPERTY;
import static com.bytechef.component.openai.constant.OpenAIConstants.MAX_TOKENS;
import static com.bytechef.component.openai.constant.OpenAIConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.openai.constant.OpenAIConstants.MESSAGES;
import static com.bytechef.component.openai.constant.OpenAIConstants.MODEL;
import static com.bytechef.component.openai.constant.OpenAIConstants.N;
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
import static util.LLMUtils.createMessage;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import util.LLMUtils;
import util.records.MessageRecord;

/**
 * @author Monika Domiter
 */
public class OpenAIChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CHAT)
        .title("Ask")
        .description("Ask anything you want.")
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
                            .required(true)))
                .required(true),
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .required(true)
                .options(LLMUtils.getEnumOptions(
                    Arrays.stream(OpenAiApi.ChatModel.values())
                        .collect(Collectors.toMap(
                            OpenAiApi.ChatModel::getValue, OpenAiApi.ChatModel::getValue)))),
            FREQUENCY_PENALTY_PROPERTY,
            LOGIT_BIAS_PROPERTY,
            MAX_TOKENS_PROPERTY,
            N_PROPERTY,
            PRESENCE_PENALTY_PROPERTY,
            STOP_PROPERTY,
            TEMPERATURE_PROPERTY,
            TOP_P_PROPERTY,
            USER_PROPERTY)
        .outputSchema(string())
        .perform(OpenAIChatAction::perform);

    private OpenAIChatAction() {
    }

    public static String perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        ChatOptions chatOptions = OpenAiChatOptions.builder()
            .withModel(inputParameters.getRequiredString(MODEL))
            .withFrequencyPenalty(inputParameters.getFloat(FREQUENCY_PENALTY))
            .withLogitBias(inputParameters.getMap(LOGIT_BIAS, new TypeReference<>() {}))
            .withMaxTokens(inputParameters.getInteger(MAX_TOKENS))
            .withN(inputParameters.getInteger(N))
            .withPresencePenalty(inputParameters.getFloat(PRESENCE_PENALTY))
            .withStop(inputParameters.getList(STOP, new TypeReference<>() {}))
            .withTemperature(inputParameters.getFloat(TEMPERATURE))
            .withTopP(inputParameters.getFloat(TOP_P))
            .withUser(inputParameters.getString(USER))
            .build();
        ChatModel chatModel =
            new OpenAiChatModel(new OpenAiApi(connectionParameters.getString(TOKEN)), (OpenAiChatOptions) chatOptions);

        List<MessageRecord> messageRecordList = inputParameters.getList(MESSAGES, new TypeReference<>() {});
        List<Message> messages = messageRecordList.stream()
            .map(messageRecord -> createMessage(messageRecord.getRole(), messageRecord.getContent()))
            .toList();

        ChatResponse response = chatModel.call(new Prompt(messages));
        return response.getResult()
            .getOutput()
            .getContent();
    }
}
