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
import com.bytechef.ee.platform.customcomponent.configuration.exception.CustomComponentErrorType;
import com.bytechef.ee.platform.customcomponent.configuration.service.CustomComponentService;
import com.bytechef.ee.platform.customcomponent.file.storage.CustomComponentFileStorage;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.file.storage.domain.FileEntry;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
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

    @Test
    void testDraftSaveUpdatesRowInPlaceAndReReadsVersion() {
        FileEntry existingComponentFile = new FileEntry("example_1.js", "file:///example_1.js");

        CustomComponent customComponent = new CustomComponent();

        customComponent.setId(42L);
        customComponent.setLanguage(Language.JAVASCRIPT);
        customComponent.setName("example");
        customComponent.setComponentVersion(1);
        customComponent.setComponent(existingComponentFile);
        customComponent.setStatus(CustomComponent.Status.DRAFT);

        CustomComponentService customComponentService = mock(CustomComponentService.class);

        when(customComponentService.getCustomComponent(42L)).thenReturn(customComponent);
        when(customComponentService.fetchCustomComponent("example", 2)).thenReturn(Optional.empty());
        when(customComponentService.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FileEntry newComponentFile = new FileEntry("example_2.js", "file:///example_2.js");

        CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);

        when(customComponentFileStorage.storeCustomComponentFile(anyString(), any()))
            .thenReturn(newComponentFile);

        CustomComponentFacadeImpl customComponentFacade = new CustomComponentFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), customComponentService,
            customComponentFileStorage);

        String content =
            "({name: 'example', title: 'Example', description: 'An example', version: 2, actions: []})";

        CustomComponent result = customComponentFacade.updateCustomComponentSource(42L, content);

        verify(customComponentFileStorage)
            .storeCustomComponentFile("example_2.js", content.getBytes(StandardCharsets.UTF_8));
        verify(customComponentService).update(customComponent);
        verify(customComponentService, never()).create(any());
        verify(customComponentFileStorage).deleteCustomComponentFile(existingComponentFile);

        assertThat(result.getComponentVersion()).isEqualTo(2);
        assertThat(result.getComponent()).isEqualTo(newComponentFile);
    }

    @Test
    void testDraftSaveAtSameVersionDeletesOldBlobEvenWhenNameUnchanged() {
        FileEntry existingComponentFile = new FileEntry("example_1.js", "file:///example_1.js");

        CustomComponent customComponent = new CustomComponent();

        customComponent.setId(42L);
        customComponent.setLanguage(Language.JAVASCRIPT);
        customComponent.setName("example");
        customComponent.setComponentVersion(1);
        customComponent.setComponent(existingComponentFile);
        customComponent.setStatus(CustomComponent.Status.DRAFT);

        CustomComponentService customComponentService = mock(CustomComponentService.class);

        when(customComponentService.getCustomComponent(42L)).thenReturn(customComponent);
        when(customComponentService.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Same logical name as the existing file, but a distinct URL -- file storage providers use
        // generateFilename=true, so every store writes a NEW physical blob even when the name is identical.
        FileEntry newComponentFile = new FileEntry("example_1.js", "file:///example_1_v2.js");

        CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);

        when(customComponentFileStorage.storeCustomComponentFile(anyString(), any()))
            .thenReturn(newComponentFile);

        CustomComponentFacadeImpl customComponentFacade = new CustomComponentFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), customComponentService,
            customComponentFileStorage);

        String content =
            "({name: 'example', title: 'Example', description: 'An example', version: 1, actions: []})";

        CustomComponent result = customComponentFacade.updateCustomComponentSource(42L, content);

        verify(customComponentFileStorage)
            .storeCustomComponentFile("example_1.js", content.getBytes(StandardCharsets.UTF_8));
        verify(customComponentService).update(customComponent);
        verify(customComponentFileStorage).deleteCustomComponentFile(existingComponentFile);

        assertThat(result.getComponentVersion()).isEqualTo(1);
        assertThat(result.getComponent()).isEqualTo(newComponentFile);
    }

    @Test
    void testDraftSaveVersionCollisionRejected() {
        CustomComponent customComponent = new CustomComponent();

        customComponent.setId(42L);
        customComponent.setLanguage(Language.JAVASCRIPT);
        customComponent.setName("example");
        customComponent.setComponentVersion(1);
        customComponent.setComponent(new FileEntry("example_1.js", "file:///example_1.js"));
        customComponent.setStatus(CustomComponent.Status.DRAFT);

        CustomComponent otherCustomComponent = new CustomComponent();

        otherCustomComponent.setId(99L);

        CustomComponentService customComponentService = mock(CustomComponentService.class);

        when(customComponentService.getCustomComponent(42L)).thenReturn(customComponent);
        when(customComponentService.fetchCustomComponent("example", 3))
            .thenReturn(Optional.of(otherCustomComponent));

        CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);

        CustomComponentFacadeImpl customComponentFacade = new CustomComponentFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), customComponentService,
            customComponentFileStorage);

        String content = "({name: 'example', version: 3, actions: []})";

        assertThatThrownBy(() -> customComponentFacade.updateCustomComponentSource(42L, content))
            .isInstanceOf(ConfigurationException.class)
            .extracting(throwable -> ((ConfigurationException) throwable).getErrorKey())
            .isEqualTo(CustomComponentErrorType.VERSION_ALREADY_EXISTS.getErrorKey());

        verify(customComponentFileStorage, never()).storeCustomComponentFile(anyString(), any());
        verify(customComponentService, never()).update(any());
        verify(customComponentService, never()).create(any());
    }

    @Test
    void testPublishedSaveSpawnsNewDraftRow() {
        CustomComponent publishedCustomComponent = new CustomComponent();

        publishedCustomComponent.setId(42L);
        publishedCustomComponent.setLanguage(Language.JAVASCRIPT);
        publishedCustomComponent.setName("example");
        publishedCustomComponent.setComponentVersion(1);
        publishedCustomComponent.setComponent(new FileEntry("example_1.js", "file:///example_1.js"));
        publishedCustomComponent.setStatus(CustomComponent.Status.PUBLISHED);

        CustomComponentService customComponentService = mock(CustomComponentService.class);

        when(customComponentService.getCustomComponent(42L)).thenReturn(publishedCustomComponent);
        when(customComponentService.fetchDraftCustomComponent("example")).thenReturn(Optional.empty());
        when(customComponentService.fetchLatestCustomComponent("example"))
            .thenReturn(Optional.of(publishedCustomComponent));
        when(customComponentService.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FileEntry newComponentFile = new FileEntry("example_2.js", "file:///example_2.js");

        CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);

        when(customComponentFileStorage.storeCustomComponentFile(anyString(), any()))
            .thenReturn(newComponentFile);

        CustomComponentFacadeImpl customComponentFacade = new CustomComponentFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), customComponentService,
            customComponentFileStorage);

        String content = "({name: 'example', version: 2, actions: []})";

        CustomComponent result = customComponentFacade.updateCustomComponentSource(42L, content);

        ArgumentCaptor<CustomComponent> customComponentCaptor = ArgumentCaptor.forClass(CustomComponent.class);

        verify(customComponentService).create(customComponentCaptor.capture());
        verify(customComponentService, never()).update(any());

        CustomComponent createdCustomComponent = customComponentCaptor.getValue();

        assertThat(createdCustomComponent.getStatus()).isEqualTo(CustomComponent.Status.DRAFT);
        assertThat(createdCustomComponent.getComponentVersion()).isEqualTo(2);
        assertThat(createdCustomComponent.isEnabled()).isTrue();
        assertThat(result).isEqualTo(createdCustomComponent);
    }

    @Test
    void testPublishedSaveWithoutVersionBumpRejected() {
        CustomComponent publishedCustomComponent = new CustomComponent();

        publishedCustomComponent.setId(42L);
        publishedCustomComponent.setLanguage(Language.JAVASCRIPT);
        publishedCustomComponent.setName("example");
        publishedCustomComponent.setComponentVersion(1);
        publishedCustomComponent.setComponent(new FileEntry("example_1.js", "file:///example_1.js"));
        publishedCustomComponent.setStatus(CustomComponent.Status.PUBLISHED);

        CustomComponentService customComponentService = mock(CustomComponentService.class);

        when(customComponentService.getCustomComponent(42L)).thenReturn(publishedCustomComponent);
        when(customComponentService.fetchDraftCustomComponent("example")).thenReturn(Optional.empty());
        when(customComponentService.fetchLatestCustomComponent("example"))
            .thenReturn(Optional.of(publishedCustomComponent));

        CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);

        CustomComponentFacadeImpl customComponentFacade = new CustomComponentFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), customComponentService,
            customComponentFileStorage);

        String content = "({name: 'example', version: 1, actions: []})";

        assertThatThrownBy(() -> customComponentFacade.updateCustomComponentSource(42L, content))
            .isInstanceOf(ConfigurationException.class)
            .extracting(throwable -> ((ConfigurationException) throwable).getErrorKey())
            .isEqualTo(CustomComponentErrorType.VERSION_NOT_BUMPED.getErrorKey());

        verify(customComponentService, never()).create(any());
        verify(customComponentService, never()).update(any());
        verify(customComponentFileStorage, never()).storeCustomComponentFile(anyString(), any());
    }

    @Test
    void testPublishedSaveWithExistingDraftRejected() {
        CustomComponent publishedCustomComponent = new CustomComponent();

        publishedCustomComponent.setId(42L);
        publishedCustomComponent.setLanguage(Language.JAVASCRIPT);
        publishedCustomComponent.setName("example");
        publishedCustomComponent.setComponentVersion(1);
        publishedCustomComponent.setComponent(new FileEntry("example_1.js", "file:///example_1.js"));
        publishedCustomComponent.setStatus(CustomComponent.Status.PUBLISHED);

        CustomComponent draftCustomComponent = new CustomComponent();

        draftCustomComponent.setId(43L);
        draftCustomComponent.setComponentVersion(2);
        draftCustomComponent.setStatus(CustomComponent.Status.DRAFT);

        CustomComponentService customComponentService = mock(CustomComponentService.class);

        when(customComponentService.getCustomComponent(42L)).thenReturn(publishedCustomComponent);
        when(customComponentService.fetchDraftCustomComponent("example"))
            .thenReturn(Optional.of(draftCustomComponent));

        CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);

        CustomComponentFacadeImpl customComponentFacade = new CustomComponentFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), customComponentService,
            customComponentFileStorage);

        String content = "({name: 'example', version: 3, actions: []})";

        assertThatThrownBy(() -> customComponentFacade.updateCustomComponentSource(42L, content))
            .isInstanceOf(ConfigurationException.class)
            .extracting(throwable -> ((ConfigurationException) throwable).getErrorKey())
            .isEqualTo(CustomComponentErrorType.DRAFT_ALREADY_EXISTS.getErrorKey());

        verify(customComponentService, never()).create(any());
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
