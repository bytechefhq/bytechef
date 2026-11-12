/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscription;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionErrorCode;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionStatus;
import com.bytechef.ee.embedded.configuration.dto.TriggerSubscriptionDTO;
import com.bytechef.ee.embedded.configuration.exception.TriggerSubscriptionEnableException;
import com.bytechef.ee.embedded.configuration.exception.TriggerSubscriptionErrorType;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.TriggerSubscriptionService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.security.ConnectedUserAuthentication;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.exception.ErrorType;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class TriggerSubscriptionFacadeTest {

    private static final String COMPONENT_NAME = "github";
    private static final long CONNECTED_USER_ID = 7L;
    private static final long CONNECTION_ID = 11L;
    private static final String EXTERNAL_USER_ID = "external-user-1";
    private static final long INTEGRATION_INSTANCE_ID = 5L;
    private static final long OTHER_CONNECTED_USER_ID = 8L;
    private static final String PUBLIC_WEBHOOK_URL = "https://93.184.216.34/hooks/github";
    private static final String STATIC_WEBHOOK_URL = "https://bytechef.example.com/webhooks/abc";
    private static final long TRIGGER_SUBSCRIPTION_ID = 3L;
    private static final String TRIGGER_NAME = "newIssue";

    private final ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);
    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final IntegrationInstanceService integrationInstanceService = mock(IntegrationInstanceService.class);
    private final TriggerDefinitionService triggerDefinitionService = mock(TriggerDefinitionService.class);
    private final TriggerSubscriptionManager triggerSubscriptionManager = mock(TriggerSubscriptionManager.class);
    private final TriggerSubscriptionService triggerSubscriptionService = mock(TriggerSubscriptionService.class);

    private ConnectedUser connectedUser;
    private TriggerSubscriptionFacade triggerSubscriptionFacade;

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void beforeEach() {
        triggerSubscriptionFacade = new TriggerSubscriptionFacadeImpl(
            new ApplicationProperties(), componentDefinitionService, connectedUserService, integrationInstanceService,
            triggerDefinitionService, triggerSubscriptionManager, triggerSubscriptionService);

        connectedUser = createConnectedUser(CONNECTED_USER_ID, Environment.PRODUCTION);

        when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(Optional.of(connectedUser));
        when(connectedUserService.getConnectedUser(CONNECTED_USER_ID)).thenReturn(connectedUser);

        mockIntegrationInstanceLookups();
    }

    @Test
    void testCreateResolvesInstanceConnectionAndDelegates() {
        Map<String, Object> input = Map.of(
            "repository", "bytechef", "labels", List.of("bug", "a=b"), "filter", Map.of("query", "$ {not} = x"),
            "separator", "=");

        when(integrationInstanceService.fetchIntegrationInstance(
            CONNECTED_USER_ID, COMPONENT_NAME, Environment.PRODUCTION))
                .thenReturn(Optional.of(createIntegrationInstance(CONNECTED_USER_ID)));

        TriggerSubscription triggerSubscription = createTriggerSubscription(
            CONNECTED_USER_ID, Environment.PRODUCTION);

        when(triggerSubscriptionManager.create(
            connectedUser, INTEGRATION_INSTANCE_ID, CONNECTION_ID, COMPONENT_NAME, 1, TRIGGER_NAME, input,
            "My subscription", "ext-1", PUBLIC_WEBHOOK_URL, 0))
                .thenReturn(triggerSubscription);
        when(triggerSubscriptionManager.getStaticWebhookUrl(triggerSubscription)).thenReturn(STATIC_WEBHOOK_URL);

        TriggerSubscriptionDTO triggerSubscriptionDTO = triggerSubscriptionFacade.createTriggerSubscription(
            EXTERNAL_USER_ID, COMPONENT_NAME, 1, TRIGGER_NAME, input, "My subscription", "ext-1",
            PUBLIC_WEBHOOK_URL, null, Environment.PRODUCTION);

        assertThat(triggerSubscriptionDTO.id()).isEqualTo(TRIGGER_SUBSCRIPTION_ID);
        assertThat(triggerSubscriptionDTO.componentName()).isEqualTo(COMPONENT_NAME);
        assertThat(triggerSubscriptionDTO.status()).isEqualTo(TriggerSubscriptionStatus.ENABLED);
        assertThat(triggerSubscriptionDTO.staticWebhookUrl()).isEqualTo(STATIC_WEBHOOK_URL);
        assertThat(triggerSubscriptionDTO.webhookUrl()).isEqualTo(PUBLIC_WEBHOOK_URL);
    }

    @Test
    void testCreateWithInstanceIdUsesTheOwnedInstance() {
        when(integrationInstanceService.getIntegrationInstance(INTEGRATION_INSTANCE_ID))
            .thenReturn(createIntegrationInstance(CONNECTED_USER_ID));

        TriggerSubscription triggerSubscription = createTriggerSubscription(
            CONNECTED_USER_ID, Environment.PRODUCTION);

        when(triggerSubscriptionManager.create(
            connectedUser, INTEGRATION_INSTANCE_ID, CONNECTION_ID, COMPONENT_NAME, 1, TRIGGER_NAME, Map.of(), null,
            null, null, 0))
                .thenReturn(triggerSubscription);

        TriggerSubscriptionDTO triggerSubscriptionDTO = triggerSubscriptionFacade.createTriggerSubscription(
            EXTERNAL_USER_ID, COMPONENT_NAME, 1, TRIGGER_NAME, Map.of(), null, null, null, INTEGRATION_INSTANCE_ID,
            Environment.PRODUCTION);

        assertThat(triggerSubscriptionDTO.id()).isEqualTo(TRIGGER_SUBSCRIPTION_ID);
    }

    @Test
    void testCreateWithoutRequiredConnectionFails() {
        when(integrationInstanceService.fetchIntegrationInstance(
            CONNECTED_USER_ID, COMPONENT_NAME, Environment.PRODUCTION))
                .thenReturn(Optional.empty());

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.isConnectionRequired()).thenReturn(true);
        when(componentDefinitionService.getComponentDefinition(COMPONENT_NAME, 1)).thenReturn(componentDefinition);

        assertThatThrownBy(() -> triggerSubscriptionFacade.createTriggerSubscription(
            EXTERNAL_USER_ID, COMPONENT_NAME, 1, TRIGGER_NAME, Map.of(), null, null, null, null,
            Environment.PRODUCTION))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> assertErrorType(exception, TriggerSubscriptionErrorType.USER_INPUT_ERROR));

        verifyNoInteractions(triggerSubscriptionManager);
    }

    @Test
    void testCreateWithoutConnectionForComponentWithoutConnectionDelegates() {
        when(integrationInstanceService.fetchIntegrationInstance(CONNECTED_USER_ID, "webhook", Environment.PRODUCTION))
            .thenReturn(Optional.empty());

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.isConnectionRequired()).thenReturn(false);
        when(componentDefinitionService.getComponentDefinition("webhook", 1)).thenReturn(componentDefinition);

        TriggerSubscription triggerSubscription = createTriggerSubscription(
            CONNECTED_USER_ID, Environment.PRODUCTION);

        when(triggerSubscriptionManager.create(
            connectedUser, null, null, "webhook", 1, "autoRespondWithHTTP200", Map.of(), null, null, null, 0))
                .thenReturn(triggerSubscription);

        TriggerSubscriptionDTO triggerSubscriptionDTO = triggerSubscriptionFacade.createTriggerSubscription(
            EXTERNAL_USER_ID, "webhook", 1, "autoRespondWithHTTP200", Map.of(), null, null, null, null,
            Environment.PRODUCTION);

        assertThat(triggerSubscriptionDTO.id()).isEqualTo(TRIGGER_SUBSCRIPTION_ID);
    }

    @Test
    void testCreateWithForeignInstanceIdIsDenied() {
        when(integrationInstanceService.getIntegrationInstance(INTEGRATION_INSTANCE_ID))
            .thenReturn(createIntegrationInstance(OTHER_CONNECTED_USER_ID));

        assertThatThrownBy(() -> triggerSubscriptionFacade.createTriggerSubscription(
            EXTERNAL_USER_ID, COMPONENT_NAME, 1, TRIGGER_NAME, Map.of(), null, null, null, INTEGRATION_INSTANCE_ID,
            Environment.PRODUCTION))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(triggerSubscriptionManager);
    }

    @Test
    void testCreateWithAnInstanceIdOfAnotherComponentIsRejected() {
        when(integrationInstanceService.getIntegrationInstance(INTEGRATION_INSTANCE_ID))
            .thenReturn(createIntegrationInstance(CONNECTED_USER_ID));

        when(integrationInstanceService.getIntegrationInstances(
            CONNECTED_USER_ID, COMPONENT_NAME, Environment.PRODUCTION))
                .thenReturn(List.of());

        assertThatThrownBy(() -> triggerSubscriptionFacade.createTriggerSubscription(
            EXTERNAL_USER_ID, COMPONENT_NAME, 1, TRIGGER_NAME, Map.of(), null, null, null, INTEGRATION_INSTANCE_ID,
            Environment.PRODUCTION))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> {
                        assertErrorType(exception, TriggerSubscriptionErrorType.USER_INPUT_ERROR);
                        assertThat(exception.getMessage()).contains(COMPONENT_NAME);
                    });

        verifyNoInteractions(triggerSubscriptionManager);
    }

    @Test
    void testCreateWithAnInstanceIdOfAnotherEnvironmentIsRejected() {
        when(integrationInstanceService.getIntegrationInstance(INTEGRATION_INSTANCE_ID))
            .thenReturn(createIntegrationInstance(CONNECTED_USER_ID));

        when(integrationInstanceService.getConnectedUserIntegrationInstances(
            CONNECTED_USER_ID, Environment.PRODUCTION))
                .thenReturn(List.of());

        assertThatThrownBy(() -> triggerSubscriptionFacade.createTriggerSubscription(
            EXTERNAL_USER_ID, COMPONENT_NAME, 1, TRIGGER_NAME, Map.of(), null, null, null, INTEGRATION_INSTANCE_ID,
            Environment.PRODUCTION))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> {
                        assertErrorType(exception, TriggerSubscriptionErrorType.USER_INPUT_ERROR);
                        assertThat(exception.getMessage()).contains(Environment.PRODUCTION.name());
                    });

        verifyNoInteractions(triggerSubscriptionManager);
    }

    @Test
    void testCreateRejectsTemplateExpressionInput() {
        assertRejectedInput(Map.of("repository", "${connection.parameters.token}"));
    }

    @Test
    void testCreateRejectsTemplateExpressionInsideText() {
        assertRejectedInput(Map.of("repository", "prefix ${trigger_1.output} suffix"));
    }

    @Test
    void testCreateRejectsFormulaInput() {
        assertRejectedInput(Map.of("repository", "=config('spring.datasource.password')"));
    }

    @Test
    void testCreateRejectsFormulaInputWithLeadingWhitespace() {
        assertRejectedInput(Map.of("repository", "  =1+1"));
    }

    @Test
    void testCreateRejectsExpressionNestedInListsAndMaps() {
        Map<String, Object> nestedMap = new HashMap<>();

        nestedMap.put("empty", null);
        nestedMap.put("items", List.of("literal", Map.of("deep", List.of("=systemProperty('user.home')"))));

        assertRejectedInput(Map.of("filter", nestedMap));
    }

    @Test
    void testCreateRejectsWebhookUrlWithUnsupportedScheme() {
        assertRejectedWebhookUrl("ftp://93.184.216.34/hooks");
    }

    @Test
    void testCreateRejectsRelativeWebhookUrl() {
        assertRejectedWebhookUrl("/hooks/github");
    }

    @Test
    void testCreateRejectsLoopbackWebhookUrl() {
        assertRejectedWebhookUrl("http://127.0.0.1:8080/actuator");
    }

    @Test
    void testCreateRejectsPrivateNetworkWebhookUrl() {
        assertRejectedWebhookUrl("http://10.0.0.5/hooks");
    }

    @Test
    void testCreateRejectsLinkLocalWebhookUrl() {
        assertRejectedWebhookUrl("http://169.254.169.254/latest/meta-data");
    }

    @Test
    void testCreateAcceptsAllowlistedPrivateWebhookUrl() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.WebhookDelivery webhookDelivery = applicationProperties.getWebhookDelivery();

        webhookDelivery.setAllowedHosts(List.of("localhost"));

        triggerSubscriptionFacade = new TriggerSubscriptionFacadeImpl(
            applicationProperties, componentDefinitionService, connectedUserService, integrationInstanceService,
            triggerDefinitionService, triggerSubscriptionManager, triggerSubscriptionService);

        when(integrationInstanceService.fetchIntegrationInstance(
            CONNECTED_USER_ID, COMPONENT_NAME, Environment.PRODUCTION))
                .thenReturn(Optional.of(createIntegrationInstance(CONNECTED_USER_ID)));

        TriggerSubscription triggerSubscription = createTriggerSubscription(
            CONNECTED_USER_ID, Environment.PRODUCTION);

        when(triggerSubscriptionManager.create(
            connectedUser, INTEGRATION_INSTANCE_ID, CONNECTION_ID, COMPONENT_NAME, 1, TRIGGER_NAME, Map.of(), null,
            null, "http://localhost:9000/hooks", 0))
                .thenReturn(triggerSubscription);

        TriggerSubscriptionDTO triggerSubscriptionDTO = triggerSubscriptionFacade.createTriggerSubscription(
            EXTERNAL_USER_ID, COMPONENT_NAME, 1, TRIGGER_NAME, Map.of(), null, null, "http://localhost:9000/hooks",
            null, Environment.PRODUCTION);

        assertThat(triggerSubscriptionDTO.id()).isEqualTo(TRIGGER_SUBSCRIPTION_ID);
    }

    @Test
    void testCreateBeyondTheSubscriptionLimitOfTheConnectedUserIsRejected() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Embedded embedded = applicationProperties.getEmbedded();

        ApplicationProperties.Embedded.TriggerSubscription triggerSubscriptionProperties =
            embedded.getTriggerSubscription();

        triggerSubscriptionProperties.setMaxPerConnectedUser(2);

        triggerSubscriptionFacade = new TriggerSubscriptionFacadeImpl(
            applicationProperties, componentDefinitionService, connectedUserService, integrationInstanceService,
            triggerDefinitionService, triggerSubscriptionManager, triggerSubscriptionService);

        when(triggerSubscriptionService.getConnectedUserTriggerSubscriptions(CONNECTED_USER_ID, Environment.PRODUCTION))
            .thenReturn(
                List.of(
                    createTriggerSubscription(CONNECTED_USER_ID, Environment.PRODUCTION),
                    createTriggerSubscription(CONNECTED_USER_ID, Environment.PRODUCTION)));

        assertThatThrownBy(() -> triggerSubscriptionFacade.createTriggerSubscription(
            EXTERNAL_USER_ID, COMPONENT_NAME, 1, TRIGGER_NAME, Map.of(), null, null, null, null,
            Environment.PRODUCTION))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> assertErrorType(exception, TriggerSubscriptionErrorType.USER_INPUT_ERROR));

        verify(triggerSubscriptionManager, never()).create(
            any(), any(), any(), any(), anyInt(), any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void testCreateWithAWebhookUrlFromAConnectedUserTokenIsRejected() {
        authenticateConnectedUser(false);

        assertThatThrownBy(() -> triggerSubscriptionFacade.createTriggerSubscription(
            EXTERNAL_USER_ID, COMPONENT_NAME, 1, TRIGGER_NAME, Map.of(), null, null, PUBLIC_WEBHOOK_URL, null,
            Environment.PRODUCTION))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> assertErrorType(exception, TriggerSubscriptionErrorType.USER_INPUT_ERROR));

        verify(triggerSubscriptionManager, never()).create(
            any(), any(), any(), any(), anyInt(), any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void testCreateWithAWebhookUrlFromASecretKeyIsAllowed() {
        authenticateConnectedUser(true);

        when(integrationInstanceService.fetchIntegrationInstance(
            CONNECTED_USER_ID, COMPONENT_NAME, Environment.PRODUCTION))
                .thenReturn(Optional.of(createIntegrationInstance(CONNECTED_USER_ID)));

        TriggerSubscription triggerSubscription = createTriggerSubscription(
            CONNECTED_USER_ID, Environment.PRODUCTION);

        when(triggerSubscriptionManager.create(
            connectedUser, INTEGRATION_INSTANCE_ID, CONNECTION_ID, COMPONENT_NAME, 1, TRIGGER_NAME, Map.of(), null,
            null, PUBLIC_WEBHOOK_URL, 0))
                .thenReturn(triggerSubscription);

        TriggerSubscriptionDTO triggerSubscriptionDTO = triggerSubscriptionFacade.createTriggerSubscription(
            EXTERNAL_USER_ID, COMPONENT_NAME, 1, TRIGGER_NAME, Map.of(), null, null, PUBLIC_WEBHOOK_URL, null,
            Environment.PRODUCTION);

        assertThat(triggerSubscriptionDTO.webhookUrl()).isEqualTo(PUBLIC_WEBHOOK_URL);
    }

    @Test
    void testUpdateChangingTheWebhookUrlFromAConnectedUserTokenIsRejected() {
        mockOwnedTriggerSubscription();
        authenticateConnectedUser(false);

        assertThatThrownBy(() -> triggerSubscriptionFacade.updateTriggerSubscription(
            EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, Map.of(), null, null, null, Environment.PRODUCTION))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> assertErrorType(exception, TriggerSubscriptionErrorType.USER_INPUT_ERROR));

        verify(triggerSubscriptionManager, never()).update(anyLong(), any(), any(), any(), any());
    }

    @Test
    void testUpdateKeepingTheWebhookUrlFromAConnectedUserTokenIsAllowed() {
        mockOwnedTriggerSubscription();
        authenticateConnectedUser(false);

        TriggerSubscription triggerSubscription = createTriggerSubscription(
            CONNECTED_USER_ID, Environment.PRODUCTION);

        when(triggerSubscriptionManager.update(TRIGGER_SUBSCRIPTION_ID, Map.of(), null, null, PUBLIC_WEBHOOK_URL))
            .thenReturn(triggerSubscription);

        triggerSubscriptionFacade.updateTriggerSubscription(
            EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, Map.of(), null, null, PUBLIC_WEBHOOK_URL,
            Environment.PRODUCTION);

        verify(triggerSubscriptionManager).update(TRIGGER_SUBSCRIPTION_ID, Map.of(), null, null, PUBLIC_WEBHOOK_URL);
    }

    @Test
    void testGetSubscriptionOfOtherEnvironmentIsNotFound() {
        ConnectedUser developmentConnectedUser = createConnectedUser(OTHER_CONNECTED_USER_ID, Environment.DEVELOPMENT);

        when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_ID, Environment.DEVELOPMENT))
            .thenReturn(Optional.of(developmentConnectedUser));
        when(triggerSubscriptionService.fetchTriggerSubscription(TRIGGER_SUBSCRIPTION_ID))
            .thenReturn(Optional.of(createTriggerSubscription(CONNECTED_USER_ID, Environment.PRODUCTION)));

        assertNotFound(
            () -> triggerSubscriptionFacade.getTriggerSubscription(
                EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, Environment.DEVELOPMENT));
    }

    @Test
    void testGetSubscriptionStoredForAnotherEnvironmentIsNotFound() {
        when(triggerSubscriptionService.fetchTriggerSubscription(TRIGGER_SUBSCRIPTION_ID))
            .thenReturn(Optional.of(createTriggerSubscription(CONNECTED_USER_ID, Environment.STAGING)));

        assertNotFound(
            () -> triggerSubscriptionFacade.getTriggerSubscription(
                EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, Environment.PRODUCTION));
    }

    @Test
    void testGetSubscriptionOfOtherUserIsNotFound() {
        when(triggerSubscriptionService.fetchTriggerSubscription(TRIGGER_SUBSCRIPTION_ID))
            .thenReturn(Optional.of(createTriggerSubscription(OTHER_CONNECTED_USER_ID, Environment.PRODUCTION)));

        assertNotFound(
            () -> triggerSubscriptionFacade.getTriggerSubscription(
                EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, Environment.PRODUCTION));
    }

    @Test
    void testGetMissingSubscriptionIsNotFound() {
        when(triggerSubscriptionService.fetchTriggerSubscription(TRIGGER_SUBSCRIPTION_ID))
            .thenReturn(Optional.empty());

        assertNotFound(
            () -> triggerSubscriptionFacade.getTriggerSubscription(
                EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, Environment.PRODUCTION));
    }

    @Test
    void testGetSubscriptionOfUnknownConnectedUserIsNotFound() {
        when(connectedUserService.fetchConnectedUser("unknown", Environment.PRODUCTION)).thenReturn(Optional.empty());

        assertNotFound(
            () -> triggerSubscriptionFacade.getTriggerSubscription(
                "unknown", TRIGGER_SUBSCRIPTION_ID, Environment.PRODUCTION));

        verify(triggerSubscriptionService, never()).fetchTriggerSubscription(anyLong());
    }

    @Test
    void testGetSubscriptionReturnsTheOwnedSubscription() {
        TriggerSubscription triggerSubscription = createTriggerSubscription(
            CONNECTED_USER_ID, Environment.PRODUCTION);

        when(triggerSubscriptionService.fetchTriggerSubscription(TRIGGER_SUBSCRIPTION_ID))
            .thenReturn(Optional.of(triggerSubscription));
        when(triggerSubscriptionManager.getStaticWebhookUrl(triggerSubscription)).thenReturn(STATIC_WEBHOOK_URL);

        TriggerSubscriptionDTO triggerSubscriptionDTO = triggerSubscriptionFacade.getTriggerSubscription(
            EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, Environment.PRODUCTION);

        assertThat(triggerSubscriptionDTO.id()).isEqualTo(TRIGGER_SUBSCRIPTION_ID);
        assertThat(triggerSubscriptionDTO.staticWebhookUrl()).isEqualTo(STATIC_WEBHOOK_URL);
    }

    @Test
    void testGetSubscriptionsReturnsTheConnectedUserSubscriptionsOfTheEnvironment() {
        when(triggerSubscriptionService.getConnectedUserTriggerSubscriptions(CONNECTED_USER_ID, Environment.PRODUCTION))
            .thenReturn(List.of(createTriggerSubscription(CONNECTED_USER_ID, Environment.PRODUCTION)));

        List<TriggerSubscriptionDTO> triggerSubscriptionDTOs = triggerSubscriptionFacade.getTriggerSubscriptions(
            EXTERNAL_USER_ID, Environment.PRODUCTION);

        assertThat(triggerSubscriptionDTOs)
            .extracting(TriggerSubscriptionDTO::id)
            .containsExactly(TRIGGER_SUBSCRIPTION_ID);
    }

    @Test
    void testGetSubscriptionsOfUnknownConnectedUserIsEmpty() {
        when(connectedUserService.fetchConnectedUser("unknown", Environment.PRODUCTION)).thenReturn(Optional.empty());

        assertThat(triggerSubscriptionFacade.getTriggerSubscriptions("unknown", Environment.PRODUCTION)).isEmpty();
    }

    @Test
    void testUpdateDelegatesForTheOwnedSubscription() {
        TriggerSubscription triggerSubscription = createTriggerSubscription(
            CONNECTED_USER_ID, Environment.PRODUCTION);

        when(triggerSubscriptionService.fetchTriggerSubscription(TRIGGER_SUBSCRIPTION_ID))
            .thenReturn(Optional.of(triggerSubscription));

        mockBoundIntegrationInstance(true);

        when(triggerSubscriptionManager.update(
            TRIGGER_SUBSCRIPTION_ID, Map.of("repository", "other"), "renamed", "ext-2", PUBLIC_WEBHOOK_URL))
                .thenReturn(triggerSubscription);

        TriggerSubscriptionDTO triggerSubscriptionDTO = triggerSubscriptionFacade.updateTriggerSubscription(
            EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, Map.of("repository", "other"), "renamed", "ext-2",
            PUBLIC_WEBHOOK_URL, Environment.PRODUCTION);

        assertThat(triggerSubscriptionDTO.id()).isEqualTo(TRIGGER_SUBSCRIPTION_ID);
    }

    @Test
    void testUpdateRejectsExpressionInput() {
        mockOwnedTriggerSubscription();

        assertThatThrownBy(() -> triggerSubscriptionFacade.updateTriggerSubscription(
            EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, Map.of("repository", "${trigger_1}"), null, null, null,
            Environment.PRODUCTION))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> assertErrorType(exception, TriggerSubscriptionErrorType.USER_INPUT_ERROR));

        verify(triggerSubscriptionManager, never()).update(anyLong(), any(), any(), any(), any());
    }

    @Test
    void testUpdateRejectsLoopbackWebhookUrl() {
        mockOwnedTriggerSubscription();

        assertThatThrownBy(() -> triggerSubscriptionFacade.updateTriggerSubscription(
            EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, Map.of(), null, null, "http://localhost:8080/hooks",
            Environment.PRODUCTION))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> assertErrorType(exception, TriggerSubscriptionErrorType.USER_INPUT_ERROR));

        verify(triggerSubscriptionManager, never()).update(anyLong(), any(), any(), any(), any());
    }

    @Test
    void testUpdateOfOtherUserSubscriptionIsNotFound() {
        when(triggerSubscriptionService.fetchTriggerSubscription(TRIGGER_SUBSCRIPTION_ID))
            .thenReturn(Optional.of(createTriggerSubscription(OTHER_CONNECTED_USER_ID, Environment.PRODUCTION)));

        assertNotFound(
            () -> triggerSubscriptionFacade.updateTriggerSubscription(
                EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, Map.of(), null, null, null, Environment.PRODUCTION));

        verify(triggerSubscriptionManager, never()).update(anyLong(), any(), any(), any(), any());
    }

    @Test
    void testEnableAndDisableDelegateForTheOwnedSubscription() {
        mockOwnedTriggerSubscription();

        triggerSubscriptionFacade.enableTriggerSubscription(
            EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, true, Environment.PRODUCTION);

        verify(triggerSubscriptionManager).enable(TRIGGER_SUBSCRIPTION_ID);

        triggerSubscriptionFacade.enableTriggerSubscription(
            EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, false, Environment.PRODUCTION);

        verify(triggerSubscriptionManager).disable(TRIGGER_SUBSCRIPTION_ID, null);
    }

    @Test
    void testDeleteDelegatesForTheOwnedSubscription() {
        mockOwnedTriggerSubscription();

        triggerSubscriptionFacade.deleteTriggerSubscription(
            EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, Environment.PRODUCTION);

        verify(triggerSubscriptionManager).delete(TRIGGER_SUBSCRIPTION_ID);
    }

    @Test
    void testDeleteOfOtherUserSubscriptionIsNotFound() {
        when(triggerSubscriptionService.fetchTriggerSubscription(TRIGGER_SUBSCRIPTION_ID))
            .thenReturn(Optional.of(createTriggerSubscription(OTHER_CONNECTED_USER_ID, Environment.PRODUCTION)));

        assertNotFound(
            () -> triggerSubscriptionFacade.deleteTriggerSubscription(
                EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, Environment.PRODUCTION));

        verify(triggerSubscriptionManager, never()).delete(anyLong());
    }

    @Test
    void testCreateOfUnknownConnectedUserIsRejected() {
        when(connectedUserService.fetchConnectedUser("unknown", Environment.PRODUCTION)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> triggerSubscriptionFacade.createTriggerSubscription(
            "unknown", COMPONENT_NAME, 1, TRIGGER_NAME, Map.of(), null, null, null, null, Environment.PRODUCTION))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> assertErrorType(exception, TriggerSubscriptionErrorType.USER_INPUT_ERROR));

        verifyNoInteractions(triggerSubscriptionManager);
    }

    @Test
    void testCreateWithDisabledIntegrationInstanceIsRejected() {
        IntegrationInstance integrationInstance = createIntegrationInstance(CONNECTED_USER_ID);

        integrationInstance.setEnabled(false);

        when(integrationInstanceService.fetchIntegrationInstance(
            CONNECTED_USER_ID, COMPONENT_NAME, Environment.PRODUCTION))
                .thenReturn(Optional.of(integrationInstance));

        assertThatThrownBy(() -> triggerSubscriptionFacade.createTriggerSubscription(
            EXTERNAL_USER_ID, COMPONENT_NAME, 1, TRIGGER_NAME, Map.of(), null, null, null, null,
            Environment.PRODUCTION))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> assertErrorType(exception, TriggerSubscriptionErrorType.USER_INPUT_ERROR));

        verifyNoInteractions(triggerSubscriptionManager);
    }

    @Test
    void testCreateWithDisabledIntegrationInstanceIdIsRejected() {
        IntegrationInstance integrationInstance = createIntegrationInstance(CONNECTED_USER_ID);

        integrationInstance.setEnabled(false);

        when(integrationInstanceService.getIntegrationInstance(INTEGRATION_INSTANCE_ID))
            .thenReturn(integrationInstance);

        assertThatThrownBy(() -> triggerSubscriptionFacade.createTriggerSubscription(
            EXTERNAL_USER_ID, COMPONENT_NAME, 1, TRIGGER_NAME, Map.of(), null, null, null, INTEGRATION_INSTANCE_ID,
            Environment.PRODUCTION))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> assertErrorType(exception, TriggerSubscriptionErrorType.USER_INPUT_ERROR));

        verifyNoInteractions(triggerSubscriptionManager);
    }

    @Test
    void testCreateEnableFailureRemovesTheSubscription() {
        when(integrationInstanceService.fetchIntegrationInstance(
            CONNECTED_USER_ID, COMPONENT_NAME, Environment.PRODUCTION))
                .thenReturn(Optional.of(createIntegrationInstance(CONNECTED_USER_ID)));

        IllegalStateException enableFailure = new IllegalStateException("Webhook registration rejected");

        when(triggerSubscriptionManager.create(
            connectedUser, INTEGRATION_INSTANCE_ID, CONNECTION_ID, COMPONENT_NAME, 1, TRIGGER_NAME, Map.of(), null,
            null, null, 0))
                .thenThrow(new TriggerSubscriptionEnableException(
                    TRIGGER_SUBSCRIPTION_ID, TriggerSubscriptionErrorCode.API_ERROR, "Webhook registration rejected",
                    enableFailure));

        assertThatThrownBy(() -> triggerSubscriptionFacade.createTriggerSubscription(
            EXTERNAL_USER_ID, COMPONENT_NAME, 1, TRIGGER_NAME, Map.of(), null, null, null, null,
            Environment.PRODUCTION))
                .isInstanceOfSatisfying(
                    TriggerSubscriptionEnableException.class,
                    exception -> {
                        assertThat(exception.getCause()).isSameAs(enableFailure);
                        assertThat(exception.getErrorCode()).isEqualTo(TriggerSubscriptionErrorCode.API_ERROR);
                        assertThat(exception.getMessage()).isEqualTo("Webhook registration rejected");
                        assertThat(exception.getTriggerSubscriptionId()).isNull();
                        assertThat(exception.getSuppressed()).isEmpty();
                    });

        verify(triggerSubscriptionManager).delete(TRIGGER_SUBSCRIPTION_ID);
    }

    @Test
    void testCreateEnableFailureKeepsTheFailureAndTheIdWhenTheCleanupFails() {
        when(integrationInstanceService.fetchIntegrationInstance(
            CONNECTED_USER_ID, COMPONENT_NAME, Environment.PRODUCTION))
                .thenReturn(Optional.of(createIntegrationInstance(CONNECTED_USER_ID)));
        when(triggerSubscriptionManager.create(
            connectedUser, INTEGRATION_INSTANCE_ID, CONNECTION_ID, COMPONENT_NAME, 1, TRIGGER_NAME, Map.of(), null,
            null, null, 0))
                .thenThrow(new TriggerSubscriptionEnableException(
                    TRIGGER_SUBSCRIPTION_ID, TriggerSubscriptionErrorCode.TIMEOUT_ERROR, "Read timed out",
                    new IllegalStateException("Read timed out")));

        IllegalStateException cleanupFailure = new IllegalStateException("Project deletion failed");

        doThrow(cleanupFailure).when(triggerSubscriptionManager)
            .delete(TRIGGER_SUBSCRIPTION_ID);

        assertThatThrownBy(() -> triggerSubscriptionFacade.createTriggerSubscription(
            EXTERNAL_USER_ID, COMPONENT_NAME, 1, TRIGGER_NAME, Map.of(), null, null, null, null,
            Environment.PRODUCTION))
                .isInstanceOfSatisfying(
                    TriggerSubscriptionEnableException.class,
                    exception -> {
                        assertThat(exception.getErrorCode()).isEqualTo(TriggerSubscriptionErrorCode.TIMEOUT_ERROR);
                        assertThat(exception.getTriggerSubscriptionId()).isEqualTo(TRIGGER_SUBSCRIPTION_ID);
                        assertThat(exception.getSuppressed()).containsExactly(cleanupFailure);
                    });
    }

    @Test
    void testEnableFailureKeepsTheSubscriptionAndCarriesItsId() {
        mockOwnedTriggerSubscription();

        TriggerSubscriptionEnableException enableException = new TriggerSubscriptionEnableException(
            TRIGGER_SUBSCRIPTION_ID, TriggerSubscriptionErrorCode.OAUTH_REFRESH, "Token expired",
            new IllegalStateException("Token expired"));

        doThrow(enableException).when(triggerSubscriptionManager)
            .enable(TRIGGER_SUBSCRIPTION_ID);

        assertThatThrownBy(() -> triggerSubscriptionFacade.enableTriggerSubscription(
            EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, true, Environment.PRODUCTION))
                .isSameAs(enableException);

        verify(triggerSubscriptionManager, never()).delete(anyLong());
    }

    @Test
    void testUpdateFailureKeepsTheSubscriptionAndCarriesItsId() {
        mockOwnedTriggerSubscription();

        TriggerSubscriptionEnableException enableException = new TriggerSubscriptionEnableException(
            TRIGGER_SUBSCRIPTION_ID, TriggerSubscriptionErrorCode.API_ERROR, "Rejected",
            new IllegalStateException("Rejected"));

        when(triggerSubscriptionManager.update(TRIGGER_SUBSCRIPTION_ID, Map.of(), null, null, null))
            .thenThrow(enableException);

        assertThatThrownBy(() -> triggerSubscriptionFacade.updateTriggerSubscription(
            EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, Map.of(), null, null, null, Environment.PRODUCTION))
                .isSameAs(enableException);

        verify(triggerSubscriptionManager, never()).delete(anyLong());
    }

    @Test
    void testEnableWithDisabledIntegrationInstanceIsRejected() {
        mockOwnedTriggerSubscription();
        mockBoundIntegrationInstance(false);

        assertThatThrownBy(() -> triggerSubscriptionFacade.enableTriggerSubscription(
            EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, true, Environment.PRODUCTION))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> assertErrorType(exception, TriggerSubscriptionErrorType.USER_INPUT_ERROR));

        verify(triggerSubscriptionManager, never()).enable(anyLong());
    }

    @Test
    void testEnableOfADisabledConnectedUserIsRejected() {
        mockOwnedTriggerSubscription();

        connectedUser.setEnabled(false);

        assertThatThrownBy(() -> triggerSubscriptionFacade.enableTriggerSubscription(
            EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, true, Environment.PRODUCTION))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> assertErrorType(exception, TriggerSubscriptionErrorType.USER_INPUT_ERROR));

        verify(triggerSubscriptionManager, never()).enable(anyLong());
    }

    @Test
    void testEnableWithDeletedIntegrationInstanceIsRejected() {
        mockOwnedTriggerSubscription();

        when(integrationInstanceService.fetchIntegrationInstance(INTEGRATION_INSTANCE_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> triggerSubscriptionFacade.enableTriggerSubscription(
            EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, true, Environment.PRODUCTION))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> assertErrorType(exception, TriggerSubscriptionErrorType.USER_INPUT_ERROR));

        verify(triggerSubscriptionManager, never()).enable(anyLong());
    }

    @Test
    void testUpdateWithDisabledIntegrationInstanceIsRejected() {
        mockOwnedTriggerSubscription();
        mockBoundIntegrationInstance(false);

        assertThatThrownBy(() -> triggerSubscriptionFacade.updateTriggerSubscription(
            EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, Map.of(), null, null, null, Environment.PRODUCTION))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> assertErrorType(exception, TriggerSubscriptionErrorType.USER_INPUT_ERROR));

        verify(triggerSubscriptionManager, never()).update(anyLong(), any(), any(), any(), any());
    }

    @Test
    void testDisableAndDeleteWithDisabledIntegrationInstanceDelegate() {
        mockOwnedTriggerSubscription();
        mockBoundIntegrationInstance(false);

        triggerSubscriptionFacade.enableTriggerSubscription(
            EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, false, Environment.PRODUCTION);
        triggerSubscriptionFacade.deleteTriggerSubscription(
            EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, Environment.PRODUCTION);

        verify(triggerSubscriptionManager).disable(TRIGGER_SUBSCRIPTION_ID, null);
        verify(triggerSubscriptionManager).delete(TRIGGER_SUBSCRIPTION_ID);
    }

    @Test
    void testGetComponentTriggersOfAnotherConnectedUserIsDenied() {
        TriggerSubscriptionFacade securedTriggerSubscriptionFacade = createSecuredTriggerSubscriptionFacade();

        SecurityContextHolder.getContext()
            .setAuthentication(new TestingAuthenticationToken("other-external-user", null));

        assertThatThrownBy(
            () -> securedTriggerSubscriptionFacade.getComponentTriggers(EXTERNAL_USER_ID, COMPONENT_NAME, 1))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(triggerDefinitionService);
    }

    @Test
    void testGetComponentTriggersOfTheAuthenticatedConnectedUserIsAllowed() {
        TriggerSubscriptionFacade securedTriggerSubscriptionFacade = createSecuredTriggerSubscriptionFacade();

        SecurityContextHolder.getContext()
            .setAuthentication(new TestingAuthenticationToken(EXTERNAL_USER_ID, null));

        when(triggerDefinitionService.getTriggerDefinitions(COMPONENT_NAME, 1)).thenReturn(List.of());

        assertThat(securedTriggerSubscriptionFacade.getComponentTriggers(EXTERNAL_USER_ID, COMPONENT_NAME, 1))
            .isEmpty();
    }

    @Test
    void testEveryConnectedUserMethodRequiresTheConnectedUserPrincipal() throws NoSuchMethodException {
        List<String> ungatedMethodNames = new ArrayList<>();

        for (Method facadeMethod : TriggerSubscriptionFacade.class.getMethods()) {
            Method implementationMethod = TriggerSubscriptionFacadeImpl.class.getDeclaredMethod(
                facadeMethod.getName(), facadeMethod.getParameterTypes());

            PreAuthorize preAuthorize = implementationMethod.getAnnotation(PreAuthorize.class);

            if (preAuthorize == null || !"#externalUserId == authentication.name".equals(preAuthorize.value())) {
                ungatedMethodNames.add(implementationMethod.toGenericString());
            }
        }

        assertThat(ungatedMethodNames).isEmpty();
    }

    private TriggerSubscriptionFacade createSecuredTriggerSubscriptionFacade() {
        ProxyFactory proxyFactory = new ProxyFactory(triggerSubscriptionFacade);

        proxyFactory.addAdvice(AuthorizationManagerBeforeMethodInterceptor.preAuthorize());

        return (TriggerSubscriptionFacade) proxyFactory.getProxy();
    }

    private static void assertErrorType(ConfigurationException exception, ErrorType errorType) {
        assertThat(exception.getEntityClass()).isEqualTo(errorType.getErrorClass());
        assertThat(exception.getErrorKey()).isEqualTo(errorType.getErrorKey());
    }

    private static void assertNotFound(Runnable runnable) {
        assertThatThrownBy(runnable::run)
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertErrorType(exception, TriggerSubscriptionErrorType.TRIGGER_SUBSCRIPTION_NOT_FOUND));
    }

    private void assertRejectedInput(Map<String, ?> input) {
        assertThatThrownBy(() -> triggerSubscriptionFacade.createTriggerSubscription(
            EXTERNAL_USER_ID, COMPONENT_NAME, 1, TRIGGER_NAME, input, null, null, null, null, Environment.PRODUCTION))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> assertErrorType(exception, TriggerSubscriptionErrorType.USER_INPUT_ERROR));

        verifyNoInteractions(triggerSubscriptionManager);
    }

    private void assertRejectedWebhookUrl(String webhookUrl) {
        assertThatThrownBy(() -> triggerSubscriptionFacade.createTriggerSubscription(
            EXTERNAL_USER_ID, COMPONENT_NAME, 1, TRIGGER_NAME, Map.of(), null, null, webhookUrl, null,
            Environment.PRODUCTION))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> assertErrorType(exception, TriggerSubscriptionErrorType.USER_INPUT_ERROR));

        verifyNoInteractions(triggerSubscriptionManager);
    }

    private static ConnectedUser createConnectedUser(long id, Environment environment) {
        ConnectedUser connectedUser = new ConnectedUser();

        connectedUser.setEnabled(true);
        connectedUser.setEnvironment(environment);
        connectedUser.setExternalId(EXTERNAL_USER_ID);

        ReflectionTestUtils.setField(connectedUser, "id", id);

        return connectedUser;
    }

    private static IntegrationInstance createIntegrationInstance(long connectedUserId) {
        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setConnectedUserId(connectedUserId);
        integrationInstance.setConnectionId(CONNECTION_ID);
        integrationInstance.setEnabled(true);
        integrationInstance.setId(INTEGRATION_INSTANCE_ID);

        return integrationInstance;
    }

    private static TriggerSubscription createTriggerSubscription(long connectedUserId, Environment environment) {
        TriggerSubscription triggerSubscription = new TriggerSubscription();

        triggerSubscription.setComponentName(COMPONENT_NAME);
        triggerSubscription.setComponentVersion(1);
        triggerSubscription.setConnectedUserId(connectedUserId);
        triggerSubscription.setEnvironment(environment);
        triggerSubscription.setIntegrationInstanceId(INTEGRATION_INSTANCE_ID);
        triggerSubscription.setParameters(Map.of("repository", "bytechef"));
        triggerSubscription.setStatus(TriggerSubscriptionStatus.ENABLED);
        triggerSubscription.setTriggerName(TRIGGER_NAME);
        triggerSubscription.setWebhookUrl(PUBLIC_WEBHOOK_URL);

        ReflectionTestUtils.setField(triggerSubscription, "createdDate", Instant.parse("2026-11-11T10:00:00Z"));
        ReflectionTestUtils.setField(triggerSubscription, "id", TRIGGER_SUBSCRIPTION_ID);
        ReflectionTestUtils.setField(
            triggerSubscription, "lastModifiedDate", Instant.parse("2026-11-11T11:00:00Z"));

        return triggerSubscription;
    }

    private void mockIntegrationInstanceLookups() {
        List<IntegrationInstance> integrationInstances = List.of(createIntegrationInstance(CONNECTED_USER_ID));

        when(integrationInstanceService.getConnectedUserIntegrationInstances(CONNECTED_USER_ID, Environment.PRODUCTION))
            .thenReturn(integrationInstances);
        when(integrationInstanceService.getIntegrationInstances(
            CONNECTED_USER_ID, COMPONENT_NAME, Environment.PRODUCTION))
                .thenReturn(integrationInstances);
    }

    private void mockBoundIntegrationInstance(boolean enabled) {
        IntegrationInstance integrationInstance = createIntegrationInstance(CONNECTED_USER_ID);

        integrationInstance.setEnabled(enabled);

        when(integrationInstanceService.fetchIntegrationInstance(INTEGRATION_INSTANCE_ID))
            .thenReturn(Optional.of(integrationInstance));
    }

    private static void authenticateConnectedUser(boolean apiKeyAuthenticated) {
        Authentication authentication = mock(
            Authentication.class, withSettings().extraInterfaces(ConnectedUserAuthentication.class));

        when(authentication.isAuthenticated()).thenReturn(true);
        when(((ConnectedUserAuthentication) authentication).apiKeyAuthenticated()).thenReturn(apiKeyAuthenticated);

        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
    }

    private void mockOwnedTriggerSubscription() {
        when(triggerSubscriptionService.fetchTriggerSubscription(TRIGGER_SUBSCRIPTION_ID))
            .thenReturn(Optional.of(createTriggerSubscription(CONNECTED_USER_ID, Environment.PRODUCTION)));

        mockBoundIntegrationInstance(true);
    }
}
