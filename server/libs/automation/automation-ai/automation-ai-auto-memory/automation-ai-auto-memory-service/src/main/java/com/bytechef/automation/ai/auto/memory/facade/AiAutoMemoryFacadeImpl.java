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

package com.bytechef.automation.ai.auto.memory.facade;

import com.bytechef.automation.ai.auto.memory.domain.AiAutoMemoryPrincipal;
import com.bytechef.automation.ai.auto.memory.domain.AiAutoMemoryPrincipalKey;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryNotFoundException;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryOwner;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPatch;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalCount;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.domain.User;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class AiAutoMemoryFacadeImpl implements AiAutoMemoryFacade {

    private final AiAutoMemoryService aiAutoMemoryService;
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectDeploymentService projectDeploymentService;
    private final UserService userService;
    private final WorkspaceFacade workspaceFacade;

    @SuppressFBWarnings("EI")
    public AiAutoMemoryFacadeImpl(
        AiAutoMemoryService aiAutoMemoryService, ProjectDeploymentFacade projectDeploymentFacade,
        ProjectDeploymentService projectDeploymentService, UserService userService, WorkspaceFacade workspaceFacade) {

        this.aiAutoMemoryService = aiAutoMemoryService;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectDeploymentService = projectDeploymentService;
        this.userService = userService;
        this.workspaceFacade = workspaceFacade;
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public void deleteAiAutoMemory(
        long workspaceId, Environment environment, long id, @Nullable AiAutoMemoryPrincipalKey principalKey) {

        long userId = getCurrentUserId();

        verifyUserCanAccessWorkspace(userId, workspaceId);

        ResolvedPrincipal resolvedPrincipal = resolveAddressablePrincipal(principalKey, userId, true);

        if (resolvedPrincipal == null) {
            throw new AiAutoMemoryNotFoundException("Memory not found");
        }

        aiAutoMemoryService.deleteById(resolvedPrincipal.toOwner(workspaceId, environment), id);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public Optional<AiAutoMemory> fetchAiAutoMemory(
        long workspaceId, Environment environment, long id, @Nullable AiAutoMemoryPrincipalKey principalKey) {

        long userId = getCurrentUserId();

        verifyUserCanAccessWorkspace(userId, workspaceId);

        ResolvedPrincipal resolvedPrincipal = resolveAddressablePrincipal(principalKey, userId, false);

        if (resolvedPrincipal == null) {
            return Optional.empty();
        }

        return aiAutoMemoryService.findById(resolvedPrincipal.toOwner(workspaceId, environment), id);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public List<AiAutoMemory> getAiAutoMemories(
        long workspaceId, Environment environment, @Nullable AiAutoMemoryType memoryType,
        @Nullable AiAutoMemoryPrincipalKey principalKey) {

        long userId = getCurrentUserId();

        verifyUserCanAccessWorkspace(userId, workspaceId);

        if (principalKey == null) {
            return listAllAddressableOwners(workspaceId, environment, memoryType, userId);
        }

        ResolvedPrincipal resolvedPrincipal = resolveAddressablePrincipal(principalKey, userId, false);

        if (resolvedPrincipal == null) {
            return List.of();
        }

        return aiAutoMemoryService.list(resolvedPrincipal.toOwner(workspaceId, environment), memoryType);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public List<AiAutoMemoryPrincipal> getAiAutoMemoryPrincipals(long workspaceId, Environment environment) {
        long userId = getCurrentUserId();

        verifyUserCanAccessWorkspace(userId, workspaceId);

        List<ResolvedPrincipalCount> resolvedPrincipalCounts = new ArrayList<>();

        for (AiAutoMemoryPrincipalCount principalCount : aiAutoMemoryService.listPrincipals(workspaceId, environment)) {
            ResolvedPrincipal resolvedPrincipal = resolvePrincipalForListing(principalCount, userId);

            if (resolvedPrincipal != null) {
                resolvedPrincipalCounts
                    .add(new ResolvedPrincipalCount(resolvedPrincipal, principalCount.memoryCount()));
            }
        }

        DeploymentLookup deploymentLookup = lookUpDeployments(
            resolvedPrincipalCounts.stream()
                .map(ResolvedPrincipalCount::principal)
                .toList());

        List<AiAutoMemoryPrincipal> aiAutoMemoryPrincipals = new ArrayList<>();

        for (ResolvedPrincipalCount resolvedPrincipalCount : resolvedPrincipalCounts) {
            ResolvedPrincipal resolvedPrincipal = resolvedPrincipalCount.principal();

            if (deploymentLookup.isEmbedded(resolvedPrincipal)) {
                continue;
            }

            aiAutoMemoryPrincipals.add(
                new AiAutoMemoryPrincipal(
                    resolvedPrincipal.principalType(), resolvedPrincipal.principalId(),
                    resolveLabel(resolvedPrincipal, deploymentLookup.namesById()),
                    resolvedPrincipalCount.memoryCount()));
        }

        return aiAutoMemoryPrincipals;
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public AiAutoMemory updateAiAutoMemory(
        long workspaceId, Environment environment, long id, long expectedVersion, AiAutoMemoryPatch patch,
        @Nullable AiAutoMemoryPrincipalKey principalKey) {

        long userId = getCurrentUserId();

        verifyUserCanAccessWorkspace(userId, workspaceId);

        ResolvedPrincipal resolvedPrincipal = resolveAddressablePrincipal(principalKey, userId, true);

        if (resolvedPrincipal == null) {
            throw new AiAutoMemoryNotFoundException("Memory not found");
        }

        return aiAutoMemoryService.updateById(
            resolvedPrincipal.toOwner(workspaceId, environment), id, expectedVersion, patch);
    }

    private long getCurrentUserId() {
        User user = userService.getCurrentUser();

        return user.getId();
    }

    private List<AiAutoMemory> listAllAddressableOwners(
        long workspaceId, Environment environment, @Nullable AiAutoMemoryType memoryType, long userId) {

        List<ResolvedPrincipal> resolvedPrincipals = new ArrayList<>();

        for (AiAutoMemoryPrincipalCount principalCount : aiAutoMemoryService.listPrincipals(workspaceId, environment)) {
            ResolvedPrincipal resolvedPrincipal = resolvePrincipalForListing(principalCount, userId);

            if (resolvedPrincipal != null) {
                resolvedPrincipals.add(resolvedPrincipal);
            }
        }

        DeploymentLookup deploymentLookup = lookUpDeployments(resolvedPrincipals);

        Set<AiAutoMemoryPrincipalKey> addressablePrincipalKeys = new HashSet<>();

        for (ResolvedPrincipal resolvedPrincipal : resolvedPrincipals) {
            if (!deploymentLookup.isEmbedded(resolvedPrincipal)) {
                addressablePrincipalKeys.add(
                    new AiAutoMemoryPrincipalKey(resolvedPrincipal.principalType(), resolvedPrincipal.principalId()));
            }
        }

        return aiAutoMemoryService.listAllOwners(workspaceId, environment, memoryType)
            .stream()
            .filter(
                memory -> addressablePrincipalKeys.contains(
                    new AiAutoMemoryPrincipalKey(memory.getPrincipalType(), memory.getPrincipalId())))
            .toList();
    }

    private DeploymentLookup lookUpDeployments(List<ResolvedPrincipal> principals) {
        List<Long> deploymentIds = principals.stream()
            .filter(principal -> principal.principalType() == AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT)
            .map(ResolvedPrincipal::principalId)
            .distinct()
            .toList();

        if (deploymentIds.isEmpty()) {
            return new DeploymentLookup(Map.of(), Set.of());
        }

        List<ProjectDeployment> projectDeployments = projectDeploymentService.getProjectDeployments(deploymentIds);

        Map<Long, String> namesById = new HashMap<>();

        for (ProjectDeployment projectDeployment : projectDeployments) {
            namesById.put(projectDeployment.getId(), projectDeployment.getName());
        }

        Set<Long> embeddedDeploymentIds = projectDeploymentFacade.getOwnedProjectDeploymentIds(
            deploymentIds, ProjectDeploymentOwner.EMBEDDED);

        return new DeploymentLookup(namesById, embeddedDeploymentIds);
    }

    private @Nullable ResolvedPrincipal resolveAddressablePrincipal(
        @Nullable AiAutoMemoryPrincipalKey principalKey, long currentUserId, boolean mutating) {

        ResolvedPrincipal resolvedPrincipal = resolvePrincipal(principalKey, currentUserId, mutating);

        if (resolvedPrincipal == null || lookUpDeployments(List.of(resolvedPrincipal)).isEmbedded(resolvedPrincipal)) {
            return null;
        }

        return resolvedPrincipal;
    }

    private static String resolveLabel(ResolvedPrincipal resolvedPrincipal, Map<Long, String> deploymentNames) {
        if (resolvedPrincipal.principalType() == AiAutoMemoryPrincipalType.USER) {
            return "My memories";
        }

        return deploymentNames.getOrDefault(
            resolvedPrincipal.principalId(), "Deployment " + resolvedPrincipal.principalId());
    }

    private static @Nullable ResolvedPrincipal resolvePrincipal(
        @Nullable AiAutoMemoryPrincipalKey principalKey, long currentUserId, boolean mutating) {

        if (principalKey == null) {
            return new ResolvedPrincipal(AiAutoMemoryPrincipalType.USER, currentUserId);
        }

        AiAutoMemoryPrincipalType principalType = principalKey.principalType();
        long principalId = principalKey.principalId();

        if (principalType == AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE) {
            throw new IllegalArgumentException(
                "INTEGRATION_INSTANCE memories are not addressable through this API: embedded integration-instance " +
                    "rows all live in the default workspace, so workspace membership does not isolate them");
        }

        if (principalType == AiAutoMemoryPrincipalType.USER) {
            return principalId == currentUserId ? new ResolvedPrincipal(principalType, principalId) : null;
        }

        if (principalType != AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT) {
            return null;
        }

        if (mutating && !SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN)) {
            return null;
        }

        return new ResolvedPrincipal(principalType, principalId);
    }

    private static @Nullable ResolvedPrincipal resolvePrincipalForListing(
        AiAutoMemoryPrincipalCount aiAutoMemoryPrincipalCount, long currentUserId) {

        AiAutoMemoryPrincipalType aiAutoMemoryPrincipalType = aiAutoMemoryPrincipalCount.principalType();

        if (aiAutoMemoryPrincipalType == AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE) {
            return null;
        }

        return resolvePrincipal(
            new AiAutoMemoryPrincipalKey(aiAutoMemoryPrincipalType, aiAutoMemoryPrincipalCount.principalId()),
            currentUserId, false);
    }

    private void verifyUserCanAccessWorkspace(long userId, long workspaceId) {
        List<Workspace> workspaces = workspaceFacade.getUserWorkspaces(userId);

        boolean isMember = workspaces.stream()
            .map(Workspace::getId)
            .anyMatch(id -> id != null && id == workspaceId);

        if (!isMember) {
            throw new AccessDeniedException("Workspace is not accessible to the current user");
        }
    }

    private record DeploymentLookup(Map<Long, String> namesById, Set<Long> embeddedDeploymentIds) {

        boolean isEmbedded(ResolvedPrincipal principal) {
            return principal.principalType() == AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT &&
                embeddedDeploymentIds.contains(principal.principalId());
        }
    }

    private record ResolvedPrincipal(AiAutoMemoryPrincipalType principalType, long principalId) {

        AiAutoMemoryOwner toOwner(long workspaceId, Environment environment) {
            return new AiAutoMemoryOwner(workspaceId, principalType, principalId, environment);
        }
    }

    private record ResolvedPrincipalCount(ResolvedPrincipal principal, int memoryCount) {
    }
}
