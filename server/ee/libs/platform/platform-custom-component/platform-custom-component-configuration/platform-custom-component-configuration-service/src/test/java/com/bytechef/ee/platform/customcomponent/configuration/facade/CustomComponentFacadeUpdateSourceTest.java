/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent.Language;
import com.bytechef.ee.platform.customcomponent.configuration.service.CustomComponentService;
import com.bytechef.ee.platform.customcomponent.file.storage.CustomComponentFileStorage;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.file.storage.domain.FileEntry;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.CacheManager;

/**
 * Verifies that {@link CustomComponentFacadeImpl#updateCustomComponentSource} acts as a compile-gate: the edited source
 * is reloaded through the real component loader before anything is persisted, so a source edit that fails to evaluate
 * or that renames the component is rejected without overwriting the stored file.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class CustomComponentFacadeUpdateSourceTest {

    @Test
    void testUpdateCustomComponentSourceStoresFileAndRefreshesMetadataForValidContent() {
        FileEntry existingComponentFile = new FileEntry("example_1.js", "file:///example_1.js");

        CustomComponent customComponent = new CustomComponent();

        customComponent.setLanguage(Language.JAVASCRIPT);
        customComponent.setName("example");
        customComponent.setComponent(existingComponentFile);

        CustomComponentService customComponentService = mock(CustomComponentService.class);

        when(customComponentService.getCustomComponent(42L)).thenReturn(customComponent);

        FileEntry newComponentFile = new FileEntry("example_1.js", "file:///example_1_new.js");

        CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);

        when(customComponentFileStorage.storeCustomComponentFile(anyString(), any()))
            .thenReturn(newComponentFile);

        CustomComponentFacadeImpl customComponentFacade = new CustomComponentFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), customComponentService,
            customComponentFileStorage);

        String content = "({name: 'example', title: 'Example', description: 'An example', version: 1, actions: []})";

        customComponentFacade.updateCustomComponentSource(42L, content);

        verify(customComponentFileStorage)
            .storeCustomComponentFile("example_1.js", content.getBytes(StandardCharsets.UTF_8));
        verify(customComponentService).update(customComponent);

        assertThat(customComponent.getComponent()).isEqualTo(newComponentFile);
        assertThat(customComponent.getTitle()).isEqualTo("Example");
        assertThat(customComponent.getDescription()).isEqualTo("An example");
    }

    @Test
    void testStoredFileNameContainsPlainVersionNumber() {
        FileEntry existingComponentFile = new FileEntry("example_1.js", "file:///example_1.js");

        CustomComponent customComponent = new CustomComponent();

        customComponent.setLanguage(Language.JAVASCRIPT);
        customComponent.setName("example");
        customComponent.setComponent(existingComponentFile);

        CustomComponentService customComponentService = mock(CustomComponentService.class);

        when(customComponentService.getCustomComponent(42L)).thenReturn(customComponent);

        FileEntry newComponentFile = new FileEntry("example_1.js", "file:///example_1_new.js");

        CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);

        when(customComponentFileStorage.storeCustomComponentFile(anyString(), any()))
            .thenReturn(newComponentFile);

        CustomComponentFacadeImpl customComponentFacade = new CustomComponentFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), customComponentService,
            customComponentFileStorage);

        String content = "({name: 'example', title: 'Example', description: 'An example', version: 1, actions: []})";

        ArgumentCaptor<String> fileNameCaptor = ArgumentCaptor.forClass(String.class);

        customComponentFacade.updateCustomComponentSource(42L, content);

        verify(customComponentFileStorage).storeCustomComponentFile(fileNameCaptor.capture(), any());

        assertEquals("example_1.js", fileNameCaptor.getValue());
    }

    @Test
    void testUpdateCustomComponentSourceRejectsJavaLanguage() {
        CustomComponent customComponent = new CustomComponent();

        customComponent.setLanguage(Language.JAVA);
        customComponent.setName("example");

        CustomComponentService customComponentService = mock(CustomComponentService.class);

        when(customComponentService.getCustomComponent(42L)).thenReturn(customComponent);

        CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);

        CustomComponentFacadeImpl customComponentFacade = new CustomComponentFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), customComponentService,
            customComponentFileStorage);

        assertThatThrownBy(() -> customComponentFacade.updateCustomComponentSource(42L, "anything"))
            .isInstanceOf(ConfigurationException.class);

        verify(customComponentFileStorage, never()).storeCustomComponentFile(anyString(), any());
    }

    @Test
    void testUpdateCustomComponentSourcePropagatesAndDoesNotStoreWhenSourceFailsToLoad() {
        CustomComponent customComponent = new CustomComponent();

        customComponent.setLanguage(Language.JAVASCRIPT);
        customComponent.setName("example");
        customComponent.setComponent(new FileEntry("example_1.js", "file:///example_1.js"));

        CustomComponentService customComponentService = mock(CustomComponentService.class);

        when(customComponentService.getCustomComponent(42L)).thenReturn(customComponent);

        CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);

        CustomComponentFacadeImpl customComponentFacade = new CustomComponentFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), customComponentService,
            customComponentFileStorage);

        assertThatThrownBy(() -> customComponentFacade.updateCustomComponentSource(42L, "this is not valid js {{{"))
            .isInstanceOf(RuntimeException.class);

        verify(customComponentFileStorage, never()).storeCustomComponentFile(anyString(), any());
        verify(customComponentService, never()).update(any());
    }

    @Test
    void testUpdateCustomComponentSourceRejectsRenameAndDoesNotStore() {
        CustomComponent customComponent = new CustomComponent();

        customComponent.setLanguage(Language.JAVASCRIPT);
        customComponent.setName("example");
        customComponent.setComponent(new FileEntry("example_1.js", "file:///example_1.js"));

        CustomComponentService customComponentService = mock(CustomComponentService.class);

        when(customComponentService.getCustomComponent(42L)).thenReturn(customComponent);

        CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);

        CustomComponentFacadeImpl customComponentFacade = new CustomComponentFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), customComponentService,
            customComponentFileStorage);

        String content = "({name: 'renamed', version: 1, actions: []})";

        assertThatThrownBy(() -> customComponentFacade.updateCustomComponentSource(42L, content))
            .isInstanceOf(ConfigurationException.class);

        verify(customComponentFileStorage, never()).storeCustomComponentFile(anyString(), any());
        verify(customComponentService, never()).update(any());
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
