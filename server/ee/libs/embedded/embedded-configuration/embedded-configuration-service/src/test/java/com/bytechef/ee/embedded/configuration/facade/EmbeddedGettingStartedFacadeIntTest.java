/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.EmbeddedGettingStartedDTO;
import com.bytechef.ee.embedded.configuration.dto.LastFailedCallDTO;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.platform.external.invocation.log.domain.ExternalInvocationLog;
import com.bytechef.ee.platform.external.invocation.log.service.ExternalInvocationLogService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.repository.PropertyRepository;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.external.invocation.ExternalInvocationStatus;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        IntegrationIntTestConfiguration.class, EmbeddedGettingStartedFacadeIntTest.MethodSecurityConfiguration.class
    },
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true",
        "bytechef.webhook-url=/webhooks/{id}"
    })
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class EmbeddedGettingStartedFacadeIntTest {

    private static final long RUNNABLE_CONNECTED_USER_ID = 42L;
    private static final long LAST_ENABLED_CONNECTED_USER_ID = 4242L;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private EmbeddedGettingStartedFacade embeddedGettingStartedFacade;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private ExternalInvocationLogService externalInvocationLogService;

    @Autowired
    private IntegrationInstanceConfigurationRepository integrationInstanceConfigurationRepository;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @MockitoBean
    private IntegrationInstanceService integrationInstanceService;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private IntegrationWorkflowRepository integrationWorkflowRepository;

    @BeforeEach
    void beforeEach() {
        reset(externalInvocationLogService);

        propertyRepository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        propertyRepository.deleteAll();
        integrationInstanceConfigurationRepository.deleteAll();
        integrationWorkflowRepository.deleteAll();
        integrationRepository.deleteAll();
    }

    @Test
    void testGetEmbeddedGettingStartedAllowedOnlyForAdmin() {
        authenticate(nonAdmin());

        assertThatThrownBy(() -> embeddedGettingStartedFacade.getEmbeddedGettingStarted())
            .isInstanceOf(AccessDeniedException.class);

        authenticate(admin());

        assertThat(embeddedGettingStartedFacade.getEmbeddedGettingStarted()).isNotNull();
    }

    @Test
    void testIsEmbeddedGettingStartedCompletedAllowedOnlyForAdmin() {
        authenticate(nonAdmin());

        assertThatThrownBy(() -> embeddedGettingStartedFacade.isEmbeddedGettingStartedCompleted())
            .isInstanceOf(AccessDeniedException.class);

        authenticate(admin());

        assertThat(embeddedGettingStartedFacade.isEmbeddedGettingStartedCompleted()).isFalse();
    }

    @Test
    void testDismissEmbeddedGettingStartedAllowedOnlyForAdmin() {
        authenticate(nonAdmin());

        assertThatThrownBy(() -> embeddedGettingStartedFacade.dismissEmbeddedGettingStarted())
            .isInstanceOf(AccessDeniedException.class);

        assertThat(propertyRepository.findAll()).isEmpty();

        authenticate(admin());

        assertThatCode(() -> embeddedGettingStartedFacade.dismissEmbeddedGettingStarted()).doesNotThrowAnyException();

        assertThat(embeddedGettingStartedFacade.isEmbeddedGettingStartedCompleted()).isTrue();
    }

    @Test
    void testAllChecksFalseOnEmptyTenant() {
        authenticate(admin());

        EmbeddedGettingStartedDTO embeddedGettingStartedDTO =
            embeddedGettingStartedFacade.getEmbeddedGettingStarted();

        assertThat(embeddedGettingStartedDTO.apiKeyCreated()).isFalse();
        assertThat(embeddedGettingStartedDTO.integrationCreated()).isFalse();
        assertThat(embeddedGettingStartedDTO.integrationPublished()).isFalse();
        assertThat(embeddedGettingStartedDTO.integrationConfigurationCreated()).isFalse();
        assertThat(embeddedGettingStartedDTO.integrationConfigurationEnabled()).isFalse();
        assertThat(embeddedGettingStartedDTO.testUserAdded()).isFalse();
        assertThat(embeddedGettingStartedDTO.testUserConnected()).isFalse();
        assertThat(embeddedGettingStartedDTO.testUserExternalId()).isNull();
        assertThat(embeddedGettingStartedDTO.firstCallMade()).isFalse();
        assertThat(embeddedGettingStartedDTO.lastFailedCall()).isNull();
        assertThat(embeddedGettingStartedDTO.completed()).isFalse();
        assertThat(embeddedGettingStartedDTO.completionType()).isNull();
        assertThat(embeddedGettingStartedDTO.integrationId()).isNull();
        assertThat(embeddedGettingStartedDTO.integrationInstanceConfigurationId()).isNull();
        assertThat(embeddedGettingStartedDTO.testUserId()).isNull();
        assertThat(embeddedGettingStartedDTO.componentName()).isNull();
        assertThat(embeddedGettingStartedDTO.componentVersion()).isNull();

        assertThat(propertyRepository.findAll()).isEmpty();
    }

    @Test
    void testTargetIdsPointAtTheNewestPublishedIntegrationAndItsConfiguration() {
        authenticate(admin());

        long publishedIntegrationId = createPublishedIntegration("httpClient", 1);
        long unpublishedIntegrationId = createIntegration("github", 1);
        long newerPublishedIntegrationId = createPublishedIntegration("github", 1);

        long publishedConfigurationId = createConfiguration(publishedIntegrationId, Environment.DEVELOPMENT, false);

        createConfiguration(unpublishedIntegrationId, Environment.DEVELOPMENT, false);

        EmbeddedGettingStartedDTO embeddedGettingStartedDTO =
            embeddedGettingStartedFacade.getEmbeddedGettingStarted();

        assertThat(embeddedGettingStartedDTO.integrationId()).isEqualTo(newerPublishedIntegrationId);
        assertThat(embeddedGettingStartedDTO.integrationInstanceConfigurationId()).isEqualTo(publishedConfigurationId);
    }

    @Test
    void testComponentIsTheComponentOfTheNewestPublishedConfiguration() {
        authenticate(admin());

        long publishedIntegrationId = createPublishedIntegration("httpClient", 2);

        createPublishedIntegration("github", 1);

        createConfiguration(publishedIntegrationId, Environment.DEVELOPMENT, false);

        EmbeddedGettingStartedDTO embeddedGettingStartedDTO =
            embeddedGettingStartedFacade.getEmbeddedGettingStarted();

        assertThat(embeddedGettingStartedDTO.componentName()).isEqualTo("httpClient");
        assertThat(embeddedGettingStartedDTO.componentVersion()).isEqualTo(2);
    }

    @Test
    void testComponentIsTheComponentOfTheIntegrationTheTestUserCanRun() {
        authenticate(admin());

        long publishedIntegrationId = createPublishedIntegration("httpClient", 2);
        long newerPublishedIntegrationId = createPublishedIntegration("github", 1);

        long publishedConfigurationId = createConfiguration(publishedIntegrationId, Environment.DEVELOPMENT, true);

        createConfiguration(newerPublishedIntegrationId, Environment.DEVELOPMENT, true);

        stubRunnableIntegrationInstance(Environment.DEVELOPMENT, publishedConfigurationId);

        EmbeddedGettingStartedDTO embeddedGettingStartedDTO =
            embeddedGettingStartedFacade.getEmbeddedGettingStarted();

        assertThat(embeddedGettingStartedDTO.testUserExternalId()).isEqualTo("external-user");
        assertThat(embeddedGettingStartedDTO.componentName()).isEqualTo("httpClient");
        assertThat(embeddedGettingStartedDTO.componentVersion()).isEqualTo(2);
    }

    @Test
    void testComponentIsTheComponentOfTheTargetIntegrationWithoutConfiguration() {
        authenticate(admin());

        createIntegration("github", 1);

        EmbeddedGettingStartedDTO embeddedGettingStartedDTO =
            embeddedGettingStartedFacade.getEmbeddedGettingStarted();

        assertThat(embeddedGettingStartedDTO.componentName()).isEqualTo("github");
        assertThat(embeddedGettingStartedDTO.componentVersion()).isEqualTo(1);
    }

    @Test
    void testTargetIntegrationIsTheNewestIntegrationWhenNoneIsPublished() {
        authenticate(admin());

        createIntegration("httpClient", 1);

        long newerIntegrationId = createIntegration("github", 1);

        EmbeddedGettingStartedDTO embeddedGettingStartedDTO =
            embeddedGettingStartedFacade.getEmbeddedGettingStarted();

        assertThat(embeddedGettingStartedDTO.integrationCreated()).isTrue();
        assertThat(embeddedGettingStartedDTO.integrationPublished()).isFalse();
        assertThat(embeddedGettingStartedDTO.integrationId()).isEqualTo(newerIntegrationId);
        assertThat(embeddedGettingStartedDTO.integrationInstanceConfigurationId()).isNull();
    }

    @Test
    void testIntegrationHasWorkflowsReflectsTheDraftOfTheTargetIntegration() {
        authenticate(admin());

        long integrationId = createIntegration("httpClient", 1);

        assertThat(embeddedGettingStartedFacade.getEmbeddedGettingStarted()
            .integrationHasWorkflows()).isFalse();

        integrationWorkflowRepository.save(
            new IntegrationWorkflow(integrationId, 1, String.valueOf(UUID.randomUUID()), UUID.randomUUID()));

        assertThat(embeddedGettingStartedFacade.getEmbeddedGettingStarted()
            .integrationHasWorkflows()).isTrue();
    }

    @Test
    void testIntegrationConfigurationEnabledOnlyForPublishedIntegration() {
        authenticate(admin());

        long publishedIntegrationId = createPublishedIntegration("httpClient", 1);
        long unpublishedIntegrationId = createIntegration("github", 1);

        createConfiguration(unpublishedIntegrationId, Environment.DEVELOPMENT, true);

        EmbeddedGettingStartedDTO embeddedGettingStartedDTO =
            embeddedGettingStartedFacade.getEmbeddedGettingStarted();

        assertThat(embeddedGettingStartedDTO.integrationCreated()).isTrue();
        assertThat(embeddedGettingStartedDTO.integrationPublished()).isTrue();
        assertThat(embeddedGettingStartedDTO.integrationConfigurationEnabled()).isFalse();

        createConfiguration(publishedIntegrationId, Environment.DEVELOPMENT, true);

        embeddedGettingStartedDTO = embeddedGettingStartedFacade.getEmbeddedGettingStarted();

        assertThat(embeddedGettingStartedDTO.integrationConfigurationEnabled()).isTrue();
    }

    @Test
    void testIntegrationConfigurationOutsideDevelopmentIsIgnored() {
        authenticate(admin());

        long publishedIntegrationId = createPublishedIntegration("httpClient", 1);

        createConfiguration(publishedIntegrationId, Environment.STAGING, true);

        EmbeddedGettingStartedDTO embeddedGettingStartedDTO =
            embeddedGettingStartedFacade.getEmbeddedGettingStarted();

        assertThat(embeddedGettingStartedDTO.integrationConfigurationCreated()).isFalse();
        assertThat(embeddedGettingStartedDTO.integrationConfigurationEnabled()).isFalse();
    }

    @Test
    void testIntegrationConfigurationCreatedCountsDisabledConfigurationsOfPublishedIntegrations() {
        authenticate(admin());

        long publishedIntegrationId = createPublishedIntegration("httpClient", 1);
        long unpublishedIntegrationId = createIntegration("github", 1);

        createConfiguration(unpublishedIntegrationId, Environment.DEVELOPMENT, false);

        EmbeddedGettingStartedDTO embeddedGettingStartedDTO =
            embeddedGettingStartedFacade.getEmbeddedGettingStarted();

        assertThat(embeddedGettingStartedDTO.integrationConfigurationCreated()).isFalse();

        createConfiguration(publishedIntegrationId, Environment.DEVELOPMENT, false);

        embeddedGettingStartedDTO = embeddedGettingStartedFacade.getEmbeddedGettingStarted();

        assertThat(embeddedGettingStartedDTO.integrationConfigurationCreated()).isTrue();
        assertThat(embeddedGettingStartedDTO.integrationConfigurationEnabled()).isFalse();
    }

    @Test
    void testTestUserAddedBeforeTheIntegrationIsProvisioned() {
        authenticate(admin());

        stubLastEnabledConnectedUser(Environment.DEVELOPMENT);

        EmbeddedGettingStartedDTO embeddedGettingStartedDTO =
            embeddedGettingStartedFacade.getEmbeddedGettingStarted();

        assertThat(embeddedGettingStartedDTO.testUserAdded()).isTrue();
        assertThat(embeddedGettingStartedDTO.testUserConnected()).isFalse();
        assertThat(embeddedGettingStartedDTO.testUserId()).isEqualTo(LAST_ENABLED_CONNECTED_USER_ID);
    }

    @Test
    void testTestUserIdIsTheConnectedUserOfTheRunnableIntegrationInstance() {
        authenticate(admin());

        long publishedIntegrationId = createPublishedIntegration("httpClient", 1);

        long configurationId = createConfiguration(publishedIntegrationId, Environment.DEVELOPMENT, true);

        stubRunnableIntegrationInstance(Environment.DEVELOPMENT, configurationId);
        stubLastEnabledConnectedUser(Environment.DEVELOPMENT);

        EmbeddedGettingStartedDTO embeddedGettingStartedDTO =
            embeddedGettingStartedFacade.getEmbeddedGettingStarted();

        assertThat(embeddedGettingStartedDTO.testUserId()).isEqualTo(RUNNABLE_CONNECTED_USER_ID);
        assertThat(embeddedGettingStartedDTO.testUserAdded()).isTrue();
        assertThat(embeddedGettingStartedDTO.testUserConnected()).isTrue();
        assertThat(embeddedGettingStartedDTO.testUserExternalId()).isEqualTo("external-user");
    }

    @Test
    void testTestUserIdFallsBackToTheLastEnabledConnectedUserWhenNothingIsRunnable() {
        authenticate(admin());

        long publishedIntegrationId = createPublishedIntegration("httpClient", 1);

        createConfiguration(publishedIntegrationId, Environment.DEVELOPMENT, true);

        stubLastEnabledConnectedUser(Environment.DEVELOPMENT);

        EmbeddedGettingStartedDTO embeddedGettingStartedDTO =
            embeddedGettingStartedFacade.getEmbeddedGettingStarted();

        assertThat(embeddedGettingStartedDTO.testUserId()).isEqualTo(LAST_ENABLED_CONNECTED_USER_ID);
        assertThat(embeddedGettingStartedDTO.testUserConnected()).isFalse();
        assertThat(embeddedGettingStartedDTO.testUserExternalId()).isNull();
    }

    @Test
    void testCompletesWhenAllFourStepsHold() {
        authenticate(admin());

        stubAllFourStepsHold(Environment.DEVELOPMENT);

        EmbeddedGettingStartedDTO embeddedGettingStartedDTO =
            embeddedGettingStartedFacade.getEmbeddedGettingStarted();

        assertThat(embeddedGettingStartedDTO.testUserConnected()).isTrue();
        assertThat(embeddedGettingStartedDTO.testUserExternalId()).isEqualTo("external-user");
        assertThat(embeddedGettingStartedDTO.completed()).isTrue();
        assertThat(embeddedGettingStartedDTO.completionType()).isEqualTo("COMPLETED");

        assertThat(propertyRepository.findAll())
            .singleElement()
            .extracting(property -> property.get("completionType"))
            .isEqualTo("COMPLETED");
    }

    @ParameterizedTest
    @EnumSource(CompletionCondition.class)
    void testDoesNotCompleteWhenOneConditionIsMissing(CompletionCondition missingCompletionCondition) {
        authenticate(admin());

        stubAllFourStepsHoldExcept(Environment.DEVELOPMENT, missingCompletionCondition);

        EmbeddedGettingStartedDTO embeddedGettingStartedDTO =
            embeddedGettingStartedFacade.getEmbeddedGettingStarted();

        assertThat(embeddedGettingStartedDTO.completed()).isFalse();
        assertThat(embeddedGettingStartedDTO.completionType()).isNull();
        assertThat(propertyRepository.findAll()).isEmpty();

        assertThat(embeddedGettingStartedFacade.isEmbeddedGettingStartedCompleted()).isFalse();
        assertThat(propertyRepository.findAll()).isEmpty();
    }

    @Test
    void testDoesNotCompleteAgainWhenRowExists() {
        authenticate(admin());

        stubAllFourStepsHold(Environment.DEVELOPMENT);

        embeddedGettingStartedFacade.dismissEmbeddedGettingStarted();

        EmbeddedGettingStartedDTO embeddedGettingStartedDTO =
            embeddedGettingStartedFacade.getEmbeddedGettingStarted();

        assertThat(embeddedGettingStartedDTO.completed()).isTrue();
        assertThat(embeddedGettingStartedDTO.completionType()).isEqualTo("DISMISSED");

        assertThat(propertyRepository.findAll())
            .singleElement()
            .extracting(property -> property.get("completionType"))
            .isEqualTo("DISMISSED");
    }

    @Test
    void testLastFailedCallNullAfterSuccess() {
        authenticate(admin());

        stubFirstCallMade(Environment.DEVELOPMENT);

        ExternalInvocationLog failedExternalInvocationLog = failedExternalInvocationLog();

        when(externalInvocationLogService.fetchLastFailedExternalInvocationLog(any(), anyInt()))
            .thenReturn(Optional.of(failedExternalInvocationLog));

        EmbeddedGettingStartedDTO embeddedGettingStartedDTO =
            embeddedGettingStartedFacade.getEmbeddedGettingStarted();

        assertThat(embeddedGettingStartedDTO.firstCallMade()).isTrue();
        assertThat(embeddedGettingStartedDTO.lastFailedCall()).isNull();
    }

    @Test
    void testLastFailedCallReportedWhenNoSuccess() {
        authenticate(admin());

        ExternalInvocationLog failedExternalInvocationLog = failedExternalInvocationLog();

        when(externalInvocationLogService.fetchLastFailedExternalInvocationLog(
            PlatformType.EMBEDDED, Environment.DEVELOPMENT.ordinal()))
                .thenReturn(Optional.of(failedExternalInvocationLog));

        EmbeddedGettingStartedDTO embeddedGettingStartedDTO =
            embeddedGettingStartedFacade.getEmbeddedGettingStarted();

        assertThat(embeddedGettingStartedDTO.firstCallMade()).isFalse();

        LastFailedCallDTO lastFailedCallDTO = embeddedGettingStartedDTO.lastFailedCall();

        assertThat(lastFailedCallDTO).isNotNull();
        assertThat(lastFailedCallDTO.status()).isEqualTo("CONNECTION_REQUIRED");
        assertThat(lastFailedCallDTO.errorMessage()).isEqualTo("Connection required");
        assertThat(lastFailedCallDTO.createdDate()).isEqualTo(Instant.parse("2026-11-11T10:00:00Z"));
    }

    @Test
    void testFirstCallMadeOutsideDevelopmentIsIgnored() {
        authenticate(admin());

        stubFirstCallMade(Environment.STAGING);

        EmbeddedGettingStartedDTO embeddedGettingStartedDTO = embeddedGettingStartedFacade.getEmbeddedGettingStarted();

        assertThat(embeddedGettingStartedDTO.firstCallMade()).isFalse();
    }

    @Test
    void testIsCompletedIgnoresEnvironmentsOtherThanDevelopment() {
        authenticate(admin());

        stubAllFourStepsHold(Environment.PRODUCTION);

        assertThat(embeddedGettingStartedFacade.isEmbeddedGettingStartedCompleted()).isFalse();

        assertThat(propertyRepository.findAll()).isEmpty();

        verify(apiKeyService).getApiKeys(Environment.DEVELOPMENT.ordinal(), PlatformType.EMBEDDED);
        verify(apiKeyService, never()).getApiKeys(Environment.PRODUCTION.ordinal(), PlatformType.EMBEDDED);
    }

    @Test
    void testIsCompletedWhenDevelopmentSetupHolds() {
        authenticate(admin());

        stubAllFourStepsHold(Environment.DEVELOPMENT);

        assertThat(embeddedGettingStartedFacade.isEmbeddedGettingStartedCompleted()).isTrue();

        assertThat(propertyRepository.findAll())
            .singleElement()
            .extracting(property -> property.get("completionType"))
            .isEqualTo("COMPLETED");

        verify(externalInvocationLogService, never()).fetchLastFailedExternalInvocationLog(any(), anyInt());
    }

    @Test
    void testIsCompletedFalseWhenDevelopmentSetupIsIncomplete() {
        authenticate(admin());

        assertThat(embeddedGettingStartedFacade.isEmbeddedGettingStartedCompleted()).isFalse();

        assertThat(propertyRepository.findAll()).isEmpty();
    }

    @Test
    void testIsCompletedDoesNotEvaluateWhenRowExists() {
        authenticate(admin());

        embeddedGettingStartedFacade.dismissEmbeddedGettingStarted();

        assertThat(embeddedGettingStartedFacade.isEmbeddedGettingStartedCompleted()).isTrue();

        verifyNoInteractions(apiKeyService, externalInvocationLogService, integrationInstanceService);
    }

    @Test
    void testDismissStoresDismissed() {
        authenticate(admin());

        embeddedGettingStartedFacade.dismissEmbeddedGettingStarted();

        assertThat(propertyRepository.findAll())
            .singleElement()
            .extracting(property -> property.get("completionType"))
            .isEqualTo("DISMISSED");
    }

    private void stubAllFourStepsHold(Environment environment) {
        stubAllFourStepsHoldExcept(environment, null);
    }

    private void stubAllFourStepsHoldExcept(
        Environment environment, CompletionCondition missingCompletionCondition) {

        if (missingCompletionCondition != CompletionCondition.API_KEY) {
            when(apiKeyService.getApiKeys(environment.ordinal(), PlatformType.EMBEDDED))
                .thenReturn(List.of(new ApiKey()));
        }

        long publishedIntegrationId = createPublishedIntegration("httpClient", 1);

        boolean configurationEnabled = missingCompletionCondition != CompletionCondition.ENABLED_CONFIGURATION;

        long configurationId = createConfiguration(publishedIntegrationId, environment, configurationEnabled);

        if (missingCompletionCondition != CompletionCondition.RUNNABLE_INTEGRATION_INSTANCE) {
            stubRunnableIntegrationInstance(environment, configurationId);
        }

        if (missingCompletionCondition != CompletionCondition.SUCCESSFUL_CALL) {
            stubFirstCallMade(environment);
        }
    }

    private void stubFirstCallMade(Environment environment) {
        when(externalInvocationLogService.hasExternalInvocationLog(
            PlatformType.EMBEDDED, environment.ordinal(), ExternalInvocationStatus.SUCCESS)).thenReturn(true);
    }

    private void stubLastEnabledConnectedUser(Environment environment) {
        ConnectedUser lastEnabledConnectedUser = new ConnectedUser(
            Map.of(), null, true, "last-enabled-user", LAST_ENABLED_CONNECTED_USER_ID, null, 0);

        when(connectedUserService.fetchLastEnabledConnectedUser(environment))
            .thenReturn(Optional.of(lastEnabledConnectedUser));
    }

    private void stubRunnableIntegrationInstance(Environment environment, long integrationInstanceConfigurationId) {
        ConnectedUser connectedUser = new ConnectedUser(
            Map.of(), null, true, "external-user", RUNNABLE_CONNECTED_USER_ID, null, 0);
        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setConnectedUserId(RUNNABLE_CONNECTED_USER_ID);
        integrationInstance.setEnabled(true);
        integrationInstance.setIntegrationInstanceConfigurationId(integrationInstanceConfigurationId);

        when(connectedUserService.getConnectedUser(anyLong())).thenReturn(connectedUser);
        when(integrationInstanceService.fetchFirstRunnableIntegrationInstance(environment))
            .thenReturn(Optional.of(integrationInstance));
    }

    private long createConfiguration(long integrationId, Environment environment, boolean enabled) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnvironment(environment);
        integrationInstanceConfiguration.setIntegrationId(integrationId);
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName("instance-configuration-" + UUID.randomUUID());

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        integrationInstanceConfigurationService.updateEnabled(integrationInstanceConfiguration.getId(), enabled);

        return integrationInstanceConfiguration.getId();
    }

    private long createIntegration(String componentName, int componentVersion) {
        Integration integration = new Integration();

        integration.setComponentName(componentName);
        integration.setComponentVersion(componentVersion);
        integration.setName("integration-" + UUID.randomUUID());

        integration = integrationService.create(integration);

        return integration.getId();
    }

    private long createPublishedIntegration(String componentName, int componentVersion) {
        long integrationId = createIntegration(componentName, componentVersion);

        integrationService.publishIntegration(integrationId, null);

        return integrationId;
    }

    private static ExternalInvocationLog failedExternalInvocationLog() {
        ExternalInvocationLog externalInvocationLog = new ExternalInvocationLog();

        externalInvocationLog.setCreatedDate(Instant.parse("2026-11-11T10:00:00Z"));
        externalInvocationLog.setErrorMessage("Connection required");
        externalInvocationLog.setStatus(ExternalInvocationStatus.CONNECTION_REQUIRED);

        return externalInvocationLog;
    }

    private static Authentication admin() {
        return new UsernamePasswordAuthenticationToken(
            "admin", "n/a", List.of(new SimpleGrantedAuthority(AuthorityConstants.ADMIN)));
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
    }

    private static Authentication nonAdmin() {
        return new UsernamePasswordAuthenticationToken(
            "user", "n/a", List.of(new SimpleGrantedAuthority(AuthorityConstants.USER)));
    }

    enum CompletionCondition {
        API_KEY, ENABLED_CONFIGURATION, RUNNABLE_INTEGRATION_INSTANCE, SUCCESSFUL_CALL
    }

    @Configuration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    static class MethodSecurityConfiguration {

        @Bean("permissionService")
        PermissionService permissionService() {
            PermissionService permissionService = mock(PermissionService.class);

            when(permissionService.isTenantAdmin())
                .thenAnswer(invocation -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN));

            return permissionService;
        }
    }
}
