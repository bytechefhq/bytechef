/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.ai.mcp.security.McpComponentEnvironmentResolver;
import com.bytechef.automation.ai.mcp.security.McpServerEnvironmentResolver;
import com.bytechef.automation.ai.mcp.security.McpToolEnvironmentResolver;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.security.ApiKeyEnvironmentResolver;
import com.bytechef.automation.configuration.security.ApiKeyOwnershipResolver;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowCreator;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.ee.automation.configuration.domain.EmbeddedUser;
import com.bytechef.ee.automation.configuration.repository.EmbeddedUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.automation.configuration.security.scope.ApiKeyPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ApiPlatformPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ConnectionPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.DataTablePermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.DeploymentPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ExecutionPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.KnowledgeBasePermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.McpPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ProjectPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.WorkflowPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.WorkspacePermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.AppEventPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.ConnectedUserPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.EmbeddedPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.EmbeddedSharedPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.IntegrationInstanceConfigurationPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.embedded.IntegrationPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.service.CurrentUserResolver;
import com.bytechef.ee.automation.configuration.service.CustomRoleScopeResolverImpl;
import com.bytechef.ee.automation.configuration.service.EmbeddedScopeCacheService;
import com.bytechef.ee.automation.configuration.service.PermissionScopeRegistry;
import com.bytechef.ee.automation.configuration.service.PermissionServiceImpl;
import com.bytechef.ee.automation.configuration.service.WorkspaceScopeCacheService;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.security.domain.SigningKey;
import com.bytechef.ee.embedded.security.facade.EmbeddedApiKeyFacade;
import com.bytechef.ee.embedded.security.facade.EmbeddedApiKeyFacadeImpl;
import com.bytechef.ee.embedded.security.facade.SigningKeyFacade;
import com.bytechef.ee.embedded.security.facade.SigningKeyFacadeImpl;
import com.bytechef.ee.embedded.security.repository.SigningKeyRepository;
import com.bytechef.ee.embedded.security.security.EmbeddedApiKeyOwnershipResolver;
import com.bytechef.ee.embedded.security.security.SigningKeyEnvironmentResolver;
import com.bytechef.ee.embedded.security.security.SigningKeyOwnershipResolver;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.ee.embedded.security.service.SigningKeyServiceImpl;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ComponentDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.configuration.facade.WebhookTriggerTestFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacadeImpl;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.facade.McpServerFacadeImpl;
import com.bytechef.platform.mcp.service.McpComponentServiceImpl;
import com.bytechef.platform.mcp.service.McpServerServiceImpl;
import com.bytechef.platform.mcp.service.McpToolServiceImpl;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.security.audit.ApiKeyAuditPublisher;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.facade.ApiKeyFacadeImpl;
import com.bytechef.platform.security.repository.ApiKeyRepository;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.service.ApiKeyServiceImpl;
import com.bytechef.platform.security.service.OwnedApiKeyCreatorImpl;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.ApiKeyRevoker;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        IntegrationIntTestConfiguration.class, EmbeddedKeyGatesIntTest.ConnectedUserServiceConfiguration.class,
        EmbeddedKeyGatesIntTest.EmbeddedMcpConfiguration.class,
        EmbeddedKeyGatesIntTest.KeyGatesConfiguration.class
    },
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true", "spring.liquibase.contexts=configuration,user"
    })
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {
    ActionDefinitionFacade.class, ClusterElementDefinitionService.class, ComponentConnectionFacade.class,
    ComponentDefinitionFacade.class, ComponentDefinitionService.class, ConnectionDefinitionService.class,
    ConnectionFacade.class, ConnectionService.class, EmbeddedPermissionEvaluator.class, EnvironmentService.class,
    JobFacade.class, JobService.class, OAuth2ParametersFacade.class, OAuth2Service.class, PrincipalJobFacade.class,
    PrincipalJobService.class, ProjectDeploymentFacade.class, ProjectDeploymentService.class,
    ProjectDeploymentWorkflowService.class, ProjectFacade.class, ProjectService.class, ProjectWorkflowCreator.class,
    ProjectWorkflowFacade.class, ProjectWorkflowService.class, TaskDispatcherDefinitionService.class,
    TriggerDefinitionFacade.class, TriggerDefinitionService.class, TriggerExecutionService.class,
    TriggerLifecycleFacade.class, WebhookTriggerTestFacade.class, WorkflowCacheManager.class,
    WorkflowNodeParameterFacade.class, WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class,
    WorkflowTestConfigurationService.class
})
class EmbeddedKeyGatesIntTest {

    private static final String DEVELOPMENT_EDITOR = "development-editor";
    private static final String EDITOR = "editor";
    private static final String EMBEDDED_ADMIN = "embedded-admin";
    private static final String NON_MEMBER = "non-member";
    private static final String OTHER_EDITOR = "other-editor";
    private static final String PRODUCTION_ADMIN = "production-admin";
    private static final String PRODUCTION_EDITOR = "production-editor";
    private static final String TENANT_ADMIN = "tenant-admin";
    private static final String VIEWER = "viewer";
    private static final Map<String, Long> USER_IDS_BY_LOGIN = Map.of(
        TENANT_ADMIN, 9800L, EDITOR, 9801L, OTHER_EDITOR, 9802L, PRODUCTION_EDITOR, 9803L, NON_MEMBER, 9804L,
        PRODUCTION_ADMIN, 9805L, VIEWER, 9806L, DEVELOPMENT_EDITOR, 9807L, EMBEDDED_ADMIN, 9808L);

    @Autowired
    private ApiKeyFacadeImpl apiKeyFacade;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private EmbeddedApiKeyFacade embeddedApiKeyFacade;

    @Autowired
    private EmbeddedUserRepository embeddedUserRepository;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private SigningKeyFacade signingKeyFacade;

    @Autowired
    private SigningKeyRepository signingKeyRepository;

    @Autowired
    private SigningKeyService signingKeyService;

    @BeforeEach
    void beforeEach() {
        embeddedUserRepository.save(EmbeddedUser.forRole(userId(EDITOR), WorkspaceRole.EDITOR));
        embeddedUserRepository.save(EmbeddedUser.forRole(userId(OTHER_EDITOR), WorkspaceRole.EDITOR));
        embeddedUserRepository.save(EmbeddedUser.forRole(userId(VIEWER), WorkspaceRole.VIEWER));
        embeddedUserRepository.save(
            EmbeddedUser.forRole(userId(PRODUCTION_EDITOR), WorkspaceRole.EDITOR, Environment.PRODUCTION));
        embeddedUserRepository.save(
            EmbeddedUser.forRole(userId(DEVELOPMENT_EDITOR), WorkspaceRole.EDITOR, Environment.DEVELOPMENT));
        embeddedUserRepository.save(EmbeddedUser.forRole(userId(EMBEDDED_ADMIN), WorkspaceRole.ADMIN));
        embeddedUserRepository.save(
            EmbeddedUser.forRole(userId(PRODUCTION_ADMIN), WorkspaceRole.ADMIN, Environment.PRODUCTION));

        permissionService.evictAllEmbeddedScopeCache();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        for (Long userId : USER_IDS_BY_LOGIN.values()) {
            embeddedUserRepository.deleteByUserId(userId);
        }

        permissionService.evictAllEmbeddedScopeCache();

        apiKeyRepository.deleteAll();
        signingKeyRepository.deleteAll();
    }

    @Test
    void testAnEmbeddedEditorCreatesAnEmbeddedKeyInTheirEnvironment() {
        authenticate(EDITOR, false);

        String secret = embeddedApiKeyFacade.createApiKey(newApiKey(Environment.PRODUCTION));

        assertThat(apiKeyRepository.findAll())
            .singleElement()
            .satisfies(apiKey -> {
                assertThat(apiKey.getSecretKey()).isEqualTo(secret);
                assertThat(apiKey.getType()).isEqualTo(PlatformType.EMBEDDED);
                assertThat(apiKey.getUserId()).isEqualTo(userId(EDITOR));
                assertThat(apiKey.getEnvironment()).isEqualTo(Environment.PRODUCTION);
            });
    }

    @Test
    void testAnEmbeddedEditorSeesEveryEmbeddedKeyOfTheEnvironment() {
        ApiKey ownKey = saveApiKey(PlatformType.EMBEDDED, EDITOR, Environment.PRODUCTION);
        ApiKey colleagueKey = saveApiKey(PlatformType.EMBEDDED, OTHER_EDITOR, Environment.PRODUCTION);

        saveApiKey(PlatformType.EMBEDDED, OTHER_EDITOR, Environment.DEVELOPMENT);
        saveApiKey(PlatformType.AUTOMATION, OTHER_EDITOR, Environment.PRODUCTION);
        saveApiKey(null, OTHER_EDITOR, Environment.PRODUCTION);

        authenticate(EDITOR, false);

        assertThat(embeddedApiKeyFacade.getApiKeys(Environment.PRODUCTION.ordinal()))
            .extracting(ApiKey::getId)
            .containsExactlyInAnyOrder(ownKey.getId(), colleagueKey.getId());

        authenticate(VIEWER, false);

        assertThatThrownBy(() -> embeddedApiKeyFacade.getApiKeys(Environment.PRODUCTION.ordinal()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testAnEmbeddedKeyIsDeletableByScopeNotOnlyByItsOwner() {
        ApiKey colleagueKey = saveApiKey(PlatformType.EMBEDDED, OTHER_EDITOR, Environment.PRODUCTION);

        authenticate(EDITOR, false);

        ApiKey renamedApiKey = new ApiKey();

        renamedApiKey.setId(colleagueKey.getId());
        renamedApiKey.setName("renamed");

        assertThat(embeddedApiKeyFacade.updateApiKey(renamedApiKey)
            .getName()).isEqualTo("renamed");
        assertThat(embeddedApiKeyFacade.getApiKey(colleagueKey.getId())
            .getId()).isEqualTo(colleagueKey.getId());

        embeddedApiKeyFacade.deleteApiKey(colleagueKey.getId());

        assertThat(apiKeyRepository.findById(colleagueKey.getId())).isEmpty();
    }

    @Test
    void testAProductionOnlyMemberCannotTouchADevelopmentKey() {
        ApiKey developmentKey = saveApiKey(PlatformType.EMBEDDED, OTHER_EDITOR, Environment.DEVELOPMENT);
        ApiKey productionKey = saveApiKey(PlatformType.EMBEDDED, OTHER_EDITOR, Environment.PRODUCTION);

        authenticate(PRODUCTION_EDITOR, false);

        long developmentKeyId = developmentKey.getId();

        ApiKey renamedApiKey = new ApiKey();

        renamedApiKey.setId(developmentKeyId);
        renamedApiKey.setName("renamed");

        assertThatThrownBy(() -> embeddedApiKeyFacade.getApiKey(developmentKeyId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedApiKeyFacade.updateApiKey(renamedApiKey))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedApiKeyFacade.deleteApiKey(developmentKeyId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedApiKeyFacade.getApiKeys(Environment.DEVELOPMENT.ordinal()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedApiKeyFacade.createApiKey(newApiKey(Environment.DEVELOPMENT)))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(embeddedApiKeyFacade.getApiKey(productionKey.getId())
            .getId()).isEqualTo(productionKey.getId());
        assertThat(apiKeyRepository.findById(developmentKeyId)).isPresent();
    }

    @Test
    void testTheEmbeddedFacadeRefusesAnAutomationOrPlatformKey() {
        ApiKey automationKey = saveApiKey(PlatformType.AUTOMATION, OTHER_EDITOR, Environment.PRODUCTION);
        ApiKey platformKey = saveApiKey(null, OTHER_EDITOR, Environment.PRODUCTION);

        for (ApiKey apiKey : List.of(automationKey, platformKey)) {
            long id = apiKey.getId();

            ApiKey renamedApiKey = new ApiKey();

            renamedApiKey.setId(id);
            renamedApiKey.setName("renamed");

            authenticate(TENANT_ADMIN, true);

            assertThatThrownBy(() -> embeddedApiKeyFacade.getApiKey(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Api key not found");
            assertThatThrownBy(() -> embeddedApiKeyFacade.updateApiKey(renamedApiKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Api key not found");
            assertThatThrownBy(() -> embeddedApiKeyFacade.deleteApiKey(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Api key not found");

            authenticate(EDITOR, false);

            assertThatThrownBy(() -> embeddedApiKeyFacade.getApiKey(id))
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> embeddedApiKeyFacade.updateApiKey(renamedApiKey))
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> embeddedApiKeyFacade.deleteApiKey(id))
                .isInstanceOf(AccessDeniedException.class);
        }

        assertThat(apiKeyRepository.findAll()).hasSize(2);
    }

    @Test
    void testRemovingAUserKeepsTheirEmbeddedKeys() {
        ApiKey embeddedKey = saveApiKey(PlatformType.EMBEDDED, EDITOR, Environment.PRODUCTION);
        ApiKey automationKey = saveApiKey(PlatformType.AUTOMATION, EDITOR, Environment.PRODUCTION);

        ((ApiKeyRevoker) apiKeyFacade).revokeAll(userId(EDITOR));

        assertThat(apiKeyRepository.findById(embeddedKey.getId())).isPresent();
        assertThat(apiKeyRepository.findById(automationKey.getId())).isEmpty();
    }

    @Test
    void testATenantAdminWithoutARowStillPasses() {
        authenticate(TENANT_ADMIN, true);

        String secret = embeddedApiKeyFacade.createApiKey(newApiKey(Environment.DEVELOPMENT));

        assertThat(secret).isNotBlank();

        List<ApiKey> apiKeys = embeddedApiKeyFacade.getApiKeys(Environment.DEVELOPMENT.ordinal());

        assertThat(apiKeys).hasSize(1);

        long id = apiKeys.getFirst()
            .getId();

        assertThat(embeddedApiKeyFacade.getApiKey(id)
            .getId()).isEqualTo(id);

        embeddedApiKeyFacade.deleteApiKey(id);

        assertThat(apiKeyRepository.findById(id)).isEmpty();

        String privateKey = signingKeyFacade.create(newSigningKey(Environment.DEVELOPMENT), PlatformType.EMBEDDED);

        assertThat(privateKey).isNotBlank();
    }

    @Test
    void testANonMemberIsDenied() {
        ApiKey embeddedKey = saveApiKey(PlatformType.EMBEDDED, EDITOR, Environment.PRODUCTION);
        SigningKey signingKey = saveSigningKey(Environment.PRODUCTION);

        authenticate(NON_MEMBER, false);

        long apiKeyId = embeddedKey.getId();
        long signingKeyId = signingKey.getId();

        assertThatThrownBy(() -> embeddedApiKeyFacade.createApiKey(newApiKey(Environment.PRODUCTION)))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedApiKeyFacade.getApiKeys(Environment.PRODUCTION.ordinal()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedApiKeyFacade.getApiKey(apiKeyId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> embeddedApiKeyFacade.deleteApiKey(apiKeyId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> signingKeyFacade.getSigningKey(signingKeyId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> signingKeyFacade.getSigningKeys(PlatformType.EMBEDDED, Environment.PRODUCTION.ordinal()))
                .isInstanceOf(AccessDeniedException.class);

        ApiKey renamedApiKey = new ApiKey();

        renamedApiKey.setId(apiKeyId);
        renamedApiKey.setName("renamed");

        SigningKey renamedSigningKey = new SigningKey();

        renamedSigningKey.setId(signingKeyId);
        renamedSigningKey.setName("renamed");

        assertThatThrownBy(() -> embeddedApiKeyFacade.updateApiKey(renamedApiKey))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> signingKeyFacade.create(newSigningKey(Environment.PRODUCTION), PlatformType.EMBEDDED))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> signingKeyFacade.update(renamedSigningKey))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> signingKeyFacade.delete(signingKeyId))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(apiKeyRepository.findAll())
            .singleElement()
            .satisfies(apiKey -> assertThat(apiKey.getName()).isEqualTo(embeddedKey.getName()));
        assertThat(signingKeyRepository.findAll())
            .singleElement()
            .satisfies(persistedSigningKey -> assertThat(persistedSigningKey.getName())
                .isEqualTo(signingKey.getName()));
    }

    @Test
    void testASigningKeyNeedsAnAdminInItsEnvironment() {
        SigningKey developmentKey = saveSigningKey(Environment.DEVELOPMENT);

        authenticate(PRODUCTION_ADMIN, false);

        assertThat(signingKeyFacade.create(newSigningKey(Environment.PRODUCTION), PlatformType.EMBEDDED))
            .isNotBlank();

        List<SigningKey> productionKeys =
            signingKeyFacade.getSigningKeys(PlatformType.EMBEDDED, Environment.PRODUCTION.ordinal());

        assertThat(productionKeys).hasSize(1);

        long productionKeyId = productionKeys.getFirst()
            .getId();

        assertThat(signingKeyFacade.getSigningKey(productionKeyId)
            .getId()).isEqualTo(productionKeyId);

        SigningKey renamedSigningKey = new SigningKey();

        renamedSigningKey.setId(productionKeyId);
        renamedSigningKey.setName("renamed");

        signingKeyFacade.update(renamedSigningKey);
        signingKeyFacade.delete(productionKeyId);

        long developmentKeyId = developmentKey.getId();

        assertThatThrownBy(() -> signingKeyFacade.create(newSigningKey(Environment.DEVELOPMENT), PlatformType.EMBEDDED))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> signingKeyFacade.getSigningKey(developmentKeyId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> signingKeyFacade.delete(developmentKeyId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> signingKeyFacade.getSigningKeys(PlatformType.EMBEDDED, Environment.DEVELOPMENT.ordinal()))
                .isInstanceOf(AccessDeniedException.class);

        authenticate(EDITOR, false);

        assertThatThrownBy(() -> signingKeyFacade.create(newSigningKey(Environment.PRODUCTION), PlatformType.EMBEDDED))
            .isInstanceOf(AccessDeniedException.class);

        authenticate(TENANT_ADMIN, true);

        assertThat(signingKeyFacade.create(newSigningKey(Environment.PRODUCTION), PlatformType.AUTOMATION))
            .isNotBlank();

        SigningKey automationKey = signingKeyRepository.findAll()
            .stream()
            .filter(signingKey -> signingKey.getType() == PlatformType.AUTOMATION)
            .findFirst()
            .orElseThrow();

        long automationKeyId = automationKey.getId();

        authenticate(EMBEDDED_ADMIN, false);

        assertThatThrownBy(() -> signingKeyFacade.getSigningKey(automationKeyId))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> signingKeyFacade.delete(automationKeyId))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(signingKeyRepository.findById(automationKeyId)).isPresent();

        authenticate(PRODUCTION_ADMIN, false);

        assertThatThrownBy(
            () -> signingKeyFacade.create(newSigningKey(Environment.PRODUCTION), PlatformType.AUTOMATION))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testTheUngatedSigningKeyServiceServesCallersWithoutAUser() {
        saveSigningKey(Environment.PRODUCTION);

        SecurityContextHolder.clearContext();

        assertThat(signingKeyService.getSigningKeys(PlatformType.EMBEDDED, Environment.PRODUCTION.ordinal()))
            .hasSize(1);
    }

    private static void authenticate(String login, boolean tenantAdmin) {
        String authority = tenantAdmin ? "ROLE_ADMIN" : "ROLE_USER";

        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(login, "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    private static ApiKey newApiKey(Environment environment) {
        ApiKey apiKey = new ApiKey();

        apiKey.setEnvironment(environment);
        apiKey.setName("key-" + UUID.randomUUID());

        return apiKey;
    }

    private static SigningKey newSigningKey(Environment environment) {
        SigningKey signingKey = new SigningKey();

        signingKey.setEnvironment(environment);
        signingKey.setName("signing-key-" + UUID.randomUUID());

        return signingKey;
    }

    private ApiKey saveApiKey(PlatformType type, String ownerLogin, Environment environment) {
        ApiKey apiKey = newApiKey(environment);

        apiKey.setType(type);
        apiKey.setUserId(userId(ownerLogin));

        return apiKeyService.create(apiKey);
    }

    private SigningKey saveSigningKey(Environment environment) {
        SigningKey signingKey = newSigningKey(environment);

        signingKey.setKeyId(UUID.randomUUID()
            .toString());
        signingKey.setPublicKey("public-key");
        signingKey.setType(PlatformType.EMBEDDED);
        signingKey.setUserId(userId(EDITOR));

        return signingKeyRepository.save(signingKey);
    }

    private static long userId(String login) {
        return USER_IDS_BY_LOGIN.get(login);
    }

    @Configuration
    @ComponentScan({
        "com.bytechef.ee.embedded.connected.user.security", "com.bytechef.ee.embedded.connected.user.service"
    })
    static class ConnectedUserServiceConfiguration {
    }

    @Configuration
    @ComponentScan(
        basePackages = "com.bytechef.ee.embedded.ai.mcp",
        excludeFilters = @Filter(
            type = FilterType.REGEX, pattern = "com\\.bytechef\\.ee\\.embedded\\.ai\\.mcp\\.config\\..*"))
    @Import({
        McpComponentEnvironmentResolver.class, McpComponentServiceImpl.class, McpServerEnvironmentResolver.class,
        McpServerFacadeImpl.class, McpServerServiceImpl.class, McpToolEnvironmentResolver.class,
        McpToolServiceImpl.class
    })
    static class EmbeddedMcpConfiguration {
    }

    @Configuration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import({
        ApiKeyAuditPublisher.class, ApiKeyEnvironmentResolver.class, ApiKeyFacadeImpl.class,
        ApiKeyOwnershipResolver.class, ApiKeyPermissionScopeProvider.class, ApiKeyServiceImpl.class,
        ApiPlatformPermissionScopeProvider.class, AppEventPermissionScopeProvider.class,
        ConnectedUserPermissionScopeProvider.class, ConnectionPermissionScopeProvider.class,
        CurrentUserResolver.class, CustomRoleScopeResolverImpl.class, DataTablePermissionScopeProvider.class,
        DeploymentPermissionScopeProvider.class, EmbeddedApiKeyFacadeImpl.class,
        EmbeddedApiKeyOwnershipResolver.class, EmbeddedPermissionScopeProvider.class, EmbeddedScopeCacheService.class,
        EmbeddedSharedPermissionScopeProvider.class, ExecutionPermissionScopeProvider.class,
        IntegrationInstanceConfigurationPermissionScopeProvider.class, IntegrationPermissionScopeProvider.class,
        KnowledgeBasePermissionScopeProvider.class, McpPermissionScopeProvider.class,
        OwnedApiKeyCreatorImpl.class, PermissionScopeRegistry.class, PermissionServiceImpl.class,
        ProjectPermissionScopeProvider.class, SigningKeyEnvironmentResolver.class, SigningKeyFacadeImpl.class,
        SigningKeyOwnershipResolver.class, SigningKeyServiceImpl.class, WorkflowPermissionScopeProvider.class,
        WorkspacePermissionScopeProvider.class, WorkspaceScopeCacheService.class
    })
    static class KeyGatesConfiguration {

        @Bean
        ResourceVisibilityResolver resourceVisibilityResolver() {
            return (resourceType, workspaceId, candidates) -> candidates.stream()
                .map(VisibilityRecord::id)
                .collect(Collectors.toSet());
        }

        @Bean
        UserService userService() {
            UserService userService = mock(UserService.class);

            when(userService.getCurrentUser()).thenAnswer(invocation -> user(
                SecurityUtils.fetchCurrentUserLogin()
                    .orElseThrow()));
            when(userService.fetchUserByLogin(anyString()))
                .thenAnswer(invocation -> Optional.of(user(invocation.getArgument(0))));
            when(userService.getUser(anyString())).thenAnswer(invocation -> user(invocation.getArgument(0)));

            return userService;
        }

        private static User user(String login) {
            User user = new User();

            user.setId(USER_IDS_BY_LOGIN.get(login));
            user.setLogin(login);

            return user;
        }

        @Bean
        WorkflowFacade workflowFacade(
            ComponentConnectionFacade componentConnectionFacade,
            ComponentDefinitionService componentDefinitionService, WorkflowValidatorFacade workflowValidatorFacade,
            WorkflowService workflowService) {

            return new WorkflowFacadeImpl(
                componentConnectionFacade, componentDefinitionService, workflowValidatorFacade, workflowService);
        }

        @Bean
        WorkflowService workflowService(
            CacheManager cacheManager, List<WorkflowCrudRepository> workflowCrudRepositories,
            List<WorkflowRepository> workflowRepositories) {

            return new WorkflowServiceImpl(cacheManager, workflowCrudRepositories, workflowRepositories);
        }
    }
}
