/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.configuration.dto.AiProviderDTO;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AiProviderFacadeTest {

    private static final int ENVIRONMENT = 2;

    @Mock
    private ComponentDefinitionService componentDefinitionService;

    @Mock
    private PropertyService propertyService;

    private AiProviderFacadeImpl facade;

    @BeforeEach
    void setUp() {
        ApplicationProperties applicationProperties = mock(ApplicationProperties.class, RETURNS_DEEP_STUBS);

        facade = new AiProviderFacadeImpl(componentDefinitionService, propertyService, applicationProperties);
    }

    @Test
    void testOnlyOpenAiSupportsEmbeddings() {
        List<ComponentDefinition> componentDefinitions = buildComponentDefinitionsForAllProviders();

        when(componentDefinitionService.getComponentDefinitions()).thenReturn(componentDefinitions);
        when(propertyService.getProperties(
            ArgumentMatchers.anyList(),
            ArgumentMatchers.eq(Scope.PLATFORM),
            ArgumentMatchers.isNull(),
            ArgumentMatchers.eq((long) ENVIRONMENT)))
                .thenReturn(List.of());

        List<AiProviderDTO> providers = facade.getAiProviders(ENVIRONMENT);

        AiProviderDTO openAi = providers.stream()
            .filter(provider -> provider.id() == Provider.OPEN_AI.getId())
            .findFirst()
            .orElseThrow();

        assertThat(openAi.supportsEmbeddings()).isTrue();
        assertThat(providers.stream()
            .filter(provider -> provider.id() != Provider.OPEN_AI.getId())
            .allMatch(provider -> !provider.supportsEmbeddings())).isTrue();
    }

    private List<ComponentDefinition> buildComponentDefinitionsForAllProviders() {
        return Arrays.stream(Provider.values())
            .map(provider -> {
                ComponentDefinition definition = mock(ComponentDefinition.class);

                when(definition.getName()).thenReturn(provider.getName());
                lenient().when(definition.getIcon())
                    .thenReturn(null);
                lenient().when(definition.getActions())
                    .thenReturn(List.of());

                return definition;
            })
            .toList();
    }
}
