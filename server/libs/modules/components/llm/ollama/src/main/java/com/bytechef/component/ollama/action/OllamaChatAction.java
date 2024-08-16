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

package com.bytechef.component.ollama.action;

import static com.bytechef.component.ollama.constant.OllamaConstants.FORMAT;
import static com.bytechef.component.ollama.constant.OllamaConstants.URL;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;

import static constants.LLMConstants.ASK;
import static constants.LLMConstants.FREQUENCY_PENALTY;
import static constants.LLMConstants.FREQUENCY_PENALTY_PROPERTY;
import static constants.LLMConstants.MESSAGE_PROPERTY;
import static constants.LLMConstants.MODEL;
import static constants.LLMConstants.N;
import static constants.LLMConstants.N_PROPERTY;
import static constants.LLMConstants.PRESENCE_PENALTY;
import static constants.LLMConstants.PRESENCE_PENALTY_PROPERTY;
import static constants.LLMConstants.SEED;
import static constants.LLMConstants.SEED_PROPERTY;
import static constants.LLMConstants.STOP;
import static constants.LLMConstants.STOP_PROPERTY;
import static constants.LLMConstants.TEMPERATURE;
import static constants.LLMConstants.TEMPERATURE_PROPERTY;
import static constants.LLMConstants.TOP_P;
import static constants.LLMConstants.TOP_P_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import util.LLMUtils;
import util.interfaces.Chat;

public class OllamaChatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(ASK)
        .title("Ask")
        .description("Ask anything you want.")
        .properties(
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .required(true)
                .options(LLMUtils.getEnumOptions(
                    Arrays.stream(OllamaModel.values())
                        .collect(Collectors.toMap(
                            OllamaModel::getName, OllamaModel::getName, (f,s)->f)))),
            string(FORMAT)
                .label("Format")
                .description("The format to return a response in.")
                .options(option("JSON", "json"))
                .defaultValue("json"),
            MESSAGE_PROPERTY,
            N_PROPERTY,
            TEMPERATURE_PROPERTY,
            STOP_PROPERTY,
            TOP_P_PROPERTY,
            FREQUENCY_PENALTY_PROPERTY,
            PRESENCE_PENALTY_PROPERTY,
            SEED_PROPERTY)
        .outputSchema(string())
        .perform(OllamaChatAction::perform);

    private OllamaChatAction() {
    }

    public static String perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return Chat.getResponse(CHAT, inputParameters, connectionParameters);
    }

    public static final Chat CHAT = new Chat() {
        @Override
        public ChatOptions createChatOptions(Parameters inputParameters) {
            return OllamaOptions.builder()
                .withModel(inputParameters.getRequiredString(MODEL))
                .withTemperature(inputParameters.getFloat(TEMPERATURE))
                .withTopP(inputParameters.getFloat(TOP_P))
                .withStop(inputParameters.getList(STOP, new TypeReference<>() {}))
                .withTopK(inputParameters.getInteger(N))
                .withFrequencyPenalty(inputParameters.getFloat(FREQUENCY_PENALTY))
                .withPresencePenalty(inputParameters.getFloat(PRESENCE_PENALTY))
                .withSeed(inputParameters.getInteger(SEED))
                .withFormat(inputParameters.getString(FORMAT))
//                .withF16KV()
//                .withKeepAlive()
//                .withLogitsAll()
//                .withUseMMap()
//                .withLowVRAM()
//                .withMainGPU()
//                .withMirostat()
//                .withMirostatEta()
//                .withMirostatTau()
//                .withNumBatch()
//                .withNumCtx()
//                .withNumGPU()
//                .withNumKeep()
//                .withNumThread()
//                .withNumPredict()
//                .withPenalizeNewline()
//                .withRepeatLastN()
//                .withRepeatPenalty()
//                .withTfsZ()
//                .withTruncate()
//                .withTypicalP()
//                .withUseMLock()
//                .withUseNUMA()
//                .withVocabOnly()
                .build();
        }

        @Override
        public ChatModel createChatModel(Parameters inputParameters, Parameters connectionParameters) {
            String url = connectionParameters.getString(URL);
            OllamaApi ollamaApi = url==null?new OllamaApi():new OllamaApi(url);
            return new OllamaChatModel(ollamaApi, (OllamaOptions) createChatOptions(inputParameters));
        }
    };
}
