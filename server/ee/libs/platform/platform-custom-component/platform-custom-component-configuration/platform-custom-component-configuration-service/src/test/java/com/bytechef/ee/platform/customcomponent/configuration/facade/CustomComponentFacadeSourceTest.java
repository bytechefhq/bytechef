/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent.Language;
import com.bytechef.ee.platform.customcomponent.configuration.service.CustomComponentService;
import com.bytechef.ee.platform.customcomponent.file.storage.CustomComponentFileStorage;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.file.storage.domain.FileEntry;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

/**
 * Verifies that {@link CustomComponentFacadeImpl#getCustomComponentSource} returns the stored source text for non-Java
 * custom components, and rejects Java custom components since compiled jars have no editable source.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class CustomComponentFacadeSourceTest {

    @Test
    void testGetCustomComponentSourceReturnsSourceForNonJavaLanguage() {
        FileEntry componentFile = new FileEntry("example.js", "file:///example.js");

        CustomComponent customComponent = new CustomComponent();

        customComponent.setLanguage(Language.JAVASCRIPT);
        customComponent.setComponent(componentFile);

        CustomComponentService customComponentService = mock(CustomComponentService.class);

        when(customComponentService.getCustomComponent(42L)).thenReturn(customComponent);

        CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);

        when(customComponentFileStorage.readCustomComponentFileContent(componentFile)).thenReturn("({name:'x'})");

        CustomComponentFacadeImpl customComponentFacade = new CustomComponentFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), customComponentService,
            customComponentFileStorage);

        String source = customComponentFacade.getCustomComponentSource(42L);

        assertThat(source).isEqualTo("({name:'x'})");
    }

    @Test
    void testGetCustomComponentSourceThrowsForJavaLanguage() {
        FileEntry componentFile = new FileEntry("example.jar", "file:///example.jar");

        CustomComponent customComponent = new CustomComponent();

        customComponent.setLanguage(Language.JAVA);
        customComponent.setComponent(componentFile);

        CustomComponentService customComponentService = mock(CustomComponentService.class);

        when(customComponentService.getCustomComponent(42L)).thenReturn(customComponent);

        CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);

        CustomComponentFacadeImpl customComponentFacade = new CustomComponentFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), customComponentService,
            customComponentFileStorage);

        assertThatThrownBy(() -> customComponentFacade.getCustomComponentSource(42L))
            .isInstanceOf(ConfigurationException.class);
    }

    private static ApplicationProperties applicationProperties(boolean javaEnabled) {
        ApplicationProperties.Component component = new ApplicationProperties.Component();

        component.getCustomComponent()
            .setJavaEnabled(javaEnabled);

        ApplicationProperties applicationProperties = mock(ApplicationProperties.class);

        when(applicationProperties.getComponent()).thenReturn(component);

        return applicationProperties;
    }
}
