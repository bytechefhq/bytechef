/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.ai.universal.image.action.definition;

import static com.bytechef.component.ai.llm.Provider.AZURE_OPEN_AI;
import static com.bytechef.component.ai.llm.Provider.OPEN_AI;
import static com.bytechef.component.ai.llm.Provider.STABILITY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PROVIDER;
import static com.bytechef.component.definition.Authorization.TOKEN;

import com.bytechef.component.ai.llm.ImageModel;
import com.bytechef.component.ai.llm.Provider;
import com.bytechef.component.ai.llm.azure.openai.action.AzureOpenAiCreateImageAction;
import com.bytechef.component.ai.llm.openai.action.OpenAiCreateImageAction;
import com.bytechef.component.ai.llm.stability.action.StabilityCreateImageAction;
import com.bytechef.component.ai.universal.image.action.AiImageAction;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Marko Kriskovic
 */
public class AiImageActionDefinition extends AbstractActionDefinitionWrapper {

    private final AiImageAction aiImageAction;
    private final ApplicationProperties.Ai.Provider aiProvider;
    private final PropertyService propertyService;

    @SuppressFBWarnings("EI")
    public AiImageActionDefinition(
        ActionDefinition actionDefinition, ApplicationProperties.Ai.Provider aiProvider,
        AiImageAction aiImageAction, PropertyService propertyService) {

        super(actionDefinition);

        this.aiImageAction = aiImageAction;
        this.aiProvider = aiProvider;
        this.propertyService = propertyService;
    }

    @Override
    public Optional<BasePerformFunction> getPerform() {
        return Optional.of((PerformFunction) this::perform);
    }

    protected Object perform(
        Parameters inputParameters, Parameters connectionParameter, ActionContext context) {

        Map<String, String> modelConnectionParametersMap = new HashMap<>();

        List<String> activeProviderKeys = propertyService.getProperties(
            Arrays.stream(Provider.values())
                .map(Provider::getKey)
                .toList(),
            Scope.PLATFORM, null)
            .stream()
            .filter(property -> property.getValue() != null && property.isEnabled())
            .map(Property::getKey)
            .toList();

        Parameters modelInputParameters = aiImageAction.createParameters(inputParameters);
        Parameters modelConnectionParameters = ParametersFactory.create(modelConnectionParametersMap);

        ImageModel imageModel = getImageModel(inputParameters, activeProviderKeys, modelConnectionParametersMap);

        return imageModel.getResponse(modelInputParameters, modelConnectionParameters);
    }

    private String getAiProviderToken(String key, List<String> activeProviderKeys) {
        return activeProviderKeys.stream()
            .filter(key::equals)
            .findFirst()
            .map(curKey -> propertyService.getProperty(curKey, Scope.PLATFORM, null))
            .map(property -> (String) property.get("apiKey"))
            .orElse(null);
    }

    private ImageModel getImageModel(
        Parameters inputParameters, List<String> activeProviderKeys, Map<String, String> modelConnectionParametersMap) {

        return switch (Provider.valueOf(inputParameters.getRequiredString(PROVIDER))) {
            case AZURE_OPEN_AI -> getAzureOpenAiImageModel(activeProviderKeys, modelConnectionParametersMap);
            case OPEN_AI -> getOpenAiImageModel(activeProviderKeys, modelConnectionParametersMap);
            case STABILITY -> getStabilityImageModel(activeProviderKeys, modelConnectionParametersMap);
            default -> throw new IllegalArgumentException("Invalid provider");
        };
    }

    private ImageModel getAzureOpenAiImageModel(
        List<String> activeProviderKeys, Map<String, String> modelConnectionParametersMap) {

        String token = getAiProviderToken(AZURE_OPEN_AI.getKey(), activeProviderKeys);

        if (token == null) {
            ApplicationProperties.Ai.Provider.AzureOpenAi azureOpenAi = aiProvider.getAzureOpenAi();

            token = azureOpenAi.getApiKey();
        }

        modelConnectionParametersMap.put(TOKEN, token);

        return AzureOpenAiCreateImageAction.IMAGE_MODEL;
    }

    private ImageModel getOpenAiImageModel(
        List<String> activeProviderKeys, Map<String, String> modelConnectionParametersMap) {

        String token = getAiProviderToken(OPEN_AI.getKey(), activeProviderKeys);

        if (token == null) {
            ApplicationProperties.Ai.Provider.OpenAi openAi = aiProvider.getOpenAi();

            token = openAi.getApiKey();
        }

        modelConnectionParametersMap.put(TOKEN, token);

        return OpenAiCreateImageAction.IMAGE_MODEL;
    }

    private ImageModel getStabilityImageModel(
        List<String> activeProviderKeys, Map<String, String> modelConnectionParametersMap) {

        String token = getAiProviderToken(STABILITY.getKey(), activeProviderKeys);

        if (token == null) {
            ApplicationProperties.Ai.Provider.Stability stability = aiProvider.getStability();

            token = stability.getApiKey();
        }

        modelConnectionParametersMap.put(TOKEN, token);

        return StabilityCreateImageAction.IMAGE_MODEL;
    }
}
