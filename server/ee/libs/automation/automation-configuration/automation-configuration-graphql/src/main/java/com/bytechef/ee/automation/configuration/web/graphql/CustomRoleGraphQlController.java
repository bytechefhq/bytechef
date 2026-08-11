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
import com.bytechef.ee.automation.configuration.service.CustomRoleService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for custom-role management. Authorization is enforced at the service layer
 * ({@code CustomRoleService} methods are {@code @PreAuthorize("isTenantAdmin()")}) and pinned by
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
    public List<CustomRole> customRoles(@Argument Long workspaceId) {
        return customRoleService.getCustomRoles(workspaceId);
    }

    /**
     * The scopes a role may be composed from. Served from the same {@code PermissionScopeProvider} registry the write
     * path validates against, so a client editor cannot offer a name the server would reject, nor omit one a module
     * added after the client was written.
     */
    @QueryMapping
    public List<String> permissionScopes() {
        return customRoleService.getPermissionScopeNames();
    }

    /**
     * The schema declares {@code scopes: [String!]!}, but property resolution would pick {@code getScopes()}, which
     * returns the {@code CustomRoleScope} row wrappers Spring Data JDBC needs for the child collection — serialising
     * them as {@code CustomRoleScope[scope=WORKFLOW_VIEW]}. Map the field explicitly to the unwrapped names.
     */
    @SchemaMapping(typeName = "CustomRole", field = "scopes")
    public Set<String> scopes(CustomRole customRole) {
        return customRole.getScopeNames();
    }

    @MutationMapping
    public CustomRole createCustomRole(@Argument Map<String, Object> input) {
        Set<String> scopeNames = toScopeNames(input);

        return customRoleService.createCustomRole(
            (String) input.get("name"), (String) input.get("description"), scopeNames);
    }

    @MutationMapping
    public CustomRole updateCustomRole(@Argument long id, @Argument Map<String, Object> input) {
        Set<String> scopeNames = toScopeNames(input);

        return customRoleService.updateCustomRole(
            id, (String) input.get("name"), (String) input.get("description"), scopeNames);
    }

    @MutationMapping
    public boolean deleteCustomRole(@Argument long id) {
        customRoleService.deleteCustomRole(id);

        return true;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> toScopeNames(Map<String, Object> input) {
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

        // Membership validation (is each name a real, module-contributed scope?) lives in CustomRoleService against
        // the PermissionScopeRegistry \u2014 the controller only enforces request shape (present, non-empty).
        return Set.copyOf(scopeNames);
    }
}
