/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.security.ResourceMembershipResolver;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.security.web.authentication.PrincipalEnvironment;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Answers a resource-scoped {@code hasPermission(...)} check directly from an embedded connected user's own membership:
 * the {@link ConnectedUserProject} the request's caller owns, the {@code ProjectDeployment}s that project and the
 * caller's own {@link ConnectedUserProjectWorkflow} reference rows point at, the {@code Connection}s
 * {@link ConnectedUserConnectionMembership} entitles them to, and (for {@code Workflow} and {@code Project}) the
 * published catalog {@link AutomationWorkflowProjectFacade} would show the same caller.
 *
 * <p>
 * Three of those are ENTITLEMENT rather than ownership -- a configuration-level shared connection, an admin-owned
 * catalog template, and the admin-owned catalog PROJECT a reference is provisioned against -- and all three are
 * therefore answered per-scope: usable, readable and referencable, not mutable. See
 * {@link #resolve(Serializable, String, String)}.
 *
 * <p>
 * As of ticket 1051 Stage 2 this resolver's answer is authoritative. For a connected user it governs, a {@code DENIED}
 * here is a 403 the caller receives, and {@code @SkipAutomationAuthorization} no longer grants that caller anything.
 *
 * <p>
 * SIX methods consult {@code ResourceMembershipDecider.decide} and so consume what this class answers. The full list
 * matters when narrowing a resource type -- reasoning from a partial one is how a live surface gets missed:
 *
 * <ol>
 * <li>{@code AutomationPermissionEvaluator.hasPermission(auth, targetDomainObject, permission)} -- the two-argument
 * form, hardcoded to {@code 'Project'}</li>
 * <li>{@code AutomationPermissionEvaluator.hasPermission(auth, targetId, targetType, permission)} -- carries ANY
 * resource type, so every {@code @PreAuthorize("hasPermission(#id, 'Workflow'|'Job', ...)")} arrives here</li>
 * <li>{@code AutomationMethodSecurityExpressionRoot.hasWorkflowScopeInEnvironment} -- hardcoded to {@code 'Workflow'},
 * and easy to miss because it reads the skip mode itself and never reaches {@code hasResourceScope}; it gates
 * {@code POST /api/platform/internal/workflows/{id}/tests}</li>
 * <li>EE {@code PermissionServiceImpl.hasWorkspaceScopeForProject(long, String, Environment)} -- hardcoded to
 * {@code 'Project'}</li>
 * <li>EE {@code PermissionServiceImpl.hasResourceScope} -- carries ANY resource type</li>
 * <li>EE {@code PermissionServiceImpl.hasWorkflowScope(String, String, Environment)} -- hardcoded to
 * {@code 'Workflow'}</li>
 * </ol>
 *
 * <p>
 * Four of the six can carry {@code 'Workflow'} or {@code 'Job'} and are therefore affected by a change to
 * {@link #resolveWorkflow}; the two {@code 'Project'}-keyed ones are not. The reverse split holds for
 * {@link #resolveProject}: those two plus the two type-carrying ones, (2) and (5), reach it. Note that (3) differs from
 * the rest in shape: it short-circuits on {@code DENY} only and otherwise falls through, so this resolver closes it by
 * DENYING rather than by out-ranking the skip check. Adding a seventh consumer means adding a line here.
 *
 * <p>
 * The caller is read from the {@code SecurityContext} via {@link SecurityUtils#fetchCurrentUserLogin()}, never from a
 * method argument, so a check cannot be satisfied by naming somebody else's external id.
 *
 * <p>
 * The environment comes off the PRINCIPAL — {@link PrincipalEnvironment} on the {@code SecurityContext}'s
 * authentication — and never from {@code EnvironmentContext}. That thread-local is bound by facades, agents and
 * advisors, and by no request entry point at all: no filter or interceptor anywhere sets it, and no embedded request
 * filter does either. On an embedded HTTP request it is simply unset, so {@code getCurrentEnvironment()} answers
 * {@link Environment#PRODUCTION} by default — silently right for production tenants and silently wrong for every other
 * environment. Reading it here would also hand back the design's own argument that a principal-driven seam survives
 * async hops where a thread-local does not.
 *
 * <p>
 * The principal is the right source and is available wherever this resolver runs: the embedded request path
 * authenticates as {@code EmbeddedApiKeyAuthenticationToken}, whose provider now carries the environment into the
 * authenticated token, and the copilot hand-off carries that same {@code Authentication} object across its worker
 * threads (see {@code CopilotToolContextUtils}) rather than rebuilding one. A principal that is not an
 * {@link AbstractApiKeyAuthenticationToken} carries no environment and is therefore not an embedded caller at all, so
 * this resolver does not govern it — which is also why an ordinary platform request now costs no query here.
 *
 * <p>
 * {@link #resolve(Serializable, String, String)} fails closed: an id of the wrong {@link Serializable} type, an unknown
 * resource, or an evaluation error all deny rather than throw or grant. {@link #governsCurrentPrincipal()} deliberately
 * does NOT, and the difference is the point rather than an inconsistency -- a {@code DENIED} can only ever deny,
 * whereas answering "not governed" delegates the decision to a path that may grant, so an unavailable truth must not be
 * synthesized there. See both methods.
 *
 * <p>
 * {@link #resolve(Serializable, String, String)} never uses
 * {@link ConnectedUserProjectService#getOrCreateConnectedUserProject} -- an authorization predicate that provisions a
 * project as a side effect would be a write on the read path and would let an unknown external id create itself one by
 * failing a permission check; see {@link ConnectedUserProjectService#fetchConnectUserProject}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ConnectedUserResourceMembershipResolver implements ResourceMembershipResolver {

    private static final Logger log = LoggerFactory.getLogger(ConnectedUserResourceMembershipResolver.class);

    private static final String PROJECT = "Project";
    private static final String WORKFLOW = "Workflow";
    private static final String PROJECT_DEPLOYMENT = "ProjectDeployment";
    private static final String PROJECT_WORKFLOW = "ProjectWorkflow";
    private static final String CONNECTION = "Connection";
    private static final String JOB = "Job";

    /**
     * The {@code Connection} scopes a CONFIGURATION-LEVEL shared connection may satisfy. Deliberately an ALLOWLIST: a
     * scope token added to {@code ConnectionPermissionScope} later must fall back to the owned set until somebody
     * decides otherwise, rather than silently inheriting the wider one because nobody remembered to block-list it.
     */
    private static final Set<String> SHAREABLE_CONNECTION_SCOPES = Set.of("CONNECTION_VIEW", "CONNECTION_USE");

    /**
     * The {@code Workflow} scopes an admin-owned CATALOG template may satisfy for a connected user who merely SEES it
     * in the published listing. {@code WorkflowPermissionScope} has four values and this is the only read one:
     * {@code WORKFLOW_EDIT} and {@code WORKFLOW_DELETE} act on the tenant admin's template, and {@code WORKFLOW_CREATE}
     * is a write too -- it is only ever checked against a {@code Project}, never a {@code Workflow}, so it cannot reach
     * this set today, and listing it would advertise a grant nothing asks for.
     *
     * <p>
     * Deliberately an ALLOWLIST, for the same reason as {@link #SHAREABLE_CONNECTION_SCOPES}.
     */
    private static final Set<String> CATALOG_READABLE_WORKFLOW_SCOPES = Set.of("WORKFLOW_VIEW");

    /**
     * The {@code Job} equivalent, and a SEPARATE set rather than a reuse: a {@code Job} is checked with
     * {@code ExecutionPermissionScope} tokens, which is a different enum from the one above, so a shared allowlist
     * would deny every job check the moment the catalog arm started consulting it.
     *
     * <p>
     * {@code EXECUTION_VIEW} has to be in it. {@code ConnectedUserCodeWorkflowReferenceFacadeImpl.getOrCreateReference}
     * provisions a {@code ProjectDeployment} on the CATALOG project and runs the catalog workflow id itself -- the
     * connected user gets no copy -- so a connected user's own run of a referenced catalog workflow produces a job
     * whose {@code workflowId} is the catalog workflow's. Dropping it would hide those runs from the user who caused
     * them. {@code EXECUTION_DELETE} is a write ({@code LogFileStorageImpl.deleteLogs}) and stays out.
     *
     * <p>
     * Membership of this set selects the catalog arm; it does not by itself grant. {@link #resolveJob} then requires
     * the run to be the CALLER'S OWN, because a visible catalog template is shared by every connected user entitled to
     * see it and the workflow id alone cannot tell their runs apart.
     */
    private static final Set<String> CATALOG_READABLE_EXECUTION_SCOPES = Set.of("EXECUTION_VIEW");

    /**
     * The {@code Project} scopes a REFERENCABLE catalog project may satisfy for a connected user who can see it in the
     * published listing. One token, and it has to be there:
     * {@code ConnectedUserCodeWorkflowReferenceFacadeImpl.getOrCreateReference} provisions the reference's
     * {@code ProjectDeployment} on the CATALOG project, and {@code ProjectDeploymentServiceImpl.create} -- the only
     * {@code hasPermission} gate anywhere on that call chain -- is
     * {@code hasPermission(#projectDeployment.projectId, 'Project', 'DEPLOYMENT_PUSH')}. Without this set FIRST-USE
     * provisioning 403s, and the enable toggle behind it is unreachable for any reference not created before the gate
     * could deny it.
     *
     * <p>
     * Deliberately an ALLOWLIST, for the same reason as {@link #SHAREABLE_CONNECTION_SCOPES}, and doubly so here:
     * {@link #resolveProject}'s owned branch grants EVERY scope, so an entitled branch that did the same would hand
     * every connected user every verb on the tenant admin's catalog project -- {@code PROJECT_DELETE},
     * {@code PROJECT_SETTINGS}, {@code WORKFLOW_CREATE}, {@code WORKFLOW_EDIT}, {@code WORKFLOW_DELETE} and
     * {@code DEPLOYMENT_PULL} are all live {@code 'Project'}-keyed tokens, and a GRANT here returns ahead of the
     * tenant-admin check and RBAC.
     *
     * <p>
     * {@code 'Project'} is checked against THREE scope enums ({@code ProjectPermissionScope},
     * {@code WorkflowPermissionScope}, {@code DeploymentPermissionScope}), which is why
     * {@link #resolveProjectDeployment} argues an allowlist could not be written for it. A single token whose name is
     * unique across all three is not that conflation: this set says "DEPLOYMENT_PUSH and nothing else", so no token
     * from another enum can fall into it by accident, and a token added to any of the three falls to the owned set.
     *
     * <p>
     * {@code DEPLOYMENT_PUSH} is nonetheless a wider verb than provisioning: it also gates
     * {@code ProjectDeploymentServiceImpl.update}, {@code ProjectServiceImpl.publishProject},
     * {@code ProjectWorkflowServiceImpl.publishWorkflow} and EE {@code ProjectGitFacadeImpl.pushProjectToGit}. TWO of
     * those five reachable effects follow from this set, not one:
     *
     * <ol>
     * <li>{@code create} -- the flow this set exists for.</li>
     * <li>{@code update} -- and this one is a genuine behaviour change, not merely a site that stays shut.
     * {@code ProjectDeploymentFacadeImpl.updateProjectDeployment} is its only entry and is gated
     * {@code 'ProjectDeployment'}/{@code DEPLOYMENT_EDIT}, which {@link #resolveProjectDeployment} already granted for
     * the caller's own reference deployment -- so the request reached the INNER {@code DEPLOYMENT_PUSH} gate and 403'd
     * one layer deeper. It now succeeds: {@code PUT /api/automation/internal/project-deployments/&#123;id&#125;}
     * against their own reference deployment can change {@code name}, {@code description}, {@code enabled},
     * {@code projectVersion} and {@code tagIds}. Defensible -- it is their own row, {@code projectId} is never
     * persisted from the request, and no other connected user's deployment is in reach -- but the worst case is worth
     * naming: pinning their own reference to a different published catalog version, or renaming it so the next
     * {@code fetchProjectDeploymentByName} provisions a second row alongside it.</li>
     * </ol>
     *
     * <p>
     * The remaining three stay shut, each behind a SECOND gate this set does not open: {@code publishProject} and
     * {@code publishWorkflow} only through {@code ProjectFacadeImpl.publishProject}
     * ({@code 'Project'}/{@code WORKFLOW_EDIT}), the admin facade's {@code isTenantAdmin()}, or a code-workflow
     * {@code deployInto} that must first pass {@code ProjectWorkflowServiceImpl.addWorkflow}
     * ({@code 'Project'}/{@code WORKFLOW_CREATE}); and {@code pushProjectToGit} has no controller at all -- its only
     * production caller is {@code ProjectGitSyncEventListenerImpl}, reached from a publish that is already denied.
     *
     * <p>
     * Adding a {@code 'Project'}-keyed {@code DEPLOYMENT_PUSH} gate with no second gate of its own means re-doing that
     * analysis, not extending this set. And the question to re-do it with is NOT "can this site be reached?" but "does
     * this site's BEHAVIOUR change?" -- {@code update} was reachable all along and still started permitting more,
     * because the denial was happening a layer below the gate that let the caller in.
     *
     * <p>
     * One last reason the set must stay at one token: the entitlement predicate behind it is CLIENT-INFLUENCEABLE.
     * {@code EmbeddedPermissionEvaluator.evaluate} returns true for a blank expression, and otherwise evaluates the
     * tenant admin's expression against {@code metadata}, {@code email} and {@code name} read off the connected-user
     * row -- all of which the connected user can set on themselves through
     * {@code PUT /api/embedded/v1/&#123;externalUserId&#125;}. That is pre-existing and not widened here (the
     * {@code Workflow} and {@code Job} catalog arms already rest on it), but it is precisely why a wider allowlist
     * later would be dangerous: the caller has partial influence over whether they are inside the entitled set.
     */
    private static final Set<String> CATALOG_PROVISIONABLE_PROJECT_SCOPES = Set.of("DEPLOYMENT_PUSH");

    private final AutomationWorkflowProjectFacade automationWorkflowProjectFacade;
    private final ConnectedUserConnectionMembership connectedUserConnectionMembership;
    private final ConnectedUserProjectService connectedUserProjectService;
    private final ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService;
    private final ConnectedUserService connectedUserService;
    private final JobService jobService;
    private final PrincipalJobService principalJobService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;

    @SuppressFBWarnings("EI")
    public ConnectedUserResourceMembershipResolver(
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade,
        ConnectedUserConnectionMembership connectedUserConnectionMembership,
        ConnectedUserProjectService connectedUserProjectService,
        ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService,
        ConnectedUserService connectedUserService, JobService jobService, PrincipalJobService principalJobService,
        ProjectDeploymentService projectDeploymentService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService) {

        this.automationWorkflowProjectFacade = automationWorkflowProjectFacade;
        this.connectedUserConnectionMembership = connectedUserConnectionMembership;
        this.connectedUserProjectService = connectedUserProjectService;
        this.connectedUserProjectWorkflowService = connectedUserProjectWorkflowService;
        this.connectedUserService = connectedUserService;
        this.jobService = jobService;
        this.principalJobService = principalJobService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
    }

    @Override
    public boolean governsCurrentPrincipal() {
        Optional<String> externalUserId = SecurityUtils.fetchCurrentUserLogin();

        if (externalUserId.isEmpty()) {
            return false;
        }

        Environment environment = fetchCurrentPrincipalEnvironment();

        if (environment == null) {
            return false;
        }

        // Deliberately NOT wrapped in a catch. This used to swallow RuntimeException and answer false, described in
        // the code as failing closed; it was the opposite. Answering false here denies GOVERNANCE, not the check --
        // ResourceMembershipDecider returns NOT_GOVERNED and the caller falls through to its ordinary path, which
        // while EmbeddedAutomationAuthorizationSkipFilter existed was the resource-scoped skip. A transient failure of
        // this one query therefore GRANTED the check. Whether `return false` failed open or closed was never a property
        // of the catch block at all; it was a property of what the caller did with the value, and nothing here said
        // which world this was running in -- which is why the catch had to go before the filter could.
        //
        // An authorization answer derived from a query that failed is not an answer. Letting the exception out makes
        // the honest response to "I cannot determine whether you may do this" a 500 rather than a silent decision in
        // whichever direction the surrounding deployment happens to point.
        //
        // Safe to rethrow because the blast radius is exactly the callers that would otherwise be answered wrongly:
        // both guards above return before this line for every principal carrying no environment. A session principal
        // carries none, an unauthenticated thread carries no login, and five of the eight
        // AbstractApiKeyAuthenticationToken subclasses are built from the User-only super constructor and so carry
        // none either -- an ordinary platform or public-API request cannot be turned into a 500 by this method.
        //
        // The copilot worker threads DO reach this line: WorkflowEditorSpringAIAgent runs as the carried
        // EmbeddedApiKeyAuthenticationToken, which carries an environment. Those threads now bind the caller's tenant
        // (CopilotSpringAIAgent.run for the agent body, RehydrateContextToolCallback's withTenant for tool calls), so
        // the query runs against the caller's own schema and this resolver governs there like anywhere else. It did
        // not always: TenantContext is a non-inheritable ThreadLocal and used to default to "public" on them, which is
        // what the now-deleted resource-scoped skip mode existed to paper over. Even then this was not an exposure,
        // because connected_user EXISTS in the public schema -- LiquibaseConfiguration's SpringLiquibase bean is
        // unconditional and runs master.xml against the connection's default schema with no
        // spring.liquibase.default-schema configured, and master.xml includes embedded/connected_user/ under
        // contextFilter "mono or user or multitenant" -- so the query returned empty rather than throwing.
        //
        // Do not restore the catch as a robustness improvement: it does not make anything more robust, it only moves
        // the failure from a visible 500 to an invisible authorization decision.
        return connectedUserService.fetchConnectedUser(externalUserId.get(), environment)
            .isPresent();
    }

    /**
     * The environment carried by the current principal, or {@code null} when it carries none -- which is the same thing
     * as "not an api-key caller", that being the only kind of principal that carries one.
     *
     * <p>
     * Delegates to {@link PrincipalEnvironment}, which {@code AutomationMethodSecurityExpressionRoot} and the two test
     * controllers also use, so the environment this resolver decides membership in and the one those sites authorise
     * and execute in are read the same way and cannot drift apart.
     *
     * <p>
     * That accessor answers empty rather than {@code 0} for a token whose provider carried no environment, and the
     * difference is the soundness of this guard: {@code 0} is a VALID ordinal ({@code DEVELOPMENT}), so a fabricated
     * one would send this resolver looking for a connected user there. Usually it finds none and answers "not
     * governed", which is merely wrong; in the case that matters it finds a DIFFERENT connected user sharing that
     * external id in DEVELOPMENT and answers from that user's memberships. An out-of-range ordinal is likewise treated
     * as absent rather than clamped: an environment that cannot be identified must not be guessed at.
     */
    private static Environment fetchCurrentPrincipalEnvironment() {
        Long environmentId = PrincipalEnvironment.fetchCurrentPrincipalEnvironmentId()
            .orElse(null);

        if (environmentId == null) {
            return null;
        }

        Environment[] environments = Environment.values();

        if (environmentId < 0 || environmentId >= environments.length) {
            return null;
        }

        return environments[environmentId.intValue()];
    }

    @Override
    public Decision resolve(Serializable id, String resourceType, String scope) {
        // scope is used for PROJECT, CONNECTION, WORKFLOW and JOB, and ignored by PROJECT_DEPLOYMENT and
        // PROJECT_WORKFLOW. The split is not an inconsistency -- it tracks whether the resource's membership set means
        // OWNERSHIP or ENTITLEMENT.
        //
        // PROJECT_DEPLOYMENT and PROJECT_WORKFLOW are pure ownership questions: the set is the caller's own
        // ConnectedUserProject and what hangs off it, and owning a resource entitles you to every verb on it, so the
        // answer genuinely does not depend on whether the caller asked WORKFLOW_VIEW or WORKFLOW_EDIT.
        //
        // PROJECT was in that list until the catalog arm was added to resolveProject. Its FIRST branch is still pure
        // ownership and still grants every scope; its second grants the admin-owned CATALOG project a reference is
        // provisioned against, which is entitlement, so it is offered only for
        // CATALOG_PROVISIONABLE_PROJECT_SCOPES -- a single token, because the owned branch's "every verb" would
        // otherwise become every verb on the tenant admin's project.
        //
        // PROJECT_DEPLOYMENT takes no scope for a second reason on top of that one, and it is the reason an allowlist
        // could not be written here even if ownership were in doubt: the token is checked against TWO enums.
        // ProjectDeploymentFacadeImpl gates nine methods on 'ProjectDeployment', eight with DeploymentPermissionScope
        // tokens (DEPLOYMENT_EDIT, DEPLOYMENT_VIEW) and one -- getProjectDeploymentWorkflow(WorkflowExecutionId) --
        // additionally with the WorkflowPermissionScope token WORKFLOW_VIEW against the same resource type. A single
        // allowlist spanning two enums is the conflation the CONNECTION/WORKFLOW/JOB sets were split up to avoid.
        //
        // CONNECTION is not, since ticket 1051 gave it a third source: connections bound at the
        // IntegrationInstanceConfiguration level are SHARED with every connected user attached to that configuration.
        // A shared connection is one you may use, not one you may rename, retag or delete -- doing either would act on
        // every other user of that configuration at once. So the connection branch picks its set by scope. This
        // comment said the opposite while the set still meant ownership; widening the set falsified the premise, and
        // the check that made the statement true is resolveConnection. Do not restore the simplification.
        //
        // WORKFLOW is ownership for its FIRST branch (a workflow of the caller's own project) and entitlement for its
        // second: isVisibleCatalogWorkflow grants an admin-owned catalog template on the strength of the caller being
        // able to SEE it in the published listing. Same shape as a shared connection, one resource type over, so the
        // same fix -- the entitled branch is offered only for the scopes in CATALOG_READABLE_WORKFLOW_SCOPES, and the
        // owned branch keeps granting every scope. JOB asks the same two questions and therefore needs the treatment
        // too, with its OWN allowlist: a Job is checked with ExecutionPermissionScope tokens, so reusing the Workflow
        // set there would deny every job check outright.
        //
        // For JOB the scope allowlist is necessary but not sufficient, and resolveJob adds the second half rather than
        // delegating to resolveWorkflow: a catalog template is shared by every connected user entitled to see it, so
        // "any job of workflow X" spans all of their runs. EXECUTION_VIEW selects the catalog arm; ownership of the
        // RUN -- the job's AUTOMATION principal id against the caller's own ProjectDeployment ids -- then decides it.
        //
        // The catalog-arm decision is taken HERE rather than inside resolveWorkflow, so that the resource type and the
        // scope enum it is checked against are chosen together at one site. Passing a raw scope string down and
        // matching it against a single set is how the two enums would get conflated.
        Optional<String> externalUserId = SecurityUtils.fetchCurrentUserLogin();

        if (externalUserId.isEmpty()) {
            return Decision.NOT_APPLICABLE;
        }

        Environment environment = fetchCurrentPrincipalEnvironment();

        if (environment == null) {
            // The principal carries no environment, so it is not an embedded caller and this resolver has nothing to
            // say about it. governsCurrentPrincipal() answers false for the same reason, so a caller following the
            // precedence rule never reaches this branch; a direct caller gets the ordinary path, not a denial.
            return Decision.NOT_APPLICABLE;
        }

        try {
            return resolve(id, resourceType, scope, externalUserId.get(), environment);
        } catch (RuntimeException exception) {
            // This catch IS fail closed, and the asymmetry with governsCurrentPrincipal() above is deliberate rather
            // than an oversight. The distinction is not how similar the two catches look, it is what the caller can do
            // with the value: DENIED reaches ResourceMembershipDecider, which maps everything that is not GRANTED to
            // Outcome.DENY, and no caller can turn that into a grant. `false` from governsCurrentPrincipal() delegates
            // the decision instead, and the delegate may grant. A value that can only ever deny is safe to synthesize
            // when the truth is unavailable; a value that hands the question to somebody else is not.
            //
            // The cost is that a transient failure here 403s rather than 500s, which is a worse diagnostic than the
            // method above now gives. It is kept because it cannot be wrong in the unsafe direction, and because
            // changing it would alter a live answer rather than an unreachable one.
            log.debug(
                "Denying resourceType=[{}] id=[{}] for principal [{}] in environment [{}] -- fail closed.",
                resourceType, id, externalUserId.get(), environment, exception);

            return Decision.DENIED;
        }
    }

    private Decision resolve(
        Serializable id, String resourceType, String scope, String externalUserId, Environment environment) {
        if (connectedUserService.fetchConnectedUser(externalUserId, environment)
            .isEmpty()) {

            return Decision.NOT_APPLICABLE;
        }

        Optional<ConnectedUserProject> connectedUserProject = connectedUserProjectService.fetchConnectUserProject(
            externalUserId, environment);

        if (connectedUserProject.isEmpty()) {
            return Decision.DENIED;
        }

        return switch (resourceType) {
            case PROJECT -> resolveProject(
                id, connectedUserProject.get(), externalUserId, environment,
                CATALOG_PROVISIONABLE_PROJECT_SCOPES.contains(scope));
            case WORKFLOW -> resolveWorkflow(
                id, connectedUserProject.get(), externalUserId, environment,
                CATALOG_READABLE_WORKFLOW_SCOPES.contains(scope));
            case PROJECT_DEPLOYMENT -> resolveProjectDeployment(id, connectedUserProject.get());
            case PROJECT_WORKFLOW -> resolveProjectWorkflow(id, connectedUserProject.get());
            case CONNECTION -> resolveConnection(id, connectedUserProject.get(), environment, scope);
            case JOB -> resolveJob(
                id, connectedUserProject.get(), externalUserId, environment,
                CATALOG_READABLE_EXECUTION_SCOPES.contains(scope));
            default -> Decision.NOT_APPLICABLE;
        };
    }

    /**
     * Two branches, the same shape {@link #resolveWorkflow} has one resource type over.
     *
     * <p>
     * The first is OWNERSHIP -- the caller's own {@link ConnectedUserProject#getProjectId()} -- and grants every scope.
     *
     * <p>
     * The second is ENTITLEMENT, and it closes the last authorization gap of ticket 1051:
     * {@code ConnectedUserCodeWorkflowReferenceFacadeImpl.getOrCreateReference} provisions the reference's
     * {@code ProjectDeployment} on the admin-owned CATALOG project, so the {@code projectId} reaching
     * {@code ProjectDeploymentServiceImpl.create}'s gate is one this caller does not own. Until this branch existed the
     * ownership test was the whole answer, {@code ResourceMembershipDecider} mapped it to DENY, and the resolver is
     * consulted ahead of the skip check, so {@code @SkipAutomationAuthorization} on the reference facade did not rescue
     * it -- FIRST-USE provisioning of a reference 403'd before the automation-hub enable toggle was ever reachable.
     *
     * <p>
     * The entitled branch is offered ONLY for {@link #CATALOG_PROVISIONABLE_PROJECT_SCOPES}; see that field for why
     * widening the ownership test instead would have been the worse hole.
     */
    private Decision resolveProject(
        Serializable id, ConnectedUserProject connectedUserProject, String externalUserId, Environment environment,
        boolean catalogEntitlementApplies) {

        if (!(id instanceof Number number)) {
            return Decision.DENIED;
        }

        long projectId = number.longValue();

        if (projectId == connectedUserProject.getProjectId()) {
            return Decision.GRANTED;
        }

        if (!catalogEntitlementApplies) {
            return Decision.DENIED;
        }

        return isReferencableCatalogProject(projectId, externalUserId, environment)
            ? Decision.GRANTED : Decision.DENIED;
    }

    /**
     * Whether {@code projectId} is a catalog project this connected user could legitimately provision a reference
     * against, read from the same permission-filtered listing
     * {@code ConnectedUserCodeWorkflowReferenceFacadeImpl.validateCatalogWorkflowTemplateVisible} gates provisioning on
     * -- so this predicate cannot grant a project OUTSIDE the listing that facade gates on. Not an equivalence, and the
     * difference is deliberate: this predicate is PROJECT-level while that check is UUID-level, so this can grant on
     * project P while the facade still refuses one particular template of P. Harmless, because the facade's check runs
     * first and is the stricter of the two; the gate merely stops being the thing that denies.
     *
     * <p>
     * That listing is narrower than "any project of the tenant admin's" in three ways at once, and all three matter:
     * {@code AutomationWorkflowProjectFacadeImpl} restricts it to projects whose name carries the
     * {@code EMBEDDED_AUTOMATION} marker (created only through the embedded admin catalog API) in the DEFAULT
     * workspace, then filters them through {@code EmbeddedPermissionEvaluator} against THIS connected user, then
     * filters each project's templates the same way. An ordinary automation project can never appear in it.
     *
     * <p>
     * At least one VISIBLE template is required, not merely the project's presence.
     * {@code AutomationWorkflowProjectFacadeImpl.getPublishedProjects()} maps every marked project, handing back an
     * empty template list rather than dropping the project when nothing is published yet -- so presence alone would
     * grant {@code DEPLOYMENT_PUSH} on a catalog project that has no referencable workflow at all. Requiring a visible
     * template cannot over-deny on the path this exists for: {@code getOrCreateReference} resolves the catalog project
     * id FROM a template it has already checked is visible, so by the time the gate is reached the list is non-empty by
     * construction.
     *
     * <p>
     * Cost, so nobody rediscovers it as a surprise: a {@code 'Project'} + {@code DEPLOYMENT_PUSH} check on a project
     * the caller does NOT own now runs the whole catalog listing. That is not a new class of problem --
     * {@link #isVisibleCatalogWorkflow} already pays the same listing for {@code WORKFLOW_VIEW}, which is far more
     * frequent -- and the owned branch still returns before reaching here, so no ordinary embedded request pays it.
     */
    private boolean isReferencableCatalogProject(long projectId, String externalUserId, Environment environment) {
        List<AutomationWorkflowProjectDTO> publishedProjects = automationWorkflowProjectFacade
            .getPublishedProjects(externalUserId, environment);

        return publishedProjects.stream()
            .filter(publishedProject -> publishedProject.id() == projectId)
            .anyMatch(publishedProject -> !CollectionUtils.isEmpty(publishedProject.workflowTemplates()));
    }

    /**
     * The arm that closes a LIVE broken flow rather than a theoretical one.
     * {@code ConnectedUserCodeWorkflowReferenceFacadeImpl.enableReference} calls
     * {@code ProjectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentId, workflowId, enable)}, which
     * is gated {@code hasPermission(#projectDeploymentId, 'ProjectDeployment', 'DEPLOYMENT_EDIT')}. Until this arm
     * existed {@code 'ProjectDeployment'} fell to the {@code default} case of
     * {@link #resolve(Serializable, String, String, String, Environment)}, and {@code ResourceMembershipDecider} maps
     * everything that is not {@code GRANTED} to {@code Outcome.DENY} -- so a governed connected user was denied
     * unconditionally, from a toggle {@code AutomationRow.tsx} renders on every automation-hub row including the
     * reference-kind ones. Note that only the REFERENCE path was broken: the copy path enables through the ungated
     * four-argument {@code enableProjectDeploymentWorkflow(projectId, workflowId, enable, environment)} overload, which
     * is an in-bean self-invocation and never crosses the security proxy.
     *
     * <p>
     * "Owns" is the union of two disjoint sources, because a connected user's deployments live in two places:
     *
     * <ol>
     * <li>Deployments of the caller's OWN {@link ConnectedUserProject#getProjectId()} -- the copy path. That project is
     * created per (external user id, environment) by
     * {@code ConnectedUserProjectWorkflowManager.getOrCreateConnectedUserProject} and is never shared, so every
     * deployment of it is this caller's. {@link #resolveProject} already grants every scope on the project itself;
     * denying its deployment would be the same over-deny one level down.</li>
     * <li>The {@code projectDeploymentId} recorded on the caller's own {@code ConnectedUserProjectWorkflow} rows -- the
     * reference path. That deployment sits on the admin's CATALOG project, so source 1 cannot see it, but the row is
     * provisioned per (catalog project, connected user) and named {@code __EMBEDDED__<externalUserId>__<ENV>}: no other
     * connected user shares it, and enabling, disabling or deleting it affects only this caller's own runs.</li>
     * </ol>
     *
     * <p>
     * Neither source can reach another connected user's deployment. Source 1 is keyed by the caller's own project id,
     * read from the {@code ConnectedUserProject} this resolver looked up from the {@code SecurityContext}'s external
     * user id; source 2 is keyed by that same row's id, so both are environment-scoped by construction --
     * {@code fetchConnectUserProject} joins {@code connected_user} on the environment carried by the PRINCIPAL.
     */
    private Decision resolveProjectDeployment(Serializable id, ConnectedUserProject connectedUserProject) {
        if (!(id instanceof Number number)) {
            return Decision.DENIED;
        }

        Set<Long> projectDeploymentIds = getOwnProjectDeploymentIds(connectedUserProject);

        return projectDeploymentIds.contains(number.longValue()) ? Decision.GRANTED : Decision.DENIED;
    }

    /**
     * The two sources enumerated on {@link #resolveProjectDeployment}, as one set. Also consulted by
     * {@link #resolveJob}, which is why it is a helper rather than inlined: the question "is this deployment the
     * caller's?" is asked by an authorization arm and by a job-ownership test, and two spellings of it would drift.
     *
     * <p>
     * A reference row's {@code projectDeploymentId} is nullable -- it is set only by
     * {@code ConnectedUserCodeWorkflowReferenceFacadeImpl.getOrCreateReference}, so every copy-mode row carries null --
     * and a null must be skipped rather than added, or the set would answer "yes" to a null-keyed lookup that means
     * "this row has no deployment at all".
     */
    private Set<Long> getOwnProjectDeploymentIds(ConnectedUserProject connectedUserProject) {
        Set<Long> projectDeploymentIds = new HashSet<>();

        for (ProjectDeployment projectDeployment : projectDeploymentService.getProjectDeployments(
            connectedUserProject.getProjectId())) {

            Long projectDeploymentId = projectDeployment.getId();

            if (projectDeploymentId != null) {
                projectDeploymentIds.add(projectDeploymentId);
            }
        }

        for (ConnectedUserProjectWorkflow connectedUserProjectWorkflow : connectedUserProjectWorkflowService
            .getConnectedUserProjectWorkflows(connectedUserProject.getId())) {

            Long projectDeploymentId = connectedUserProjectWorkflow.getProjectDeploymentId();

            if (projectDeploymentId != null) {
                projectDeploymentIds.add(projectDeploymentId);
            }
        }

        return projectDeploymentIds;
    }

    private Decision resolveProjectWorkflow(Serializable id, ConnectedUserProject connectedUserProject) {
        if (!(id instanceof Number number)) {
            return Decision.DENIED;
        }

        try {
            ProjectWorkflow projectWorkflow = projectWorkflowService.getProjectWorkflow(number.longValue());

            return projectWorkflow.getProjectId() == connectedUserProject.getProjectId()
                ? Decision.GRANTED : Decision.DENIED;
        } catch (IllegalArgumentException exception) {
            // ProjectWorkflowServiceImpl.getProjectWorkflow throws for an unknown id -- an ordinary "not theirs"
            // answer here, not an infrastructure failure worth a WARN of its own.
            return Decision.DENIED;
        }
    }

    /**
     * Delegates to {@link ConnectedUserConnectionMembership}, which is also what
     * {@code ConnectedUserConnectionFacadeImpl.getConnections} lists from. The union used to be written out here and
     * again there, and the failure mode of two copies is precisely the one this resolver produces: a connection the
     * picker offers and this method then denies. One call site each means they cannot diverge.
     *
     * <p>
     * Which of the two sets applies depends on the scope, because they answer different questions. The wider one is
     * ENTITLEMENT and includes the connections bound at the caller's integration instance configurations -- shared with
     * every connected user on that configuration, and therefore usable but not theirs to change. The narrower one is
     * OWNERSHIP. Granting a mutating scope from the wider set would let one connected user rename, retag or delete the
     * tenant admin's connection out from under every other user of that configuration, and it is live rather than
     * theoretical: this decider is consulted BEFORE {@code hasResourceScope}, and a GRANT here returns ahead of the
     * tenant-admin check and RBAC, so {@code PUT /api/automation/internal/connections/{id}} would have taken it.
     */
    private Decision resolveConnection(
        Serializable id, ConnectedUserProject connectedUserProject, Environment environment, String scope) {

        if (!(id instanceof Number number)) {
            return Decision.DENIED;
        }

        long connectedUserId = connectedUserProject.getConnectedUserId();

        Set<Long> connectionIds = SHAREABLE_CONNECTION_SCOPES.contains(scope)
            ? connectedUserConnectionMembership.getConnectionIds(connectedUserId, environment)
            : connectedUserConnectionMembership.getOwnedConnectionIds(connectedUserId, environment);

        return connectionIds.contains(number.longValue()) ? Decision.GRANTED : Decision.DENIED;
    }

    /**
     * Two branches answering two different questions.
     *
     * <p>
     * The first is OWNERSHIP -- the workflow belongs to the caller's own {@link ConnectedUserProject} -- and grants
     * every scope, because owning a workflow does entitle you to edit and delete it. That covers the embedded builder:
     * {@code ConnectedUserProjectFacadeImpl.copyWorkflowTemplate} copies a catalog template into the connected user's
     * OWN project before it is ever edited, so the builder's editing surfaces never present a catalog workflow id to a
     * {@code WORKFLOW_EDIT} gate.
     *
     * <p>
     * The second is ENTITLEMENT: {@link #isVisibleCatalogWorkflow} grants an admin-owned published template because the
     * caller can SEE it, which its own javadoc describes as being able to READ it. {@code WORKFLOW_EDIT} and
     * {@code WORKFLOW_DELETE} are live tokens on this resource type, so before {@code catalogEntitlementApplies} the
     * ability to see a template in the catalog listing answered GRANTED for editing and deleting the tenant admin's
     * copy of it -- an edit every other connected user of that catalog would then inherit, and a delete that would take
     * the template away from all of them. Same entitlement-mistaken-for-ownership defect as the shared connection, and
     * live for the same reason: at the four {@code 'Workflow'}/{@code 'Job'}-carrying consumers listed on this class, a
     * GRANT returned ahead of the tenant-admin check and RBAC, and at
     * {@code AutomationMethodSecurityExpressionRoot.hasWorkflowScopeInEnvironment} the mere absence of a DENY fell
     * through to a skip mode that granted any workflow id in the tenant.
     *
     * <p>
     * The flag is computed by the caller from the allowlist matching the resource type's own scope enum; see
     * {@link #resolve(Serializable, String, String)}. Skipping the catalog probe when it cannot grant anything also
     * keeps the {@code getPublishedProjects} listing off every mutating check.
     */
    private Decision resolveWorkflow(
        Serializable id, ConnectedUserProject connectedUserProject, String externalUserId, Environment environment,
        boolean catalogEntitlementApplies) {

        if (!(id instanceof String workflowId)) {
            return Decision.DENIED;
        }

        if (isOwnProjectWorkflow(workflowId, connectedUserProject)) {
            return Decision.GRANTED;
        }

        if (!catalogEntitlementApplies) {
            return Decision.DENIED;
        }

        return isVisibleCatalogWorkflow(workflowId, externalUserId, environment) ? Decision.GRANTED : Decision.DENIED;
    }

    /**
     * The OWNERSHIP branch of {@link #resolveWorkflow}, extracted because {@link #resolveJob} asks the same question
     * and must answer it identically -- a job of a workflow in the caller's own project is theirs under every scope,
     * and that is the guard a narrowing of the catalog arm must not disturb.
     */
    private boolean isOwnProjectWorkflow(String workflowId, ConnectedUserProject connectedUserProject) {
        Optional<Project> project = projectService.fetchWorkflowProject(workflowId);

        if (project.isEmpty()) {
            return false;
        }

        Project ownProject = project.get();

        return Objects.equals(ownProject.getId(), connectedUserProject.getProjectId());
    }

    /**
     * Mirrors {@code ConnectedUserCodeWorkflowReferenceFacadeImpl.validateCatalogWorkflowTemplateVisible}: a connected
     * user may legitimately read an admin-owned catalog workflow outside their own project, provided the catalog
     * listing {@link AutomationWorkflowProjectFacade#getPublishedProjects(String, Environment)} would show them.
     * Matched by the workflow's stable template uuid rather than its (republish-churning) workflow id, the same key the
     * catalog listing itself uses.
     */
    private boolean isVisibleCatalogWorkflow(String workflowId, String externalUserId, Environment environment) {
        Optional<ProjectWorkflow> projectWorkflow = projectWorkflowService.fetchWorkflowProjectWorkflow(workflowId);

        if (projectWorkflow.isEmpty()) {
            return false;
        }

        String workflowUuid = projectWorkflow.get()
            .getUuidAsString();

        if (workflowUuid == null) {
            return false;
        }

        List<AutomationWorkflowProjectDTO> publishedProjects = automationWorkflowProjectFacade
            .getPublishedProjects(externalUserId, environment);

        return publishedProjects.stream()
            .flatMap(publishedProject -> CollectionUtils.stream(publishedProject.workflowTemplates()))
            .anyMatch(workflowTemplate -> Objects.equals(workflowTemplate.workflowUuid(), workflowUuid));
    }

    /**
     * A job is answered by its workflow -- {@link #isOwnProjectWorkflow} for the OWNED branch, unchanged and granting
     * every scope -- plus one predicate {@link #resolveWorkflow} has no use for.
     *
     * <p>
     * The catalog arm has to stay open for {@code EXECUTION_VIEW}, because a connected user's run of a REFERENCED
     * catalog workflow is a job on the catalog workflow id itself: {@code getOrCreateReference} provisions a
     * {@code ProjectDeployment} on the CATALOG project and runs the catalog workflow with no copy taken (see
     * {@link #CATALOG_READABLE_EXECUTION_SCOPES}). Denying the arm outright would hide users' own executions from them,
     * so it is narrowed rather than closed.
     *
     * <p>
     * Narrowed by OWNERSHIP OF THE RUN, which the workflow id alone cannot express: a visible catalog template is
     * shared by every connected user entitled to see it, so "any job of workflow X" spans all of their runs at once.
     * {@code PrincipalJobService.fetchJobPrincipalId(jobId, PlatformType.AUTOMATION)} answers the run's
     * {@code PrincipalJob.principalId}, which for {@code AUTOMATION} is the {@code projectDeploymentId} the run was
     * created under -- {@code ProjectDeploymentFacadeImpl.createProjectDeploymentWorkflowJob} passes exactly that id to
     * {@code PrincipalJobFacade.createJob}, and {@code ConnectedUserProjectTaskDispatcherPreSendProcessor} reads it
     * back the same way. Intersecting it with {@link #getOwnProjectDeploymentIds} answers "is this the caller's own
     * run?" -- reference deployments are provisioned per (catalog project, connected user), so a run of the same
     * template by a different connected user carries a different principal id and is now DENIED.
     *
     * <p>
     * Subflows inherit the answer for free: {@code PrincipalJobFacadeImpl.createChildJob} copies the parent's principal
     * id onto the child, so a child job of the caller's own run is in the same set.
     *
     * <p>
     * A job with NO {@code PrincipalJob} row -- an editor or test run, which {@code TestWorkflowExecutorImpl} starts
     * through {@code JobSyncExecutor} without registering a principal -- is denied on this arm rather than waved
     * through. That costs a connected user nothing: starting a test is gated
     * {@code hasWorkflowScopeInEnvironment(#id, 'WORKFLOW_EDIT', ...)}, and {@code WORKFLOW_EDIT} on a catalog template
     * is already DENIED by {@link #CATALOG_READABLE_WORKFLOW_SCOPES}, so no connected user can produce a principal-less
     * job on a catalog workflow. Their own project's test runs are unaffected: those take the OWNED branch above, which
     * never consults this predicate.
     *
     * <p>
     * The ownership test runs BEFORE {@link #isVisibleCatalogWorkflow} because it is the cheaper of the two -- two
     * keyed reads against the {@code getPublishedProjects} listing -- and because it is the stronger predicate.
     */
    private Decision resolveJob(
        Serializable id, ConnectedUserProject connectedUserProject, String externalUserId, Environment environment,
        boolean catalogEntitlementApplies) {

        if (!(id instanceof Number number)) {
            return Decision.DENIED;
        }

        long jobId = number.longValue();

        Optional<Job> job = jobService.fetchJob(jobId);

        if (job.isEmpty()) {
            return Decision.DENIED;
        }

        String workflowId = job.get()
            .getWorkflowId();

        if (isOwnProjectWorkflow(workflowId, connectedUserProject)) {
            return Decision.GRANTED;
        }

        if (!catalogEntitlementApplies) {
            return Decision.DENIED;
        }

        if (!isOwnJob(jobId, connectedUserProject)) {
            return Decision.DENIED;
        }

        return isVisibleCatalogWorkflow(workflowId, externalUserId, environment) ? Decision.GRANTED : Decision.DENIED;
    }

    /**
     * Whether {@code jobId} was run under one of the caller's own {@code ProjectDeployment}s. See {@link #resolveJob}
     * for why {@code PlatformType.AUTOMATION} is the right id space and why an absent principal is a denial.
     */
    private boolean isOwnJob(long jobId, ConnectedUserProject connectedUserProject) {
        Optional<Long> jobPrincipalId = principalJobService.fetchJobPrincipalId(jobId, PlatformType.AUTOMATION);

        if (jobPrincipalId.isEmpty()) {
            return false;
        }

        Set<Long> projectDeploymentIds = getOwnProjectDeploymentIds(connectedUserProject);

        return projectDeploymentIds.contains(jobPrincipalId.get());
    }
}
