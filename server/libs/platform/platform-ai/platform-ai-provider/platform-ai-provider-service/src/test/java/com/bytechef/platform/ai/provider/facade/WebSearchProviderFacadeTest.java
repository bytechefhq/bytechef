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

package com.bytechef.platform.ai.provider.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.ai.provider.dto.WebSearchProviderDTO;
import com.bytechef.platform.ai.websearch.WebSearchProvider;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WebSearchProviderFacadeTest {

    private static final String ADMIN_EXPRESSION = "hasAuthority(\"ROLE_ADMIN\")";
    private static final long ENVIRONMENT = 1;

    private final ApplicationProperties applicationProperties = new ApplicationProperties();

    @Mock
    private ComponentDefinitionService componentDefinitionService;

    @Mock
    private PropertyService propertyService;

    private WebSearchProviderFacadeImpl webSearchProviderFacade;

    @BeforeEach
    void beforeEach() {
        webSearchProviderFacade = new WebSearchProviderFacadeImpl(
            applicationProperties, componentDefinitionService, propertyService);
    }

    @Test
    void testFetchEnabledApiKeyReturnsTheSavedKeyOfAnEnabledProvider() {
        stubProperty(WebSearchProvider.BRAVE, property(true, "saved-key"));

        assertThat(webSearchProviderFacade.fetchEnabledApiKey(WebSearchProvider.BRAVE, (int) ENVIRONMENT))
            .isEqualTo("saved-key");
    }

    @Test
    void testFetchEnabledApiKeyReturnsNullForADisabledProviderEvenWithAPropertiesKey() {
        configureBrave(true, "properties-key");

        stubProperty(WebSearchProvider.BRAVE, property(false, "saved-key"));

        assertThat(webSearchProviderFacade.fetchEnabledApiKey(WebSearchProvider.BRAVE, (int) ENVIRONMENT))
            .isNull();
    }

    @Test
    void testFetchEnabledApiKeyFallsBackToThePropertiesKeyWhenTheSavedProviderHasNoKey() {
        configureBrave(false, "properties-key");

        stubProperty(WebSearchProvider.BRAVE, property(true, null));

        assertThat(webSearchProviderFacade.fetchEnabledApiKey(WebSearchProvider.BRAVE, (int) ENVIRONMENT))
            .isEqualTo("properties-key");
    }

    @Test
    void testFetchEnabledApiKeyUsesThePropertiesKeyOnlyWhenThePropertiesEnableTheProvider() {
        configureBrave(false, "properties-key");

        when(propertyService.fetchProperty(WebSearchProvider.BRAVE.getKey(), Scope.PLATFORM, null, ENVIRONMENT))
            .thenReturn(Optional.empty());

        assertThat(webSearchProviderFacade.fetchEnabledApiKey(WebSearchProvider.BRAVE, (int) ENVIRONMENT))
            .isNull();

        configureBrave(true, "properties-key");

        assertThat(webSearchProviderFacade.fetchEnabledApiKey(WebSearchProvider.BRAVE, (int) ENVIRONMENT))
            .isEqualTo("properties-key");
    }

    @Test
    void testGetWebSearchProvidersMasksTheKeyAndTellsWhereItComesFrom() {
        configureFirecrawl(true, "fc-properties-1234");

        when(propertyService.getProperties(anyList(), eq(Scope.PLATFORM), isNull(), eq(ENVIRONMENT)))
            .thenReturn(List.of(withKey(WebSearchProvider.BRAVE, property(true, "brave-secret-9876"))));
        when(componentDefinitionService.fetchComponentDefinition(anyString(), isNull()))
            .thenReturn(Optional.empty());

        List<WebSearchProviderDTO> webSearchProviders = webSearchProviderFacade.getWebSearchProviders(
            (int) ENVIRONMENT);

        assertThat(webSearchProviders)
            .extracting(WebSearchProviderDTO::name)
            .containsExactly("brave", "firecrawl");

        WebSearchProviderDTO brave = webSearchProviders.getFirst();

        assertThat(brave.enabled()).isTrue();
        assertThat(brave.maskedApiKey()).isEqualTo("********9876");
        assertThat(brave.maskedApiKey()).doesNotContain("secret");
        assertThat(brave.apiKeyFromProperties()).isFalse();

        WebSearchProviderDTO firecrawl = webSearchProviders.getLast();

        assertThat(firecrawl.enabled()).isTrue();
        assertThat(firecrawl.maskedApiKey()).isEqualTo("********1234");
        assertThat(firecrawl.apiKeyFromProperties()).isTrue();
    }

    @Test
    void testGetWebSearchProvidersReportsAnEnabledProviderWithoutAKeyAsDisabled() {
        when(propertyService.getProperties(anyList(), eq(Scope.PLATFORM), isNull(), eq(ENVIRONMENT)))
            .thenReturn(List.of(withKey(WebSearchProvider.BRAVE, property(true, null))));
        when(componentDefinitionService.fetchComponentDefinition(anyString(), isNull()))
            .thenReturn(Optional.empty());

        WebSearchProviderDTO brave = webSearchProviderFacade.getWebSearchProviders((int) ENVIRONMENT)
            .getFirst();

        assertThat(brave.enabled()).isFalse();
        assertThat(brave.maskedApiKey()).isNull();
    }

    @Test
    void testEnableWebSearchProviderCreatesTheProviderBeforeSwitchingIt() {
        String key = WebSearchProvider.FIRECRAWL.getKey();

        when(propertyService.fetchProperty(key, Scope.PLATFORM, null, ENVIRONMENT))
            .thenReturn(Optional.empty());

        webSearchProviderFacade.enableWebSearchProvider("firecrawl", false, (int) ENVIRONMENT);

        InOrder inOrder = inOrder(propertyService);

        inOrder.verify(propertyService)
            .save(key, Map.of("apiKeyFromProperties", true), Scope.PLATFORM, null, ENVIRONMENT);
        inOrder.verify(propertyService)
            .update(key, false, Scope.PLATFORM, null, ENVIRONMENT);
    }

    @Test
    void testEnableWebSearchProviderSwitchesASavedProviderWithoutResavingIt() {
        String key = WebSearchProvider.BRAVE.getKey();

        when(propertyService.fetchProperty(key, Scope.PLATFORM, null, ENVIRONMENT))
            .thenReturn(Optional.of(property(true, "saved-key")));

        webSearchProviderFacade.enableWebSearchProvider("brave", true, (int) ENVIRONMENT);

        verify(propertyService, never()).save(anyString(), anyMap(), eq(Scope.PLATFORM), isNull(), eq(ENVIRONMENT));
        verify(propertyService).update(key, true, Scope.PLATFORM, null, ENVIRONMENT);
    }

    @Test
    void testUpdateWebSearchProviderSavesTheStrippedKey() {
        webSearchProviderFacade.updateWebSearchProvider("brave", "  new-key  ", (int) ENVIRONMENT);

        verify(propertyService).save(
            WebSearchProvider.BRAVE.getKey(), Map.of("apiKey", "new-key"), Scope.PLATFORM, null, ENVIRONMENT);
    }

    @Test
    void testUpdateWebSearchProviderRejectsABlankKeyAndAnUnknownProvider() {
        assertThatThrownBy(() -> webSearchProviderFacade.updateWebSearchProvider("brave", " ", (int) ENVIRONMENT))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> webSearchProviderFacade.updateWebSearchProvider("bing", "key", (int) ENVIRONMENT))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testAdminOperationsRequireAdminAndKeyResolutionDoesNot() throws NoSuchMethodException {
        for (String methodName : List.of(
            "deleteWebSearchProvider", "enableWebSearchProvider", "getWebSearchProviders",
            "updateWebSearchProvider")) {

            Method method = findMethod(methodName);

            assertThat(method.getAnnotation(PreAuthorize.class))
                .as("%s must be admin only", methodName)
                .isNotNull()
                .extracting(PreAuthorize::value)
                .isEqualTo(ADMIN_EXPRESSION);
        }

        assertThat(
            WebSearchProviderFacadeImpl.class
                .getMethod("fetchEnabledApiKey", WebSearchProvider.class, int.class)
                .getAnnotation(PreAuthorize.class))
                    .as("agent runs resolve web search keys on threads without an admin principal")
                    .isNull();
    }

    private void configureBrave(boolean enabled, String apiKey) {
        ApplicationProperties.Ai.Brave brave = applicationProperties.getAi()
            .getBrave();

        brave.setApiKey(apiKey);
        brave.setEnabled(enabled);
    }

    private void configureFirecrawl(boolean enabled, String apiKey) {
        ApplicationProperties.Ai.Firecrawl firecrawl = applicationProperties.getAi()
            .getFirecrawl();

        firecrawl.setApiKey(apiKey);
        firecrawl.setEnabled(enabled);
    }

    private static Method findMethod(String name) {
        for (Method method : WebSearchProviderFacadeImpl.class.getDeclaredMethods()) {
            if (method.getName()
                .equals(name)) {

                return method;
            }
        }

        throw new IllegalStateException("No method " + name);
    }

    private static Property property(boolean enabled, String apiKey) {
        Property property = new Property();

        property.setEnabled(enabled);
        property.setValue(apiKey == null ? Map.of("apiKeyFromProperties", true) : Map.of("apiKey", apiKey));

        return property;
    }

    private void stubProperty(WebSearchProvider webSearchProvider, Property property) {
        when(propertyService.fetchProperty(webSearchProvider.getKey(), Scope.PLATFORM, null, ENVIRONMENT))
            .thenReturn(Optional.of(property));
    }

    private static Property withKey(WebSearchProvider webSearchProvider, Property property) {
        property.setKey(webSearchProvider.getKey());

        return property;
    }
}
