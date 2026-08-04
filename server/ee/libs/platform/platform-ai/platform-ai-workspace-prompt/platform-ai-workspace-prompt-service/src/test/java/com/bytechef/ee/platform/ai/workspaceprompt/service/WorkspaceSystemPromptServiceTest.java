/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.workspaceprompt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.workspaceprompt.domain.WorkspaceSystemPrompt;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class WorkspaceSystemPromptServiceTest {

    private final PropertyService propertyService = mock(PropertyService.class);
    private final WorkspaceSystemPromptServiceImpl service = new WorkspaceSystemPromptServiceImpl(propertyService);

    @Test
    void testFetchReturnsStoredPrompt() {
        Property property = new Property();

        property.setValue(Map.of("prompt", "Always answer in German."));

        when(propertyService.fetchProperty(
            WorkspaceSystemPrompt.PROPERTY_KEY, Property.Scope.WORKSPACE, 7L))
                .thenReturn(Optional.of(property));

        assertThat(service.fetchWorkspaceSystemPrompt(7L)).contains("Always answer in German.");
    }

    @Test
    void testFetchReturnsEmptyWhenNoRow() {
        when(propertyService.fetchProperty(
            WorkspaceSystemPrompt.PROPERTY_KEY, Property.Scope.WORKSPACE, 7L))
                .thenReturn(Optional.empty());

        assertThat(service.fetchWorkspaceSystemPrompt(7L)).isEmpty();
    }

    @Test
    void testSaveStripsAndStoresPrompt() {
        Optional<String> saved = service.saveWorkspaceSystemPrompt(7L, "  Be concise.  ");

        assertThat(saved).contains("Be concise.");

        verify(propertyService).save(
            eq(WorkspaceSystemPrompt.PROPERTY_KEY), eq(Map.of("prompt", "Be concise.")),
            eq(Property.Scope.WORKSPACE), eq(7L));
    }

    @Test
    void testSaveBlankDeletesRow() {
        assertThat(service.saveWorkspaceSystemPrompt(7L, "   ")).isEmpty();
        assertThat(service.saveWorkspaceSystemPrompt(7L, null)).isEmpty();

        verify(propertyService, org.mockito.Mockito.times(2))
            .delete(WorkspaceSystemPrompt.PROPERTY_KEY, Property.Scope.WORKSPACE, 7L);
    }

    @Test
    void testSaveRejectsOverLengthPrompt() {
        String tooLong = "x".repeat(WorkspaceSystemPrompt.MAX_LENGTH + 1);

        assertThatThrownBy(() -> service.saveWorkspaceSystemPrompt(7L, tooLong))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
