/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.configuration.service;

/**
 * Core authorization engine. Registered as Spring bean {@code "permissionService"}; the
 * {@code AutomationPermissionEvaluator} delegates the {@code hasPermission(...)} SpEL built-in to it, and direct Java
 * callers invoke it as well.
 *
 * @author Ivica Cardic
 */
public interface PermissionService {

    /**
     * Returns {@code true} if {@code userId} matches the current authenticated user. Returns {@code false} when no
     * SecurityContext is available (fail-closed). Used by {@code @PreAuthorize} SpEL expressions to implement
     * self-access checks without depending on the shape of the {@code authentication.principal} object.
     */
    boolean isCurrentUser(long userId);

    /**
     * Returns {@code true} if the current user has the {@code ROLE_ADMIN} authority (global tenant administrator).
     */
    boolean isTenantAdmin();
}
