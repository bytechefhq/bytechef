/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ComponentPolicyVisibilityProviderTest {

    private final ComponentPolicyService componentPolicyService = mock(ComponentPolicyService.class);
    private final ComponentPolicyVisibilityProvider componentPolicyVisibilityProvider =
        new ComponentPolicyVisibilityProvider(componentPolicyService);

    @Test
    void testIsVisibleReflectsDisabledSet() {
        when(componentPolicyService.getDisabledComponentNames()).thenReturn(Set.of("slack"));

        assertThat(componentPolicyVisibilityProvider.isVisible("slack")).isFalse();
        assertThat(componentPolicyVisibilityProvider.isVisible("mailchimp")).isTrue();
    }

    @Test
    void testActionInvisibleWhenOperationDisabled() {
        when(componentPolicyService.getDisabledComponentNames()).thenReturn(Set.of());
        when(componentPolicyService.getDisabledOperationKeys()).thenReturn(Set.of("slack#ACTION#sendMessage"));

        assertThat(componentPolicyVisibilityProvider.isActionVisible("slack", "sendMessage")).isFalse();
        assertThat(componentPolicyVisibilityProvider.isActionVisible("slack", "sendDirectMessage")).isTrue();
        assertThat(componentPolicyVisibilityProvider.isTriggerVisible("slack", "sendMessage")).isTrue();
    }

    @Test
    void testOperationInvisibleWhenWholeComponentDisabled() {
        when(componentPolicyService.getDisabledComponentNames()).thenReturn(Set.of("slack"));
        when(componentPolicyService.getDisabledOperationKeys()).thenReturn(Set.of());

        assertThat(componentPolicyVisibilityProvider.isActionVisible("slack", "sendMessage")).isFalse();
        assertThat(componentPolicyVisibilityProvider.isTriggerVisible("slack", "newMessage")).isFalse();
    }
}
