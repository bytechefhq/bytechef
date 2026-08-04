/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.policy.web.graphql;

import com.bytechef.ee.platform.component.policy.ComponentOperationPolicy;
import com.bytechef.ee.platform.component.policy.ComponentPolicy;
import com.bytechef.ee.platform.component.policy.ComponentPolicyService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL surface for tenant-wide component visibility policies. Admin-only.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
public class ComponentPolicyGraphQlController {

    private final ComponentDefinitionService componentDefinitionService;
    private final ComponentPolicyService componentPolicyService;

    @SuppressFBWarnings("EI2")
    public ComponentPolicyGraphQlController(
        ComponentDefinitionService componentDefinitionService, ComponentPolicyService componentPolicyService) {

        this.componentDefinitionService = componentDefinitionService;
        this.componentPolicyService = componentPolicyService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<ComponentPolicyItem> componentPolicies() {
        Set<String> disabledComponentNames = componentPolicyService.getDisabledComponentNames();

        // Policies are keyed by component name only, while the registry can hold several definitions per name
        // (multiple versions; the redis component and the redis vector store cluster element share a name), so the
        // list must collapse to one row per name.
        Map<String, ComponentDefinition> componentDefinitionsByName =
            componentDefinitionService.getComponentDefinitions()
                .stream()
                .collect(
                    Collectors.toMap(
                        ComponentDefinition::getName, Function.identity(),
                        ComponentPolicyGraphQlController::preferHighestVersion));

        return componentDefinitionsByName.values()
            .stream()
            .map(componentDefinition -> toItem(componentDefinition, disabledComponentNames))
            .sorted(Comparator.comparing(ComponentPolicyItem::sortKey, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ComponentPolicyItem updateComponentPolicy(@Argument String name, @Argument boolean enabled) {
        ComponentPolicy componentPolicy = componentPolicyService.updateComponentPolicy(name, enabled);

        ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(name, null);

        return new ComponentPolicyItem(
            componentDefinition.getName(), componentDefinition.getTitle(), componentDefinition.getDescription(),
            componentDefinition.getIcon(), componentDefinition.getVersion(), componentPolicy.isEnabled());
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<ComponentOperationPolicyItem> componentOperationPolicies(@Argument String componentName) {
        return componentPolicyService.getComponentOperationPolicies(componentName)
            .stream()
            .map(componentOperationPolicy -> new ComponentOperationPolicyItem(
                componentOperationPolicy.getComponentName(), componentOperationPolicy.getOperationType(),
                componentOperationPolicy.getOperationName()))
            .toList();
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean updateComponentOperationPolicy(
        @Argument String componentName, @Argument ComponentOperationPolicy.OperationType operationType,
        @Argument String operationName, @Argument boolean enabled) {

        componentPolicyService.updateComponentOperationPolicy(componentName, operationType, operationName, enabled);

        return true;
    }

    private static ComponentDefinition preferHighestVersion(
        ComponentDefinition firstComponentDefinition, ComponentDefinition secondComponentDefinition) {

        return firstComponentDefinition.getVersion() >= secondComponentDefinition.getVersion()
            ? firstComponentDefinition
            : secondComponentDefinition;
    }

    private static ComponentPolicyItem toItem(
        ComponentDefinition componentDefinition, Set<String> disabledComponentNames) {

        return new ComponentPolicyItem(
            componentDefinition.getName(), componentDefinition.getTitle(), componentDefinition.getDescription(),
            componentDefinition.getIcon(), componentDefinition.getVersion(),
            !disabledComponentNames.contains(componentDefinition.getName()));
    }

    public record ComponentPolicyItem(
        String name, @Nullable String title, @Nullable String description, @Nullable String icon, int version,
        boolean enabled) {

        String sortKey() {
            return title == null ? name : title;
        }
    }

    public record ComponentOperationPolicyItem(
        String componentName, ComponentOperationPolicy.OperationType operationType, String operationName) {
    }
}
