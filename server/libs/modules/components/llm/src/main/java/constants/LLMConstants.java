package constants;

import com.bytechef.component.definition.ComponentDSL;

import java.util.List;

import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;

public class LLMConstants {
    public static final String ASK = "ask";
    public static final String CONTENT = "content";
    public static final String CREATE_IMAGE = "createImage";
    public static final String CREATE_SPEECH = "createSpeech";
    public static final String CREATE_TRANSCRIPTION = "createTranscription";
    public static final Integer DEFAULT_SIZE = 1024;
    public static final String ENDPOINT = "endpoint";
    public static final String FILE = "file";
    public static final String FREQUENCY_PENALTY = "frequencyPenalty";
    public static final String INPUT = "input";
    public static final String LANGUAGE = "language";
    public static final String LOGIT_BIAS = "logitBias";
    public static final String MAX_TOKENS = "maxTokens";
    public static final String MESSAGES = "messages";
    public static final String MODEL = "model";
    public static final String N = "n";
    public static final String PRESENCE_PENALTY = "presencePenalty";
    public static final String PROMPT = "prompt";
    public static final String QUALITY = "quality";
    public static final String RESPONSE_FORMAT = "responseFormat";
    public static final String ROLE = "role";
    public static final String HEIGHT = "height";
    public static final String STOP = "stop";
    public static final String STYLE = "style";
    public static final String TEMPERATURE = "temperature";
    public static final String TOP_P = "topP";
    public static final String USER = "user";
    public static final String URL = "url";
    public static final String SEED = "seed";
    public static final String VOICE = "voice";
    public static final String SPEED = "speed";
    public static final String WIDTH = "width";
    public static final String WEIGHT = "weight";

    public static final ComponentDSL.ModifiableNumberProperty FREQUENCY_PENALTY_PROPERTY = number(FREQUENCY_PENALTY)
        .label("Frequency penalty")
        .description(
            "Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in " +
                "the text so far, decreasing the model's likelihood to repeat the same line verbatim.")
        .defaultValue(0)
        .minValue(-2)
        .maxValue(2)
        .required(false);

    public static final ComponentDSL.ModifiableObjectProperty LOGIT_BIAS_PROPERTY = object(LOGIT_BIAS)
        .label("Logit bias")
        .description("Modify the likelihood of specified tokens appearing in the completion.")
        .additionalProperties(number())
        .required(false);

    public static final ComponentDSL.ModifiableIntegerProperty MAX_TOKENS_PROPERTY = integer(MAX_TOKENS)
        .label("Max tokens")
        .description("The maximum number of tokens to generate in the chat completion.")
        .required(false);

    public static final ComponentDSL.ModifiableIntegerProperty N_PROPERTY = integer(N)
        .label("Number of chat completion choices")
        .description("How many chat completion choices to generate for each input message.")
        .defaultValue(1)
        .required(false);

    public static final ComponentDSL.ModifiableNumberProperty PRESENCE_PENALTY_PROPERTY = number(PRESENCE_PENALTY)
        .label("Presence penalty")
        .description(
            "Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they appear in the " +
                "text so far, increasing the model's likelihood to talk about new topics.")
        .defaultValue(0)
        .minValue(-2)
        .maxValue(2)
        .required(false);

    public static final ComponentDSL.ModifiableArrayProperty STOP_PROPERTY = array(STOP)
        .label("Stop")
        .description("Up to 4 sequences where the API will stop generating further tokens.")
        .items(string())
        .required(false);

    public static final ComponentDSL.ModifiableNumberProperty TEMPERATURE_PROPERTY = number(TEMPERATURE)
        .label("Temperature")
        .description(
            "Controls randomness:  Higher values will make the output more random, while lower values like will make " +
                "it more focused and deterministic.")
        .defaultValue(1)
        .minValue(0)
        .maxValue(2)
        .required(false);

    public static final ComponentDSL.ModifiableNumberProperty TOP_P_PROPERTY = number(TOP_P)
        .label("Top p")
        .description(
            "An alternative to sampling with temperature, called nucleus sampling,  where the model considers the " +
                "results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top " +
                "10% probability mass are considered.")
        .defaultValue(1)
        .required(false);

    public static final ComponentDSL.ModifiableStringProperty USER_PROPERTY = string(USER)
        .label("User")
        .description(
            "A unique identifier representing your end-user, which can help OpenAI to monitor and detect abuse.")
        .required(false);

    public static final ComponentDSL.ModifiableIntegerProperty SEED_PROPERTY = integer(SEED)
        .label("Seed")
        .description("Keeping the same seed would output the same response.")
        .required(false);

    public static final ComponentDSL.ModifiableArrayProperty MESSAGE_PROPERTY = array(MESSAGES)
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
        .required(true);

    public static final ComponentDSL.ModifiableStringProperty LANGUAGE_PROPERTY = string(LANGUAGE)
        .label("Language")
        .description("The language of the input audio.")
        .options(
            List.of(
                option("Afrikaans", "af"),
                option("Arabic", "ar"),
                option("Armenian", "hy"),
                option("Azerbaijani", "az"),
                option("Belarusian", "be"),
                option("Bosnian", "bs"),
                option("Bulgarian", "bg"),
                option("Catalan", "ca"),
                option("Chinese (Simplified)", "zh"),
                option("Croatian", "hr"),
                option("Czech", "cs"),
                option("Danish", "da"),
                option("Dutch", "nl"),
                option("Greek", "el"),
                option("Estonian", "et"),
                option("English", "en"),
                option("Finnish", "fi"),
                option("French", "fr"),
                option("Galician", "gl"),
                option("German", "de"),
                option("Hebrew", "he"),
                option("Hindi", "hi"),
                option("Hungarian", "hu"),
                option("Icelandic", "is"),
                option("Indonesian", "id"),
                option("Italian", "it"),
                option("Japanese", "ja"),
                option("Kazakh", "kk"),
                option("Kannada", "kn"),
                option("Korean", "ko"),
                option("Lithuanian", "lt"),
                option("Latvian", "lv"),
                option("Maori", "ma"),
                option("Macedonian", "mk"),
                option("Marathi", "mr"),
                option("Malay", "ms"),
                option("Nepali", "ne"),
                option("Norwegian", "no"),
                option("Persian", "fa"),
                option("Polish", "pl"),
                option("Portuguese", "pt"),
                option("Romanian", "ro"),
                option("Russian", "ru"),
                option("Slovak", "sk"),
                option("Slovenian", "sl"),
                option("Serbian", "sr"),
                option("Spanish", "es"),
                option("Swedish", "sv"),
                option("Swahili", "sw"),
                option("Tamil", "ta"),
                option("Tagalog", "tl"),
                option("Thai", "th"),
                option("Turkish", "tr"),
                option("Ukrainian", "uk"),
                option("Urdu", "ur"),
                option("Vietnamese", "vi"),
                option("Welsh", "cy")))
        .required(false);

    private LLMConstants() {};
}
