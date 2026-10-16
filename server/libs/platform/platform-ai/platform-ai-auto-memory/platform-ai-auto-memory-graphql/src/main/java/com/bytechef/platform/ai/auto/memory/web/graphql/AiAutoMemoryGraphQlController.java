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
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.ownership.ProjectOwner;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryConcurrentModificationException;
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
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@PreAuthorize("isAuthenticated()")
public class AiAutoMemoryGraphQlController {

    private static final Logger log = LoggerFactory.getLogger(AiAutoMemoryGraphQlController.class);

    static final ErrorClassification CONFLICT = ErrorClassification.errorClassification("CONFLICT");

    private final AiAutoMemoryService aiAutoMemoryService;
    private final UserService userService;
    private final WorkspaceFacade workspaceFacade;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectFacade projectFacade;

    @SuppressFBWarnings("EI")
    public AiAutoMemoryGraphQlController(
        AiAutoMemoryService aiAutoMemoryService, UserService userService, WorkspaceFacade workspaceFacade,
        ProjectDeploymentService projectDeploymentService, ProjectFacade projectFacade) {

        this.aiAutoMemoryService = aiAutoMemoryService;
        this.userService = userService;
        this.workspaceFacade = workspaceFacade;
        this.projectDeploymentService = projectDeploymentService;
        this.projectFacade = projectFacade;
    }

    @QueryMapping
    public List<AiAutoMemory> aiAutoMemories(
        @Argument long workspaceId, @Argument int environment, @Argument @Nullable AiAutoMemoryType memoryType,
        @Argument @Nullable AiAutoMemoryPrincipalInput principal) {

        long userId = userService.getCurrentUser()
            .getId();

        verifyUserCanAccessWorkspace(userId, workspaceId);

        if (principal == null) {
            return listAllAddressableOwners(workspaceId, toEnvironment(environment), memoryType, userId);
        }

        ResolvedPrincipal resolvedPrincipal = resolveAddressablePrincipal(principal, userId, false);

        if (resolvedPrincipal == null) {
            return List.of();
        }

        return aiAutoMemoryService.list(
            resolvedPrincipal.toOwner(workspaceId, toEnvironment(environment)), memoryType);
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

        Set<PrincipalKey> addressablePrincipalKeys = new HashSet<>();

        for (ResolvedPrincipal resolvedPrincipal : resolvedPrincipals) {
            if (!deploymentLookup.isEmbedded(resolvedPrincipal)) {
                addressablePrincipalKeys
                    .add(new PrincipalKey(resolvedPrincipal.principalType(), resolvedPrincipal.principalId()));
            }
        }

        return aiAutoMemoryService.listAllOwners(workspaceId, environment, memoryType)
            .stream()
            .filter(
                memory -> addressablePrincipalKeys
                    .contains(new PrincipalKey(memory.getPrincipalType(), memory.getPrincipalId())))
            .toList();
    }

    @QueryMapping
    @Nullable
    public AiAutoMemory aiAutoMemory(
        @Argument long workspaceId, @Argument long id, @Argument int environment,
        @Argument @Nullable AiAutoMemoryPrincipalInput principal) {

        long userId = userService.getCurrentUser()
            .getId();

        verifyUserCanAccessWorkspace(userId, workspaceId);

        ResolvedPrincipal resolvedPrincipal = resolveAddressablePrincipal(principal, userId, false);

        if (resolvedPrincipal == null) {
            return null;
        }

        Optional<AiAutoMemory> memory = aiAutoMemoryService.findById(
            resolvedPrincipal.toOwner(workspaceId, toEnvironment(environment)), id);

        return memory.orElse(null);
    }

    @QueryMapping
    public List<AiAutoMemoryPrincipal> aiAutoMemoryPrincipals(@Argument long workspaceId, @Argument int environment) {
        long userId = userService.getCurrentUser()
            .getId();

        verifyUserCanAccessWorkspace(userId, workspaceId);

        List<AiAutoMemoryPrincipalCount> aiAutoMemoryPrincipalCounts =
            aiAutoMemoryService.listPrincipals(workspaceId, toEnvironment(environment));

        List<ResolvedPrincipalCount> resolvedPrincipalCounts = new ArrayList<>();

        for (AiAutoMemoryPrincipalCount principalCount : aiAutoMemoryPrincipalCounts) {
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

        List<Long> projectIds = projectDeployments.stream()
            .map(ProjectDeployment::getProjectId)
            .distinct()
            .toList();

        Set<Long> embeddedProjectIds = projectFacade.getOwnedProjectIds(
            projectIds, ProjectOwner.EMBEDDED_CONNECTED_USER);

        Set<Long> embeddedDeploymentIds = projectDeployments.stream()
            .filter(projectDeployment -> embeddedProjectIds.contains(projectDeployment.getProjectId()))
            .map(ProjectDeployment::getId)
            .collect(Collectors.toSet());

        return new DeploymentLookup(namesById, embeddedDeploymentIds);
    }

    private record DeploymentLookup(Map<Long, String> namesById, Set<Long> embeddedDeploymentIds) {

        boolean isEmbedded(ResolvedPrincipal principal) {
            return principal.principalType() == AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT
                && embeddedDeploymentIds.contains(principal.principalId());
        }
    }

    private record ResolvedPrincipalCount(ResolvedPrincipal principal, int memoryCount) {
    }

    private @Nullable ResolvedPrincipal resolvePrincipalForListing(
        AiAutoMemoryPrincipalCount aiAutoMemoryPrincipalCount, long currentUserId) {

        AiAutoMemoryPrincipalType aiAutoMemoryPrincipalType = aiAutoMemoryPrincipalCount.principalType();

        if (aiAutoMemoryPrincipalType == AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE) {
            return null;
        }

        return resolvePrincipal(
            new AiAutoMemoryPrincipalInput(
                aiAutoMemoryPrincipalType, aiAutoMemoryPrincipalCount.principalId()),
            currentUserId, false);
    }

    private static String resolveLabel(ResolvedPrincipal resolvedPrincipal, Map<Long, String> deploymentNames) {
        if (resolvedPrincipal.principalType() == AiAutoMemoryPrincipalType.USER) {
            return "My memories";
        }

        return deploymentNames.getOrDefault(
            resolvedPrincipal.principalId(), "Deployment " + resolvedPrincipal.principalId());
    }

    @MutationMapping
    public AiAutoMemory updateAiAutoMemory(@Argument UpdateAiAutoMemoryInput input) {
        long userId = userService.getCurrentUser()
            .getId();

        verifyUserCanAccessWorkspace(userId, input.workspaceId());

        ResolvedPrincipal principal = resolveAddressablePrincipal(input.principal(), userId, true);

        if (principal == null) {
            throw new AiAutoMemoryNotFoundException("Memory not found");
        }

        AiAutoMemoryPatch patch = new AiAutoMemoryPatch(
            input.title(), input.description(), input.memoryType(), input.content());

        return aiAutoMemoryService.updateById(
            principal.toOwner(input.workspaceId(), toEnvironment(input.environment())), input.id(),
            input.expectedVersion(), patch);
    }

    @MutationMapping
    public boolean deleteAiAutoMemory(
        @Argument long workspaceId, @Argument long id, @Argument int environment,
        @Argument @Nullable AiAutoMemoryPrincipalInput principal) {

        long userId = userService.getCurrentUser()
            .getId();

        verifyUserCanAccessWorkspace(userId, workspaceId);

        ResolvedPrincipal resolvedPrincipal = resolveAddressablePrincipal(principal, userId, true);

        if (resolvedPrincipal == null) {
            throw new AiAutoMemoryNotFoundException("Memory not found");
        }

        aiAutoMemoryService.deleteById(resolvedPrincipal.toOwner(workspaceId, toEnvironment(environment)), id);

        return true;
    }

    @SchemaMapping(typeName = "AiAutoMemory", field = "principalId")
    public long principalId(AiAutoMemory memory) {
        return memory.getPrincipalId();
    }

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
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
    }

    @SchemaMapping(typeName = "AiAutoMemory", field = "environmentId")
    public long environmentId(AiAutoMemory memory) {
        return memory.getEnvironmentOrdinal();
    }

    @SchemaMapping(typeName = "AiAutoMemory", field = "updatedAt")
    @Nullable
    public Long updatedAt(AiAutoMemory memory) {
        return memory.getUpdatedAt() == null ? null
            : memory.getUpdatedAt()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
    }

    @GraphQlExceptionHandler
    public GraphQLError handleNotFound(
        AiAutoMemoryNotFoundException exception, DataFetchingEnvironment dataFetchingEnvironment) {

        return GraphqlErrorBuilder.newError(dataFetchingEnvironment)
            .errorType(ErrorType.NOT_FOUND)
            .message(exception.getMessage())
            .build();
    }

    @GraphQlExceptionHandler
    public GraphQLError handleConflict(
        AiAutoMemoryConcurrentModificationException exception, DataFetchingEnvironment dataFetchingEnvironment) {

        logRejection(dataFetchingEnvironment, exception);

        return GraphqlErrorBuilder.newError(dataFetchingEnvironment)
            .errorType(CONFLICT)
            .message("Memory '" + exception.getName() + "' was changed by someone else. Reload it and try again.")
            .build();
    }

    @GraphQlExceptionHandler(IllegalArgumentException.class)
    public GraphQLError handleBadRequest(
        IllegalArgumentException exception, DataFetchingEnvironment dataFetchingEnvironment) {

        Field field = dataFetchingEnvironment.getField();

        log.warn("Rejected auto-memory request {}", field.getName(), exception);

        return GraphqlErrorBuilder.newError(dataFetchingEnvironment)
            .errorType(ErrorType.BAD_REQUEST)
            .message(exception.getMessage())
            .build();
    }

    @Nullable
    ResolvedPrincipal resolvePrincipal(
        @Nullable AiAutoMemoryPrincipalInput principal, long currentUserId, boolean mutating) {

        if (principal == null) {
            return new ResolvedPrincipal(AiAutoMemoryPrincipalType.USER, currentUserId);
        }

        AiAutoMemoryPrincipalType principalType = principal.principalType();
        long principalId = principal.principalId();

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

    private static void logRejection(DataFetchingEnvironment dataFetchingEnvironment, RuntimeException exception) {
        if (log.isDebugEnabled()) {
            Field field = dataFetchingEnvironment.getField();

            log.debug("Rejected auto-memory request {}", field.getName(), exception);
        }
    }

    private @Nullable ResolvedPrincipal resolveAddressablePrincipal(
        @Nullable AiAutoMemoryPrincipalInput principal, long currentUserId, boolean mutating) {

        ResolvedPrincipal resolvedPrincipal = resolvePrincipal(principal, currentUserId, mutating);

        if (resolvedPrincipal == null || lookUpDeployments(List.of(resolvedPrincipal)).isEmbedded(resolvedPrincipal)) {
            return null;
        }

        return resolvedPrincipal;
    }

    private static Environment toEnvironment(int environment) {
        Environment[] environments = Environment.values();

        if (environment < 0 || environment >= environments.length) {
            throw new IllegalArgumentException("Unknown environment: " + environment);
        }

        return environments[environment];
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

    public record AiAutoMemoryPrincipalInput(AiAutoMemoryPrincipalType principalType, long principalId) {

        public AiAutoMemoryPrincipalInput {
            Objects.requireNonNull(principalType, "principalType");
        }
    }

    public record AiAutoMemoryPrincipal(
        AiAutoMemoryPrincipalType principalType, long principalId, String label, int memoryCount) {
    }

    public record UpdateAiAutoMemoryInput(
        long id, long workspaceId, int environment, long expectedVersion, @Nullable String title,
        @Nullable String description, @Nullable AiAutoMemoryType memoryType, @Nullable String content,
        @Nullable AiAutoMemoryPrincipalInput principal) {
    }

    private record ResolvedPrincipal(AiAutoMemoryPrincipalType principalType, long principalId) {

        AiAutoMemoryOwner toOwner(long workspaceId, Environment environment) {
            return new AiAutoMemoryOwner(workspaceId, principalType, principalId, environment);
        }
    }

    private record PrincipalKey(AiAutoMemoryPrincipalType principalType, long principalId) {
    }
}
