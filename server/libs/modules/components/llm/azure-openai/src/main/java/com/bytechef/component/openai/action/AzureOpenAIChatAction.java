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
import static constants.LLMConstants.CHAT;
import static constants.LLMConstants.CONTENT;
import static constants.LLMConstants.ENDPOINT;
import static constants.LLMConstants.FREQUENCY_PENALTY;
import static constants.LLMConstants.FREQUENCY_PENALTY_PROPERTY;
import static constants.LLMConstants.LOGIT_BIAS;
import static constants.LLMConstants.LOGIT_BIAS_PROPERTY;
import static constants.LLMConstants.MAX_TOKENS;
import static constants.LLMConstants.MAX_TOKENS_PROPERTY;
import static constants.LLMConstants.MESSAGES;
import static constants.LLMConstants.MODEL;
import static constants.LLMConstants.N;
import static constants.LLMConstants.N_PROPERTY;
import static constants.LLMConstants.PRESENCE_PENALTY;
import static constants.LLMConstants.PRESENCE_PENALTY_PROPERTY;
import static constants.LLMConstants.ROLE;
import static constants.LLMConstants.STOP;
import static constants.LLMConstants.STOP_PROPERTY;
import static constants.LLMConstants.TEMPERATURE;
import static constants.LLMConstants.TEMPERATURE_PROPERTY;
import static constants.LLMConstants.TOP_P;
import static constants.LLMConstants.TOP_P_PROPERTY;
import static constants.LLMConstants.USER;
import static constants.LLMConstants.USER_PROPERTY;
import static util.LLMUtils.createMessage;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.KeyCredential;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import java.util.List;

import org.springframework.ai.azure.openai.AzureOpenAiChatModel;
import org.springframework.ai.azure.openai.AzureOpenAiChatOptions;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import util.records.MessageRecord;

/**
 * @author Monika Domiter
 */
public class AzureOpenAIChatAction {

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
                .description("Deployment name, written in string.")
                .exampleValue("gpt-4o")
                .required(true),
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
        .perform(AzureOpenAIChatAction::perform);

    private AzureOpenAIChatAction() {
    }

    public static String perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        OpenAIClient openAIClient = new OpenAIClientBuilder()
            .credential(new KeyCredential(connectionParameters.getString(TOKEN)))
            .endpoint(connectionParameters.getString(ENDPOINT))
            .buildClient();

        ChatOptions chatOptions = AzureOpenAiChatOptions.builder()
            .withDeploymentName(inputParameters.getRequiredString(MODEL))
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
            new AzureOpenAiChatModel(openAIClient, (AzureOpenAiChatOptions) chatOptions);


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
