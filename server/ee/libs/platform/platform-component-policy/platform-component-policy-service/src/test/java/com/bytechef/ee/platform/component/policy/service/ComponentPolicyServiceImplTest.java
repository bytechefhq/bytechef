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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.component.policy.ComponentOperationPolicy;
import com.bytechef.ee.platform.component.policy.ComponentPolicy;
import com.bytechef.ee.platform.component.policy.repository.ComponentOperationPolicyRepository;
import com.bytechef.ee.platform.component.policy.repository.ComponentPolicyRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ComponentPolicyServiceImplTest {

    private final ComponentOperationPolicyRepository componentOperationPolicyRepository =
        mock(ComponentOperationPolicyRepository.class);
    private final ComponentPolicyRepository componentPolicyRepository = mock(ComponentPolicyRepository.class);
    private final ComponentPolicyServiceImpl componentPolicyService =
        new ComponentPolicyServiceImpl(componentPolicyRepository, componentOperationPolicyRepository);

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

    @Test
    void testUpdateComponentOperationPolicyDisableInsertsRow() {
        when(componentOperationPolicyRepository.findByComponentNameAndOperationTypeAndOperationName(
            "slack", 0, "sendMessage")).thenReturn(Optional.empty());

        componentPolicyService.updateComponentOperationPolicy(
            "slack", ComponentOperationPolicy.OperationType.ACTION, "sendMessage", false);

        verify(componentOperationPolicyRepository).save(any(ComponentOperationPolicy.class));
    }

    @Test
    void testUpdateComponentOperationPolicyDisableIsIdempotent() {
        when(componentOperationPolicyRepository.findByComponentNameAndOperationTypeAndOperationName(
            "slack", 0, "sendMessage")).thenReturn(Optional.of(
                new ComponentOperationPolicy("slack", ComponentOperationPolicy.OperationType.ACTION, "sendMessage")));

        componentPolicyService.updateComponentOperationPolicy(
            "slack", ComponentOperationPolicy.OperationType.ACTION, "sendMessage", false);

        verify(componentOperationPolicyRepository, never()).save(any());
    }

    @Test
    void testUpdateComponentOperationPolicyDisableTreatsConcurrentDuplicateInsertAsIdempotent() {
        when(componentOperationPolicyRepository.findByComponentNameAndOperationTypeAndOperationName(
            "slack", 0, "sendMessage")).thenReturn(Optional.empty());
        when(componentOperationPolicyRepository.save(any(ComponentOperationPolicy.class)))
            .thenThrow(new DuplicateKeyException("duplicate key"));

        componentPolicyService.updateComponentOperationPolicy(
            "slack", ComponentOperationPolicy.OperationType.ACTION, "sendMessage", false);

        verify(componentOperationPolicyRepository).save(any(ComponentOperationPolicy.class));
    }

    @Test
    void testUpdateComponentOperationPolicyEnableDeletesRow() {
        ComponentOperationPolicy row = new ComponentOperationPolicy(
            "slack", ComponentOperationPolicy.OperationType.ACTION, "sendMessage");

        when(componentOperationPolicyRepository.findByComponentNameAndOperationTypeAndOperationName(
            "slack", 0, "sendMessage")).thenReturn(Optional.of(row));

        componentPolicyService.updateComponentOperationPolicy(
            "slack", ComponentOperationPolicy.OperationType.ACTION, "sendMessage", true);

        verify(componentOperationPolicyRepository).delete(row);
    }

    @Test
    void testGetDisabledOperationKeys() {
        when(componentOperationPolicyRepository.findAll()).thenReturn(List.of(
            new ComponentOperationPolicy("slack", ComponentOperationPolicy.OperationType.ACTION, "sendMessage")));

        assertThat(componentPolicyService.getDisabledOperationKeys()).containsExactly("slack#ACTION#sendMessage");
    }

    @Test
    void testGetComponentOperationPolicies() {
        ComponentOperationPolicy policy = new ComponentOperationPolicy(
            "slack", ComponentOperationPolicy.OperationType.ACTION, "sendMessage");

        when(componentOperationPolicyRepository.findAllByComponentName("slack")).thenReturn(List.of(policy));

        assertThat(componentPolicyService.getComponentOperationPolicies("slack")).containsExactly(policy);
    }
}
