/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.customcomponent.configuration.audit.CustomComponentAuditEvent;
import com.bytechef.ee.platform.customcomponent.configuration.audit.CustomComponentAuditPublisher;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent;
import com.bytechef.ee.platform.customcomponent.configuration.exception.CustomComponentErrorType;
import com.bytechef.ee.platform.customcomponent.configuration.repository.CustomComponentRepository;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.file.storage.domain.FileEntry;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CustomComponentServiceTest {

    private final CustomComponentAuditPublisher customComponentAuditPublisher =
        mock(CustomComponentAuditPublisher.class);
    private final CustomComponentRepository customComponentRepository = mock(CustomComponentRepository.class);
    private final CustomComponentServiceImpl customComponentService = new CustomComponentServiceImpl(
        customComponentAuditPublisher, customComponentRepository);

    @Test
    void testPublishCustomComponentFlipsDraftAndStampsPublishedDate() {
        CustomComponent customComponent = new CustomComponent();

        customComponent.setId(1L);
        customComponent.setName("test");
        customComponent.setStatus(CustomComponent.Status.DRAFT);

        when(customComponentRepository.findById(1L)).thenReturn(Optional.of(customComponent));
        when(customComponentRepository.save(any(CustomComponent.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CustomComponent published = customComponentService.publishCustomComponent(1L);

        assertEquals(CustomComponent.Status.PUBLISHED, published.getStatus());
        assertNotNull(published.getPublishedDate());
        verify(customComponentAuditPublisher).publish(
            eq(CustomComponentAuditEvent.CUSTOM_COMPONENT_PUBLISHED), eq(1L), any());
    }

    @Test
    void testPublishCustomComponentRejectsPublishedRow() {
        CustomComponent customComponent = new CustomComponent();

        customComponent.setId(1L);
        customComponent.setStatus(CustomComponent.Status.PUBLISHED);

        when(customComponentRepository.findById(1L)).thenReturn(Optional.of(customComponent));

        ConfigurationException exception = assertThrows(
            ConfigurationException.class, () -> customComponentService.publishCustomComponent(1L));

        assertEquals(CustomComponentErrorType.COMPONENT_NOT_DRAFT.getErrorKey(), exception.getErrorKey());
        verify(customComponentRepository, never()).save(any());
    }

    @Test
    void testUpdatePersistsComponentFileAndVersionAndStatus() {
        CustomComponent existing = new CustomComponent();

        existing.setId(1L);
        existing.setName("test");
        existing.setComponentVersion(1);
        existing.setStatus(CustomComponent.Status.DRAFT);

        when(customComponentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(customComponentRepository.save(any(CustomComponent.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CustomComponent incoming = new CustomComponent();

        incoming.setId(1L);
        incoming.setComponent(new FileEntry("test_2.js", "base64://x"));
        incoming.setComponentVersion(2);
        incoming.setStatus(CustomComponent.Status.DRAFT);
        incoming.setTitle("Test");

        CustomComponent saved = customComponentService.update(incoming);

        assertEquals(2, saved.getComponentVersion());
        assertNotNull(saved.getComponent());
        assertEquals("Test", saved.getTitle());
    }
}
