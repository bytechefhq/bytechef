package com.bytechef.component.ai.text.analysis;

import com.bytechef.component.amazon.bedrock.action.AmazonBedrockAnthropic2ChatAction;
import com.bytechef.component.amazon.bedrock.action.AmazonBedrockAnthropic3ChatAction;
import com.bytechef.component.amazon.bedrock.action.AmazonBedrockCohereChatAction;
import com.bytechef.component.amazon.bedrock.action.AmazonBedrockJurassic2ChatAction;
import com.bytechef.component.amazon.bedrock.action.AmazonBedrockLlamaChatAction;
import com.bytechef.component.amazon.bedrock.action.AmazonBedrockTitanChatAction;
import com.bytechef.component.anthropic.action.AnthropicChatAction;
import com.bytechef.component.azure.openai.action.AzureOpenAIChatAction;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.groq.action.GroqChatAction;
import com.bytechef.component.hugging.face.action.HuggingFaceChatAction;
import com.bytechef.component.llm.Chat;
import com.bytechef.component.mistral.action.MistralChatAction;
import com.bytechef.component.nvidia.action.NVIDIAChatAction;
import com.bytechef.component.openai.action.OpenAIChatAction;
import com.bytechef.component.vertex.gemini.action.VertexGeminiChatAction;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ParameterConnection;
import com.bytechef.platform.component.definition.ParametersFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.bytechef.component.definition.Authorization.TOKEN;

/**
 * @author Marko Kriskovic
 */
public class AiTextAnalysisActionDefinition extends AbstractActionDefinitionWrapper {

    private final ApplicationProperties.Ai.Component component;
    public AiTextAnalysisActionDefinition(ActionDefinition actionDefinition, ApplicationProperties.Ai.Component component) {
        super(actionDefinition);

        this.component = component;
    }

    @Override
    public Optional<PerformFunction> getPerform() {
        return Optional.of((MultipleConnectionsPerformFunction) this::perform);
    }

    protected String perform(
        Parameters inputParameters, Map<String,? extends ParameterConnection> connectionParameters, Parameters extensions, ActionContext context) {
        Map<String, String> maliciousMap = new HashMap<>();

        Integer connectionProvider = inputParameters.getInteger("connectionProvider");
        Chat chat = switch (connectionProvider) {
            case 0 -> {
                maliciousMap.put(TOKEN, component.getAmazonBedrock().getApiKey());
                yield AmazonBedrockAnthropic2ChatAction.CHAT;
            }
            case 1 -> {
                maliciousMap.put(TOKEN, component.getAmazonBedrock().getApiKey());
                yield AmazonBedrockAnthropic3ChatAction.CHAT;
            }
            case 2 -> {
                maliciousMap.put(TOKEN, component.getAmazonBedrock().getApiKey());
                yield AmazonBedrockCohereChatAction.CHAT;
            }
            case 3 -> {
                maliciousMap.put(TOKEN, component.getAmazonBedrock().getApiKey());
                yield AmazonBedrockJurassic2ChatAction.CHAT;
            }
            case 4 -> {
                maliciousMap.put(TOKEN, component.getAmazonBedrock().getApiKey());
                yield AmazonBedrockLlamaChatAction.CHAT;
            }
            case 5 -> {
                maliciousMap.put(TOKEN, component.getAmazonBedrock().getApiKey());
                yield AmazonBedrockTitanChatAction.CHAT;
            }
            case 6 -> {
                maliciousMap.put(TOKEN, component.getAnthropic().getApiKey());
                yield AnthropicChatAction.CHAT;
            }
            case 7 -> {
                maliciousMap.put(TOKEN, component.getAzureOpenAi().getApiKey());
                yield AzureOpenAIChatAction.CHAT;
            }
            case 8 -> {
                maliciousMap.put(TOKEN, component.getGroq().getApiKey());
                yield GroqChatAction.CHAT;
            }
            case 9 -> {
                maliciousMap.put(TOKEN, component.getNVIDIA().getApiKey());
                yield NVIDIAChatAction.CHAT;
            }
            case 10 -> {
                maliciousMap.put(TOKEN, component.getHuggingFace().getApiKey());
                yield HuggingFaceChatAction.CHAT;
            }
            case 11 -> {
                maliciousMap.put(TOKEN, component.getMistral().getApiKey());
                yield MistralChatAction.CHAT;
            }
            case 12 -> {
                maliciousMap.put(TOKEN, component.getOpenAi().getApiKey());
                yield OpenAIChatAction.CHAT;
            }
            case 13 -> {
                maliciousMap.put(TOKEN, component.getVertexGemini().getApiKey());
                yield VertexGeminiChatAction.CHAT;
            }
            default -> null;
        };

        Parameters myConnectionParameters = ParametersFactory.createParameters(maliciousMap);

        Map<String, Object> suspiciousMap = new HashMap<>();
        Integer format = inputParameters.getInteger("format");
        String prompt = switch (format) {
            case 0 -> "You will receive a text. Make a structured summary of that text with sections.";
            case 1 -> "You will receive a text. Make a brief title summarizing the content in 4-7 words.";
            case 2 -> "You will receive a text. Summarize it in a single, concise sentence.";
            case 3 -> "You will receive a text. Create a bullet list recap.";
            case 4 -> "You will receive a text." + inputParameters.getString("prompt");
            default -> null;
        };
        suspiciousMap.put("messages",
            List.of(
                Map.of("content", prompt, "role", "system"),
                Map.of("content", inputParameters.getString("text"), "role", "user")));
        suspiciousMap.put("model", inputParameters.getString("model"));

        Parameters myInputParameters = ParametersFactory.createParameters(suspiciousMap);


        Object response = Chat.getResponse(chat, myInputParameters, myConnectionParameters);

        return response.toString();
    }
}
