/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.security.ConnectedUserAccessDecider;
import com.bytechef.automation.configuration.security.EmbeddedResourceResolver;
import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.ee.automation.configuration.repository.EmbeddedUserRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * Pins the EE behavior of {@code hasResourceScope} (workspace-scope path, fail-closed when no workspace) and
 * {@code isResourceOwner} (isCurrentUser path).
 *
 * @author Ivica Cardic
 * @version ee
 */
class PermissionServiceResourceTest {

    private static final String CONNECTION_VIEW = "CONNECTION_VIEW";

    private final ConnectionService connectionService = Mockito.mock(ConnectionService.class);
    private final CurrentUserResolver currentUserResolver = Mockito.mock(CurrentUserResolver.class);
    private final PermissionScopeRegistry permissionScopeRegistry = Mockito.mock(PermissionScopeRegistry.class);
    private final ProjectRepository projectRepository = Mockito.mock(ProjectRepository.class);
    private final WorkspaceScopeCacheService workspaceScopeCacheService =
        Mockito.mock(WorkspaceScopeCacheService.class);
    private final WorkspaceUserRepository workspaceUserRepository = Mockito.mock(WorkspaceUserRepository.class);

    private final EmbeddedScopeCacheService embeddedScopeCacheService = Mockito.mock(EmbeddedScopeCacheService.class);

    private PermissionServiceImpl service(ResourceOwnershipResolver... resolvers) {
        return service(List.of(resolvers), List.of());
    }

    private PermissionServiceImpl service(
        List<ResourceOwnershipResolver> ownershipResolvers, List<ResourceEnvironmentResolver> environmentResolvers) {

        return service(ownershipResolvers, environmentResolvers, new StaticListableBeanFactory());
    }

    private PermissionServiceImpl service(
        List<ResourceOwnershipResolver> ownershipResolvers, List<ResourceEnvironmentResolver> environmentResolvers,
        StaticListableBeanFactory beanFactory) {

        return new PermissionServiceImpl(
            currentUserResolver, permissionScopeRegistry, projectRepository, workspaceScopeCacheService,
            workspaceUserRepository, ownershipResolvers, List.of(), permissiveResolver(), environmentResolvers,
            new StaticListableBeanFactory().getBeanProvider(ConnectedUserAccessDecider.class),
            embeddedScopeCacheService, Mockito.mock(EmbeddedUserRepository.class),
            beanFactory.getBeanProvider(EmbeddedResourceResolver.class),
            connectionServiceBeanFactory().getBeanProvider(ConnectionService.class));
    }

    private static ResourceEnvironmentResolver environmentResolver(String type, Optional<Environment> environment) {
        return new ResourceEnvironmentResolver() {
            @Override
            public String resourceType() {
                return type;
            }

            @Override
            public Optional<Environment> fetchEnvironment(Serializable id) {
                return environment;
            }
        };
    }

    private static ResourceEnvironmentResolver throwingEnvironmentResolver(String type) {
        return new ResourceEnvironmentResolver() {
            @Override
            public String resourceType() {
                return type;
            }

            @Override
            public Optional<Environment> fetchEnvironment(Serializable id) {
                throw new IllegalStateException("boom");
            }
        };
    }

    private static ResourceOwnershipResolver resolver(String type, ResourceOwner owner) {
        return new ResourceOwnershipResolver() {
            @Override
            public String resourceType() {
                return type;
            }

            @Override
            public ResourceOwner resolveOwner(long id) {
                return owner;
            }
        };
    }

    @Test
    void testTwoResolversForOneTypeChainUntilOneKnowsTheOwner() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));
        when(embeddedScopeCacheService.getEmbeddedScopes(7L)).thenReturn(Set.of("EMBEDDED_VIEW"));

        PermissionServiceImpl service = service(
            List.of(resolver("Connection", ResourceOwner.unknown()),
                resolver("Connection", ResourceOwner.ofEmbedded())),
            List.of());

        assertThat(service.hasResourceScope(1L, "Connection", "EMBEDDED_VIEW")).isTrue();
    }

    @Test
    void testAnEmbeddedOwnerWithAnEnvironmentIsCheckedInThatEnvironment() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));
        when(embeddedScopeCacheService.getEmbeddedScopes(7L, Environment.DEVELOPMENT))
            .thenReturn(Set.of("EMBEDDED_VIEW"));
        when(embeddedScopeCacheService.getEmbeddedScopes(7L, Environment.PRODUCTION)).thenReturn(Set.of());

        PermissionServiceImpl service = service(
            List.of(resolver("Connection", ResourceOwner.ofEmbedded())),
            List.of(environmentResolver("Connection", Optional.of(Environment.PRODUCTION))));

        assertThat(service.hasResourceScope(1L, "Connection", "EMBEDDED_VIEW")).isFalse();
    }

    @Test
    void testAnEnvironmentLessEmbeddedResourceUsesTheUnawareAnswer() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));
        when(embeddedScopeCacheService.getEmbeddedScopes(7L)).thenReturn(Set.of("INTEGRATION_EDIT"));

        PermissionServiceImpl service = service(
            List.of(resolver("Integration", ResourceOwner.ofEmbedded())), List.of());

        assertThat(service.hasResourceScope(1L, "Integration", "INTEGRATION_EDIT")).isTrue();
    }

    @Test
    void testAnEnvironmentBearingEmbeddedResourceWithoutAnEnvironmentNeedsTheScopeEverywhere() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));
        when(embeddedScopeCacheService.getEmbeddedScopes(7L, Environment.DEVELOPMENT))
            .thenReturn(Set.of("EMBEDDED_VIEW"));
        when(embeddedScopeCacheService.getEmbeddedScopes(7L, Environment.STAGING)).thenReturn(Set.of());
        lenient().when(embeddedScopeCacheService.getEmbeddedScopes(7L))
            .thenReturn(Set.of("EMBEDDED_VIEW"));

        PermissionServiceImpl service = service(
            List.of(resolver("Connection", ResourceOwner.ofEmbedded())),
            List.of(environmentResolver("Connection", Optional.empty())));

        assertThat(service.hasResourceScope(1L, "Connection", "EMBEDDED_VIEW")).isFalse();

        when(embeddedScopeCacheService.getEmbeddedScopes(7L, Environment.STAGING))
            .thenReturn(Set.of("EMBEDDED_VIEW"));
        when(embeddedScopeCacheService.getEmbeddedScopes(7L, Environment.PRODUCTION))
            .thenReturn(Set.of("EMBEDDED_VIEW"));

        assertThat(service.hasResourceScope(1L, "Connection", "EMBEDDED_VIEW")).isTrue();
    }

    @Test
    void testAnEmbeddedResourceWhoseEnvironmentResolverThrowsDenies() {
        lenient().when(currentUserResolver.fetchCurrentUserId())
            .thenReturn(OptionalLong.of(7L));
        lenient().when(embeddedScopeCacheService.getEmbeddedScopes(anyLong()))
            .thenReturn(Set.of("EMBEDDED_VIEW"));
        lenient().when(embeddedScopeCacheService.getEmbeddedScopes(anyLong(), any()))
            .thenReturn(Set.of("EMBEDDED_VIEW"));

        PermissionServiceImpl service = service(
            List.of(resolver("Connection", ResourceOwner.ofEmbedded())),
            List.of(throwingEnvironmentResolver("Connection")));

        assertThat(service.hasResourceScope(1L, "Connection", "EMBEDDED_VIEW")).isFalse();
    }

    @Test
    void testEveryResolverUnknownDeniesWithoutThrowing() {
        lenient().when(currentUserResolver.fetchCurrentUserId())
            .thenReturn(OptionalLong.of(7L));

        PermissionServiceImpl service = service(
            List.of(resolver("Connection", ResourceOwner.unknown()), resolver("Connection", ResourceOwner.unknown())),
            List.of());

        assertThat(service.hasResourceScope(1L, "Connection", "EMBEDDED_VIEW")).isFalse();
    }

    @Test
    void testAnEnvironmentResolverThatThrowsDenies() {
        lenient().when(currentUserResolver.fetchCurrentUserId())
            .thenReturn(OptionalLong.of(7L));
        lenient().when(workspaceScopeCacheService.getWorkspaceScopes(7L, 42L))
            .thenReturn(Set.of("CONNECTION_DELETE"));

        PermissionServiceImpl service = service(
            List.of(resolver("Connection", ResourceOwner.unknown()),
                resolver("Connection", ResourceOwner.ofWorkspace(42L))),
            List.of(
                environmentResolver("Connection", Optional.empty()), throwingEnvironmentResolver("Connection")));

        assertThat(service.hasResourceScope(1L, "Connection", "CONNECTION_DELETE")).isFalse();
    }

    @Test
    void testAnEmbeddedOwnerIsCheckedInTheNamedEnvironment() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));
        when(embeddedScopeCacheService.getEmbeddedScopes(7L, Environment.STAGING)).thenReturn(Set.of("EMBEDDED_VIEW"));

        PermissionServiceImpl service = service(resolver("Connection", ResourceOwner.ofEmbedded()));

        assertThat(service.hasResourceScopeInEnvironment(1L, "Connection", "EMBEDDED_VIEW", Environment.STAGING))
            .isTrue();
    }

    @Test
    void testIsResourceOwnerComparesTheOwnerOfAnEmbeddedResource() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));

        PermissionServiceImpl service = service(resolver("ApiKey", ResourceOwner.ofEmbedded(7L)));

        assertThat(service.isResourceOwner("ApiKey", 1L)).isTrue();
    }

    @Test
    void testAnEmbeddedOwnerIsDeniedByResourceRole() {
        lenient().when(currentUserResolver.fetchCurrentUserId())
            .thenReturn(OptionalLong.of(7L));

        PermissionServiceImpl service = service(resolver("KnowledgeBase", ResourceOwner.ofEmbedded()));

        assertThat(service.hasResourceRole(1L, "KnowledgeBase", "VIEWER")).isFalse();
    }

    @Test
    void testAnEmbeddedConnectionIsDeniedInAWorkspace() {
        lenient().when(currentUserResolver.fetchCurrentUserId())
            .thenReturn(OptionalLong.of(7L));

        PermissionServiceImpl service = service(
            List.of(resolver("Connection", ResourceOwner.ofEmbedded())),
            List.of(environmentResolver("Connection", Optional.of(Environment.PRODUCTION))));

        assertThat(service.canUseConnectionInWorkspace(1L, 42L, Environment.PRODUCTION)).isFalse();
    }

    @Test
    void testAnEmbeddedProjectOwnerDeniesAMemberAndGrantsATenantAdmin() {
        lenient().when(currentUserResolver.fetchCurrentUserId())
            .thenReturn(OptionalLong.of(7L));
        lenient().when(embeddedScopeCacheService.getEmbeddedScopes(anyLong()))
            .thenReturn(Set.of("WORKFLOW_VIEW", "CONNECTION_VIEW"));
        lenient().when(embeddedScopeCacheService.getEmbeddedScopes(anyLong(), any()))
            .thenReturn(Set.of("WORKFLOW_VIEW", "CONNECTION_VIEW"));

        PermissionServiceImpl service = service(
            List.of(
                resolver("ProjectWorkflow", ResourceOwner.ofEmbeddedProject()),
                resolver("Connection", ResourceOwner.ofEmbeddedProject())),
            List.of(
                environmentResolver("ProjectWorkflow", Optional.of(Environment.DEVELOPMENT)),
                environmentResolver("Connection", Optional.of(Environment.DEVELOPMENT))));

        assertThat(service.hasResourceScope(1L, "ProjectWorkflow", "WORKFLOW_VIEW")).isFalse();
        assertThat(
            service.hasResourceScopeInEnvironment(1L, "ProjectWorkflow", "WORKFLOW_VIEW", Environment.DEVELOPMENT))
                .isFalse();
        assertThat(service.hasResourceRole(1L, "ProjectWorkflow", "VIEWER")).isFalse();
        assertThat(service.canUseConnectionInWorkspace(1L, 42L, Environment.DEVELOPMENT)).isFalse();

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
                .thenReturn(true);

            assertThat(service.hasResourceScope(1L, "ProjectWorkflow", "WORKFLOW_VIEW")).isTrue();
            assertThat(
                service.hasResourceScopeInEnvironment(
                    1L, "ProjectWorkflow", "WORKFLOW_VIEW", Environment.DEVELOPMENT))
                        .isTrue();
        }
    }

    @Test
    void testAnEmbeddedOwnerThatIsNoEmbeddedProjectIsAnsweredByTheEmbeddedScopeWhateverItsType() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));
        when(embeddedScopeCacheService.getEmbeddedScopes(7L, Environment.DEVELOPMENT))
            .thenReturn(Set.of("WORKFLOW_VIEW"));

        PermissionServiceImpl service = service(resolver("Project", ResourceOwner.ofEmbedded()));

        assertThat(service.hasResourceScopeInEnvironment(1L, "Project", "WORKFLOW_VIEW", Environment.DEVELOPMENT))
            .isTrue();
    }

    @Test
    void testADefaultWorkspaceEditorCannotUseAConnectedUserProjectWorkflow() {
        EmbeddedResourceResolver embeddedResourceResolver = Mockito.mock(EmbeddedResourceResolver.class);

        when(embeddedResourceResolver.isEmbeddedProjectWorkflow("workflow-1")).thenReturn(true);

        givenAnEditorHoldingConnectionViewInTheDefaultWorkspace();

        PermissionServiceImpl service = service(
            List.of(resolver("Connection", ResourceOwner.ofWorkspace(42L))),
            List.of(environmentResolver("Connection", Optional.of(Environment.DEVELOPMENT))),
            embeddedBeanFactory(embeddedResourceResolver));

        assertThat(service.canUseConnectionInWorkflow(1L, "workflow-1", Environment.DEVELOPMENT)).isFalse();
    }

    @Test
    void testADefaultWorkspaceEditorCanUseAConnectionInAnAutomationProjectWorkflow() {
        givenAnEditorHoldingConnectionViewInTheDefaultWorkspace();

        PermissionServiceImpl service = service(
            List.of(resolver("Connection", ResourceOwner.ofWorkspace(42L))),
            List.of(environmentResolver("Connection", Optional.of(Environment.DEVELOPMENT))),
            embeddedBeanFactory(Mockito.mock(EmbeddedResourceResolver.class)));

        assertThat(service.canUseConnectionInWorkflow(1L, "workflow-1", Environment.DEVELOPMENT)).isTrue();
    }

    @Test
    void testAnEmbeddedMemberUsesAnEmbeddedConnectionInAHubProjectWorkflow() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));
        when(embeddedScopeCacheService.getEmbeddedScopes(7L, Environment.DEVELOPMENT))
            .thenReturn(Set.of(CONNECTION_VIEW));
        when(connectionService.fetchConnection(1L))
            .thenReturn(Optional.of(connection(PlatformType.EMBEDDED, Environment.DEVELOPMENT)));

        EmbeddedResourceResolver embeddedResourceResolver = Mockito.mock(EmbeddedResourceResolver.class);

        when(embeddedResourceResolver.isHubProjectWorkflow("workflow-1")).thenReturn(true);

        PermissionServiceImpl service = service(
            List.of(), List.of(), embeddedBeanFactory(embeddedResourceResolver));

        assertThat(service.canUseConnectionInWorkflow(1L, "workflow-1", Environment.DEVELOPMENT)).isTrue();
    }

    @Test
    void testAnEmbeddedConnectionFromAnotherEnvironmentCannotBeBoundIntoTheRun() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));
        lenient().when(embeddedScopeCacheService.getEmbeddedScopes(anyLong(), any()))
            .thenReturn(Set.of(CONNECTION_VIEW));
        when(connectionService.fetchConnection(1L))
            .thenReturn(Optional.of(connection(PlatformType.EMBEDDED, Environment.PRODUCTION)));

        EmbeddedResourceResolver embeddedResourceResolver = Mockito.mock(EmbeddedResourceResolver.class);

        when(embeddedResourceResolver.isIntegrationWorkflow("workflow-1")).thenReturn(true);

        PermissionServiceImpl service = service(
            List.of(), List.of(), embeddedBeanFactory(embeddedResourceResolver));

        assertThat(service.canUseConnectionInWorkflow(1L, "workflow-1", Environment.DEVELOPMENT)).isFalse();
    }

    @Test
    void testAnAbsentConnectionServiceDeniesAnIntegrationWorkflowConnection() {
        lenient().when(currentUserResolver.fetchCurrentUserId())
            .thenReturn(OptionalLong.of(7L));
        lenient().when(embeddedScopeCacheService.getEmbeddedScopes(anyLong(), any()))
            .thenReturn(Set.of(CONNECTION_VIEW));

        EmbeddedResourceResolver embeddedResourceResolver = Mockito.mock(EmbeddedResourceResolver.class);

        when(embeddedResourceResolver.isIntegrationWorkflow("workflow-1")).thenReturn(true);

        PermissionServiceImpl service = new PermissionServiceImpl(
            currentUserResolver, permissionScopeRegistry, projectRepository, workspaceScopeCacheService,
            workspaceUserRepository, List.of(), List.of(), permissiveResolver(), List.of(),
            new StaticListableBeanFactory().getBeanProvider(ConnectedUserAccessDecider.class),
            embeddedScopeCacheService, Mockito.mock(EmbeddedUserRepository.class),
            embeddedBeanFactory(embeddedResourceResolver).getBeanProvider(EmbeddedResourceResolver.class),
            new StaticListableBeanFactory().getBeanProvider(ConnectionService.class));

        assertThat(service.canUseConnectionInWorkflow(1L, "workflow-1", Environment.DEVELOPMENT)).isFalse();
    }

    private void givenAnEditorHoldingConnectionViewInTheDefaultWorkspace() {
        Project project = new Project();

        project.setWorkspaceId(42L);

        lenient().when(currentUserResolver.fetchCurrentUserId())
            .thenReturn(OptionalLong.of(7L));
        lenient().when(projectRepository.findByWorkflowId("workflow-1"))
            .thenReturn(Optional.of(project));
        lenient().when(workspaceScopeCacheService.getWorkspaceScopes(7L, 42L, Environment.DEVELOPMENT))
            .thenReturn(Set.of(CONNECTION_VIEW));
    }

    @Test
    void testAnEmbeddedMemberUsesAnEmbeddedConnectionInAnIntegrationWorkflow() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));
        when(embeddedScopeCacheService.getEmbeddedScopes(7L, Environment.DEVELOPMENT))
            .thenReturn(Set.of(CONNECTION_VIEW));
        when(connectionService.fetchConnection(1L))
            .thenReturn(Optional.of(connection(PlatformType.EMBEDDED, Environment.DEVELOPMENT)));

        EmbeddedResourceResolver embeddedResourceResolver = Mockito.mock(EmbeddedResourceResolver.class);

        when(embeddedResourceResolver.isIntegrationWorkflow("workflow-1")).thenReturn(true);

        PermissionServiceImpl service = service(
            List.of(), List.of(), embeddedBeanFactory(embeddedResourceResolver));

        assertThat(service.canUseConnectionInWorkflow(1L, "workflow-1", Environment.DEVELOPMENT)).isTrue();
    }

    @Test
    void testAnEmbeddedConnectionInAnotherEnvironmentIsRefused() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));
        when(embeddedScopeCacheService.getEmbeddedScopes(7L, Environment.DEVELOPMENT))
            .thenReturn(Set.of(CONNECTION_VIEW));
        when(embeddedScopeCacheService.getEmbeddedScopes(7L, Environment.PRODUCTION)).thenReturn(Set.of());
        when(connectionService.fetchConnection(1L))
            .thenReturn(Optional.of(connection(PlatformType.EMBEDDED, Environment.PRODUCTION)));

        EmbeddedResourceResolver embeddedResourceResolver = Mockito.mock(EmbeddedResourceResolver.class);

        when(embeddedResourceResolver.isIntegrationWorkflow("workflow-1")).thenReturn(true);

        PermissionServiceImpl service = service(
            List.of(), List.of(), embeddedBeanFactory(embeddedResourceResolver));

        assertThat(service.canUseConnectionInWorkflow(1L, "workflow-1", Environment.DEVELOPMENT)).isFalse();
    }

    @Test
    void testAnAutomationConnectionIsRefusedInAnIntegrationWorkflow() {
        lenient().when(currentUserResolver.fetchCurrentUserId())
            .thenReturn(OptionalLong.of(7L));
        lenient().when(embeddedScopeCacheService.getEmbeddedScopes(anyLong(), any()))
            .thenReturn(Set.of(CONNECTION_VIEW));
        when(connectionService.fetchConnection(1L))
            .thenReturn(Optional.of(connection(PlatformType.AUTOMATION, Environment.DEVELOPMENT)));

        EmbeddedResourceResolver embeddedResourceResolver = Mockito.mock(EmbeddedResourceResolver.class);

        when(embeddedResourceResolver.isIntegrationWorkflow("workflow-1")).thenReturn(true);

        PermissionServiceImpl service = service(
            List.of(), List.of(), embeddedBeanFactory(embeddedResourceResolver));

        assertThat(service.canUseConnectionInWorkflow(1L, "workflow-1", Environment.DEVELOPMENT)).isFalse();
    }

    @Test
    void testAMissingConnectionIsRefusedInAnIntegrationWorkflow() {
        lenient().when(currentUserResolver.fetchCurrentUserId())
            .thenReturn(OptionalLong.of(7L));
        when(connectionService.fetchConnection(1L)).thenReturn(Optional.empty());

        EmbeddedResourceResolver embeddedResourceResolver = Mockito.mock(EmbeddedResourceResolver.class);

        when(embeddedResourceResolver.isIntegrationWorkflow("workflow-1")).thenReturn(true);

        PermissionServiceImpl service = service(
            List.of(), List.of(), embeddedBeanFactory(embeddedResourceResolver));

        assertThat(service.canUseConnectionInWorkflow(1L, "workflow-1", Environment.DEVELOPMENT)).isFalse();
    }

    private StaticListableBeanFactory connectionServiceBeanFactory() {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

        beanFactory.addBean("connectionService", connectionService);

        return beanFactory;
    }

    private static StaticListableBeanFactory embeddedBeanFactory(EmbeddedResourceResolver embeddedResourceResolver) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

        beanFactory.addBean("embeddedResourceResolver", embeddedResourceResolver);

        return beanFactory;
    }

    private static Connection connection(PlatformType type, Environment environment) {
        Connection connection = new Connection();

        connection.setEnvironmentId(environment.ordinal());
        connection.setType(type);

        return connection;
    }

    /**
     * A resolver that hides nothing, so these tests exercise workspace-scope and ownership resolution rather than
     * visibility. The visibility precondition has its own test class.
     */
    private static ResourceVisibilityResolver permissiveResolver() {
        return (resourceType, workspaceId, candidates) -> candidates.stream()
            .map(VisibilityRecord::id)
            .collect(Collectors.toSet());
    }
}
