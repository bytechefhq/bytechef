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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.ai.provider.dto.WebSearchProviderConnection;
import com.bytechef.platform.ai.provider.dto.WebSearchProviderDTO;
import com.bytechef.platform.ai.provider.dto.WebSearchProviderTestResultDTO;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WebSearchProviderFacadeTest {

    private static final String ADMIN_EXPRESSION = "hasAuthority(\"ROLE_ADMIN\")";
    private static final String BRAVE_BASE_URL = "https://api.search.brave.com/res/v1";
    private static final long ENVIRONMENT = 1;
    private static final String FIRECRAWL_INTERNAL_URL = "https://firecrawl.internal/v2";

    private final ApplicationProperties applicationProperties = new ApplicationProperties();

    @Mock
    private ComponentDefinitionService componentDefinitionService;

    @Mock
    private PropertyService propertyService;

    @Mock
    private WebSearchProviderConnectionTester webSearchProviderConnectionTester;

    @Mock
    private ObjectProvider<WebSearchProviderConnectionTester> webSearchProviderConnectionTesterProvider;

    private WebSearchProviderFacadeImpl webSearchProviderFacade;

    @BeforeEach
    void beforeEach() {
        webSearchProviderFacade = new WebSearchProviderFacadeImpl(
            applicationProperties, componentDefinitionService, propertyService,
            webSearchProviderConnectionTesterProvider);
    }

    @Test
    void testFetchEnabledConnectionReturnsTheSavedKeyOfAnEnabledProvider() {
        stubProperty(WebSearchProvider.BRAVE, property(true, "saved-key"));

        assertThat(webSearchProviderFacade.fetchEnabledConnection(WebSearchProvider.BRAVE, (int) ENVIRONMENT))
            .isEqualTo(new WebSearchProviderConnection("saved-key", BRAVE_BASE_URL));
    }

    @Test
    void testFetchEnabledConnectionUsesTheSavedBaseUrl() {
        Property property = new Property();

        property.setEnabled(true);
        property.setValue(Map.of("apiKey", "saved-key", "baseUrl", FIRECRAWL_INTERNAL_URL));

        stubProperty(WebSearchProvider.FIRECRAWL, property);

        assertThat(webSearchProviderFacade.fetchEnabledConnection(WebSearchProvider.FIRECRAWL, (int) ENVIRONMENT))
            .isEqualTo(new WebSearchProviderConnection("saved-key", FIRECRAWL_INTERNAL_URL));
    }

    @Test
    void testFetchEnabledConnectionReturnsNullForADisabledProviderEvenWithAPropertiesKey() {
        configureBrave(true, "properties-key");

        stubProperty(WebSearchProvider.BRAVE, property(false, "saved-key"));

        assertThat(webSearchProviderFacade.fetchEnabledConnection(WebSearchProvider.BRAVE, (int) ENVIRONMENT))
            .isNull();
    }

    @Test
    void testFetchEnabledConnectionFallsBackToThePropertiesKeyWhenTheSavedProviderHasNoKey() {
        configureBrave(false, "properties-key");

        stubProperty(WebSearchProvider.BRAVE, property(true, null));

        assertThat(webSearchProviderFacade.fetchEnabledConnection(WebSearchProvider.BRAVE, (int) ENVIRONMENT))
            .isEqualTo(new WebSearchProviderConnection("properties-key", BRAVE_BASE_URL));
    }

    @Test
    void testFetchEnabledConnectionUsesThePropertiesKeyOnlyWhenThePropertiesEnableTheProvider() {
        configureBrave(false, "properties-key");

        when(propertyService.fetchProperty(WebSearchProvider.BRAVE.getKey(), Scope.PLATFORM, null, ENVIRONMENT))
            .thenReturn(Optional.empty());

        assertThat(webSearchProviderFacade.fetchEnabledConnection(WebSearchProvider.BRAVE, (int) ENVIRONMENT))
            .isNull();

        configureBrave(true, "properties-key");

        assertThat(webSearchProviderFacade.fetchEnabledConnection(WebSearchProvider.BRAVE, (int) ENVIRONMENT))
            .isEqualTo(new WebSearchProviderConnection("properties-key", BRAVE_BASE_URL));
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
        assertThat(brave.apiKeyFromProperties()).isFalse();
        assertThat(brave.baseUrl()).isEqualTo(BRAVE_BASE_URL);
        assertThat(brave.defaultBaseUrl()).isEqualTo(BRAVE_BASE_URL);

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
    void testUpdateWebSearchProviderSavesTheStrippedKeyAndBaseUrl() {
        when(propertyService.fetchProperty(WebSearchProvider.FIRECRAWL.getKey(), Scope.PLATFORM, null, ENVIRONMENT))
            .thenReturn(Optional.empty());

        webSearchProviderFacade.updateWebSearchProvider(
            "firecrawl", "  new-key  ", " " + FIRECRAWL_INTERNAL_URL + " ", (int) ENVIRONMENT);

        verify(propertyService).save(
            WebSearchProvider.FIRECRAWL.getKey(), Map.of("apiKey", "new-key", "baseUrl", FIRECRAWL_INTERNAL_URL),
            Scope.PLATFORM, null, ENVIRONMENT);
    }

    @Test
    void testUpdateWebSearchProviderKeepsTheSavedKeyWhenOnlyTheBaseUrlChanges() {
        stubProperty(WebSearchProvider.FIRECRAWL, property(true, "saved-key"));

        webSearchProviderFacade.updateWebSearchProvider(
            "firecrawl", null, FIRECRAWL_INTERNAL_URL, (int) ENVIRONMENT);

        verify(propertyService).save(
            WebSearchProvider.FIRECRAWL.getKey(), Map.of("apiKey", "saved-key", "baseUrl", FIRECRAWL_INTERNAL_URL),
            Scope.PLATFORM, null, ENVIRONMENT);
    }

    @Test
    void testUpdateWebSearchProviderGoesBackToThePropertiesBaseUrlWhenTheBaseUrlIsBlank() {
        stubProperty(WebSearchProvider.BRAVE, property(true, "saved-key"));

        webSearchProviderFacade.updateWebSearchProvider("brave", " ", "", (int) ENVIRONMENT);

        verify(propertyService).save(
            WebSearchProvider.BRAVE.getKey(), Map.of("apiKey", "saved-key"), Scope.PLATFORM, null, ENVIRONMENT);
    }

    @Test
    void testUpdateWebSearchProviderKeepsUsingThePropertiesKeyWhenNoKeyIsSaved() {
        configureBrave(true, "properties-key");

        when(propertyService.fetchProperty(WebSearchProvider.BRAVE.getKey(), Scope.PLATFORM, null, ENVIRONMENT))
            .thenReturn(Optional.empty());

        webSearchProviderFacade.updateWebSearchProvider("brave", null, null, (int) ENVIRONMENT);

        verify(propertyService).save(
            WebSearchProvider.BRAVE.getKey(), Map.of("apiKeyFromProperties", true), Scope.PLATFORM, null,
            ENVIRONMENT);
    }

    @Test
    void testUpdateWebSearchProviderRejectsAMissingKeyABadBaseUrlAndAnUnknownProvider() {
        when(propertyService.fetchProperty(WebSearchProvider.BRAVE.getKey(), Scope.PLATFORM, null, ENVIRONMENT))
            .thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> webSearchProviderFacade.updateWebSearchProvider("brave", " ", null, (int) ENVIRONMENT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
            () -> webSearchProviderFacade.updateWebSearchProvider("brave", "key", "ftp://host", (int) ENVIRONMENT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
            () -> webSearchProviderFacade.updateWebSearchProvider("bing", "key", null, (int) ENVIRONMENT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testTestWebSearchProviderUsesTheTypedKeyOrElseTheSavedOne() {
        stubProperty(WebSearchProvider.BRAVE, property(true, "saved-key"));

        when(webSearchProviderConnectionTesterProvider.getIfAvailable()).thenReturn(webSearchProviderConnectionTester);

        assertThat(webSearchProviderFacade.testWebSearchProvider("brave", "typed-key", null, (int) ENVIRONMENT))
            .isEqualTo(WebSearchProviderTestResultDTO.succeeded());
        assertThat(webSearchProviderFacade.testWebSearchProvider("brave", null, null, (int) ENVIRONMENT))
            .isEqualTo(WebSearchProviderTestResultDTO.succeeded());

        verify(webSearchProviderConnectionTester)
            .test(WebSearchProvider.BRAVE, new WebSearchProviderConnection("typed-key", BRAVE_BASE_URL));
        verify(webSearchProviderConnectionTester)
            .test(WebSearchProvider.BRAVE, new WebSearchProviderConnection("saved-key", BRAVE_BASE_URL));
    }

    @Test
    void testTestWebSearchProviderChecksTheDefaultBaseUrlWhenTheBaseUrlIsCleared() {
        Property property = new Property();

        property.setEnabled(true);
        property.setValue(Map.of("apiKey", "saved-key", "baseUrl", FIRECRAWL_INTERNAL_URL));

        stubProperty(WebSearchProvider.FIRECRAWL, property);

        when(webSearchProviderConnectionTesterProvider.getIfAvailable()).thenReturn(webSearchProviderConnectionTester);

        assertThat(webSearchProviderFacade.testWebSearchProvider("firecrawl", null, "  ", (int) ENVIRONMENT))
            .isEqualTo(WebSearchProviderTestResultDTO.succeeded());

        String defaultBaseUrl = applicationProperties.getAi()
            .getFirecrawl()
            .getBaseUrl();

        verify(webSearchProviderConnectionTester)
            .test(WebSearchProvider.FIRECRAWL, new WebSearchProviderConnection("saved-key", defaultBaseUrl));
    }

    @Test
    void testTestWebSearchProviderReportsTheMostSpecificProviderError() {
        stubProperty(WebSearchProvider.BRAVE, property(true, "saved-key"));

        when(webSearchProviderConnectionTesterProvider.getIfAvailable()).thenReturn(webSearchProviderConnectionTester);
        doThrow(new IllegalStateException("search failed", new IllegalStateException("401 Unauthorized")))
            .when(webSearchProviderConnectionTester)
            .test(eq(WebSearchProvider.BRAVE), any());

        assertThat(webSearchProviderFacade.testWebSearchProvider("brave", null, null, (int) ENVIRONMENT))
            .isEqualTo(WebSearchProviderTestResultDTO.failure("401 Unauthorized"));
    }

    @Test
    void testTestWebSearchProviderAsksForAKeyWhenThereIsNone() {
        when(propertyService.fetchProperty(WebSearchProvider.FIRECRAWL.getKey(), Scope.PLATFORM, null, ENVIRONMENT))
            .thenReturn(Optional.empty());

        assertThat(webSearchProviderFacade.testWebSearchProvider("firecrawl", null, null, (int) ENVIRONMENT))
            .isEqualTo(WebSearchProviderTestResultDTO.failure("Enter an API key to test."));

        verify(webSearchProviderConnectionTesterProvider, never()).getIfAvailable();
    }

    @Test
    void testTestWebSearchProviderRejectsABadBaseUrlWithoutCallingTheProvider() {
        when(propertyService.fetchProperty(WebSearchProvider.FIRECRAWL.getKey(), Scope.PLATFORM, null, ENVIRONMENT))
            .thenReturn(Optional.empty());

        assertThat(webSearchProviderFacade.testWebSearchProvider("firecrawl", "key", "not a url", (int) ENVIRONMENT))
            .isEqualTo(WebSearchProviderTestResultDTO.failure("The base URL must be an http or https URL."));

        verify(webSearchProviderConnectionTesterProvider, never()).getIfAvailable();
    }

    @Test
    void testAdminOperationsRequireAdminAndConnectionResolutionDoesNot() throws NoSuchMethodException {
        for (String methodName : List.of(
            "deleteWebSearchProvider", "enableWebSearchProvider", "getWebSearchProviders", "testWebSearchProvider",
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
                .getMethod("fetchEnabledConnection", WebSearchProvider.class, int.class)
                .getAnnotation(PreAuthorize.class))
                    .as("agent runs resolve web search connections on threads without an admin principal")
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
