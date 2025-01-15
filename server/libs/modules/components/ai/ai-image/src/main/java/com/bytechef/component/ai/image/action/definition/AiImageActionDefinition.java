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

package com.bytechef.component.ai.image.action.definition;

import static com.bytechef.component.ai.image.constant.AiImageConstants.MODEL_PROVIDER;
import static com.bytechef.component.definition.Authorization.TOKEN;

import com.bytechef.component.ai.image.action.AiImageAction;
import com.bytechef.component.ai.llm.ImageModel;
import com.bytechef.component.ai.llm.azure.openai.action.AzureOpenAiCreateImageAction;
import com.bytechef.component.ai.llm.stability.action.StabilityCreateImageAction;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.openai.action.OpenAiCreateImageAction;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.ParametersFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Marko Kriskovic
 */
public class AiImageActionDefinition extends AbstractActionDefinitionWrapper {

    private final ApplicationProperties.Ai.Component component;
    private final AiImageAction aiImageAction;

    @SuppressFBWarnings("EI")
    public AiImageActionDefinition(
        ActionDefinition actionDefinition, ApplicationProperties.Ai.Component component,
        AiImageAction aiImageAction) {

        super(actionDefinition);

        this.component = component;
        this.aiImageAction = aiImageAction;
    }

    @Override
    public Optional<PerformFunction> getPerform() {
        return Optional.of((SingleConnectionPerformFunction) this::perform);
    }

    protected String perform(
        Parameters inputParameters, Parameters connectionParameter, ActionContext context) {

        Map<String, String> modelConnectionParametersMap = new HashMap<>();

        ImageModel imageModel = switch (inputParameters.getRequiredInteger(MODEL_PROVIDER)) {
            case 0 -> {
                ApplicationProperties.Ai.Component.AzureOpenAi azureOpenAi = component.getAzureOpenAi();

                modelConnectionParametersMap.put(TOKEN, azureOpenAi.getApiKey());

                yield AzureOpenAiCreateImageAction.IMAGE_MODEL;
            }
            case 1 -> {
                ApplicationProperties.Ai.Component.OpenAi openAi = component.getOpenAi();

                modelConnectionParametersMap.put(TOKEN, openAi.getApiKey());

                yield OpenAiCreateImageAction.IMAGE_MODEL;
            }
            case 2 -> {
                ApplicationProperties.Ai.Component.Stability stability = component.getStability();

                modelConnectionParametersMap.put(TOKEN, stability.getApiKey());

                yield StabilityCreateImageAction.IMAGE_MODEL;
            }
            default -> throw new IllegalArgumentException("Invalid connection provider");
        };

        Parameters modelConnectionParameters = ParametersFactory.createParameters(modelConnectionParametersMap);

        Parameters modelInputParameters = aiImageAction.createParameters(inputParameters);

        Object response = imageModel.getResponse(modelInputParameters, modelConnectionParameters);

        return response.toString();
    }
}
