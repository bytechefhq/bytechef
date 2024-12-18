package com.bytechef.component.ai.text.analysis.action;

import com.bytechef.component.ai.text.analysis.action.definition.AiTextAnalysisActionDefinition;
import com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants;
import com.bytechef.component.amazon.bedrock.constant.AmazonBedrockConstants;
import com.bytechef.component.anthropic.constant.AnthropicConstants;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.mistral.constant.MistralConstants;
import com.bytechef.component.openai.constant.OpenAIConstants;
import com.bytechef.component.vertex.gemini.constant.VertexGeminiConstants;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.ParametersFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.CATEGORIES;
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.CATEGORY;
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.EXAMPLES;
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.FORMAT;
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.MODEL_PROVIDER;
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.SAMPLE;
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.TEXT;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.llm.constant.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;

public class ClassifyTextAction implements AITextAnalysisAction{
    public final AiTextAnalysisActionDefinition actionDefinition;

    public ClassifyTextAction(ApplicationProperties.Ai.Component component) {
        this.actionDefinition = getActionDefinition(component);
    }

    private AiTextAnalysisActionDefinition getActionDefinition(ApplicationProperties.Ai.Component component) {
        return new AiTextAnalysisActionDefinition(
            action(AiTextAnalysisConstants.CLASSIFY_TEXT)
                .title("Classify Text")
                .description("AI reads, analyzes and classifies your text into one of defined categories.")
                .properties(
                    integer(MODEL_PROVIDER)
                        .label("Model provider")
                        .options(
                            option("Amazon Bedrock: Anthropic 2", 0),
                            option("Amazon Bedrock: Anthropic 3", 1),
                            option("Amazon Bedrock: Cohere", 2),
                            option("Amazon Bedrock: Jurassic 2", 3),
                            option("Amazon Bedrock: Llama", 4),
                            option("Amazon Bedrock: Titan", 5),
                            option("Anthropic", 6),
                            option("Azure Open AI", 7),
                            option("Groq", 8),
                            option("NVIDIA", 9),
                            option("Hugging Face", 10),
                            option("Mistral", 11),
                            option("Open AI", 12),
                            option("Vertex Gemini", 13))
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(AmazonBedrockConstants.ANTHROPIC2_MODELS)
                        .displayCondition("modelProvider == 0")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(AmazonBedrockConstants.ANTHROPIC3_MODELS)
                        .displayCondition("modelProvider == 1")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(AmazonBedrockConstants.COHERE_MODELS)
                        .displayCondition("modelProvider == 2")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(AmazonBedrockConstants.JURASSIC2_MODELS)
                        .displayCondition("modelProvider == 3")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(AmazonBedrockConstants.LLAMA_MODELS)
                        .displayCondition("modelProvider == 4")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(AmazonBedrockConstants.TITAN_MODELS)
                        .displayCondition("modelProvider == 5")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(AnthropicConstants.MODELS)
                        .displayCondition("modelProvider == 6")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .displayCondition("modelProvider >= 7 && modelProvider <= 9")
                        .required(true),
                    string(MODEL)
                        .label("URL")
                        .description("Url of the inference endpoint.")
                        .displayCondition("modelProvider == 10")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(MistralConstants.MODELS)
                        .displayCondition("modelProvider == 11")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(OpenAIConstants.MODELS)
                        .displayCondition("modelProvider == 12")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(VertexGeminiConstants.MODELS)
                        .displayCondition("modelProvider == 13")
                        .required(true),
                    string(TEXT)
                        .label("Text")
                        .description("The text that is to be classified.")
                        .required(true),
                    array(CATEGORIES)
                        .label("Categories")
                        .description("A list of categories that the model can choose from.")
                        .items(string())
                        .required(true),
                    array(EXAMPLES)
                        .label("Examples")
                        .description("You can classify a few samples, to guide your model on how to classify the real data.")
                        .items(
                            object().properties(
                                string(SAMPLE)
                                    .label("Sample")
                                    .description("Sample data that you want to classify manually."),
                                string(CATEGORY)
                                    .label("Category")
                                    .description("Which of the categories above does the sample belong to?"))),
                    MAX_TOKENS_PROPERTY,
                    TEMPERATURE_PROPERTY)
                .output(),
            component, this);
    }

    public Parameters createParameters(Parameters inputParameters) {
        Map<String, Object> modelInputParametersMap = new HashMap<>();

        String prompt = "You will receive a list of categories and a text. You will answer which of the given categories fits the given text the most.";

        modelInputParametersMap.put("messages",
            List.of(
                Map.of("content", prompt, "role", "system"),
                Map.of("content", inputParameters.getString(TEXT), "role", "user")));
        modelInputParametersMap.put("model", inputParameters.getString(MODEL));

        return ParametersFactory.createParameters(modelInputParametersMap);
    }
}
