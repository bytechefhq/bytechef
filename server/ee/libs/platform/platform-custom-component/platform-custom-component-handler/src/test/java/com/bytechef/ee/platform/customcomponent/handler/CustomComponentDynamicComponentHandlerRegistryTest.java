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
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent;
import com.bytechef.ee.platform.customcomponent.configuration.service.CustomComponentService;
import com.bytechef.ee.platform.customcomponent.file.storage.CustomComponentFileStorage;
import com.bytechef.ee.platform.customcomponent.loader.ComponentHandlerLoader;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

/**
 * @version ee
 *
 * @author Claude Fable 5
 */
@ExtendWith(MockitoExtension.class)
class CustomComponentDynamicComponentHandlerRegistryTest {

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private CustomComponentFileStorage customComponentFileStorage;

    @Mock
    private CustomComponentService customComponentService;

    private CustomComponentDynamicComponentHandlerRegistry registry;

    @BeforeEach
    void setUp() {
        ApplicationProperties.Component component = new ApplicationProperties.Component();

        ApplicationProperties.Component.CustomComponent customComponent =
            new ApplicationProperties.Component.CustomComponent();
        customComponent.setJavaLoader(ApplicationProperties.Component.CustomComponent.JavaLoader.CLASS_LOADER);

        component.setCustomComponent(customComponent);

        when(applicationProperties.getComponent()).thenReturn(component);

        registry = new CustomComponentDynamicComponentHandlerRegistry(
            applicationProperties, cacheManager, customComponentFileStorage, customComponentService);
    }

    @Test
    void testGetComponentHandlersExcludesDrafts() {
        CustomComponent draft = new CustomComponent();

        draft.setEnabled(true);
        draft.setName("test");
        draft.setStatus(CustomComponent.Status.DRAFT);

        when(customComponentService.getCustomComponents()).thenReturn(List.of(draft));

        assertThat(registry.getComponentHandlers()).isEmpty();
    }

    @Test
    void testGetComponentHandlersExcludesDisabled() {
        CustomComponent disabled = new CustomComponent();

        disabled.setEnabled(false);
        disabled.setName("test");
        disabled.setStatus(CustomComponent.Status.PUBLISHED);

        when(customComponentService.getCustomComponents()).thenReturn(List.of(disabled));

        assertThat(registry.getComponentHandlers()).isEmpty();
    }

    /**
     * A component that cannot be loaded — its guest language no longer resolving, its file gone, a jar compiled against
     * a since-changed API — must not take the rest of the registry down with it.
     *
     * <p>
     * The load is driven entirely through the static {@link ComponentHandlerLoader} stub, keyed on the component's
     * language, so exactly one of the two components throws and the other succeeds on every run. Removing the try/catch
     * in {@link CustomComponentDynamicComponentHandlerRegistry#getComponentHandlers()} makes the call propagate the
     * RuntimeException instead of returning the surviving handler, which fails this test.
     */
    @Test
    void testGetComponentHandlersSkipsAComponentThatFailsToLoad() {
        CustomComponent unloadable = publishedCustomComponent("unloadable", 1, CustomComponent.Language.PYTHON);
        CustomComponent loadable = publishedCustomComponent("loadable", 7, CustomComponent.Language.JAVASCRIPT);

        when(customComponentService.getCustomComponents()).thenReturn(List.of(unloadable, loadable));

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getName()).thenReturn("loadable");

        ComponentHandler componentHandler = mock(ComponentHandler.class);

        when(componentHandler.getDefinition()).thenReturn(componentDefinition);

        try (MockedStatic<ComponentHandlerLoader> componentHandlerLoader = mockStatic(ComponentHandlerLoader.class)) {
            componentHandlerLoader
                .when(() -> ComponentHandlerLoader.loadComponentHandler(
                    any(), eq(CustomComponent.Language.PYTHON), any(), any(), any()))
                .thenThrow(new RuntimeException("language 'python' is not installed"));
            componentHandlerLoader
                .when(() -> ComponentHandlerLoader.loadComponentHandler(
                    any(), eq(CustomComponent.Language.JAVASCRIPT), any(), any(), any()))
                .thenReturn(componentHandler);

            List<? extends ComponentHandler> componentHandlers = registry.getComponentHandlers();

            assertThat(componentHandlers).hasSize(1);

            ComponentDefinition survivingDefinition = componentHandlers.get(0)
                .getDefinition();

            assertThat(survivingDefinition.getName()).isEqualTo("loadable");
            assertThat(survivingDefinition.getVersion()).isEqualTo(7);
        }
    }

    @Test
    void testFetchComponentHandlerExcludesDraft() {
        CustomComponent draft = new CustomComponent();

        draft.setName("test");
        draft.setStatus(CustomComponent.Status.DRAFT);

        when(customComponentService.fetchCustomComponent("test", 2)).thenReturn(Optional.of(draft));

        assertThat(registry.fetchComponentHandler("test", 2)).isEmpty();
    }

    private static CustomComponent publishedCustomComponent(
        String name, int componentVersion, CustomComponent.Language language) {

        CustomComponent customComponent = new CustomComponent();

        customComponent.setComponentVersion(componentVersion);
        customComponent.setEnabled(true);
        customComponent.setLanguage(language);
        customComponent.setName(name);
        customComponent.setStatus(CustomComponent.Status.PUBLISHED);

        return customComponent;
    }
}
