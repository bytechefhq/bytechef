/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.exception.CustomRoleErrorType;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScope;
import com.bytechef.ee.automation.configuration.service.CustomRoleService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for custom-role management. Authorization is enforced at the service layer
 * ({@code CustomRoleService} methods are {@code @PreAuthorize("@permissionService.isTenantAdmin()")}) and pinned by
 * {@code PreAuthorizeAnnotationTest}. Do NOT add caching or transform logic to controller methods that could precede
 * the service call without also adding a matching {@code @PreAuthorize} here — an unguarded cache lookup would serve
 * previously-authorized data across security boundaries.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@ConditionalOnEEVersion
@SuppressFBWarnings("EI2")
public class CustomRoleGraphQlController {

    private final CustomRoleService customRoleService;

    public CustomRoleGraphQlController(CustomRoleService customRoleService) {
        this.customRoleService = customRoleService;
    }

    @QueryMapping
    public List<CustomRole> customRoles() {
        return customRoleService.getCustomRoles();
    }

    @QueryMapping
    public CustomRole customRole(@Argument long id) {
        return customRoleService.getCustomRole(id);
    }

    @MutationMapping
    public CustomRole createCustomRole(@Argument Map<String, Object> input) {
        Set<PermissionScope> scopes = toPermissionScopes(input);

        return customRoleService.createCustomRole(
            (String) input.get("name"), (String) input.get("description"), scopes);
    }

    @MutationMapping
    public CustomRole updateCustomRole(@Argument long id, @Argument Map<String, Object> input) {
        Set<PermissionScope> scopes = toPermissionScopes(input);

        return customRoleService.updateCustomRole(
            id, (String) input.get("name"), (String) input.get("description"), scopes);
    }

    @MutationMapping
    public boolean deleteCustomRole(@Argument long id) {
        customRoleService.deleteCustomRole(id);

        return true;
    }

    @SuppressWarnings("unchecked")
    private Set<PermissionScope> toPermissionScopes(Map<String, Object> input) {
        Object rawScopes = input.get("scopes");

        // Validation failures at the GraphQL boundary MUST go through ConfigurationException + CustomRoleErrorType so
        // the global resolver maps them to BAD_REQUEST with a structured errorKey the client can switch on. Throwing
        // bare IllegalArgumentException collapses into INTERNAL_ERROR with the message text lost.
        if (rawScopes == null) {
            throw new ConfigurationException(
                "'scopes' field is required",
                CustomRoleErrorType.SCOPES_REQUIRED);
        }

        List<String> scopeNames = (List<String>) rawScopes;

        if (scopeNames.isEmpty()) {
            throw new ConfigurationException(
                "'scopes' must contain at least one permission \u2014 a custom role with zero scopes grants no "
                    + "access and silently locks out affected users",
                CustomRoleErrorType.SCOPES_REQUIRED);
        }

        return scopeNames.stream()
            .map(scopeName -> {
                try {
                    return PermissionScope.valueOf(scopeName);
                } catch (IllegalArgumentException illegalArgumentException) {
                    throw new ConfigurationException(
                        "Invalid permission scope: '" + scopeName + "'. Valid scopes: "
                            + Arrays.toString(PermissionScope.values()),
                        illegalArgumentException,
                        CustomRoleErrorType.INVALID_SCOPE);
                }
            })
            .collect(Collectors.toSet());
    }
}
