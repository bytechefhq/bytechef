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

package com.bytechef.component.openai.constant;

import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ComponentDSL.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableNumberProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableStringProperty;

/**
 * @author Monika Domiter
 */
public final class OpenAIConstants {

    public static final String ASK_CHAT_GPT = "askChatGPT";
    public static final String CONTENT = "content";
    public static final String CREATE_ASSISTANT = "createAssistant";
    public static final String CREATE_IMAGE = "createImage";
    public static final String CREATE_SPEECH = "createSpeech";
    public static final String CREATE_TRANSCRIPTION = "createTranscription";
    public static final String CREATE_TRANSLATION = "createTranslation";
    public static final String DALL_E_3 = "dall-e-3";
    public static final String DALL_E_2 = "dall-e-2";
    public static final String DEFAULT_SIZE = "1024x1024";
    public static final String DESCRIPTION = "description";
    public static final String DETAIL = "detail";
    public static final String FILE = "file";
    public static final String FILE_IDS = "fileIds";
    public static final String FREQUENCY_PENALTY = "frequencyPenalty";
    public static final String FUNCTION = "function";
    public static final String IMAGE_URL = "imageUrl";
    public static final String INSTRUCTIONS = "instructions";
    public static final String INPUT = "input";
    public static final String LANGUAGE = "language";
    public static final String LOGIT_BIAS = "logitBias";
    public static final String MAX_TOKENS = "maxTokens";
    public static final String MESSAGES = "messages";
    public static final String METADATA = "metadata";
    public static final String MODEL = "model";
    public static final String N = "n";
    public static final String NAME = "name";
    public static final String OPENAI = "openai";
    public static final String PARAMETERS = "parameters";
    public static final String PRESENCE_PENALTY = "presencePenalty";
    public static final String PROMPT = "prompt";
    public static final String QUALITY = "quality";
    public static final String RESPONSE_FORMAT = "responseFormat";
    public static final String ROLE = "role";
    public static final String SIZE = "size";
    public static final String SPEED = "speed";
    public static final String STOP = "stop";
    public static final String STYLE = "style";
    public static final String TEMPERATURE = "temperature";
    public static final String TOOLS = "tools";
    public static final String TOP_P = "topP";
    public static final String TYPE = "type";
    public static final String URL = "url";
    public static final String USER = "user";
    public static final String VOICE = "voice";
    public static final String WHISPER_1 = "whisper-1";

    public static final String VISION_PROMPT = "visionPrompt";

    public static final ModifiableNumberProperty FREQUENCY_PENALTY_PROPERTY = number(FREQUENCY_PENALTY)
        .label("Frequency penalty")
        .description(
            "Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in " +
                "the text so far, decreasing the model's likelihood to repeat the same line verbatim.")
        .defaultValue(0)
        .minValue(-2)
        .maxValue(2)
        .required(false);

    public static final ModifiableObjectProperty LOGIT_BIAS_PROPERTY = object(LOGIT_BIAS)
        .label("Logit bias")
        .description("Modify the likelihood of specified tokens appearing in the completion.")
        .additionalProperties(number())
        .required(false);

    public static final ModifiableIntegerProperty MAX_TOKENS_PROPERTY = integer(MAX_TOKENS)
        .label("Max tokens")
        .description("The maximum number of tokens to generate in the chat completion.")
        .required(false);

    public static final ModifiableIntegerProperty N_PROPERTY = integer(N)
        .label("Number of chat completion choices")
        .description("How many chat completion choices to generate for each input message.")
        .defaultValue(1)
        .required(false);

    public static final ModifiableNumberProperty PRESENCE_PENALTY_PROPERTY = number(PRESENCE_PENALTY)
        .label("Presence penalty")
        .description(
            "Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they appear in the " +
                "text so far, increasing the model's likelihood to talk about new topics.")
        .defaultValue(0)
        .minValue(-2)
        .maxValue(2)
        .required(false);

    public static final ModifiableArrayProperty STOP_PROPERTY = array(STOP)
        .label("Stop")
        .description("Up to 4 sequences where the API will stop generating further tokens.")
        .items(string())
        .required(false);

    public static final ModifiableNumberProperty TEMPERATURE_PROPERTY = number(TEMPERATURE)
        .label("Temperature")
        .description(
            "Controls randomness:  Higher values will make the output more random, while lower values like will make " +
                "it more focused and deterministic.")
        .defaultValue(1)
        .minValue(0)
        .maxValue(2)
        .required(false);

    public static final ModifiableNumberProperty TOP_P_PROPERTY = number(TOP_P)
        .label("Top p")
        .description(
            "An alternative to sampling with temperature, called nucleus sampling,  where the model considers the " +
                "results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top " +
                "10% probability mass are considered.")
        .defaultValue(1)
        .required(false);

    public static final ModifiableStringProperty USER_PROPERTY = string(USER)
        .label("User")
        .description(
            "A unique identifier representing your end-user, which can help OpenAI to monitor and detect abuse.")
        .required(false);

    private OpenAIConstants() {
    }
}
