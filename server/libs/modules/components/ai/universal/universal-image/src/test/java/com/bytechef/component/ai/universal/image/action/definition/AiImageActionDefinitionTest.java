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

import static com.bytechef.component.ai.llm.constant.LLMConstants.PROVIDER;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.bytechef.component.ai.llm.ImageModel;
import com.bytechef.component.ai.llm.LLMModelRegistry;
import com.bytechef.component.ai.universal.image.action.AiImageAction;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.config.ApplicationProperties.Ai;
import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AiImageActionDefinitionTest {

    private static final Long ENVIRONMENT_ID = 5L;

    private final ActionContext actionContext = mock(
        ActionContext.class, withSettings().extraInterfaces(ActionContextAware.class));
    private final AiImageAction aiImageAction = mock(AiImageAction.class);
    private final ImageModel imageModel = mock(ImageModel.class);
    private final PropertyService propertyService = mock(PropertyService.class);

    static Stream<Arguments> switchProviders() {
        return Stream.of(
            Arguments.of(Provider.AZURE_OPEN_AI, apiKeySetter((aiProvider, apiKey) -> aiProvider.getAzureOpenAi()
                .setApiKey(apiKey))),
            Arguments.of(Provider.OPEN_AI, apiKeySetter((aiProvider, apiKey) -> aiProvider.getOpenAi()
                .setApiKey(apiKey))),
            Arguments.of(Provider.STABILITY, apiKeySetter((aiProvider, apiKey) -> aiProvider.getStability()
                .setApiKey(apiKey))));
    }

    @ParameterizedTest
    @MethodSource("switchProviders")
    void testPerformUsesTheConfiguredApiKeyWhenNoPropertyIsActive(
        Provider provider, BiConsumer<Ai.Provider, String> apiKeySetter) {

        Ai.Provider aiProvider = new Ai.Provider();

        apiKeySetter.accept(aiProvider, "configured-" + provider.getKey());

        when(propertyService.getProperties(anyList(), eq(Scope.PLATFORM), isNull(), eq(ENVIRONMENT_ID)))
            .thenReturn(List.of(createProperty(provider.getKey(), false, "property-" + provider.getKey())));

        Parameters connectionParameters = performAndCaptureConnectionParameters(provider, aiProvider);

        assertEquals(Map.of(TOKEN, "configured-" + provider.getKey()), connectionParameters.toMap());
        verify(propertyService, never()).getProperty(anyString(), any(), any(), any());
    }

    @ParameterizedTest
    @MethodSource("switchProviders")
    void testPerformPrefersTheActivePropertyApiKeyOverTheConfiguredOne(
        Provider provider, BiConsumer<Ai.Provider, String> apiKeySetter) {

        Ai.Provider aiProvider = new Ai.Provider();

        apiKeySetter.accept(aiProvider, "configured-" + provider.getKey());

        Property property = createProperty(provider.getKey(), true, "property-" + provider.getKey());

        when(propertyService.getProperties(anyList(), eq(Scope.PLATFORM), isNull(), eq(ENVIRONMENT_ID)))
            .thenReturn(List.of(property));
        when(propertyService.getProperty(provider.getKey(), Scope.PLATFORM, null, ENVIRONMENT_ID))
            .thenReturn(property);

        Parameters connectionParameters = performAndCaptureConnectionParameters(provider, aiProvider);

        assertEquals(Map.of(TOKEN, "property-" + provider.getKey()), connectionParameters.toMap());
    }

    @Test
    void testPerformRejectsAProviderTheSwitchDidNotHandle() {
        when(propertyService.getProperties(anyList(), eq(Scope.PLATFORM), isNull(), eq(ENVIRONMENT_ID)))
            .thenReturn(List.of());

        AiImageActionDefinition aiImageActionDefinition = createAiImageActionDefinition(new Ai.Provider());

        for (Provider provider : List.of(Provider.ANTHROPIC, Provider.HUGGING_FACE, Provider.OLLAMA)) {
            Parameters inputParameters = ParametersFactory.create(Map.of(PROVIDER, provider.name()));

            assertThrows(
                IllegalArgumentException.class,
                () -> aiImageActionDefinition.perform(inputParameters, null, actionContext));
        }

        verify(propertyService, never()).getProperty(anyString(), any(), any(), any());
    }

    private Parameters performAndCaptureConnectionParameters(Provider provider, Ai.Provider aiProvider) {

        Parameters inputParameters = ParametersFactory.create(Map.of(PROVIDER, provider.name(), "model", "model-1"));
        Parameters modelInputParameters = ParametersFactory.create(
            Map.of("model", "model-1", "n", 1, "size", "1024x1024", "responseFormat", "url"));

        when(aiImageAction.createParameters(same(inputParameters))).thenReturn(modelInputParameters);
        when(((ActionContextAware) actionContext).getEnvironmentId()).thenReturn(ENVIRONMENT_ID);
        when(imageModel.getResponse(any(), any())).thenReturn("response");

        Object response;

        try (MockedStatic<LLMModelRegistry> llmModelRegistry = mockStatic(LLMModelRegistry.class)) {
            llmModelRegistry.when(() -> LLMModelRegistry.getImageModel(provider))
                .thenReturn(imageModel);

            response = createAiImageActionDefinition(aiProvider).perform(inputParameters, null, actionContext);
        }

        assertEquals("response", response);

        ArgumentCaptor<Parameters> connectionParametersCaptor = ArgumentCaptor.forClass(Parameters.class);

        verify(imageModel).getResponse(same(modelInputParameters), connectionParametersCaptor.capture());

        return connectionParametersCaptor.getValue();
    }

    private AiImageActionDefinition createAiImageActionDefinition(Ai.Provider aiProvider) {
        return new AiImageActionDefinition(mock(ActionDefinition.class), aiProvider, aiImageAction, propertyService);
    }

    private static BiConsumer<Ai.Provider, String> apiKeySetter(BiConsumer<Ai.Provider, String> apiKeySetter) {
        return apiKeySetter;
    }

    private static Property createProperty(String key, boolean enabled, String apiKey) {
        Property property = new Property();

        property.setEnabled(enabled);
        property.setKey(key);
        property.setValue(Map.of("apiKey", apiKey));

        return property;
    }
}
