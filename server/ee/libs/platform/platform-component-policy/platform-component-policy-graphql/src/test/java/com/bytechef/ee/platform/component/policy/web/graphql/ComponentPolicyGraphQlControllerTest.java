/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.policy.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.component.policy.ComponentOperationPolicy;
import com.bytechef.ee.platform.component.policy.ComponentPolicy;
import com.bytechef.ee.platform.component.policy.ComponentPolicyService;
import com.bytechef.ee.platform.component.policy.web.graphql.ComponentPolicyGraphQlController.ComponentOperationPolicyItem;
import com.bytechef.ee.platform.component.policy.web.graphql.ComponentPolicyGraphQlController.ComponentPolicyItem;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ComponentPolicyGraphQlControllerTest {

    private final ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);
    private final ComponentPolicyService componentPolicyService = mock(ComponentPolicyService.class);
    private final ComponentPolicyGraphQlController controller =
        new ComponentPolicyGraphQlController(componentDefinitionService, componentPolicyService);

    @Test
    void testComponentPoliciesMergesDisabledFlag() {
        ComponentDefinition slack = mock(ComponentDefinition.class);
        ComponentDefinition mailchimp = mock(ComponentDefinition.class);

        when(slack.getName()).thenReturn("slack");
        when(slack.getTitle()).thenReturn("Slack");
        when(slack.getVersion()).thenReturn(1);
        when(mailchimp.getName()).thenReturn("mailchimp");
        when(mailchimp.getTitle()).thenReturn("Mailchimp");
        when(mailchimp.getVersion()).thenReturn(1);

        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(slack, mailchimp));
        when(componentPolicyService.getDisabledComponentNames()).thenReturn(Set.of("slack"));

        List<ComponentPolicyItem> result = controller.componentPolicies();

        assertThat(result)
            .extracting(ComponentPolicyItem::name, ComponentPolicyItem::enabled)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("mailchimp", true),
                org.assertj.core.groups.Tuple.tuple("slack", false));
    }

    @Test
    void testComponentPoliciesCollapsesSameNamedDefinitionsToOneRow() {
        ComponentDefinition redis = mock(ComponentDefinition.class);
        ComponentDefinition redisVectorStore = mock(ComponentDefinition.class);

        when(redis.getName()).thenReturn("redis");
        when(redis.getTitle()).thenReturn("Redis");
        when(redis.getVersion()).thenReturn(1);
        when(redisVectorStore.getName()).thenReturn("redis");
        when(redisVectorStore.getTitle()).thenReturn("Redis");
        when(redisVectorStore.getVersion()).thenReturn(2);

        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(redis, redisVectorStore));
        when(componentPolicyService.getDisabledComponentNames()).thenReturn(Set.of());

        List<ComponentPolicyItem> result = controller.componentPolicies();

        assertThat(result)
            .extracting(ComponentPolicyItem::name, ComponentPolicyItem::version)
            .containsExactly(org.assertj.core.groups.Tuple.tuple("redis", 2));
    }

    @Test
    void testUpdateComponentPolicyReturnsUpdatedItem() {
        ComponentPolicy componentPolicy = new ComponentPolicy("slack");

        componentPolicy.setEnabled(false);

        ComponentDefinition slack = mock(ComponentDefinition.class);

        when(slack.getName()).thenReturn("slack");
        when(slack.getTitle()).thenReturn("Slack");
        when(slack.getVersion()).thenReturn(1);

        when(componentPolicyService.updateComponentPolicy("slack", false)).thenReturn(componentPolicy);
        when(componentDefinitionService.getComponentDefinition(any(), any())).thenReturn(slack);

        ComponentPolicyItem result = controller.updateComponentPolicy("slack", false);

        assertThat(result.name()).isEqualTo("slack");
        assertThat(result.enabled()).isFalse();
    }

    @Test
    void testComponentOperationPoliciesReturnsDisabledRows() {
        when(componentPolicyService.getComponentOperationPolicies("slack")).thenReturn(List.of(
            new ComponentOperationPolicy("slack", ComponentOperationPolicy.OperationType.ACTION, "sendMessage")));

        List<ComponentOperationPolicyItem> items = controller.componentOperationPolicies("slack");

        assertThat(items).hasSize(1);
        assertThat(items.getFirst()
            .operationType()).isEqualTo(
                ComponentOperationPolicy.OperationType.ACTION);
        assertThat(items.getFirst()
            .operationName()).isEqualTo("sendMessage");
    }

    @Test
    void testUpdateComponentOperationPolicyDelegatesToService() {
        boolean result = controller.updateComponentOperationPolicy(
            "slack", ComponentOperationPolicy.OperationType.ACTION, "sendMessage", false);

        assertThat(result).isTrue();
        verify(componentPolicyService).updateComponentOperationPolicy(
            "slack", ComponentOperationPolicy.OperationType.ACTION, "sendMessage", false);
    }
}
