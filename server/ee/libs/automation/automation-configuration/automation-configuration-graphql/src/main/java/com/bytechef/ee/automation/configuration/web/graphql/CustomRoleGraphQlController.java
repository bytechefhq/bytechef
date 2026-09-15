/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.dto.BuiltInRoleDTO;
import com.bytechef.ee.automation.configuration.dto.PermissionScopeGroupDTO;
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
 * GraphQL controller for custom-role management. Authorization is enforced at the service layer and pinned by
 * {@code PreAuthorizeAnnotationTest}: creating, updating and deleting a custom role is reserved for tenant admins,
 * listing custom roles is also open to a member holding {@code WORKSPACE_MEMBER_MANAGE} on the named workspace, and the
 * built-in roles and the scope catalogue are open to any authenticated user. Do NOT add caching or transform logic to
 * controller methods that could precede the service call without also adding a matching {@code @PreAuthorize} here — an
 * unguarded cache lookup would serve previously-authorized data across security boundaries.
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

    /**
     * The fixed role tiers, so the management page can show what they already grant. Assembled from the same
     * {@code minimumRole} declarations the authorization checks read, so the page cannot describe a tier the server no
     * longer applies.
     */
    @QueryMapping
    public List<BuiltInRoleDTO> builtInRoles() {
        return customRoleService.getBuiltInRoles();
    }

    @QueryMapping
    public List<CustomRole> customRoles(@Argument Long workspaceId) {
        return customRoleService.getCustomRoles(workspaceId);
    }

    /**
     * The scopes a role may be composed from, grouped by the module that owns them. Served from the same
     * {@code PermissionScopeProvider} registry the write path validates against, so a client editor cannot offer a name
     * the server would reject, nor omit one a module added after the client was written — and the grouping comes from
     * that same registration rather than from a client reading module names out of scope names.
     */
    @QueryMapping
    public List<PermissionScopeGroupDTO> permissionScopeGroups() {
        return customRoleService.getPermissionScopeGroups();
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
