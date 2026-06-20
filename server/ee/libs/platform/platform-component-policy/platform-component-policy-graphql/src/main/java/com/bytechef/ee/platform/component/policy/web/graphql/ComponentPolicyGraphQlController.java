/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.policy.web.graphql;

import com.bytechef.ee.platform.component.policy.ComponentPolicy;
import com.bytechef.ee.platform.component.policy.ComponentPolicyService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
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

        return componentDefinitionService.getComponentDefinitions()
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
}
