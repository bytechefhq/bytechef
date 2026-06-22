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

package com.bytechef.component.ai.universal.text.action.definition;

import static com.bytechef.component.ai.llm.constant.LLMConstants.PROVIDER;
import static com.bytechef.component.definition.Authorization.TOKEN;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.LLMModelRegistry;
import com.bytechef.component.ai.universal.text.action.AiTextAction;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.ActionContextAware;
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
public class AiTextActionDefinition extends AbstractActionDefinitionWrapper {

    private final ApplicationProperties.Ai.Provider aiProvider;
    private final AiTextAction aiTextAction;
    private final PropertyService propertyService;

    @SuppressFBWarnings("EI")
    public AiTextActionDefinition(
        ActionDefinition actionDefinition, ApplicationProperties.Ai.Provider aiProvider, AiTextAction aiTextAction,
        PropertyService propertyService) {

        super(actionDefinition);

        this.aiProvider = aiProvider;
        this.aiTextAction = aiTextAction;
        this.propertyService = propertyService;
    }

    @Override
    public Optional<BasePerformFunction> getPerform() {
        return Optional.of((PerformFunction) this::perform);
    }

    protected Object perform(Parameters inputParameters, Parameters connectionParameter, ActionContext context) {
        Map<String, String> modelConnectionParametersMap = new HashMap<>();

        ActionContextAware actionContextAware = (ActionContextAware) context;

        Long environmentId = actionContextAware.getEnvironmentId();

        List<String> providerKeys = Arrays.stream(Provider.values())
            .map(Provider::getKey)
            .toList();

        List<String> activeProviderKeys =
            propertyService.getProperties(providerKeys, Scope.PLATFORM, null, environmentId)
                .stream()
                .filter(property -> property.getValue() != null && property.isEnabled())
                .map(Property::getKey)
                .toList();

        Provider provider = Provider.valueOf(inputParameters.getRequiredString(PROVIDER));

        String token = resolveToken(provider, activeProviderKeys, environmentId);

        Parameters modelInputParameters = aiTextAction.createParameters(inputParameters);

        modelConnectionParametersMap.put(TOKEN, token);

        ChatModel chatModel = LLMModelRegistry.getChatModel(provider);

        return chatModel.getResponse(
            modelInputParameters, ParametersFactory.create(modelConnectionParametersMap), context, false,
            modelInputParameters.containsPath("response.responseFormat"));
    }

    private String resolveToken(Provider provider, List<String> activeProviderKeys, Long environmentId) {
        String providerKey = provider.getKey();

        String token = activeProviderKeys.stream()
            .filter(providerKey::equals)
            .findFirst()
            .map(matchedKey -> propertyService.getProperty(matchedKey, Scope.PLATFORM, null, environmentId))
            .map(property -> (String) property.get("apiKey"))
            .orElse(null);

        if (token == null) {
            token = aiProvider.getProviderApiKey(providerKey);
        }

        return token;
    }
}
