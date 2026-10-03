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

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
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
 * GraphQL surface for long-term auto-memory rows. Every operation is workspace + environment scoped; single-row
 * operations are also principal scoped, and the listing operations filter owners through the same decision table.
 * Cross-workspace access is rejected with Spring Security's {@link AccessDeniedException}; missing rows surface as a
 * {@code NOT_FOUND} GraphQL error, a write that lost a race with another writer as {@code CONFLICT}, and invalid input
 * as {@code BAD_REQUEST}. NOT_FOUND and BAD_REQUEST carry the exception's message; CONFLICT carries a reload hint of
 * its own.
 *
 * <p>
 * The class-level {@code @PreAuthorize("isAuthenticated()")} requires a signed-in caller; per-method authorization is
 * enforced inline by checking workspace membership against {@link WorkspaceFacade#getUserWorkspaces(long)}. That facade
 * returns every workspace to a tenant admin, and otherwise only the workspaces in which the edition's
 * {@code PermissionService} gives the caller a role, which closes the cross-workspace privilege-escalation vector.
 *
 * <p>
 * Workspace membership does not isolate embedded memory: integration instances and connected-user projects all live in
 * the default workspace. Integration-instance memory is therefore never addressable here, and a deployment of an
 * embedded connected-user project is treated like a principal the caller may not address.
 *
 * @author Ivica Cardic
 */
@Controller
@PreAuthorize("isAuthenticated()")
public class AiAutoMemoryGraphQlController {

    private static final Logger log = LoggerFactory.getLogger(AiAutoMemoryGraphQlController.class);

    /**
     * Not one of Spring's {@link ErrorType}s: a lost race is neither a bad request nor a missing row, and a client
     * reacts to it differently — by reloading the memory rather than correcting its input.
     */
    static final ErrorClassification CONFLICT = ErrorClassification.errorClassification("CONFLICT");

    /**
     * The name prefix of the projects embedded connected users build, matching the automation repositories' filter.
     */
    private static final String EMBEDDED_PROJECT_NAME_PREFIX = "__EMBEDDED__";

    private final AiAutoMemoryService aiAutoMemoryService;
    private final UserService userService;
    private final WorkspaceFacade workspaceFacade;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI")
    public AiAutoMemoryGraphQlController(
        AiAutoMemoryService aiAutoMemoryService, UserService userService, WorkspaceFacade workspaceFacade,
        ProjectDeploymentService projectDeploymentService, ProjectService projectService) {

        this.aiAutoMemoryService = aiAutoMemoryService;
        this.userService = userService;
        this.workspaceFacade = workspaceFacade;
        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
    }

    @QueryMapping
    public List<AiAutoMemory> aiAutoMemories(
        @Argument long workspaceId, @Argument int environment, @Argument @Nullable AiAutoMemoryType memoryType,
        @Argument @Nullable AiAutoMemoryPrincipalInput principal) {

        long userId = userService.getCurrentUser()
            .getId();

        verifyUserCanAccessWorkspace(userId, workspaceId);

        // On THIS query an absent principal means "every owner I may see" — the Memories page's All scope. The
        // by-id query and the mutations keep the opposite default (absent = the caller), because there the
        // principal is an authorization decision about one row rather than a listing scope.
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

    /**
     * The All scope: one owner-agnostic read, then the same per-owner decision table the picker uses, so a caller never
     * sees a memory it could not have reached by picking that owner explicitly. Filtering here rather than in the
     * repository keeps the authorization in the one place that already owns it.
     */
    private List<AiAutoMemory> listAllAddressableOwners(
        long workspaceId, Environment environment, @Nullable AiAutoMemoryType memoryType, long userId) {

        List<ResolvedPrincipal> resolvedPrincipals = new ArrayList<>();

        for (AiAutoMemoryPrincipalCount principalCount : aiAutoMemoryService.listPrincipals(workspaceId, environment)) {
            ResolvedPrincipal resolved = resolvePrincipalForListing(principalCount, userId);

            if (resolved != null) {
                resolvedPrincipals.add(resolved);
            }
        }

        DeploymentLookup deploymentLookup = lookUpDeployments(resolvedPrincipals);

        Set<PrincipalKey> addressable = new HashSet<>();

        for (ResolvedPrincipal resolved : resolvedPrincipals) {
            if (!deploymentLookup.isEmbedded(resolved)) {
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

        List<AiAutoMemoryPrincipalCount> principalCounts =
            aiAutoMemoryService.listPrincipals(workspaceId, toEnvironment(environment));

        List<ResolvedPrincipalCount> addressable = new ArrayList<>();

        for (AiAutoMemoryPrincipalCount principalCount : principalCounts) {
            // Reuses the read path's decision table so the picker can never offer an owner the reads refuse.
            ResolvedPrincipal resolved = resolvePrincipalForListing(principalCount, userId);

            if (resolved != null) {
                addressable.add(new ResolvedPrincipalCount(resolved, principalCount.memoryCount()));
            }
        }

        DeploymentLookup deploymentLookup = lookUpDeployments(
            addressable.stream()
                .map(ResolvedPrincipalCount::principal)
                .toList());

        List<AiAutoMemoryPrincipal> principals = new ArrayList<>();

        for (ResolvedPrincipalCount resolvedCount : addressable) {
            ResolvedPrincipal resolved = resolvedCount.principal();

            if (deploymentLookup.isEmbedded(resolved)) {
                continue;
            }

            principals.add(
                new AiAutoMemoryPrincipal(
                    resolved.principalType(), resolved.principalId(),
                    resolveLabel(resolved, deploymentLookup.namesById()), resolvedCount.memoryCount()));
        }

        return principals;
    }

    /**
     * Looks up every deployment among the principals in two batch calls — deployments, then their projects. A
     * per-principal lookup inside the callers' loops would issue queries per owner, which is the N+1 this exists to
     * avoid.
     */
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

        Set<Long> embeddedProjectIds = projectIds.isEmpty() ? Set.of()
            : projectService.getProjects(projectIds)
                .stream()
                .filter(AiAutoMemoryGraphQlController::isEmbeddedProject)
                .map(Project::getId)
                .collect(Collectors.toSet());

        Set<Long> embeddedDeploymentIds = projectDeployments.stream()
            .filter(projectDeployment -> embeddedProjectIds.contains(projectDeployment.getProjectId()))
            .map(ProjectDeployment::getId)
            .collect(Collectors.toSet());

        return new DeploymentLookup(namesById, embeddedDeploymentIds);
    }

    private static boolean isEmbeddedProject(Project project) {
        String name = project.getName();

        return name != null && name.startsWith(EMBEDDED_PROJECT_NAME_PREFIX);
    }

    /**
     * Deployment names for labels, and the deployments that belong to embedded connected-user projects.
     */
    private record DeploymentLookup(Map<Long, String> namesById, Set<Long> embeddedDeploymentIds) {

        boolean isEmbedded(ResolvedPrincipal principal) {
            return principal.principalType() == AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT
                && embeddedDeploymentIds.contains(principal.principalId());
        }
    }

    /**
     * Pairs a principal the caller may address with how much memory it holds.
     */
    private record ResolvedPrincipalCount(ResolvedPrincipal principal, int memoryCount) {
    }

    /**
     * The listing variant of {@link #resolvePrincipal}: same rules, and a principal this caller may not address is
     * dropped the same way ({@code null}). The one difference is {@code INTEGRATION_INSTANCE}, which
     * {@link #resolvePrincipal} rejects as a bad request; a catalogue legitimately contains such entries, so they are
     * skipped here instead.
     */
    private @Nullable ResolvedPrincipal resolvePrincipalForListing(
        AiAutoMemoryPrincipalCount principalCount, long currentUserId) {

        AiAutoMemoryPrincipalType principalType = principalCount.principalType();

        if (principalType == AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE) {
            return null;
        }

        return resolvePrincipal(
            new AiAutoMemoryPrincipalInput(principalType, principalCount.principalId()), currentUserId, false);
    }

    /**
     * A USER principal reaching here is the caller: {@link #resolvePrincipal} resolves no other user.
     */
    private static String resolveLabel(ResolvedPrincipal principal, Map<Long, String> deploymentNames) {
        if (principal.principalType() == AiAutoMemoryPrincipalType.USER) {
            return "My memories";
        }

        // A deployment deleted while its memory rows remain is absent from the batch lookup: label it by id rather
        // than dropping the entry, so the orphaned memory stays reachable for cleanup.
        return deploymentNames.getOrDefault(principal.principalId(), "Deployment " + principal.principalId());
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

    /**
     * Exposed alongside {@link #principalId} because the id alone does not identify an owner: memory of every principal
     * type shares one table, so the same numeric id means a different owner under a different principal type. The pair
     * is exposed for exactly that reason, rather than assuming the id is a user id.
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
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
    }

    /**
     * The schema's {@code environmentId} is the environment's ordinal — the value the client sends back as
     * {@code environment} — which the domain exposes as {@link AiAutoMemory#getEnvironmentOrdinal()}.
     */
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

    /**
     * Without these handlers the domain exceptions are unmapped and reach the client as a bare {@code INTERNAL_ERROR},
     * hiding messages such as "title is required" from the user. Not-found keeps the service's deliberately generic
     * message, so a probe still cannot tell "not yours" from "no such memory".
     */
    @GraphQlExceptionHandler
    public GraphQLError handleNotFound(
        AiAutoMemoryNotFoundException exception, DataFetchingEnvironment dataFetchingEnvironment) {

        return GraphqlErrorBuilder.newError(dataFetchingEnvironment)
            .errorType(ErrorType.NOT_FOUND)
            .message(exception.getMessage())
            .build();
    }

    /**
     * Another write changed or removed the memory between the caller's read and this write, so nothing was written. The
     * exception's own message tells the agent to read and redo its edit; this one fits the page's update and delete.
     */
    @GraphQlExceptionHandler
    public GraphQLError handleConflict(
        AiAutoMemoryConcurrentModificationException exception, DataFetchingEnvironment dataFetchingEnvironment) {

        logRejection(dataFetchingEnvironment, exception);

        return GraphqlErrorBuilder.newError(dataFetchingEnvironment)
            .errorType(CONFLICT)
            .message("Memory '" + exception.getName() + "' was changed by someone else. Reload it and try again.")
            .build();
    }

    /**
     * A bare {@link IllegalArgumentException} is logged at WARN because it can also come from a bug below the
     * validation layer, and an exception handled here is not logged by the framework.
     */
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

    private static void logRejection(DataFetchingEnvironment dataFetchingEnvironment, RuntimeException exception) {
        if (log.isDebugEnabled()) {
            Field field = dataFetchingEnvironment.getField();

            log.debug("Rejected auto-memory request {}", field.getName(), exception);
        }
    }

    /**
     * {@link #resolvePrincipal}, plus the embedded check a deployment principal needs: a deployment of an embedded
     * connected-user project is denied the same way as any other principal the caller may not address.
     */
    private @Nullable ResolvedPrincipal resolveAddressablePrincipal(
        @Nullable AiAutoMemoryPrincipalInput principal, long currentUserId, boolean mutating) {

        ResolvedPrincipal resolvedPrincipal = resolvePrincipal(principal, currentUserId, mutating);

        if (resolvedPrincipal == null || lookUpDeployments(List.of(resolvedPrincipal)).isEmbedded(resolvedPrincipal)) {
            return null;
        }

        return resolvedPrincipal;
    }

    /**
     * Decides which principal the caller may address. Returns {@code null} when the caller may not address the
     * requested principal — reads map that to an empty result and mutations to not-found, so a caller cannot tell "not
     * yours" from "no such memory" and ids stay unenumerable. An absent principal is the caller. Throws for
     * {@code INTEGRATION_INSTANCE}, which this API never addresses.
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

    /**
     * The principal a request resolved to, after defaulting and authorization.
     */
    record ResolvedPrincipal(AiAutoMemoryPrincipalType principalType, long principalId) {

        AiAutoMemoryOwner toOwner(long workspaceId, Environment environment) {
            return new AiAutoMemoryOwner(workspaceId, principalType, principalId, environment);
        }
    }

    /**
     * The owner a request addresses. A type and an id always arrive together — the schema has no way to send one
     * without the other.
     */
    public record AiAutoMemoryPrincipalInput(AiAutoMemoryPrincipalType principalType, long principalId) {

        public AiAutoMemoryPrincipalInput {
            Objects.requireNonNull(principalType, "principalType");
        }
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
     * is not reachable from this session. {@code expectedVersion} is the version the edit was based on; a memory
     * changed since is not overwritten. Omitting {@code principal} targets the signed-in user.
     */
    public record UpdateAiAutoMemoryInput(
        long id, long workspaceId, int environment, long expectedVersion, @Nullable String title,
        @Nullable String description, @Nullable AiAutoMemoryType memoryType, @Nullable String content,
        @Nullable AiAutoMemoryPrincipalInput principal) {
    }
}
