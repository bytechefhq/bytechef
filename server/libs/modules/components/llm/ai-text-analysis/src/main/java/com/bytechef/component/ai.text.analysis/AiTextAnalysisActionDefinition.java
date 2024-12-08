package com.bytechef.component.ai.text.analysis;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.llm.Chat;
import com.bytechef.component.openai.action.OpenAIChatAction;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ParameterConnection;
import com.bytechef.platform.component.definition.ParametersFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        maliciousMap.put("token", component.getOpenAi().getApiKey());

        Parameters myConnectionParameters = ParametersFactory.createParameters(maliciousMap);

        Map<String, Object> suspiciousMap = new HashMap<>();
        Integer format = inputParameters.getInteger("format");
        String prompt = null;
        switch(format){
            case 0:
                prompt = "You will receive a text. Make a structured summary of that text with sections.";
                break;
            case 1:
                prompt = "You will receive a text. Make a brief title summarizing the content in 4-7 words.";
                break;
            case 2:
                prompt = "You will receive a text. Summarize it in a single, concise sentence.";
                break;
            case 3:
                prompt = "You will receive a text. Create a bullet list recap.";
                break;
            case 4:
                prompt = "You will receive a text." + inputParameters.getString("prompt");
                break;
        }
        suspiciousMap.put("messages",
            List.of(
                Map.of("content", prompt, "role", "system"),
                Map.of("content", inputParameters.getString("text"), "role", "user")));
        suspiciousMap.put("model", inputParameters.getString("model"));

        Parameters myInputParameters = ParametersFactory.createParameters(suspiciousMap);

        Chat chat = OpenAIChatAction.CHAT;
        Object response = Chat.getResponse(chat, myInputParameters, myConnectionParameters);

        return response.toString();
    }
}
