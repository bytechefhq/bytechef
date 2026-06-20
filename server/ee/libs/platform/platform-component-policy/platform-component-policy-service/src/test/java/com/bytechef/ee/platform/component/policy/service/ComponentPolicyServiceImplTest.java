/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.component.policy.ComponentPolicy;
import com.bytechef.ee.platform.component.policy.repository.ComponentPolicyRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ComponentPolicyServiceImplTest {

    private final ComponentPolicyRepository componentPolicyRepository = mock(ComponentPolicyRepository.class);
    private final ComponentPolicyServiceImpl componentPolicyService =
        new ComponentPolicyServiceImpl(componentPolicyRepository);

    @Test
    void testIsEnabledDefaultsToTrueWhenNoRow() {
        when(componentPolicyRepository.findById("slack")).thenReturn(Optional.empty());

        assertThat(componentPolicyService.isEnabled("slack")).isTrue();
    }

    @Test
    void testIsEnabledReflectsStoredRow() {
        ComponentPolicy componentPolicy = new ComponentPolicy("slack");

        componentPolicy.setEnabled(false);

        when(componentPolicyRepository.findById("slack")).thenReturn(Optional.of(componentPolicy));

        assertThat(componentPolicyService.isEnabled("slack")).isFalse();
    }

    @Test
    void testGetDisabledComponentNames() {
        ComponentPolicy componentPolicy = new ComponentPolicy("slack");

        componentPolicy.setEnabled(false);

        when(componentPolicyRepository.findByEnabled(false)).thenReturn(List.of(componentPolicy));

        assertThat(componentPolicyService.getDisabledComponentNames()).containsExactly("slack");
    }

    @Test
    void testUpdateComponentPolicyUpserts() {
        when(componentPolicyRepository.findById("slack")).thenReturn(Optional.empty());
        when(componentPolicyRepository.save(any(ComponentPolicy.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ComponentPolicy result = componentPolicyService.updateComponentPolicy("slack", false);

        assertThat(result.getComponentName()).isEqualTo("slack");
        assertThat(result.isEnabled()).isFalse();
    }
}
