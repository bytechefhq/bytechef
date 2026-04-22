/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.gateway.domain.AiPromptVersion;
import com.bytechef.ee.automation.ai.gateway.domain.AiPromptVersionType;
import com.bytechef.ee.automation.ai.gateway.repository.AiPromptVersionRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link AiPromptVersionServiceImpl}. Focuses on the {@code setActiveVersion} invariant — only one
 * version per {@code (promptId, environment)} may be active — and the validation guards on {@code create} / {@code
 * update}.
 *
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiPromptVersionServiceTest {

    @Mock
    private AiPromptVersionRepository aiPromptVersionRepository;

    private AiPromptVersionServiceImpl aiPromptVersionService;

    @BeforeEach
    void setUp() {
        aiPromptVersionService = new AiPromptVersionServiceImpl(aiPromptVersionRepository);
    }

    @Test
    void testSetActiveVersionDeactivatesSiblingsAndActivatesTarget() {
        AiPromptVersion targetVersion = newVersion(100L, 77L, 3);
        AiPromptVersion currentlyActiveVersion = newVersion(99L, 77L, 2);

        currentlyActiveVersion.setEnvironment("prod");
        currentlyActiveVersion.setActive(true);

        when(aiPromptVersionRepository.findById(100L)).thenReturn(Optional.of(targetVersion));
        when(aiPromptVersionRepository.findAllByPromptIdAndEnvironmentAndActive(77L, "prod", true))
            .thenReturn(List.of(currentlyActiveVersion));
        when(aiPromptVersionRepository.save(any(AiPromptVersion.class))).thenAnswer(
            invocation -> invocation.getArgument(0));

        aiPromptVersionService.setActiveVersion(100L, "prod");

        assertThat(currentlyActiveVersion.isActive())
            .as("the prior active version must be deactivated so no two versions share (promptId, environment)")
            .isFalse();
        assertThat(targetVersion.isActive()).isTrue();
        assertThat(targetVersion.getEnvironment()).isEqualTo("prod");

        // Both versions saved — one deactivated, one activated.
        verify(aiPromptVersionRepository, times(2)).save(any(AiPromptVersion.class));
    }

    @Test
    void testSetActiveVersionThrowsWhenTargetMissing() {
        when(aiPromptVersionRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiPromptVersionService.setActiveVersion(404L, "prod"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("AiPromptVersion not found with id: 404");

        verify(aiPromptVersionRepository, never()).save(any());
    }

    @Test
    void testCreateRejectsVersionWithAssignedId() {
        AiPromptVersion versionWithId = newVersion(555L, 77L, 1);

        assertThatThrownBy(() -> aiPromptVersionService.create(versionWithId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("promptVersion id must be null");

        verify(aiPromptVersionRepository, never()).save(any());
    }

    @Test
    void testUpdateRequiresId() {
        AiPromptVersion versionWithoutId = new AiPromptVersion(
            77L, 1, AiPromptVersionType.TEXT, "hello", "tester");

        assertThatThrownBy(() -> aiPromptVersionService.update(versionWithoutId))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("promptVersion id must not be null");

        verify(aiPromptVersionRepository, never()).save(any());
    }

    private static AiPromptVersion newVersion(long id, long promptId, int versionNumber) {
        AiPromptVersion version = new AiPromptVersion(
            promptId, versionNumber, AiPromptVersionType.TEXT, "hello {{name}}", "tester");

        try {
            Field idField = AiPromptVersion.class.getDeclaredField("id");

            idField.setAccessible(true);
            idField.set(version, id);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new AssertionError("failed to seed id", reflectiveOperationException);
        }

        return version;
    }
}
