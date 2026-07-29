/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent.Language;
import com.bytechef.ee.platform.customcomponent.configuration.exception.CustomComponentErrorType;
import com.bytechef.ee.platform.customcomponent.configuration.service.CustomComponentService;
import com.bytechef.ee.platform.customcomponent.file.storage.CustomComponentFileStorage;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.file.storage.domain.FileEntry;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.CacheManager;

/**
 * Verifies that {@link CustomComponentFacadeImpl#createEmptyCustomComponent} reads the JavaScript starter template,
 * substitutes the requested name, loads the result through the real component loader, and persists it. This exercises
 * the real polyglot loader (no mocking of the load boundary) so a starter template that fails to evaluate would fail
 * this test, not just an IntTest.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class CustomComponentFacadeCreateEmptyTest {

    @Test
    void testCreateEmptyCustomComponentLoadsTemplateAndPersistsForJavascript() {
        CustomComponentService customComponentService = mock(CustomComponentService.class);

        when(customComponentService.fetchCustomComponent("acme", 1))
            .thenReturn(Optional.empty());
        when(customComponentService.create(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);

        when(customComponentFileStorage.storeCustomComponentFile(anyString(), any()))
            .thenReturn(new FileEntry("acme_1.js", "file:///acme_1.js"));

        CustomComponentFacadeImpl customComponentFacade = new CustomComponentFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), customComponentService,
            customComponentFileStorage);

        CustomComponent customComponent = customComponentFacade.createEmptyCustomComponent(
            "acme", Language.JAVASCRIPT);

        ArgumentCaptor<CustomComponent> customComponentCaptor = ArgumentCaptor.forClass(CustomComponent.class);

        verify(customComponentService).create(customComponentCaptor.capture());

        CustomComponent createdCustomComponent = customComponentCaptor.getValue();

        assertThat(createdCustomComponent.getName()).isEqualTo("acme");
        assertThat(createdCustomComponent.getLanguage()).isEqualTo(Language.JAVASCRIPT);
        assertThat(createdCustomComponent.getTitle()).isEqualTo("acme");
        assertThat(createdCustomComponent.getComponentVersion()).isEqualTo(1);
        assertThat(customComponent.getName()).isEqualTo("acme");
    }

    @ParameterizedTest
    @EnumSource(value = Language.class, names = "JAVASCRIPT", mode = EnumSource.Mode.EXCLUDE)
    void testCreateEmptyCustomComponentRejectsNonJavascriptLanguages(Language language) {
        CustomComponentService customComponentService = mock(CustomComponentService.class);

        CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);

        CustomComponentFacadeImpl customComponentFacade = new CustomComponentFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), customComponentService,
            customComponentFileStorage);

        assertThatThrownBy(() -> customComponentFacade.createEmptyCustomComponent("acme", language))
            .isInstanceOf(ConfigurationException.class);

        verify(customComponentService, never()).create(any());
    }

    @Test
    void testCreateEmptyCustomComponentRejectsDuplicateName() {
        CustomComponentService customComponentService = mock(CustomComponentService.class);

        when(customComponentService.fetchCustomComponent("acme", 1))
            .thenReturn(Optional.of(new CustomComponent()));

        CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);

        CustomComponentFacadeImpl customComponentFacade = new CustomComponentFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), customComponentService,
            customComponentFileStorage);

        assertThatThrownBy(() -> customComponentFacade.createEmptyCustomComponent("acme", Language.JAVASCRIPT))
            .isInstanceOf(ConfigurationException.class);

        verify(customComponentService, never()).create(any());
        verify(customComponentFileStorage, never()).storeCustomComponentFile(anyString(), any());
    }

    @Test
    void testCreateEmptyCustomComponentRejectsNameContainingQuote() {
        CustomComponentService customComponentService = mock(CustomComponentService.class);

        CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);

        CustomComponentFacadeImpl customComponentFacade = new CustomComponentFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), customComponentService,
            customComponentFileStorage);

        assertThatThrownBy(
            () -> customComponentFacade.createEmptyCustomComponent("acme\"; alert(1); //", Language.JAVASCRIPT))
                .isExactlyInstanceOf(ConfigurationException.class)
                .extracting(throwable -> ((ConfigurationException) throwable).getErrorKey())
                .isEqualTo(CustomComponentErrorType.INVALID_COMPONENT_NAME.getErrorKey());

        verify(customComponentService, never()).create(any());
        verify(customComponentFileStorage, never()).storeCustomComponentFile(anyString(), any());
    }

    @Test
    void testCreateEmptyCustomComponentRejectsBlankName() {
        CustomComponentService customComponentService = mock(CustomComponentService.class);

        CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);

        CustomComponentFacadeImpl customComponentFacade = new CustomComponentFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), customComponentService,
            customComponentFileStorage);

        assertThatThrownBy(() -> customComponentFacade.createEmptyCustomComponent("   ", Language.JAVASCRIPT))
            .isInstanceOf(ConfigurationException.class)
            .extracting(throwable -> ((ConfigurationException) throwable).getErrorKey())
            .isEqualTo(CustomComponentErrorType.INVALID_COMPONENT_NAME.getErrorKey());

        verify(customComponentService, never()).create(any());
        verify(customComponentFileStorage, never()).storeCustomComponentFile(anyString(), any());
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
