/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.security.ResourceMembershipResolver.Decision;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * {@link ConnectedUserResourceMembershipResolver}'s {@link Decision} logic, which as of ticket 1051 Stage 2 is
 * authoritative: a {@link Decision#DENIED} here is a 403 the caller receives.
 *
 * <p>
 * Nothing here binds {@code EnvironmentContext}, deliberately: production never binds it on an embedded request either,
 * and a test that does proves the wrong thing. The environment comes from the authenticated principal, so each test
 * puts a real {@link EmbeddedApiKeyAuthenticationToken} in the {@code SecurityContext}, built exactly as
 * {@code EmbeddedApiKeyAuthenticationProvider} builds it.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserResourceMembershipResolverTest {

    private static final String EXTERNAL_USER_ID = "connected-user-1";
    private static final long CONNECTED_USER_ID = 9L;
    private static final long OWN_PROJECT_ID = 100L;
    private static final long OTHER_PROJECT_ID = 200L;
    private static final long OWN_PROJECT_DEPLOYMENT_ID = 300L;
    private static final long OWN_REFERENCE_PROJECT_DEPLOYMENT_ID = 301L;
    private static final long OTHER_USERS_PROJECT_DEPLOYMENT_ID = 302L;
    private static final long CONNECTED_USER_PROJECT_ID = 1L;
    private static final long INTEGRATION_INSTANCE_CONFIGURATION_ID = 500L;
    private static final long OTHER_INTEGRATION_INSTANCE_CONFIGURATION_ID = 501L;
    private static final long DEVELOPMENT_INTEGRATION_INSTANCE_CONFIGURATION_ID = 502L;

    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;
    private ConnectedUserConnectionService connectedUserConnectionService;
    private ConnectedUserProjectService connectedUserProjectService;
    private ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService;
    private ConnectedUserService connectedUserService;
    private IntegrationInstanceService integrationInstanceService;
    private JobService jobService;
    private PrincipalJobService principalJobService;
    private ProjectDeploymentService projectDeploymentService;
    private ProjectService projectService;
    private IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private ProjectWorkflowService projectWorkflowService;
    private ConnectedUserResourceMembershipResolver resolver;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        authenticateAsConnectedUser(Environment.PRODUCTION);

        automationWorkflowProjectFacade = mock(AutomationWorkflowProjectFacade.class);
        connectedUserConnectionService = mock(ConnectedUserConnectionService.class);
        connectedUserProjectService = mock(ConnectedUserProjectService.class);
        connectedUserProjectWorkflowService = mock(ConnectedUserProjectWorkflowService.class);
        connectedUserService = mock(ConnectedUserService.class);
        integrationInstanceConfigurationWorkflowService = mock(IntegrationInstanceConfigurationWorkflowService.class);
        integrationInstanceService = mock(IntegrationInstanceService.class);
        jobService = mock(JobService.class);
        principalJobService = mock(PrincipalJobService.class);
        projectDeploymentService = mock(ProjectDeploymentService.class);
        projectService = mock(ProjectService.class);
        projectWorkflowService = mock(ProjectWorkflowService.class);

        // Real, not mocked: a mocked union would let this resolver's tests pass while it and
        // ConnectedUserConnectionFacadeImpl -- which lists from the same class -- disagree about what is entitled.
        ConnectedUserConnectionMembership connectedUserConnectionMembership = new ConnectedUserConnectionMembership(
            connectedUserConnectionService, integrationInstanceConfigurationWorkflowService,
            integrationInstanceService);

        resolver = new ConnectedUserResourceMembershipResolver(
            automationWorkflowProjectFacade, connectedUserConnectionMembership, connectedUserProjectService,
            connectedUserProjectWorkflowService, connectedUserService, jobService, principalJobService,
            projectDeploymentService, projectService, projectWorkflowService);

        securityUtilsMock = mockStatic(SecurityUtils.class);

        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.of(EXTERNAL_USER_ID));

        lenient().when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(Optional.of(connectedUser()));
        lenient().when(connectedUserProjectService.fetchConnectUserProject(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(Optional.of(connectedUserProject()));
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();

        SecurityContextHolder.clearContext();
    }

    /**
     * Mirrors {@code EmbeddedApiKeyAuthenticationProvider.authenticate}: the AUTHENTICATED token carries the
     * environment, and it is that token -- not the converter's -- that {@code ApiKeyAuthenticationFilter} puts in the
     * {@code SecurityContext}.
     */
    private static void authenticateAsConnectedUser(Environment environment) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new EmbeddedApiKeyAuthenticationToken(
                    environment.ordinal(), new User(EXTERNAL_USER_ID, "", List.of())));
    }

    // -- Ticket 1051 fix round: the environment comes from the principal ------------------------------------------

    /**
     * The regression this round undoes. {@code EnvironmentContext} is bound by no request entry point anywhere in the
     * repo, so on an embedded request it is unset: a resolver reading it either guessed PRODUCTION or, once it stopped
     * guessing, denied every connected user every check. Nothing here binds it, and everything still resolves.
     */
    @Test
    void testResolvesFromThePrincipalWithNoEnvironmentContextBound() {
        assertThat(resolver.governsCurrentPrincipal()).isTrue();
        assertThat(resolver.resolve(OWN_PROJECT_ID, "Project", "PROJECT_DELETE")).isEqualTo(Decision.GRANTED);
    }

    /**
     * And it is the principal's OWN environment, not a default: a DEVELOPMENT principal is looked up in DEVELOPMENT.
     */
    @Test
    void testUsesThePrincipalsOwnEnvironmentRatherThanAProductionDefault() {
        authenticateAsConnectedUser(Environment.DEVELOPMENT);

        when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_ID, Environment.DEVELOPMENT))
            .thenReturn(Optional.of(connectedUser()));
        when(connectedUserProjectService.fetchConnectUserProject(EXTERNAL_USER_ID, Environment.DEVELOPMENT))
            .thenReturn(Optional.of(connectedUserProject()));

        assertThat(resolver.governsCurrentPrincipal()).isTrue();
        assertThat(resolver.resolve(OWN_PROJECT_ID, "Project", "PROJECT_DELETE")).isEqualTo(Decision.GRANTED);
    }

    /**
     * A principal carrying no environment is not an embedded caller: not governed, and NOT_APPLICABLE rather than
     * DENIED, so the ordinary path decides instead of a 403 being invented for it.
     */
    @Test
    void testPrincipalWithoutAnApiKeyTokenIsNotGoverned() {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(EXTERNAL_USER_ID, "n/a", List.of()));

        assertThat(resolver.governsCurrentPrincipal()).isFalse();
        assertThat(resolver.resolve(OWN_PROJECT_ID, "Project", "PROJECT_DELETE")).isEqualTo(Decision.NOT_APPLICABLE);
    }

    /**
     * The guard reads {@code fetchEnvironmentId()}, not {@code getEnvironmentId()}, so an api-key token whose provider
     * built it without an environment is "unknown" rather than a confident DEVELOPMENT. Were it read the other way,
     * this principal would be governed and resolved against whatever connected user happens to share its external id in
     * DEVELOPMENT -- someone else's memberships answering for it.
     */
    @Test
    void testApiKeyTokenWithoutAnEnvironmentIsNotGoverned() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new EmbeddedApiKeyAuthenticationToken(new User(EXTERNAL_USER_ID, "", List.of())));

        assertThat(resolver.governsCurrentPrincipal()).isFalse();
        assertThat(resolver.resolve(OWN_PROJECT_ID, "Project", "PROJECT_DELETE")).isEqualTo(Decision.NOT_APPLICABLE);
    }

    @Test
    void testGovernsCurrentPrincipalTrueForAConnectedUser() {
        assertThat(resolver.governsCurrentPrincipal()).isTrue();
    }

    @Test
    void testGovernsCurrentPrincipalFalseWhenNotAConnectedUser() {
        when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(Optional.empty());

        assertThat(resolver.governsCurrentPrincipal()).isFalse();
    }

    @Test
    void testGovernsCurrentPrincipalFalseWithNoSecurityContext() {
        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.empty());

        assertThat(resolver.governsCurrentPrincipal()).isFalse();
    }

    /**
     * The G4 path from ticket 1051's Stage 4 audit. This used to swallow the exception and answer false, which denied
     * GOVERNANCE rather than the check -- the caller then fell through to the embedded resource-scoped skip and the
     * check was GRANTED. Whether {@code return false} failed open or closed was a property of what the caller did with
     * it, not of the catch block, and nothing in the resolver said which. An authorization answer derived from a query
     * that failed is not an answer, so the failure is now visible.
     */
    @Test
    void testGovernsCurrentPrincipalPropagatesALookupFailure() {
        when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenThrow(new IllegalStateException("connection pool exhausted"));

        assertThatThrownBy(() -> resolver.governsCurrentPrincipal())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("connection pool exhausted");
    }

    /**
     * The blast radius of the rethrow above. A session principal carries no environment, so it returns before the query
     * and cannot be turned into a 500 by a failing connected-user lookup -- which is what makes letting the exception
     * out safe rather than merely honest.
     */
    @Test
    void testGovernsCurrentPrincipalDoesNotQueryForASessionPrincipal() {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("platform-user", "", List.of()));

        lenient().when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenThrow(new IllegalStateException("must not be reached"));

        assertThat(resolver.governsCurrentPrincipal()).isFalse();
    }

    /**
     * The deliberate asymmetry with the rethrow above: {@code resolve(...)} still answers DENIED on a failed lookup.
     * DENIED can only ever deny -- ResourceMembershipDecider maps everything that is not GRANTED to Outcome.DENY, and
     * no caller can turn it into a grant -- so synthesizing it when the truth is unavailable is safe in a way that
     * synthesizing "not governed" is not.
     */
    @Test
    void testResolveStillDeniesOnALookupFailure() {
        when(projectService.fetchWorkflowProject("workflow-lookup-failure"))
            .thenThrow(new IllegalStateException("connection pool exhausted"));

        assertThat(resolver.resolve("workflow-lookup-failure", "Workflow", "WORKFLOW_EDIT")).isEqualTo(Decision.DENIED);
    }

    // -- Resolve: a principal this resolver does not govern at all -------------------------------------------

    @Test
    void testResolveNotApplicableWhenPrincipalIsNotAConnectedUser() {
        when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(Optional.empty());

        assertThat(resolver.resolve(OWN_PROJECT_ID, "Project", "PROJECT_DELETE")).isEqualTo(Decision.NOT_APPLICABLE);
    }

    @Test
    void testResolveNotApplicableWithNoSecurityContext() {
        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.empty());

        assertThat(resolver.resolve(OWN_PROJECT_ID, "Project", "PROJECT_DELETE")).isEqualTo(Decision.NOT_APPLICABLE);
    }

    @Test
    void testResolveDeniesEveryCheckWhenTheConnectedUserHasNoProjectYet() {
        // "If cup is absent, every answer is DENIED -- a connected user with no project owns nothing." Distinct from
        // NOT_APPLICABLE: this principal IS governed (a real ConnectedUser row exists), it simply has not had its
        // ConnectedUserProject lazily provisioned yet. The resolver must never provision one itself: its constructor
        // depends on ConnectedUserProjectService (fetch-only) rather than ConnectedUserProjectWorkflowManager (which
        // carries getOrCreateConnectedUserProject), so it has no way to.
        when(connectedUserProjectService.fetchConnectUserProject(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(Optional.empty());

        assertThat(resolver.resolve(OWN_PROJECT_ID, "Project", "PROJECT_DELETE")).isEqualTo(Decision.DENIED);
    }

    // -- Data tables and knowledge bases ------------------------------------------------------------------------

    /**
     * Pins a deliberate omission rather than an oversight.
     *
     * <p>
     * {@code WorkspaceDataTableFacadeImpl} and {@code WorkspaceKnowledgeBaseFacadeImpl} both carry
     * {@code hasPermission(#id, 'DataTable'|'KnowledgeBase', ...)}, so a connected user reaching them arrives here,
     * falls to the {@code default} arm as {@code NOT_APPLICABLE}, and -- because this principal IS governed --
     * {@code ResourceMembershipDecider} maps that to {@code DENY}.
     *
     * <p>
     * That denial is correct. The per-account ownership design gives connected users data tables and knowledge bases at
     * RUNTIME only, through the owner filter in the services; managing them is the vendor admin's job, through the
     * embedded console. Adding cases here would be a GRANT with no surface asking for one.
     *
     * <p>
     * If a connected-user-facing API is ever built, this test is the place that says what changing these two answers
     * means.
     */
    @Test
    void testResolveDeniesDataTableAndKnowledgeBaseForAConnectedUser() {
        assertThat(resolver.resolve(1L, "DataTable", "DATA_TABLE_VIEW")).isEqualTo(Decision.NOT_APPLICABLE);
        assertThat(resolver.resolve(1L, "DataTable", "DATA_TABLE_EDIT")).isEqualTo(Decision.NOT_APPLICABLE);
        assertThat(resolver.resolve(1L, "KnowledgeBase", "KNOWLEDGE_BASE_VIEW")).isEqualTo(Decision.NOT_APPLICABLE);
        assertThat(resolver.resolve(1L, "KnowledgeBase", "KNOWLEDGE_BASE_EDIT")).isEqualTo(Decision.NOT_APPLICABLE);
    }

    // -- Project -----------------------------------------------------------------------------------------------

    @Test
    void testResolveProjectGrantedForOwnProject() {
        assertThat(resolver.resolve(OWN_PROJECT_ID, "Project", "PROJECT_DELETE")).isEqualTo(Decision.GRANTED);
    }

    @Test
    void testResolveProjectDeniedForAnotherProject() {
        assertThat(resolver.resolve(OTHER_PROJECT_ID, "Project", "PROJECT_DELETE")).isEqualTo(Decision.DENIED);
    }

    @Test
    void testResolveProjectDeniedForNonNumericId() {
        assertThat(resolver.resolve("not-a-number", "Project", "PROJECT_DELETE")).isEqualTo(Decision.DENIED);
    }

    /**
     * The gap this arm closes. {@code ConnectedUserCodeWorkflowReferenceFacadeImpl.getOrCreateReference} provisions the
     * reference's {@code ProjectDeployment} on the CATALOG project, so the {@code projectId} reaching
     * {@code ProjectDeploymentServiceImpl.create}'s {@code DEPLOYMENT_PUSH} gate is the tenant admin's, not this
     * caller's -- and until this arm existed the ownership test denied it, 403ing first-use provisioning before the
     * automation-hub enable toggle was ever reachable.
     */
    @Test
    void testResolveProjectGrantedForDeploymentPushOnAVisibleCatalogProject() {
        stubVisibleCatalogProject();

        assertThat(resolver.resolve(OTHER_PROJECT_ID, "Project", "DEPLOYMENT_PUSH")).isEqualTo(Decision.GRANTED);
    }

    /**
     * The entitlement is the catalog listing's, so a catalog project this connected user's permission expression hides
     * grants nothing -- the same predicate {@code validateCatalogWorkflowTemplateVisible} applies, so a project granted
     * here can never be one that facade would then refuse to provision from.
     */
    @Test
    void testResolveProjectDeniedForDeploymentPushOnAnInvisibleCatalogProject() {
        when(automationWorkflowProjectFacade.getPublishedProjects(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(List.of());

        assertThat(resolver.resolve(OTHER_PROJECT_ID, "Project", "DEPLOYMENT_PUSH")).isEqualTo(Decision.DENIED);
    }

    /**
     * The allowlist guard, and the one that matters most here.
     * {@link #testResolveProjectGrantedForEveryScopeOnOwnProject} shows the OWNED branch grants every verb; an entitled
     * branch behaving the same way would hand every connected user {@code PROJECT_DELETE}, {@code WORKFLOW_EDIT} and
     * the rest on the tenant admin's catalog project, and a GRANT here returns ahead of the tenant-admin check and RBAC
     * so nothing downstream would refuse it. Only {@code DEPLOYMENT_PUSH} is offered.
     */
    @Test
    void testResolveProjectDeniedForEveryOtherScopeOnAVisibleCatalogProject() {
        stubVisibleCatalogProject();

        assertThat(resolver.resolve(OTHER_PROJECT_ID, "Project", "PROJECT_DELETE")).isEqualTo(Decision.DENIED);
        assertThat(resolver.resolve(OTHER_PROJECT_ID, "Project", "PROJECT_SETTINGS")).isEqualTo(Decision.DENIED);
        assertThat(resolver.resolve(OTHER_PROJECT_ID, "Project", "WORKFLOW_VIEW")).isEqualTo(Decision.DENIED);
        assertThat(resolver.resolve(OTHER_PROJECT_ID, "Project", "WORKFLOW_CREATE")).isEqualTo(Decision.DENIED);
        assertThat(resolver.resolve(OTHER_PROJECT_ID, "Project", "WORKFLOW_EDIT")).isEqualTo(Decision.DENIED);
        assertThat(resolver.resolve(OTHER_PROJECT_ID, "Project", "WORKFLOW_DELETE")).isEqualTo(Decision.DENIED);
        assertThat(resolver.resolve(OTHER_PROJECT_ID, "Project", "DEPLOYMENT_PULL")).isEqualTo(Decision.DENIED);
        assertThat(resolver.resolve(OTHER_PROJECT_ID, "Project", "DEPLOYMENT_EDIT")).isEqualTo(Decision.DENIED);
        assertThat(resolver.resolve(OTHER_PROJECT_ID, "Project", "DEPLOYMENT_VIEW")).isEqualTo(Decision.DENIED);
    }

    /**
     * The allowlist is an allowlist. A scope token added to any of the three enums {@code 'Project'} is checked against
     * -- which is what an unrecognised token looks like to this class -- must not inherit the catalog arm by default.
     */
    @Test
    void testResolveProjectDeniesAnUnrecognisedScopeOnAVisibleCatalogProject() {
        stubVisibleCatalogProject();

        assertThat(resolver.resolve(OTHER_PROJECT_ID, "Project", "PROJECT_SOME_FUTURE_VERB"))
            .isEqualTo(Decision.DENIED);
    }

    /**
     * The over-deny guard. The owned branch is untouched by the narrowing: the caller's own
     * {@code ConnectedUserProject} project still answers GRANTED for every scope, including ones this class has never
     * heard of.
     */
    @Test
    void testResolveProjectGrantedForEveryScopeOnOwnProject() {
        assertThat(resolver.resolve(OWN_PROJECT_ID, "Project", "PROJECT_DELETE")).isEqualTo(Decision.GRANTED);
        assertThat(resolver.resolve(OWN_PROJECT_ID, "Project", "PROJECT_SETTINGS")).isEqualTo(Decision.GRANTED);
        assertThat(resolver.resolve(OWN_PROJECT_ID, "Project", "WORKFLOW_CREATE")).isEqualTo(Decision.GRANTED);
        assertThat(resolver.resolve(OWN_PROJECT_ID, "Project", "WORKFLOW_EDIT")).isEqualTo(Decision.GRANTED);
        assertThat(resolver.resolve(OWN_PROJECT_ID, "Project", "WORKFLOW_DELETE")).isEqualTo(Decision.GRANTED);
        assertThat(resolver.resolve(OWN_PROJECT_ID, "Project", "DEPLOYMENT_PUSH")).isEqualTo(Decision.GRANTED);
        assertThat(resolver.resolve(OWN_PROJECT_ID, "Project", "PROJECT_SOME_FUTURE_VERB"))
            .isEqualTo(Decision.GRANTED);
    }

    /**
     * Presence in the listing is not enough. {@code AutomationWorkflowProjectFacadeImpl.getPublishedProjects()} maps
     * every marked project and hands back an EMPTY template list rather than dropping the project when nothing is
     * published yet, so a presence-only predicate would grant {@code DEPLOYMENT_PUSH} on a catalog project with no
     * referencable workflow at all.
     */
    @Test
    void testResolveProjectDeniedForACatalogProjectWithNoVisibleTemplate() {
        AutomationWorkflowProjectDTO catalogProject = new AutomationWorkflowProjectDTO(
            OTHER_PROJECT_ID, "Catalog", "", null, List.of(), false, 1, null, List.of(), null, false);

        when(automationWorkflowProjectFacade.getPublishedProjects(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(List.of(catalogProject));

        assertThat(resolver.resolve(OTHER_PROJECT_ID, "Project", "DEPLOYMENT_PUSH")).isEqualTo(Decision.DENIED);
    }

    /**
     * A scope the catalog arm cannot satisfy must not pay for the catalog listing either.
     */
    @Test
    void testResolveProjectSkipsTheCatalogListingForANonProvisioningScope() {
        assertThat(resolver.resolve(OTHER_PROJECT_ID, "Project", "PROJECT_DELETE")).isEqualTo(Decision.DENIED);

        verify(automationWorkflowProjectFacade, never()).getPublishedProjects(EXTERNAL_USER_ID, Environment.PRODUCTION);
    }

    /**
     * An admin-owned catalog project on {@code OTHER_PROJECT_ID}, carrying one template this connected user can see in
     * the permission-filtered published listing.
     */
    private void stubVisibleCatalogProject() {
        ConnectedUserWorkflowTemplateDTO template = new ConnectedUserWorkflowTemplateDTO(
            UUID.randomUUID()
                .toString(),
            "Template", "", null, List.of(), List.of(), null);

        AutomationWorkflowProjectDTO catalogProject = new AutomationWorkflowProjectDTO(
            OTHER_PROJECT_ID, "Catalog", "", null, List.of(), true, 1, 1, List.of(template), null, false);

        lenient().when(automationWorkflowProjectFacade.getPublishedProjects(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(List.of(catalogProject));
    }

    // -- ProjectDeployment ---------------------------------------------------------------------------------------

    /**
     * The live broken flow this arm closes. {@code ConnectedUserCodeWorkflowReferenceFacadeImpl.enableReference} --
     * reached from the automation-hub enable toggle {@code AutomationRow.tsx} renders on every row, reference-kind ones
     * included -- calls {@code ProjectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentId, ...)},
     * gated {@code hasPermission(#projectDeploymentId, 'ProjectDeployment', 'DEPLOYMENT_EDIT')}. Before this arm
     * {@code 'ProjectDeployment'} hit the resolver's {@code default} case and answered NOT_APPLICABLE, which
     * {@code ResourceMembershipDecider} maps to {@code Outcome.DENY}: a governed connected user could not enable or
     * disable their OWN reference. This assertion is the one that was red.
     */
    @Test
    void testResolveProjectDeploymentGrantedForOwnReferenceDeployment() {
        stubOwnProjectDeployments();

        assertThat(resolver.resolve(OWN_REFERENCE_PROJECT_DEPLOYMENT_ID, "ProjectDeployment", "DEPLOYMENT_EDIT"))
            .isEqualTo(Decision.GRANTED);
    }

    /**
     * The second source. A deployment of the caller's own {@code ConnectedUserProject} project -- the copy path -- is
     * theirs too: that project is created per (external user id, environment) and never shared, and
     * {@link #testResolveProjectGrantedForOwnProject} already grants every scope on the project itself, so denying its
     * deployment would be the same over-deny one level down.
     */
    @Test
    void testResolveProjectDeploymentGrantedForOwnProjectDeployment() {
        stubOwnProjectDeployments();

        assertThat(resolver.resolve(OWN_PROJECT_DEPLOYMENT_ID, "ProjectDeployment", "DEPLOYMENT_EDIT"))
            .isEqualTo(Decision.GRANTED);
    }

    /**
     * The widening's boundary. Another connected user's reference deployment on the SAME catalog project is not in
     * either source, so it stays denied -- and it is a deployment id an attacker can guess, since these are dense
     * sequential longs. A GRANT here would return ahead of the tenant-admin check and RBAC and let one connected user
     * enable, disable or delete another's automation.
     */
    @Test
    void testResolveProjectDeploymentDeniedForAnotherConnectedUsersDeployment() {
        stubOwnProjectDeployments();

        assertThat(resolver.resolve(OTHER_USERS_PROJECT_DEPLOYMENT_ID, "ProjectDeployment", "DEPLOYMENT_EDIT"))
            .isEqualTo(Decision.DENIED);
        assertThat(resolver.resolve(OTHER_USERS_PROJECT_DEPLOYMENT_ID, "ProjectDeployment", "DEPLOYMENT_VIEW"))
            .isEqualTo(Decision.DENIED);
    }

    /**
     * Ownership, so every scope -- and the arm cannot be an allowlist even if that were in doubt, because
     * {@code ProjectDeploymentFacadeImpl} checks this resource type against TWO enums: eight gates carry
     * {@code DeploymentPermissionScope} tokens and {@code getProjectDeploymentWorkflow(WorkflowExecutionId)}
     * additionally carries the {@code WorkflowPermissionScope} token {@code WORKFLOW_VIEW} against the same
     * {@code 'ProjectDeployment'} type. A single set spanning two enums is exactly the conflation the connection and
     * workflow allowlists were split up to avoid.
     */
    @Test
    void testResolveProjectDeploymentGrantedForEveryScopeOnAnOwnedDeployment() {
        stubOwnProjectDeployments();

        assertThat(resolver.resolve(OWN_REFERENCE_PROJECT_DEPLOYMENT_ID, "ProjectDeployment", "DEPLOYMENT_VIEW"))
            .isEqualTo(Decision.GRANTED);
        assertThat(resolver.resolve(OWN_REFERENCE_PROJECT_DEPLOYMENT_ID, "ProjectDeployment", "WORKFLOW_VIEW"))
            .isEqualTo(Decision.GRANTED);
        assertThat(
            resolver.resolve(OWN_REFERENCE_PROJECT_DEPLOYMENT_ID, "ProjectDeployment", "DEPLOYMENT_SOME_FUTURE_VERB"))
                .isEqualTo(Decision.GRANTED);
    }

    /**
     * A copy-mode {@code ConnectedUserProjectWorkflow} carries a null {@code projectDeploymentId} --
     * {@code getOrCreateReference} is the only writer of that column -- so the null must be skipped rather than added
     * to the set.
     */
    @Test
    void testResolveProjectDeploymentSkipsAReferenceRowWithNoDeployment() {
        when(projectDeploymentService.getProjectDeployments(OWN_PROJECT_ID)).thenReturn(List.of());
        when(connectedUserProjectWorkflowService.getConnectedUserProjectWorkflows(CONNECTED_USER_PROJECT_ID))
            .thenReturn(List.of(new ConnectedUserProjectWorkflow()));

        assertThat(resolver.resolve(OWN_REFERENCE_PROJECT_DEPLOYMENT_ID, "ProjectDeployment", "DEPLOYMENT_EDIT"))
            .isEqualTo(Decision.DENIED);
    }

    @Test
    void testResolveProjectDeploymentDeniedForNonNumericId() {
        assertThat(resolver.resolve("not-a-number", "ProjectDeployment", "DEPLOYMENT_EDIT"))
            .isEqualTo(Decision.DENIED);
    }

    /**
     * Deployment {@value #OWN_PROJECT_DEPLOYMENT_ID} deploys the caller's own project (the copy path); deployment
     * {@value #OWN_REFERENCE_PROJECT_DEPLOYMENT_ID} is the caller's own reference deployment on the admin's catalog
     * project. Deployment {@value #OTHER_USERS_PROJECT_DEPLOYMENT_ID} belongs to a different connected user of that
     * same catalog project and is stubbed into neither source.
     */
    private void stubOwnProjectDeployments() {
        when(projectDeploymentService.getProjectDeployments(OWN_PROJECT_ID))
            .thenReturn(List.of(projectDeployment(OWN_PROJECT_DEPLOYMENT_ID)));
        when(connectedUserProjectWorkflowService.getConnectedUserProjectWorkflows(CONNECTED_USER_PROJECT_ID))
            .thenReturn(List.of(connectedUserProjectWorkflow(OWN_REFERENCE_PROJECT_DEPLOYMENT_ID)));
    }

    private static ProjectDeployment projectDeployment(long id) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(id);

        return projectDeployment;
    }

    private static ConnectedUserProjectWorkflow connectedUserProjectWorkflow(long projectDeploymentId) {
        ConnectedUserProjectWorkflow connectedUserProjectWorkflow = new ConnectedUserProjectWorkflow();

        connectedUserProjectWorkflow.setProjectDeploymentId(projectDeploymentId);

        return connectedUserProjectWorkflow;
    }

    // -- ProjectWorkflow -----------------------------------------------------------------------------------------

    @Test
    void testResolveProjectWorkflowGrantedWhenItsProjectIsOwn() {
        ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);

        when(projectWorkflow.getProjectId()).thenReturn(OWN_PROJECT_ID);
        when(projectWorkflowService.getProjectWorkflow(55L)).thenReturn(projectWorkflow);

        assertThat(resolver.resolve(55L, "ProjectWorkflow", "WORKFLOW_EDIT")).isEqualTo(Decision.GRANTED);
    }

    @Test
    void testResolveProjectWorkflowDeniedWhenItsProjectIsNotOwn() {
        ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);

        when(projectWorkflow.getProjectId()).thenReturn(OTHER_PROJECT_ID);
        when(projectWorkflowService.getProjectWorkflow(55L)).thenReturn(projectWorkflow);

        assertThat(resolver.resolve(55L, "ProjectWorkflow", "WORKFLOW_EDIT")).isEqualTo(Decision.DENIED);
    }

    @Test
    void testResolveProjectWorkflowDeniedWhenUnknown() {
        when(projectWorkflowService.getProjectWorkflow(55L)).thenThrow(new IllegalArgumentException("missing"));

        assertThat(resolver.resolve(55L, "ProjectWorkflow", "WORKFLOW_EDIT")).isEqualTo(Decision.DENIED);
    }

    // -- Connection ----------------------------------------------------------------------------------------------

    @Test
    void testResolveConnectionGrantedViaIntegrationInstance() {
        IntegrationInstance integrationInstance = mock(IntegrationInstance.class);

        when(integrationInstance.getConnectionId()).thenReturn(77L);
        when(integrationInstanceService.getConnectedUserIntegrationInstances(CONNECTED_USER_ID, Environment.PRODUCTION))
            .thenReturn(List.of(integrationInstance));

        assertThat(resolver.resolve(77L, "Connection", "CONNECTION_USE")).isEqualTo(Decision.GRANTED);
    }

    @Test
    void testResolveConnectionGrantedViaConnectedUserConnection() {
        when(connectedUserConnectionService.getConnectionIds(CONNECTED_USER_ID)).thenReturn(List.of(88L));

        assertThat(resolver.resolve(88L, "Connection", "CONNECTION_USE")).isEqualTo(Decision.GRANTED);
    }

    @Test
    void testResolveConnectionDeniedWhenNoSourceHasIt() {
        assertThat(resolver.resolve(99L, "Connection", "CONNECTION_USE")).isEqualTo(Decision.DENIED);
    }

    /**
     * The grant this ticket restores. A connection bound at the CONFIGURATION level is inherited by every connected
     * user whose instance derives from that configuration; it is on neither the user's own instance nor their own
     * connections, so before this it resolved DENIED while the picker happily listed it.
     */
    @Test
    void testResolveConnectionGrantedViaIntegrationInstanceConfiguration() {
        when(integrationInstanceService.getConnectedUserIntegrationInstances(CONNECTED_USER_ID, Environment.PRODUCTION))
            .thenReturn(List.of(integrationInstance(77L, INTEGRATION_INSTANCE_CONFIGURATION_ID)));
        when(
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                List.of(INTEGRATION_INSTANCE_CONFIGURATION_ID)))
                    .thenReturn(List.of(integrationInstanceConfigurationWorkflow(78L)));

        assertThat(resolver.resolve(78L, "Connection", "CONNECTION_USE")).isEqualTo(Decision.GRANTED);
    }

    /**
     * A configuration the caller has no instance for is never consulted, so its connections are not theirs. One
     * configuration serves many connected users; enumerating configurations rather than the caller's own instances
     * would grant each of them the others' connections.
     */
    @Test
    void testResolveConnectionDeniedForAConfigurationTheUserHasNoInstanceFor() {
        when(integrationInstanceService.getConnectedUserIntegrationInstances(CONNECTED_USER_ID, Environment.PRODUCTION))
            .thenReturn(List.of(integrationInstance(77L, INTEGRATION_INSTANCE_CONFIGURATION_ID)));
        when(
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                List.of(INTEGRATION_INSTANCE_CONFIGURATION_ID)))
                    .thenReturn(List.of(integrationInstanceConfigurationWorkflow(78L)));

        assertThat(resolver.resolve(999L, "Connection", "CONNECTION_USE")).isEqualTo(Decision.DENIED);

        verify(integrationInstanceConfigurationWorkflowService, never())
            .getIntegrationInstanceConfigurationWorkflows(List.of(OTHER_INTEGRATION_INSTANCE_CONFIGURATION_ID));
    }

    /**
     * The environment axis, which source 3 inherits rather than re-applies. The instance lookup is environment-scoped,
     * so a DEVELOPMENT caller's instances carry only DEVELOPMENT configuration ids: the PRODUCTION twin's configuration
     * is never walked and its connection is denied. Reopening this would reintroduce, through a new door, the
     * cross-environment hole this ticket closed.
     */
    @Test
    void testResolveConnectionDeniedForTheSameConfigurationFamilyInAnotherEnvironment() {
        authenticateAsConnectedUser(Environment.DEVELOPMENT);

        when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_ID, Environment.DEVELOPMENT))
            .thenReturn(Optional.of(connectedUser()));
        when(connectedUserProjectService.fetchConnectUserProject(EXTERNAL_USER_ID, Environment.DEVELOPMENT))
            .thenReturn(Optional.of(connectedUserProject()));

        when(
            integrationInstanceService.getConnectedUserIntegrationInstances(
                CONNECTED_USER_ID, Environment.DEVELOPMENT))
                    .thenReturn(
                        List.of(integrationInstance(77L, DEVELOPMENT_INTEGRATION_INSTANCE_CONFIGURATION_ID)));
        when(
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                List.of(DEVELOPMENT_INTEGRATION_INSTANCE_CONFIGURATION_ID)))
                    .thenReturn(List.of(integrationInstanceConfigurationWorkflow(78L)));
        when(
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                List.of(INTEGRATION_INSTANCE_CONFIGURATION_ID)))
                    .thenReturn(List.of(integrationInstanceConfigurationWorkflow(88L)));

        assertThat(resolver.resolve(78L, "Connection", "CONNECTION_USE")).isEqualTo(Decision.GRANTED);
        assertThat(resolver.resolve(88L, "Connection", "CONNECTION_USE")).isEqualTo(Decision.DENIED);

        verify(integrationInstanceConfigurationWorkflowService, never())
            .getIntegrationInstanceConfigurationWorkflows(List.of(INTEGRATION_INSTANCE_CONFIGURATION_ID));
    }

    /**
     * A shared connection is one you may USE, not one you may rename, retag or delete. Ticket 1051 widened this
     * resolver's set from ownership to entitlement, which silently turned every configuration-level connection into one
     * any attached connected user could mutate: the decider is consulted BEFORE {@code hasResourceScope}, and a GRANT
     * here returns ahead of the tenant-admin check and RBAC, so {@code PUT /api/automation/internal/connections/{id}}
     * -- gated {@code CONNECTION_EDIT} -- would have taken it.
     */
    @Test
    void testResolveConnectionDeniedForAMutatingScopeOnAConfigurationSharedConnection() {
        stubConfigurationSharedConnection();

        assertThat(resolver.resolve(78L, "Connection", "CONNECTION_USE")).isEqualTo(Decision.GRANTED);
        assertThat(resolver.resolve(78L, "Connection", "CONNECTION_VIEW")).isEqualTo(Decision.GRANTED);
        assertThat(resolver.resolve(78L, "Connection", "CONNECTION_EDIT")).isEqualTo(Decision.DENIED);
        assertThat(resolver.resolve(78L, "Connection", "CONNECTION_DELETE")).isEqualTo(Decision.DENIED);
    }

    /**
     * The narrowing must not over-deny: a connection the caller actually OWNS stays mutable under every scope, because
     * owning a resource does entitle you to every verb on it. Only the shared third source is scope-restricted.
     */
    @Test
    void testResolveConnectionGrantedForAMutatingScopeOnAnOwnedConnection() {
        stubConfigurationSharedConnection();

        assertThat(resolver.resolve(77L, "Connection", "CONNECTION_EDIT")).isEqualTo(Decision.GRANTED);
        assertThat(resolver.resolve(77L, "Connection", "CONNECTION_DELETE")).isEqualTo(Decision.GRANTED);
    }

    /**
     * The allowlist is an allowlist. An unrecognised scope token -- which is what a scope added to
     * {@code ConnectionPermissionScope} later looks like to this class -- falls back to the owned set rather than
     * inheriting the wider one by default.
     */
    @Test
    void testResolveConnectionDeniesAnUnrecognisedScopeOnASharedConnection() {
        stubConfigurationSharedConnection();

        assertThat(resolver.resolve(78L, "Connection", "CONNECTION_SOME_FUTURE_VERB")).isEqualTo(Decision.DENIED);
        assertThat(resolver.resolve(77L, "Connection", "CONNECTION_SOME_FUTURE_VERB")).isEqualTo(Decision.GRANTED);
    }

    /**
     * Connection 77 is on the caller's own instance (OWNED); connection 78 is bound at that instance's configuration
     * (ENTITLED but not owned).
     */
    private void stubConfigurationSharedConnection() {
        when(integrationInstanceService.getConnectedUserIntegrationInstances(CONNECTED_USER_ID, Environment.PRODUCTION))
            .thenReturn(List.of(integrationInstance(77L, INTEGRATION_INSTANCE_CONFIGURATION_ID)));
        when(
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                List.of(INTEGRATION_INSTANCE_CONFIGURATION_ID)))
                    .thenReturn(List.of(integrationInstanceConfigurationWorkflow(78L)));
    }

    private static IntegrationInstance integrationInstance(
        long connectionId, long integrationInstanceConfigurationId) {

        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setConnectionId(connectionId);
        integrationInstance.setIntegrationInstanceConfigurationId(integrationInstanceConfigurationId);

        return integrationInstance;
    }

    private static IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow(
        long connectionId) {

        IntegrationInstanceConfigurationWorkflowConnection workflowConnection =
            new IntegrationInstanceConfigurationWorkflowConnection(connectionId, "connection", "node");

        return new IntegrationInstanceConfigurationWorkflow(List.of(workflowConnection), Map.of(), "workflow-1");
    }

    // -- Workflow --------------------------------------------------------------------------------------------------

    @Test
    void testResolveWorkflowGrantedForOwnProject() {
        Project ownProject = new Project();

        ownProject.setId(OWN_PROJECT_ID);

        when(projectService.fetchWorkflowProject("workflow-1")).thenReturn(Optional.of(ownProject));

        assertThat(resolver.resolve("workflow-1", "Workflow", "WORKFLOW_EDIT")).isEqualTo(Decision.GRANTED);
    }

    @Test
    void testResolveWorkflowDeniedForAnotherProjectAndNotInTheCatalog() {
        Project otherProject = new Project();

        otherProject.setId(OTHER_PROJECT_ID);

        when(projectService.fetchWorkflowProject("workflow-1")).thenReturn(Optional.of(otherProject));
        when(projectWorkflowService.fetchWorkflowProjectWorkflow("workflow-1")).thenReturn(Optional.empty());

        assertThat(resolver.resolve("workflow-1", "Workflow", "WORKFLOW_EDIT")).isEqualTo(Decision.DENIED);
    }

    @Test
    void testResolveWorkflowGrantedForAVisibleCatalogTemplate() {
        stubVisibleCatalogWorkflow();

        assertThat(resolver.resolve("workflow-1", "Workflow", "WORKFLOW_VIEW")).isEqualTo(Decision.GRANTED);
    }

    /**
     * The catalog arm is ENTITLEMENT, not ownership: the template belongs to the tenant admin and this caller merely
     * SEES it in the published listing. Editing it would rewrite the template every other connected user of that
     * catalog gets, and deleting it would take it away from all of them. Before the split, being able to see a template
     * answered GRANTED for both -- and a GRANT here returns ahead of the tenant-admin check and RBAC, so nothing
     * downstream would have refused it.
     */
    @Test
    void testResolveWorkflowDeniedForAMutatingScopeOnAVisibleCatalogTemplate() {
        stubVisibleCatalogWorkflow();

        assertThat(resolver.resolve("workflow-1", "Workflow", "WORKFLOW_EDIT")).isEqualTo(Decision.DENIED);
        assertThat(resolver.resolve("workflow-1", "Workflow", "WORKFLOW_DELETE")).isEqualTo(Decision.DENIED);
    }

    /**
     * The narrowing must not over-deny, and this is the test that matters most: a workflow in the caller's OWN
     * ConnectedUserProject stays editable and deletable. That is the embedded builder's whole surface --
     * {@code ConnectedUserProjectFacadeImpl.copyWorkflowTemplate} copies a catalog template into the connected user's
     * own project before it is ever edited -- so a regression here would break the builder rather than merely
     * tightening the catalog.
     */
    @Test
    void testResolveWorkflowGrantedForEveryScopeOnOwnProject() {
        Project ownProject = new Project();

        ownProject.setId(OWN_PROJECT_ID);

        when(projectService.fetchWorkflowProject("workflow-1")).thenReturn(Optional.of(ownProject));

        assertThat(resolver.resolve("workflow-1", "Workflow", "WORKFLOW_VIEW")).isEqualTo(Decision.GRANTED);
        assertThat(resolver.resolve("workflow-1", "Workflow", "WORKFLOW_CREATE")).isEqualTo(Decision.GRANTED);
        assertThat(resolver.resolve("workflow-1", "Workflow", "WORKFLOW_EDIT")).isEqualTo(Decision.GRANTED);
        assertThat(resolver.resolve("workflow-1", "Workflow", "WORKFLOW_DELETE")).isEqualTo(Decision.GRANTED);
        assertThat(resolver.resolve("workflow-1", "Workflow", "WORKFLOW_SOME_FUTURE_VERB"))
            .isEqualTo(Decision.GRANTED);
    }

    /**
     * The allowlist is an allowlist. A scope token added to {@code WorkflowPermissionScope} later -- which is what an
     * unrecognised token looks like to this class -- must not inherit the catalog arm by default.
     */
    @Test
    void testResolveWorkflowDeniesAnUnrecognisedScopeOnAVisibleCatalogTemplate() {
        stubVisibleCatalogWorkflow();

        assertThat(resolver.resolve("workflow-1", "Workflow", "WORKFLOW_SOME_FUTURE_VERB")).isEqualTo(Decision.DENIED);
    }

    /**
     * A scope the catalog arm cannot satisfy must not pay for the catalog listing either.
     */
    @Test
    void testResolveWorkflowSkipsTheCatalogListingForAMutatingScope() {
        stubVisibleCatalogWorkflow();

        assertThat(resolver.resolve("workflow-1", "Workflow", "WORKFLOW_EDIT")).isEqualTo(Decision.DENIED);

        verify(automationWorkflowProjectFacade, never()).getPublishedProjects(EXTERNAL_USER_ID, Environment.PRODUCTION);
    }

    @Test
    void testResolveWorkflowDeniedForNonStringId() {
        assertThat(resolver.resolve(5L, "Workflow", "WORKFLOW_EDIT")).isEqualTo(Decision.DENIED);
    }

    /**
     * Workflow "workflow-1" is an admin-owned published catalog template ({@code OTHER_PROJECT_ID}) that this connected
     * user can see in the catalog listing.
     */
    private void stubVisibleCatalogWorkflow() {
        Project otherProject = new Project();

        otherProject.setId(OTHER_PROJECT_ID);

        UUID templateUuid = UUID.randomUUID();

        ProjectWorkflow catalogProjectWorkflow = mock(ProjectWorkflow.class);

        lenient().when(catalogProjectWorkflow.getUuidAsString())
            .thenReturn(templateUuid.toString());

        when(projectService.fetchWorkflowProject("workflow-1")).thenReturn(Optional.of(otherProject));
        lenient().when(projectWorkflowService.fetchWorkflowProjectWorkflow("workflow-1"))
            .thenReturn(Optional.of(catalogProjectWorkflow));

        ConnectedUserWorkflowTemplateDTO template = new ConnectedUserWorkflowTemplateDTO(
            templateUuid.toString(), "Template", "", null, List.of(), List.of(), null);

        AutomationWorkflowProjectDTO catalogProject = new AutomationWorkflowProjectDTO(
            OTHER_PROJECT_ID, "Catalog", "", null, List.of(), true, 1, 1, List.of(template), null, false);

        lenient().when(automationWorkflowProjectFacade.getPublishedProjects(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(List.of(catalogProject));
    }

    // -- Job -------------------------------------------------------------------------------------------------------

    @Test
    void testResolveJobGrantedWhenItsWorkflowIsOwn() {
        Project ownProject = new Project();

        ownProject.setId(OWN_PROJECT_ID);

        Job job = mock(Job.class);

        when(job.getWorkflowId()).thenReturn("workflow-1");
        when(jobService.fetchJob(321L)).thenReturn(Optional.of(job));
        when(projectService.fetchWorkflowProject("workflow-1")).thenReturn(Optional.of(ownProject));

        assertThat(resolver.resolve(321L, "Job", "EXECUTION_VIEW")).isEqualTo(Decision.GRANTED);
    }

    @Test
    void testResolveJobDeniedWhenUnknown() {
        when(jobService.fetchJob(321L)).thenReturn(Optional.empty());

        assertThat(resolver.resolve(321L, "Job", "EXECUTION_VIEW")).isEqualTo(Decision.DENIED);
    }

    /**
     * A job whose workflow the caller owns is theirs under every scope, deleting its logs included.
     */
    @Test
    void testResolveJobGrantedForEveryScopeWhenItsWorkflowIsOwn() {
        Project ownProject = new Project();

        ownProject.setId(OWN_PROJECT_ID);

        Job job = mock(Job.class);

        when(job.getWorkflowId()).thenReturn("workflow-1");
        when(jobService.fetchJob(321L)).thenReturn(Optional.of(job));
        when(projectService.fetchWorkflowProject("workflow-1")).thenReturn(Optional.of(ownProject));

        assertThat(resolver.resolve(321L, "Job", "EXECUTION_VIEW")).isEqualTo(Decision.GRANTED);
        assertThat(resolver.resolve(321L, "Job", "EXECUTION_DELETE")).isEqualTo(Decision.GRANTED);
    }

    /**
     * The catalog arm has to stay open for {@code EXECUTION_VIEW}, because a connected user's own run of a REFERENCED
     * catalog workflow is a job on the catalog workflow id itself:
     * {@code ConnectedUserCodeWorkflowReferenceFacadeImpl.getOrCreateReference} provisions a {@code ProjectDeployment}
     * on the catalog project and runs the catalog workflow directly, with no copy taken. Denying it would hide a user's
     * own executions from them.
     */
    @Test
    void testResolveJobGrantedForAReadScopeOnACatalogWorkflow() {
        stubCatalogWorkflowJob();

        assertThat(resolver.resolve(321L, "Job", "EXECUTION_VIEW")).isEqualTo(Decision.GRANTED);
    }

    /**
     * {@code EXECUTION_DELETE} is a write ({@code LogFileStorageImpl.deleteLogs}) on an execution of somebody else's
     * template, so it is outside the job allowlist. Note the allowlist consulted here is
     * {@code CATALOG_READABLE_EXECUTION_SCOPES}, NOT the {@code Workflow} one -- a Job carries
     * {@code ExecutionPermissionScope} tokens, and reusing the workflow set would have denied the test above too.
     */
    @Test
    void testResolveJobDeniedForAMutatingScopeOnACatalogWorkflow() {
        stubCatalogWorkflowJob();

        assertThat(resolver.resolve(321L, "Job", "EXECUTION_DELETE")).isEqualTo(Decision.DENIED);
    }

    /**
     * The job allowlist is an allowlist too.
     */
    @Test
    void testResolveJobDeniesAnUnrecognisedScopeOnACatalogWorkflow() {
        stubCatalogWorkflowJob();

        assertThat(resolver.resolve(321L, "Job", "EXECUTION_SOME_FUTURE_VERB")).isEqualTo(Decision.DENIED);
    }

    /**
     * Job 321 is the CALLER'S OWN run of the admin-owned catalog workflow "workflow-1" -- the shape a referenced
     * catalog workflow produces: the run carries the caller's own reference {@code ProjectDeployment} as its
     * {@code AUTOMATION} principal id, because {@code getOrCreateReference} provisions that deployment on the catalog
     * project and runs the catalog workflow id itself, with no copy taken.
     */
    private void stubCatalogWorkflowJob() {
        stubCatalogWorkflowJob(321L, OWN_REFERENCE_PROJECT_DEPLOYMENT_ID);
    }

    /**
     * As above, but naming the {@code ProjectDeployment} the run was created under, so a test can stub a run of the
     * SAME catalog template belonging to a different connected user.
     */
    private void stubCatalogWorkflowJob(long jobId, long jobPrincipalId) {
        Job job = mock(Job.class);

        lenient().when(job.getWorkflowId())
            .thenReturn("workflow-1");

        when(jobService.fetchJob(jobId)).thenReturn(Optional.of(job));
        lenient().when(principalJobService.fetchJobPrincipalId(jobId, PlatformType.AUTOMATION))
            .thenReturn(Optional.of(jobPrincipalId));

        stubOwnProjectDeployments();
        stubVisibleCatalogWorkflow();
    }

    /**
     * The narrowing. Job 400 ran the SAME visible catalog template as the caller's own job 321, but under a DIFFERENT
     * connected user's reference {@code ProjectDeployment}. Before this, {@code EXECUTION_VIEW} on it was GRANTED
     * purely because the caller could see the template -- a within-tenant cross-user read of another connected user's
     * job inputs, outputs and logs ({@code ProjectWorkflowExecutionFacadeImpl.getJobTaskExecutions},
     * {@code LogFileStorageImpl}), and one whose id is a dense sequential long. Reference deployments are provisioned
     * per (catalog project, connected user), so the other user's run carries a principal id the caller's set does not
     * contain.
     */
    @Test
    void testResolveJobDeniedForAnotherConnectedUsersRunOfTheSameCatalogWorkflow() {
        stubCatalogWorkflowJob(400L, OTHER_USERS_PROJECT_DEPLOYMENT_ID);

        assertThat(resolver.resolve(400L, "Job", "EXECUTION_VIEW")).isEqualTo(Decision.DENIED);
    }

    /**
     * The over-deny guard for the narrowing, and the one that matters most: a job the caller DID cause -- their own
     * reference deployment's run of the catalog workflow -- stays readable. Denying it would hide users' own executions
     * from them, which is the trap the residual note warned about.
     */
    @Test
    void testResolveJobGrantedForTheCallersOwnRunOfACatalogWorkflow() {
        stubCatalogWorkflowJob();

        assertThat(resolver.resolve(321L, "Job", "EXECUTION_VIEW")).isEqualTo(Decision.GRANTED);
    }

    /**
     * A job with no {@code PrincipalJob} row -- an editor or test run, which {@code TestWorkflowExecutorImpl} starts
     * through {@code JobSyncExecutor} without registering a principal -- is not attributable to anybody, so it is
     * denied on the catalog arm. This costs a connected user nothing: starting a test is gated
     * {@code hasWorkflowScopeInEnvironment(#id, 'WORKFLOW_EDIT', ...)}, and {@code WORKFLOW_EDIT} on a catalog template
     * is already denied by {@link #testResolveWorkflowDeniedForAMutatingScopeOnAVisibleCatalogTemplate}, so no
     * connected user can produce a principal-less job on a catalog workflow in the first place.
     */
    @Test
    void testResolveJobDeniedForACatalogRunWithNoPrincipal() {
        stubCatalogWorkflowJob();

        when(principalJobService.fetchJobPrincipalId(321L, PlatformType.AUTOMATION)).thenReturn(Optional.empty());

        assertThat(resolver.resolve(321L, "Job", "EXECUTION_VIEW")).isEqualTo(Decision.DENIED);
    }

    /**
     * The other over-deny guard. A job of a workflow in the caller's OWN project takes the ownership branch, which
     * never consults the run's principal at all -- so an own-project test run (no {@code PrincipalJob} row) and an
     * own-project deployment run alike stay granted under EVERY scope. A regression here would blind the embedded
     * builder to its own executions.
     */
    @Test
    void testResolveJobGrantedForEveryScopeOnAnOwnProjectJobWithoutConsultingItsPrincipal() {
        Project ownProject = new Project();

        ownProject.setId(OWN_PROJECT_ID);

        Job job = mock(Job.class);

        when(job.getWorkflowId()).thenReturn("workflow-1");
        when(jobService.fetchJob(321L)).thenReturn(Optional.of(job));
        when(projectService.fetchWorkflowProject("workflow-1")).thenReturn(Optional.of(ownProject));
        lenient().when(principalJobService.fetchJobPrincipalId(321L, PlatformType.AUTOMATION))
            .thenReturn(Optional.empty());

        assertThat(resolver.resolve(321L, "Job", "EXECUTION_VIEW")).isEqualTo(Decision.GRANTED);
        assertThat(resolver.resolve(321L, "Job", "EXECUTION_DELETE")).isEqualTo(Decision.GRANTED);
        assertThat(resolver.resolve(321L, "Job", "EXECUTION_SOME_FUTURE_VERB")).isEqualTo(Decision.GRANTED);

        verify(principalJobService, never()).fetchJobPrincipalId(321L, PlatformType.AUTOMATION);
    }

    /**
     * The id space the intersection is taken in. {@code PrincipalJob} rows are keyed by (jobId, type), and an embedded
     * connected user's automation-bridge run is an {@code AUTOMATION} row whose principal id is a
     * {@code projectDeploymentId} -- the same convention {@code ConnectedUserProjectTaskDispatcherPreSendProcessor}
     * relies on. Asking for {@code EMBEDDED} would answer from the integration-instance-configuration id space instead,
     * where the caller's deployment ids mean nothing, and would silently deny every catalog read.
     */
    @Test
    void testResolveJobReadsThePrincipalInTheAutomationIdSpace() {
        stubCatalogWorkflowJob();

        assertThat(resolver.resolve(321L, "Job", "EXECUTION_VIEW")).isEqualTo(Decision.GRANTED);

        verify(principalJobService).fetchJobPrincipalId(321L, PlatformType.AUTOMATION);
        verify(principalJobService, never()).fetchJobPrincipalId(321L, PlatformType.EMBEDDED);
    }

    /**
     * Ownership of the run is checked BEFORE the catalog listing, so a job that is not the caller's does not pay for
     * {@code getPublishedProjects} -- the same shape as
     * {@link #testResolveWorkflowSkipsTheCatalogListingForAMutatingScope}, one predicate later.
     */
    @Test
    void testResolveJobSkipsTheCatalogListingForAnotherUsersRun() {
        stubCatalogWorkflowJob(400L, OTHER_USERS_PROJECT_DEPLOYMENT_ID);

        assertThat(resolver.resolve(400L, "Job", "EXECUTION_VIEW")).isEqualTo(Decision.DENIED);

        verify(automationWorkflowProjectFacade, never()).getPublishedProjects(EXTERNAL_USER_ID, Environment.PRODUCTION);
    }

    // -- Unrecognised resource type ----------------------------------------------------------------------------

    @Test
    void testResolveNotApplicableForAnUnrecognisedResourceType() {
        assertThat(resolver.resolve(1L, "KnowledgeBase", "KNOWLEDGE_BASE_VIEW")).isEqualTo(Decision.NOT_APPLICABLE);
    }

    private static ConnectedUser connectedUser() {
        ConnectedUser connectedUser = new ConnectedUser();

        connectedUser.setExternalId(EXTERNAL_USER_ID);
        connectedUser.setEnvironment(Environment.PRODUCTION);

        return connectedUser;
    }

    private static ConnectedUserProject connectedUserProject() {
        ConnectedUserProject connectedUserProject = new ConnectedUserProject();

        connectedUserProject.setId(CONNECTED_USER_PROJECT_ID);
        connectedUserProject.setConnectedUserId(CONNECTED_USER_ID);
        connectedUserProject.setProjectId(OWN_PROJECT_ID);

        return connectedUserProject;
    }
}
