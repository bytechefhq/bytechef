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
import com.bytechef.platform.component.service.ComponentDefinitionService;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CustomComponentFacadeUploadStatusTest {

    @Test
    void testUploadCreatesPublishedRow() {
        CustomComponentService customComponentService = mock(CustomComponentService.class);

        when(customComponentService.fetchCustomComponent("example", 1)).thenReturn(Optional.empty());
        when(customComponentService.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);

        when(customComponentFileStorage.storeCustomComponentFile(anyString(), any()))
            .thenReturn(new FileEntry("example_1.js", "file:///example_1.js"));

        CustomComponentFacadeImpl customComponentFacade = new CustomComponentFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), customComponentService,
            customComponentFileStorage, mock(ObjectProvider.class));

        String content =
            "({name: 'example', title: 'Example', description: 'An example', version: 1, actions: []})";

        customComponentFacade.save(content.getBytes(StandardCharsets.UTF_8), Language.JAVASCRIPT);

        ArgumentCaptor<CustomComponent> customComponentCaptor = ArgumentCaptor.forClass(CustomComponent.class);

        verify(customComponentService).create(customComponentCaptor.capture());

        CustomComponent createdCustomComponent = customComponentCaptor.getValue();

        assertThat(createdCustomComponent.getStatus()).isEqualTo(CustomComponent.Status.PUBLISHED);
        assertThat(createdCustomComponent.getPublishedDate()).isNotNull();
    }

    @Test
    void testUploadOntoDraftOwnedVersionRejected() {
        CustomComponent draftCustomComponent = new CustomComponent();

        draftCustomComponent.setId(7L);
        draftCustomComponent.setName("example");
        draftCustomComponent.setComponentVersion(1);
        draftCustomComponent.setStatus(CustomComponent.Status.DRAFT);

        CustomComponentService customComponentService = mock(CustomComponentService.class);

        when(customComponentService.fetchCustomComponent("example", 1))
            .thenReturn(Optional.of(draftCustomComponent));

        CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);

        CustomComponentFacadeImpl customComponentFacade = new CustomComponentFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), customComponentService,
            customComponentFileStorage, mock(ObjectProvider.class));

        String content =
            "({name: 'example', title: 'Example', description: 'An example', version: 1, actions: []})";

        assertThatThrownBy(
            () -> customComponentFacade.save(content.getBytes(StandardCharsets.UTF_8), Language.JAVASCRIPT))
                .isInstanceOf(ConfigurationException.class)
                .extracting(throwable -> ((ConfigurationException) throwable).getErrorKey())
                .isEqualTo(CustomComponentErrorType.VERSION_ALREADY_EXISTS.getErrorKey());

        verify(customComponentFileStorage, never()).storeCustomComponentFile(anyString(), any());
        verify(customComponentService, never()).update(any());
        verify(customComponentService, never()).create(any());
    }

    @Test
    void testUploadOntoPublishedVersionUpdatesInPlace() {
        CustomComponent publishedCustomComponent = new CustomComponent();

        publishedCustomComponent.setId(7L);
        publishedCustomComponent.setName("example");
        publishedCustomComponent.setComponentVersion(1);
        publishedCustomComponent.setStatus(CustomComponent.Status.PUBLISHED);
        publishedCustomComponent.setComponent(new FileEntry("example_1.js", "file:///example_1_old.js"));

        CustomComponentService customComponentService = mock(CustomComponentService.class);

        when(customComponentService.fetchCustomComponent("example", 1))
            .thenReturn(Optional.of(publishedCustomComponent));
        when(customComponentService.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FileEntry newComponentFile = new FileEntry("example_1.js", "file:///example_1_new.js");

        CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);

        when(customComponentFileStorage.storeCustomComponentFile(anyString(), any()))
            .thenReturn(newComponentFile);

        CustomComponentFacadeImpl customComponentFacade = new CustomComponentFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), customComponentService,
            customComponentFileStorage, mock(ObjectProvider.class));

        String content =
            "({name: 'example', title: 'Example', description: 'An example', version: 1, actions: []})";

        customComponentFacade.save(content.getBytes(StandardCharsets.UTF_8), Language.JAVASCRIPT);

        verify(customComponentService).update(publishedCustomComponent);
        verify(customComponentService, never()).create(any());

        assertThat(publishedCustomComponent.getComponent()).isEqualTo(newComponentFile);
        assertThat(publishedCustomComponent.getStatus()).isEqualTo(CustomComponent.Status.PUBLISHED);
    }

    @Test
    void testUploadRejectsComponentNameThatIsNotLettersAndDigits() {
        CustomComponentService customComponentService = mock(CustomComponentService.class);
        CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);

        CustomComponentFacadeImpl customComponentFacade = new CustomComponentFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), customComponentService,
            customComponentFileStorage, mock(ObjectProvider.class));

        String content = "({name: 'acme-tasks', version: 1, actions: []})";

        assertThatThrownBy(
            () -> customComponentFacade.save(content.getBytes(StandardCharsets.UTF_8), Language.JAVASCRIPT))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("acme-tasks")
                .extracting(throwable -> ((ConfigurationException) throwable).getErrorKey())
                .isEqualTo(CustomComponentErrorType.INVALID_COMPONENT_NAME.getErrorKey());

        verify(customComponentFileStorage, never()).storeCustomComponentFile(anyString(), any());
    }

    @Test
    void testUploadRejectsActionNameThatIsNotLettersAndDigits() {
        CustomComponentService customComponentService = mock(CustomComponentService.class);
        CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);

        CustomComponentFacadeImpl customComponentFacade = new CustomComponentFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), customComponentService,
            customComponentFileStorage, mock(ObjectProvider.class));

        String content =
            "({name: 'acmeTasks', version: 1, actions: [{name: 'create_task', perform: function() { return 1; }}]})";

        assertThatThrownBy(
            () -> customComponentFacade.save(content.getBytes(StandardCharsets.UTF_8), Language.JAVASCRIPT))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("action name 'create_task'")
                .extracting(throwable -> ((ConfigurationException) throwable).getErrorKey())
                .isEqualTo(CustomComponentErrorType.INVALID_COMPONENT_NAME.getErrorKey());

        verify(customComponentFileStorage, never()).storeCustomComponentFile(anyString(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUploadRejectsBuiltInComponentNameUnlessACustomComponentAlreadyOwnsIt() {
        ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);

        when(componentDefinitionService.hasComponentDefinition("example", null)).thenReturn(true);

        ObjectProvider<ComponentDefinitionService> componentDefinitionServiceProvider = mock(ObjectProvider.class);

        when(componentDefinitionServiceProvider.getIfAvailable()).thenReturn(componentDefinitionService);

        CustomComponentService customComponentService = mock(CustomComponentService.class);
        CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);

        CustomComponentFacadeImpl customComponentFacade = new CustomComponentFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), customComponentService,
            customComponentFileStorage, componentDefinitionServiceProvider);

        byte[] content = "({name: 'example', version: 2, actions: []})".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> customComponentFacade.save(content, Language.JAVASCRIPT))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("built-in component");

        CustomComponent publishedCustomComponent = new CustomComponent();

        publishedCustomComponent.setName("example");
        publishedCustomComponent.setComponentVersion(1);
        publishedCustomComponent.setStatus(CustomComponent.Status.PUBLISHED);

        when(customComponentService.fetchLatestCustomComponent("example"))
            .thenReturn(Optional.of(publishedCustomComponent));
        when(customComponentService.fetchCustomComponent("example", 2)).thenReturn(Optional.empty());
        when(customComponentService.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(customComponentFileStorage.storeCustomComponentFile(anyString(), any()))
            .thenReturn(new FileEntry("example_2.js", "file:///example_2.js"));

        CustomComponent customComponent = customComponentFacade.save(content, Language.JAVASCRIPT);

        assertThat(customComponent.getComponentVersion()).isEqualTo(2);
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
