/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security.scope;

import com.bytechef.automation.configuration.security.constant.PermissionScopeType;

/**
 * The API Platform permission scopes, governing API collections and their endpoints.
 *
 * <p>
 * Its own family rather than a reuse of {@code WORKSPACE_*} or {@code DEPLOYMENT_*}, per D1 of
 * {@code docs/superpowers/specs/2026-09-06-api-collection-authorization-design.md}. Publishing an API is at least as
 * consequential as creating an MCP server, which has its own family; and although every collection is backed by a
 * synthetic {@code __API_COLLECTION__} {@code ProjectDeployment}, that backing is an implementation detail the UI never
 * shows, so granting deployment rights must not silently confer API-publishing rights.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum ApiPlatformPermissionScope implements PermissionScopeType {

    API_PLATFORM_VIEW,
    API_PLATFORM_CREATE,
    API_PLATFORM_EDIT,
    API_PLATFORM_DELETE
}
