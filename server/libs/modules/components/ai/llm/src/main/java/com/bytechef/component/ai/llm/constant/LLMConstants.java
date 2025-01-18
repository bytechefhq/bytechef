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

package com.bytechef.component.ai.llm.constant;

import static com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import static com.bytechef.component.definition.ComponentDsl.ModifiableIntegerProperty;
import static com.bytechef.component.definition.ComponentDsl.ModifiableNumberProperty;
import static com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import static com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Property.ControlType.JSON_SCHEMA_BUILDER;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.ChatModel.Role;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Property.ControlType;
import java.util.List;

/**
 * @author Marko Kriskovic
 */
public class LLMConstants {

    public static final String ASK = "ask";
    public static final String CONTENT = "content";
    public static final String CREATE_IMAGE = "createImage";
    public static final String CREATE_SPEECH = "createSpeech";
    public static final String CREATE_TRANSCRIPTION = "createTranscription";
    public static final String SIZE = "size";
    public static final String ENDPOINT = "endpoint";
    public static final String FILE = "file";
    public static final String FREQUENCY_PENALTY = "frequencyPenalty";
    public static final String INPUT = "input";
    public static final String LANGUAGE = "language";
    public static final String LOGIT_BIAS = "logitBias";
    public static final String MAX_TOKENS = "maxTokens";
    public static final String MESSAGES = "messages";
    public static final String IMAGE_MESSAGES = "imageMessages";
    public static final String MODEL = "model";
    public static final String N = "n";
    public static final String PRESENCE_PENALTY = "presencePenalty";
    public static final String PROMPT = "prompt";
    public static final String RESPONSE = "response";
    public static final String RESPONSE_FORMAT = "responseFormat";
    public static final String RESPONSE_SCHEMA = "responseSchema";
    public static final String ROLE = "role";
    public static final String STOP = "stop";
    public static final String STYLE = "style";
    public static final String TEMPERATURE = "temperature";
    public static final String TOP_P = "topP";
    public static final String TOP_K = "topK";
    public static final String USER = "user";
    public static final String URL = "url";
    public static final String SEED = "seed";
    public static final String VOICE = "voice";
    public static final String SPEED = "speed";
    public static final String WEIGHT = "weight";

    public static final ModifiableNumberProperty FREQUENCY_PENALTY_PROPERTY = number(FREQUENCY_PENALTY)
        .label("Frequency Penalty")
        .description(
            "Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in " +
                "the text so far, decreasing the model's likelihood to repeat the same line verbatim.")
        .defaultValue(0)
        .minValue(-2)
        .maxValue(2)
        .advancedOption(true);

    public static final ModifiableArrayProperty IMAGE_MESSAGE_PROPERTY = array(IMAGE_MESSAGES)
        .label("Messages")
        .description("A list of messages comprising the conversation so far.")
        .items(
            object().properties(
                string(CONTENT)
                    .label("Content")
                    .description("The contents of the message.")
                    .required(true),
                number(WEIGHT)
                    .label("Weight")
                    .description("Weight of the prompt")
                    .required(false)))
        .required(true);

    public static final ModifiableStringProperty LANGUAGE_PROPERTY = string(LANGUAGE)
        .label("Language")
        .description("The language of the input audio.")
        .options(
            List.of(
                option(Language.AF.getLabel(), Language.AF.name()),
                option(Language.AR.getLabel(), Language.AR.name()),
                option(Language.HY.getLabel(), Language.HY.name()),
                option(Language.AZ.getLabel(), Language.AZ.name()),
                option(Language.BE.getLabel(), Language.BE.name()),
                option(Language.BS.getLabel(), Language.BS.name()),
                option(Language.BG.getLabel(), Language.BG.name()),
                option(Language.CA.getLabel(), Language.CA.name()),
                option(Language.ZH.getLabel(), Language.ZH.name()),
                option(Language.HR.getLabel(), Language.HR.name()),
                option(Language.CS.getLabel(), Language.CS.name()),
                option(Language.DA.getLabel(), Language.DA.name()),
                option(Language.NL.getLabel(), Language.NL.name()),
                option(Language.EL.getLabel(), Language.EL.name()),
                option(Language.ET.getLabel(), Language.ET.name()),
                option(Language.EN.getLabel(), Language.EN.name()),
                option(Language.FI.getLabel(), Language.FI.name()),
                option(Language.FR.getLabel(), Language.FR.name()),
                option(Language.GL.getLabel(), Language.GL.name()),
                option(Language.DE.getLabel(), Language.DE.name()),
                option(Language.HE.getLabel(), Language.HE.name()),
                option(Language.HI.getLabel(), Language.HI.name()),
                option(Language.HU.getLabel(), Language.HU.name()),
                option(Language.IS.getLabel(), Language.IS.name()),
                option(Language.ID.getLabel(), Language.ID.name()),
                option(Language.IT.getLabel(), Language.IT.name()),
                option(Language.JA.getLabel(), Language.JA.name()),
                option(Language.KK.getLabel(), Language.KK.name()),
                option(Language.KN.getLabel(), Language.KN.name()),
                option(Language.KO.getLabel(), Language.KO.name()),
                option(Language.LT.getLabel(), Language.LT.name()),
                option(Language.LV.getLabel(), Language.LV.name()),
                option(Language.MA.getLabel(), Language.MA.name()),
                option(Language.MK.getLabel(), Language.MK.name()),
                option(Language.MR.getLabel(), Language.MR.name()),
                option(Language.MS.getLabel(), Language.MS.name()),
                option(Language.NE.getLabel(), Language.NE.name()),
                option(Language.NO.getLabel(), Language.NO.name()),
                option(Language.FA.getLabel(), Language.FA.name()),
                option(Language.PL.getLabel(), Language.PL.name()),
                option(Language.PT.getLabel(), Language.PT.name()),
                option(Language.RO.getLabel(), Language.RO.name()),
                option(Language.RU.getLabel(), Language.RU.name()),
                option(Language.SK.getLabel(), Language.SK.name()),
                option(Language.SL.getLabel(), Language.SL.name()),
                option(Language.SR.getLabel(), Language.SR.name()),
                option(Language.ES.getLabel(), Language.ES.name()),
                option(Language.SV.getLabel(), Language.SV.name()),
                option(Language.SW.getLabel(), Language.SW.name()),
                option(Language.TA.getLabel(), Language.TA.name()),
                option(Language.TL.getLabel(), Language.TL.name()),
                option(Language.TH.getLabel(), Language.TH.name()),
                option(Language.TR.getLabel(), Language.TR.name()),
                option(Language.UK.getLabel(), Language.UK.name()),
                option(Language.UR.getLabel(), Language.UR.name()),
                option(Language.VI.getLabel(), Language.VI.name()),
                option(Language.CY.getLabel(), Language.CY.name())))
        .required(false);

    public static final ModifiableObjectProperty LOGIT_BIAS_PROPERTY = object(LOGIT_BIAS)
        .label("Logit Bias")
        .description("Modify the likelihood of specified tokens appearing in the completion.")
        .additionalProperties(number())
        .advancedOption(true);

    public static final ModifiableIntegerProperty MAX_TOKENS_PROPERTY = integer(MAX_TOKENS)
        .label("Max Tokens")
        .description("The maximum number of tokens to generate in the chat completion.")
        .advancedOption(true);

    public static final ModifiableArrayProperty MESSAGES_PROPERTY = array(MESSAGES)
        .label("Messages")
        .description("A list of messages comprising the conversation so far.")
        .placeholder("Add message")
        .items(
            object()
                .label("Message")
                .properties(
                    string(ROLE)
                        .label("Role")
                        .description("The role of the messages author.")
                        .options(
                            option("System", Role.SYSTEM.name()),
                            option("User", Role.USER.name()),
                            option("Assistant", Role.ASSISTANT.name()),
                            option("Tool", Role.TOOL.name()))
                        .required(true),
                    string(CONTENT)
                        .label("Content")
                        .description("The contents of the message.")
                        .controlType(ControlType.TEXT_AREA)
                        .required(true),
                    array("attachments")
                        .label("Attachments")
                        .description(
                            "Only text and image files are supported. Also, only certain models supports images. Please check the documentation.")
                        .displayCondition("%s == '%s'".formatted("messages[index].role", Role.USER.name()))
                        .items(fileEntry())
                        .required(false)))
        .required(true);

    public static final ModifiableIntegerProperty N_PROPERTY = integer(N)
        .label("Number of Chat Completion Choices")
        .description("How many chat completion choices to generate for each input message.")
        .defaultValue(1)
        .advancedOption(true);

    public static final ModifiableIntegerProperty TOP_K_PROPERTY = integer(TOP_K)
        .label("Top K")
        .description("Specify the number of token choices the generative uses to generate the next token.")
        .defaultValue(1)
        .advancedOption(true);

    public static final ComponentDsl.ModifiableNumberProperty PRESENCE_PENALTY_PROPERTY = number(PRESENCE_PENALTY)
        .label("Presence Penalty")
        .description(
            "Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they appear in the " +
                "text so far, increasing the model's likelihood to talk about new topics.")
        .defaultValue(0)
        .minValue(-2)
        .maxValue(2)
        .advancedOption(true);

    public static final ModifiableObjectProperty RESPONSE_PROPERTY = object(RESPONSE)
        .label("Response")
        .description("The response from the API.")
        .properties(
            string(RESPONSE_FORMAT)
                .label("Response Format")
                .description("In which format do you want the response to be in?")
                .options(
                    option("Text", ChatModel.ResponseFormat.TEXT.name(), "Text response"),
                    option("JSON", ChatModel.ResponseFormat.JSON.name(), "JSON response with key-value pairs"))
                .defaultValue(ChatModel.ResponseFormat.TEXT.name())
                .required(false),
            string(RESPONSE_SCHEMA)
                .label("Response Schema")
                .description("Define the JSON schema for the response.")
                .controlType(JSON_SCHEMA_BUILDER)
                .displayCondition("response.responseFormat == '%s'".formatted(ChatModel.ResponseFormat.JSON.name()))
                .required(false))
        .required(false);

    public static final ModifiableIntegerProperty SEED_PROPERTY = integer(SEED)
        .label("Seed")
        .description("Keeping the same seed would output the same response.")
        .advancedOption(true);

    public static final ModifiableArrayProperty STOP_PROPERTY = array(STOP)
        .label("Stop")
        .description("Up to 4 sequences where the API will stop generating further tokens.")
        .items(string())
        .advancedOption(true);

    public static final ModifiableNumberProperty TEMPERATURE_PROPERTY = number(TEMPERATURE)
        .label("Temperature")
        .description(
            "Controls randomness:  Higher values will make the output more random, while lower values like will make " +
                "it more focused and deterministic.")
        .defaultValue(1)
        .minValue(0)
        .maxValue(2)
        .advancedOption(true);

    public static final ModifiableNumberProperty TOP_P_PROPERTY = number(TOP_P)
        .label("Top P")
        .description(
            "An alternative to sampling with temperature, called nucleus sampling,  where the model considers the " +
                "results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top " +
                "10% probability mass are considered.")
        .defaultValue(1)
        .advancedOption(true);

    public static final ModifiableStringProperty USER_PROPERTY = string(USER)
        .label("User")
        .description(
            "A unique identifier representing your end-user, which can help admins to monitor and detect abuse.")
        .required(false)
        .advancedOption(true);

    private LLMConstants() {
    }
}
