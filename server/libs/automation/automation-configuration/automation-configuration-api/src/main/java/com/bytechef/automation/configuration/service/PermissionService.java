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

import com.bytechef.automation.configuration.security.constant.PermissionScopeType;
import com.bytechef.automation.configuration.security.constant.ProjectRoleType;
import com.bytechef.automation.configuration.security.constant.WorkspaceRoleType;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * Core authorization engine. Registered as Spring bean {@code "permissionService"} so that {@code @PreAuthorize} SpEL
 * expressions can call it directly:
 *
 * <pre>
 * {@code @PreAuthorize("@permissionService.hasProjectScope(#projectId, 'WORKFLOW_EDIT')")}
 * </pre>
 *
 * <p>
 * The {@code String}-based methods exist for SpEL compatibility (SpEL cannot reference EE-only enum constants in CE
 * SpEL contexts). Internal Java callers should prefer the typed {@code *Typed} convenience variants which forward to
 * the string methods but give compile-time scope/role validation. The typed variants are named distinctly (e.g.,
 * {@code hasWorkspaceRoleTyped} vs {@code hasWorkspaceRole}) rather than sharing a method name because SpEL's
 * reflective overload resolution cannot convert a String literal to the erasure of {@code <E extends Enum<E>>} and
 * would otherwise abort {@code @PreAuthorize} evaluation with a raw-{@code Enum} conversion error.
 *
 * <p>
 * Role hierarchy is encoded by the {@code privilegeRank} field on the EE {@code WorkspaceRole} / {@code ProjectRole}
 * enums (lower rank = higher privilege). Workspace and project roles form separate hierarchies; the typed variants
 * accept the EE enum types (referenced as fully qualified names below so the CE module does not require an EE classpath
 * dependency).
 *
 * @author Ivica Cardic
 */
public interface PermissionService {

    /**
     * Returns {@code true} if the current user has the {@code ROLE_ADMIN} authority (global tenant administrator).
     */
    boolean isTenantAdmin();

    /**
     * Returns {@code true} if {@code userId} matches the current authenticated user. Returns {@code false} when no
     * SecurityContext is available (fail-closed). Used by {@code @PreAuthorize} SpEL expressions to implement
     * self-access checks without depending on the shape of the {@code authentication.principal} object.
     */
    boolean isCurrentUser(long userId);

    /**
     * Returns {@code true} if the current user's workspace role is at least {@code minimumRole} by the role's
     * {@code privilegeRank}. Not cached. Tenant admins always return {@code true}.
     *
     * @param minimumRole a {@code com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole} name
     */
    boolean hasWorkspaceRole(long workspaceId, String minimumRole);

    /**
     * Returns {@code true} if the current user has the given permission scope for the project. Scopes are resolved from
     * the user's project role via {@code BuiltInRoleScopes}, or from a custom role (EE). Results are cached per
     * (userId, projectId). Tenant admins always return {@code true}.
     *
     * @param scope a {@code com.bytechef.ee.automation.configuration.security.constant.PermissionScope} name
     */
    boolean hasProjectScope(long projectId, String scope);

    /**
     * Returns {@code true} if the current user's project role is at least {@code minimumRole} by the role's
     * {@code privilegeRank}. Only the built-in {@code ProjectRole} is consulted — members whose membership is via a
     * custom role are <em>not</em> considered to satisfy this check, even if the custom role grants scopes equivalent
     * to {@code ADMIN}. This asymmetry is deliberate: it closes a self-promotion path where a custom-role holder with
     * {@code PROJECT_MANAGE_USERS} could otherwise elevate themselves to built-in {@code ADMIN}. Orphan-recovery logic
     * that needs to count custom-role admins as effective admins lives in {@code validateNotLastEffectiveAdmin}, not in
     * this method. Not cached. Tenant admins always return {@code true}.
     *
     * @param minimumRole a {@code com.bytechef.ee.automation.configuration.security.constant.ProjectRole} name
     */
    boolean hasProjectRole(long projectId, String minimumRole);

    /**
     * Returns the set of permission scope names the current user has for the given project.
     */
    Set<String> getMyProjectScopes(long projectId);

    /**
     * Returns the workspace role name for the current user in the given workspace, or {@code null} if the user is not a
     * member.
     */
    String getMyWorkspaceRole(long workspaceId);

    /**
     * Evicts the cached permission scopes for a specific (userId, projectId) pair. Must be called after role or
     * membership changes.
     */
    void evictProjectScopeCache(long userId, long projectId);

    /**
     * Batch variant of {@link #evictProjectScopeCache(long, long)}. EE implementations register a single
     * {@code TransactionSynchronization.afterCommit} callback that iterates the supplied pairs, instead of scheduling N
     * callbacks (one per pair). Matters on workspace-wide operations like workspace deletion where the pair set is
     * {@code O(members \u00d7 projects)} \u2014 scheduling that many synchronizations per transaction stresses the
     * synchronization manager and allocates a closure per pair for no functional gain. CE is a no-op.
     */
    default void evictProjectScopeCaches(Collection<UserProjectPair> userProjectPairs) {
        for (UserProjectPair pair : userProjectPairs) {
            evictProjectScopeCache(pair.userId(), pair.projectId());
        }
    }

    /**
     * Evicts the entire project scopes cache for all users. Use after bulk operations (e.g., custom role scope changes)
     * that affect multiple users.
     */
    void evictAllProjectScopeCache();

    /**
     * Identifies a single {@code (userId, projectId)} entry in the project scopes cache. Value type so callers can
     * build a collection without resorting to {@code long[]} (no type safety) or two parallel lists (easy to desync).
     */
    record UserProjectPair(long userId, long projectId) {
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Typed convenience methods — forward to the String variants for compile-time safety on internal callers. The
    // name deliberately differs from the String methods (hasWorkspaceRoleTyped vs hasWorkspaceRole): sharing a method
    // name with the String overloads makes Spring SpEL's reflective overload resolution try to convert String
    // arguments to the raw {@code Enum} target (the erasure of {@code <E extends Enum<E>>}), which fails with
    // "The target type java.lang.Enum does not refer to an enum" and aborts @PreAuthorize evaluation BEFORE the
    // correct String overload is ever considered. Keeping the typed variants under a distinct name lets SpEL resolve
    // each call site unambiguously.
    //
    // The marker types live in CE so this interface compiles without an EE dependency; the EE enums (WorkspaceRole,
    // PermissionScope, ProjectRole) implement them. The {@code <E extends Enum<E> & T>} bound is load-bearing: the
    // bare marker-interface bound would let a hand-rolled class that just returns a spoofed {@code name()} satisfy
    // the signature and forge any scope string. Requiring the argument to also be an enum closes that hole, because
    // Java forbids user-defined enum types from escaping the {@code Enum<E>} hierarchy.
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * Typed variant of {@link #hasWorkspaceRole(long, String)}. The {@code <E extends Enum<E> & WorkspaceRoleType>}
     * bound forces the argument to be an enum that implements the marker interface, preventing hand-rolled
     * {@code WorkspaceRoleType} impls from forging role strings. See the type-ladder comment above for why this method
     * name differs from the SpEL-facing String variant.
     */
    default <E extends Enum<E> & WorkspaceRoleType> boolean hasWorkspaceRoleTyped(long workspaceId, E minimumRole) {
        Objects.requireNonNull(minimumRole, "'minimumRole' must not be null");

        return hasWorkspaceRole(workspaceId, minimumRole.name());
    }

    /**
     * Typed variant of {@link #hasProjectScope(long, String)}. See {@link #hasWorkspaceRoleTyped(long, Enum)} for the
     * rationale behind both the generic {@code Enum<E> & ...} bound and the distinct method name.
     */
    default <E extends Enum<E> & PermissionScopeType> boolean hasProjectScopeTyped(long projectId, E scope) {
        Objects.requireNonNull(scope, "'scope' must not be null");

        return hasProjectScope(projectId, scope.name());
    }

    /**
     * Typed variant of {@link #hasProjectRole(long, String)}. See {@link #hasWorkspaceRoleTyped(long, Enum)} for the
     * rationale behind both the generic {@code Enum<E> & ...} bound and the distinct method name.
     */
    default <E extends Enum<E> & ProjectRoleType> boolean hasProjectRoleTyped(long projectId, E minimumRole) {
        Objects.requireNonNull(minimumRole, "'minimumRole' must not be null");

        return hasProjectRole(projectId, minimumRole.name());
    }
}
