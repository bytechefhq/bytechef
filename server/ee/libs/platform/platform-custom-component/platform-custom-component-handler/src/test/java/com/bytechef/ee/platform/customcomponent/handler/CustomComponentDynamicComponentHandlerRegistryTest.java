/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent;
import com.bytechef.ee.platform.customcomponent.configuration.service.CustomComponentService;
import com.bytechef.ee.platform.customcomponent.file.storage.CustomComponentFileStorage;
import com.bytechef.ee.platform.customcomponent.loader.ComponentHandlerLoader;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.cache.CacheManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CustomComponentDynamicComponentHandlerRegistryTest {

    private final CustomComponentService customComponentService = mock(CustomComponentService.class);

    private CustomComponentDynamicComponentHandlerRegistry registry;

    @BeforeEach
    void beforeEach() {
        registry = new CustomComponentDynamicComponentHandlerRegistry(
            mock(CacheManager.class), mock(CustomComponentFileStorage.class), customComponentService);
    }

    @Test
    void testGetComponentHandlersExcludesDisabled() {
        CustomComponent disabledCustomComponent = customComponent("disabled", 1, CustomComponent.Language.JAVASCRIPT);

        disabledCustomComponent.setEnabled(false);

        when(customComponentService.getCustomComponents()).thenReturn(List.of(disabledCustomComponent));

        assertThat(registry.getComponentHandlers()).isEmpty();
    }

    @Test
    void testGetComponentHandlersSkipsAComponentThatFailsToLoad() {
        CustomComponent unloadableCustomComponent = customComponent("unloadable", 1, CustomComponent.Language.RUBY);
        CustomComponent loadableCustomComponent = customComponent("loadable", 7, CustomComponent.Language.JAVASCRIPT);

        when(customComponentService.getCustomComponents())
            .thenReturn(List.of(unloadableCustomComponent, loadableCustomComponent));

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getName()).thenReturn("loadable");
        when(componentDefinition.getActions()).thenReturn(Optional.empty());

        ComponentHandler componentHandler = mock(ComponentHandler.class);

        when(componentHandler.getDefinition()).thenReturn(componentDefinition);

        try (MockedStatic<ComponentHandlerLoader> componentHandlerLoader = mockStatic(ComponentHandlerLoader.class)) {
            componentHandlerLoader
                .when(() -> ComponentHandlerLoader.loadComponentHandler(
                    any(), eq(CustomComponent.Language.RUBY), any(), any()))
                .thenThrow(new IllegalArgumentException("A language with id 'ruby' is not installed."));
            componentHandlerLoader
                .when(() -> ComponentHandlerLoader.loadComponentHandler(
                    any(), eq(CustomComponent.Language.JAVASCRIPT), any(), any()))
                .thenReturn(componentHandler);

            List<? extends ComponentHandler> componentHandlers = registry.getComponentHandlers();

            assertThat(componentHandlers).hasSize(1);

            ComponentDefinition loadedComponentDefinition = componentHandlers.getFirst()
                .getDefinition();

            assertThat(loadedComponentDefinition.getName()).isEqualTo("loadable");
            assertThat(loadedComponentDefinition.getVersion()).isEqualTo(7);
        }
    }

    private static CustomComponent customComponent(
        String name, int componentVersion, CustomComponent.Language language) {

        CustomComponent customComponent = new CustomComponent();

        customComponent.setComponentVersion(componentVersion);
        customComponent.setEnabled(true);
        customComponent.setLanguage(language);
        customComponent.setName(name);

        return customComponent;
    }
}
