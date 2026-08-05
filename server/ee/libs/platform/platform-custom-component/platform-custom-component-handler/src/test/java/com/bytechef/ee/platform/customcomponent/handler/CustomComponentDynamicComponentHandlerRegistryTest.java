/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent;
import com.bytechef.ee.platform.customcomponent.configuration.service.CustomComponentService;
import com.bytechef.ee.platform.customcomponent.file.storage.CustomComponentFileStorage;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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

    @Test
    void testFetchComponentHandlerExcludesDraft() {
        CustomComponent draft = new CustomComponent();

        draft.setName("test");
        draft.setStatus(CustomComponent.Status.DRAFT);

        when(customComponentService.fetchCustomComponent("test", 2)).thenReturn(Optional.of(draft));

        assertThat(registry.fetchComponentHandler("test", 2)).isEmpty();
    }
}
