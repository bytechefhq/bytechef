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

package com.bytechef.platform.connection.aiprovider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AiProviderConnectionSourceImplTest {

    private static final int ENVIRONMENT_ID = 0;

    private final PropertyService propertyService = mock(PropertyService.class);

    private AiProviderConnectionSourceImpl aiProviderConnectionSource;

    @BeforeEach
    void beforeEach() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getAi()
            .getProvider()
            .getOpenAi()
            .setApiKey("config-api-key");

        aiProviderConnectionSource = new AiProviderConnectionSourceImpl(propertyService, applicationProperties);
    }

    @Test
    void testAConfiguredApiKeyEnablesAProviderWithoutASavedSetting() {
        givenSavedProperty(null);

        assertThat(aiProviderConnectionSource.isEnabled(Provider.OPEN_AI, ENVIRONMENT_ID)).isTrue();
        assertThat(aiProviderConnectionSource.getApiKey(Provider.OPEN_AI, ENVIRONMENT_ID)).contains("config-api-key");
    }

    @Test
    void testASavedDisabledSettingWinsOverAConfiguredApiKey() {
        givenSavedProperty(property(false, Map.of()));

        assertThat(aiProviderConnectionSource.isEnabled(Provider.OPEN_AI, ENVIRONMENT_ID)).isFalse();
        assertThat(aiProviderConnectionSource.getApiKey(Provider.OPEN_AI, ENVIRONMENT_ID)).isEmpty();
    }

    @Test
    void testASavedEnabledSettingUsesItsOwnApiKey() {
        givenSavedProperty(property(true, Map.of("apiKey", "saved-api-key")));

        assertThat(aiProviderConnectionSource.isEnabled(Provider.OPEN_AI, ENVIRONMENT_ID)).isTrue();
        assertThat(aiProviderConnectionSource.getApiKey(Provider.OPEN_AI, ENVIRONMENT_ID)).contains("saved-api-key");
    }

    private void givenSavedProperty(Property property) {
        when(propertyService.fetchProperty(Provider.OPEN_AI.getKey(), Scope.PLATFORM, null, (long) ENVIRONMENT_ID))
            .thenReturn(Optional.ofNullable(property));
    }

    private static Property property(boolean enabled, Map<String, ?> value) {
        Property property = new Property();

        property.setEnabled(enabled);
        property.setKey(Provider.OPEN_AI.getKey());
        property.setValue(value);

        return property;
    }
}
