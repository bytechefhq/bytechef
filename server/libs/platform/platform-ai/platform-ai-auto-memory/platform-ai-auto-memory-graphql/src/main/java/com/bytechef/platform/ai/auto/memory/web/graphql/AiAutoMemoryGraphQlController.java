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

package com.bytechef.platform.ai.auto.memory.web.graphql;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryNotFoundException;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalCount;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL surface for long-term auto-memory rows. Replaces the prior Command-Center-scoped controller — the underlying
 * {@link AiAutoMemoryService} contract is unchanged; only the location and naming differ. All operations are workspace
 * + principal + environment scoped. Cross-workspace access is rejected with Spring Security's
 * {@link AccessDeniedException}; missing rows surface as the service-layer not-found exception.
 *
 * <p>
 * The class-level {@code @PreAuthorize("isAuthenticated()")} mirrors the prior controller; per-method authorization is
 * enforced inline by checking workspace membership against {@link WorkspaceFacade#getUserWorkspaces(long)}. In CE that
 * facade returns every workspace, so the gate is permissive there; in EE it returns only accessible workspaces, which
 * closes the cross-workspace privilege-escalation vector.
 *
 * @author Ivica Cardic
 */
@Controller
@PreAuthorize("isAuthenticated()")
public class AiAutoMemoryGraphQlController {

    private final AiAutoMemoryService aiAutoMemoryService;
    private final UserService userService;
    private final WorkspaceFacade workspaceFacade;
    private final ProjectDeploymentService projectDeploymentService;

    @SuppressFBWarnings("EI")
    public AiAutoMemoryGraphQlController(
        AiAutoMemoryService aiAutoMemoryService, UserService userService, WorkspaceFacade workspaceFacade,
        ProjectDeploymentService projectDeploymentService) {

        this.aiAutoMemoryService = aiAutoMemoryService;
        this.userService = userService;
        this.workspaceFacade = workspaceFacade;
        this.projectDeploymentService = projectDeploymentService;
    }

    @QueryMapping
    public List<AiAutoMemory> aiAutoMemories(
        @Argument long workspaceId, @Argument int environment, @Argument @Nullable AiAutoMemoryType memoryType,
        @Argument @Nullable AiAutoMemoryPrincipalType principalType, @Argument @Nullable Long principalId) {

        long userId = userService.getCurrentUser()
            .getId();

        verifyUserCanAccessWorkspace(userId, workspaceId);

        // On THIS query an absent principal means "every owner I may see" — the Memories page's All scope. The
        // by-id query and the mutations keep the opposite default (absent = the caller), because there the
        // principal is an authorization decision about one row rather than a listing scope.
        if (principalType == null && principalId == null) {
            return listAllAddressableOwners(workspaceId, environment, memoryType, userId);
        }

        ResolvedPrincipal principal = resolvePrincipal(principalType, principalId, userId, false);

        if (principal == null) {
            return List.of();
        }

        return aiAutoMemoryService.list(
            workspaceId, principal.principalType(), principal.principalId(), environment, memoryType);
    }

    /**
     * The All scope: one owner-agnostic read, then the same per-owner decision table the picker uses, so a caller never
     * sees a memory it could not have reached by picking that owner explicitly. Filtering here rather than in the
     * repository keeps the authorization in the one place that already owns it.
     */
    private List<AiAutoMemory> listAllAddressableOwners(
        long workspaceId, int environment, @Nullable AiAutoMemoryType memoryType, long userId) {

        Set<PrincipalKey> addressable = new HashSet<>();

        for (AiAutoMemoryPrincipalCount principalCount : aiAutoMemoryService.listPrincipals(workspaceId, environment)) {
            ResolvedPrincipal resolved = resolvePrincipalForListing(principalCount, userId);

            if (resolved != null) {
                addressable.add(new PrincipalKey(resolved.principalType(), resolved.principalId()));
            }
        }

        return aiAutoMemoryService.listAllOwners(workspaceId, environment, memoryType)
            .stream()
            .filter(
                memory -> addressable.contains(
                    new PrincipalKey(memory.getPrincipalType(), memory.getPrincipalId())))
            .toList();
    }

    /**
     * The owner pair, for set membership in {@link #listAllAddressableOwners}.
     */
    private record PrincipalKey(AiAutoMemoryPrincipalType principalType, long principalId) {
    }

    @QueryMapping
    @Nullable
    public AiAutoMemory aiAutoMemory(
        @Argument long workspaceId, @Argument long id, @Argument int environment,
        @Argument @Nullable AiAutoMemoryPrincipalType principalType, @Argument @Nullable Long principalId) {

        long userId = userService.getCurrentUser()
            .getId();

        verifyUserCanAccessWorkspace(userId, workspaceId);

        ResolvedPrincipal principal = resolvePrincipal(principalType, principalId, userId, false);

        if (principal == null) {
            return null;
        }

        Optional<AiAutoMemory> memory = aiAutoMemoryService.findById(
            workspaceId, principal.principalType(), principal.principalId(), id, environment);

        return memory.orElse(null);
    }

    @QueryMapping
    public List<AiAutoMemoryPrincipal> aiAutoMemoryPrincipals(@Argument long workspaceId, @Argument int environment) {
        long userId = userService.getCurrentUser()
            .getId();

        verifyUserCanAccessWorkspace(userId, workspaceId);

        List<AiAutoMemoryPrincipalCount> principalCounts =
            aiAutoMemoryService.listPrincipals(workspaceId, environment);

        List<ResolvedPrincipalCount> addressable = new ArrayList<>();

        for (AiAutoMemoryPrincipalCount principalCount : principalCounts) {
            // Reuses the read path's decision table so the picker can never offer an owner the reads refuse.
            ResolvedPrincipal resolved = resolvePrincipalForListing(principalCount, userId);

            if (resolved != null) {
                addressable.add(new ResolvedPrincipalCount(resolved, principalCount.memoryCount()));
            }
        }

        Map<Long, String> deploymentNames = resolveDeploymentNames(addressable);

        List<AiAutoMemoryPrincipal> principals = new ArrayList<>();

        for (ResolvedPrincipalCount resolvedCount : addressable) {
            ResolvedPrincipal resolved = resolvedCount.principal();

            principals.add(
                new AiAutoMemoryPrincipal(
                    resolved.principalType(), resolved.principalId(),
                    resolveLabel(resolved, userId, deploymentNames), resolvedCount.memoryCount()));
        }

        return principals;
    }

    /**
     * Resolves every deployment name in one call. A per-principal lookup inside the loop above would issue one query
     * per owner, which is the N+1 this exists to avoid.
     */
    private Map<Long, String> resolveDeploymentNames(List<ResolvedPrincipalCount> addressable) {
        List<Long> deploymentIds = addressable.stream()
            .map(ResolvedPrincipalCount::principal)
            .filter(principal -> principal.principalType() == AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT)
            .map(ResolvedPrincipal::principalId)
            .toList();

        if (deploymentIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> namesById = new HashMap<>();

        for (ProjectDeployment projectDeployment : projectDeploymentService.getProjectDeployments(deploymentIds)) {
            namesById.put(projectDeployment.getId(), projectDeployment.getName());
        }

        return namesById;
    }

    /**
     * Pairs a principal the caller may address with how much memory it holds.
     */
    private record ResolvedPrincipalCount(ResolvedPrincipal principal, int memoryCount) {
    }

    /**
     * The listing variant of {@link #resolvePrincipal}: same rules, but a principal this caller may not address is
     * skipped rather than raised, because a catalogue legitimately contains entries the caller cannot open.
     */
    private @Nullable ResolvedPrincipal resolvePrincipalForListing(
        AiAutoMemoryPrincipalCount principalCount, long currentUserId) {

        AiAutoMemoryPrincipalType principalType = principalCount.principalType();

        if (principalType == AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE) {
            return null;
        }

        return resolvePrincipal(principalType, principalCount.principalId(), currentUserId, false);
    }

    private static String resolveLabel(
        ResolvedPrincipal principal, long currentUserId, Map<Long, String> deploymentNames) {

        if (principal.principalType() == AiAutoMemoryPrincipalType.USER) {
            return principal.principalId() == currentUserId ? "My memories" : "Memories";
        }

        // A deployment deleted while its memory rows remain is absent from the batch lookup: label it by id rather
        // than dropping the entry, so the orphaned memory stays reachable for cleanup.
        return deploymentNames.getOrDefault(principal.principalId(), "Deployment " + principal.principalId());
    }

    @MutationMapping
    public AiAutoMemory updateAiAutoMemory(@Argument UpdateAiAutoMemoryInput input) {
        if (input.title() == null && input.description() == null && input.memoryType() == null
            && input.content() == null) {
            throw new IllegalArgumentException(
                "UpdateAiAutoMemoryInput requires at least one of title, description, memoryType, content");
        }

        long userId = userService.getCurrentUser()
            .getId();

        verifyUserCanAccessWorkspace(userId, input.workspaceId());

        ResolvedPrincipal principal = resolvePrincipal(input.principalType(), input.principalId(), userId, true);

        if (principal == null) {
            throw new AiAutoMemoryNotFoundException("AiAutoMemory not found");
        }

        return aiAutoMemoryService.updateById(
            input.workspaceId(), principal.principalType(), principal.principalId(), input.id(),
            input.title(), input.description(), input.memoryType(), input.content(), input.environment());
    }

    @MutationMapping
    public boolean deleteAiAutoMemory(
        @Argument long workspaceId, @Argument long id, @Argument int environment,
        @Argument @Nullable AiAutoMemoryPrincipalType principalType, @Argument @Nullable Long principalId) {

        long userId = userService.getCurrentUser()
            .getId();

        verifyUserCanAccessWorkspace(userId, workspaceId);

        ResolvedPrincipal principal = resolvePrincipal(principalType, principalId, userId, true);

        if (principal == null) {
            throw new AiAutoMemoryNotFoundException("AiAutoMemory not found");
        }

        aiAutoMemoryService.deleteById(
            workspaceId, principal.principalType(), principal.principalId(), id, environment);

        return true;
    }

    @SchemaMapping(typeName = "AiAutoMemory", field = "principalId")
    public long principalId(AiAutoMemory memory) {
        return memory.getPrincipalId();
    }

    /**
     * Exposed alongside {@link #principalId} because the id alone does not identify an owner: user-owned and
     * deployment-owned memory share one table, so the same numeric id means a different owner under a different
     * principal type. The pair is exposed for exactly that reason, rather than assuming the id is a user id.
     */
    @SchemaMapping(typeName = "AiAutoMemory", field = "principalType")
    public String principalType(AiAutoMemory memory) {
        return memory.getPrincipalType()
            .name();
    }

    @SchemaMapping(typeName = "AiAutoMemory", field = "memoryType")
    public String memoryType(AiAutoMemory memory) {
        return memory.getMemoryType()
            .name();
    }

    @SchemaMapping(typeName = "AiAutoMemory", field = "createdAt")
    @Nullable
    public Long createdAt(AiAutoMemory memory) {
        return memory.getCreatedAt() == null ? null
            : memory.getCreatedAt()
                .toInstant(java.time.ZoneOffset.UTC)
                .toEpochMilli();
    }

    @SchemaMapping(typeName = "AiAutoMemory", field = "updatedAt")
    @Nullable
    public Long updatedAt(AiAutoMemory memory) {
        return memory.getUpdatedAt() == null ? null
            : memory.getUpdatedAt()
                .toInstant(java.time.ZoneOffset.UTC)
                .toEpochMilli();
    }

    /**
     * Decides which principal the caller may address. Returns {@code null} when the caller may not address the
     * requested principal — reads map that to an empty result and mutations to not-found, so a caller cannot tell "not
     * yours" from "no such memory" and ids stay unenumerable. Throws only for malformed input, which is a client bug
     * rather than an ownership signal.
     *
     * <p>
     * {@code PROJECT_DEPLOYMENT} needs no ownership lookup: every service call filters on {@code workspaceId} and the
     * caller's membership of that workspace is verified before this runs, so a deployment in another workspace matches
     * nothing. {@code USER} cannot rely on that — two members of one workspace both have rows under it — which is the
     * only reason the own-id guard exists.
     * </p>
     */
    @Nullable
    ResolvedPrincipal resolvePrincipal(
        @Nullable AiAutoMemoryPrincipalType principalType, @Nullable Long principalId, long currentUserId,
        boolean mutating) {

        if ((principalType == null) != (principalId == null)) {
            throw new IllegalArgumentException(
                "principalType and principalId must be supplied together or both omitted");
        }

        if (principalType == null) {
            return new ResolvedPrincipal(AiAutoMemoryPrincipalType.USER, currentUserId);
        }

        if (principalType == AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE) {
            throw new IllegalArgumentException(
                "INTEGRATION_INSTANCE memories are not addressable through this API: embedded integration-instance " +
                    "rows all live in the default workspace, so workspace membership does not isolate them");
        }

        if (principalType == AiAutoMemoryPrincipalType.USER) {
            return principalId == currentUserId ? new ResolvedPrincipal(principalType, principalId) : null;
        }

        // Default-deny: everything below is PROJECT_DEPLOYMENT's rules. A principal type appended to the enum later
        // must be denied until someone decides its rules, rather than silently inheriting these.
        if (principalType != AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT) {
            return null;
        }

        // Deployment memory is written by a running workflow, and editing it changes how a live agent behaves on its
        // next run with no notification to anyone — so mutating requires admin while reading does not.
        if (mutating && !SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN)) {
            return null;
        }

        return new ResolvedPrincipal(principalType, principalId);
    }

    private void verifyUserCanAccessWorkspace(long userId, long workspaceId) {
        boolean isMember = workspaceFacade.getUserWorkspaces(userId)
            .stream()
            .map(Workspace::getId)
            .anyMatch(id -> id != null && id == workspaceId);

        if (!isMember) {
            throw new AccessDeniedException("Workspace is not accessible to the current user");
        }
    }

    /**
     * The principal a request resolved to, after defaulting and authorization.
     */
    record ResolvedPrincipal(AiAutoMemoryPrincipalType principalType, long principalId) {
    }

    /**
     * One selectable owner in the Memories picker.
     */
    public record AiAutoMemoryPrincipal(
        AiAutoMemoryPrincipalType principalType, long principalId, String label, int memoryCount) {
    }

    /**
     * Workspace, id, environment, and the patch fields. Environment identifies which row the primary key addresses
     * rather than a value to write — a memory's environment is immutable post-create, and a row in another environment
     * is not reachable from this session. {@code principalType}/{@code principalId} are supplied together or both
     * omitted; omitting them targets the signed-in user.
     */
    public record UpdateAiAutoMemoryInput(
        long id, long workspaceId, int environment, @Nullable String title, @Nullable String description,
        @Nullable AiAutoMemoryType memoryType, @Nullable String content,
        @Nullable AiAutoMemoryPrincipalType principalType, @Nullable Long principalId) {
    }
}
