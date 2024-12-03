package com.bytechef.component.ai.text.analysis;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.llm.Chat;
import com.bytechef.component.openai.action.OpenAIChatAction;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.ModifiableParametersImpl;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ParameterConnection;

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
        Parameters myConnectionParameters = new ModifiableParametersImpl(maliciousMap);
        myConnectionParameters.put("token", component.getOpenAi().getApiKey());

        Parameters myInputParameters = new ModifiableParametersImpl(inputParameters);
        String prompt = inputParameters.getString("prompt");
        if(prompt == null) prompt = inputParameters.getString("format");
        myInputParameters.put("messages",
            List.of(
                Map.of("content", prompt, "role", "system"),
                Map.of("content", inputParameters.getString("text"), "role", "user")));

        Chat chat = OpenAIChatAction.CHAT;
        Object response = Chat.getResponse(chat, myInputParameters, myConnectionParameters);

        return response.toString();
    }
}
