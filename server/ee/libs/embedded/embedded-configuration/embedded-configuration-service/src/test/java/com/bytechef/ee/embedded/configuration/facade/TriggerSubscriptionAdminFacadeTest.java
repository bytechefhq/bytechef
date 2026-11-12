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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscription;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionDisableReason;
import com.bytechef.ee.embedded.configuration.dto.TriggerSubscriptionTestEventResultDTO;
import com.bytechef.ee.embedded.configuration.dto.TriggerSubscriptionWebhookSettingsDTO;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.TriggerSubscriptionService;
import com.bytechef.ee.embedded.configuration.trigger.subscription.TriggerSubscriptionEventSender;
import com.bytechef.ee.embedded.configuration.trigger.subscription.TriggerSubscriptionWebhookSettingsAccessor;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.notification.delivery.WebhookDelivery;
import com.bytechef.platform.notification.delivery.WebhookDeliveryService;
import com.bytechef.platform.notification.delivery.WebhookTestResult;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class TriggerSubscriptionAdminFacadeTest {

    private static final long CONNECTED_USER_ID = 42L;

    private static final long ENVIRONMENT_ID = 1L;
    private static final long INTEGRATION_INSTANCE_ID = 5L;
    private static final long TRIGGER_SUBSCRIPTION_ID = 3L;

    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final EnvironmentService environmentService = mock(EnvironmentService.class);
    private final IntegrationInstanceService integrationInstanceService = mock(IntegrationInstanceService.class);
    private final TriggerSubscriptionEventSender triggerSubscriptionEventSender =
        mock(TriggerSubscriptionEventSender.class);
    private final TriggerSubscriptionManager triggerSubscriptionManager = mock(TriggerSubscriptionManager.class);
    private final TriggerSubscriptionService triggerSubscriptionService = mock(TriggerSubscriptionService.class);
    private final TriggerSubscriptionWebhookSettingsAccessor triggerSubscriptionWebhookSettingsAccessor =
        mock(TriggerSubscriptionWebhookSettingsAccessor.class);
    private final WebhookDeliveryService webhookDeliveryService = mock(WebhookDeliveryService.class);
    private final TriggerSubscriptionAdminFacade triggerSubscriptionAdminFacade =
        new TriggerSubscriptionAdminFacadeImpl(
            new ApplicationProperties(), connectedUserService, environmentService, integrationInstanceService,
            triggerSubscriptionEventSender, triggerSubscriptionManager, triggerSubscriptionService,
            triggerSubscriptionWebhookSettingsAccessor, webhookDeliveryService);

    @Test
    void testDisableConnectedUserTriggerSubscriptionsDelegates() {
        triggerSubscriptionAdminFacade.disableConnectedUserTriggerSubscriptions(
            CONNECTED_USER_ID, TriggerSubscriptionDisableReason.CONNECTED_USER_DISABLED);

        verify(triggerSubscriptionManager).disableByConnectedUserId(
            CONNECTED_USER_ID, TriggerSubscriptionDisableReason.CONNECTED_USER_DISABLED);
    }

    @Test
    void testDeleteConnectedUserTriggerSubscriptionsDelegates() {
        triggerSubscriptionAdminFacade.deleteConnectedUserTriggerSubscriptions(CONNECTED_USER_ID);

        verify(triggerSubscriptionManager).deleteByConnectedUserId(CONNECTED_USER_ID);
    }

    @Test
    void testEnableDelegatesToManager() {
        TriggerSubscription triggerSubscription = mock(TriggerSubscription.class);

        when(triggerSubscription.getConnectedUserId()).thenReturn(CONNECTED_USER_ID);
        when(triggerSubscription.getIntegrationInstanceId()).thenReturn(INTEGRATION_INSTANCE_ID);

        mockConnectedUser(true);

        IntegrationInstance integrationInstance = mock(IntegrationInstance.class);

        when(integrationInstance.isEnabled()).thenReturn(true);
        when(integrationInstanceService.fetchIntegrationInstance(INTEGRATION_INSTANCE_ID))
            .thenReturn(Optional.of(integrationInstance));
        when(triggerSubscriptionService.getTriggerSubscription(TRIGGER_SUBSCRIPTION_ID))
            .thenReturn(triggerSubscription);

        triggerSubscriptionAdminFacade.enableTriggerSubscription(TRIGGER_SUBSCRIPTION_ID, true);

        verify(triggerSubscriptionManager).enable(TRIGGER_SUBSCRIPTION_ID);
    }

    @Test
    void testEnableRejectsDisabledIntegrationInstance() {
        TriggerSubscription triggerSubscription = mock(TriggerSubscription.class);

        when(triggerSubscription.getIntegrationInstanceId()).thenReturn(INTEGRATION_INSTANCE_ID);

        IntegrationInstance integrationInstance = mock(IntegrationInstance.class);

        when(integrationInstance.isEnabled()).thenReturn(false);
        when(integrationInstanceService.fetchIntegrationInstance(INTEGRATION_INSTANCE_ID))
            .thenReturn(Optional.of(integrationInstance));
        when(triggerSubscriptionService.getTriggerSubscription(TRIGGER_SUBSCRIPTION_ID))
            .thenReturn(triggerSubscription);

        assertThatThrownBy(
            () -> triggerSubscriptionAdminFacade.enableTriggerSubscription(TRIGGER_SUBSCRIPTION_ID, true))
                .isInstanceOf(ConfigurationException.class);

        verify(triggerSubscriptionManager, never()).enable(TRIGGER_SUBSCRIPTION_ID);
    }

    @Test
    void testEnableRejectsDisabledConnectedUser() {
        TriggerSubscription triggerSubscription = mock(TriggerSubscription.class);

        when(triggerSubscription.getConnectedUserId()).thenReturn(CONNECTED_USER_ID);
        when(triggerSubscription.getIntegrationInstanceId()).thenReturn(null);
        when(triggerSubscriptionService.getTriggerSubscription(TRIGGER_SUBSCRIPTION_ID))
            .thenReturn(triggerSubscription);

        mockConnectedUser(false);

        assertThatThrownBy(
            () -> triggerSubscriptionAdminFacade.enableTriggerSubscription(TRIGGER_SUBSCRIPTION_ID, true))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("Connected user %d is disabled".formatted(CONNECTED_USER_ID));

        verify(triggerSubscriptionManager, never()).enable(TRIGGER_SUBSCRIPTION_ID);
    }

    @Test
    void testDisableDelegatesToManager() {
        triggerSubscriptionAdminFacade.enableTriggerSubscription(TRIGGER_SUBSCRIPTION_ID, false);

        verify(triggerSubscriptionManager).disable(TRIGGER_SUBSCRIPTION_ID, null);
    }

    @Test
    void testDeleteTriggerSubscriptionDelegatesToManager() {
        triggerSubscriptionAdminFacade.deleteTriggerSubscription(TRIGGER_SUBSCRIPTION_ID);

        verify(triggerSubscriptionManager).delete(TRIGGER_SUBSCRIPTION_ID);
    }

    @Test
    void testUpdateWebhookUrlWithABlankValueClearsIt() {
        when(environmentService.getEnvironment(ENVIRONMENT_ID)).thenReturn(Environment.DEVELOPMENT);

        triggerSubscriptionAdminFacade.updateWebhookUrl(ENVIRONMENT_ID, " ");

        verify(triggerSubscriptionWebhookSettingsAccessor).saveWebhookUrl(Environment.DEVELOPMENT, null);
    }

    @Test
    void testUpdateWebhookUrlRejectsPrivateAddress() {
        when(environmentService.getEnvironment(ENVIRONMENT_ID)).thenReturn(Environment.PRODUCTION);

        assertThatThrownBy(
            () -> triggerSubscriptionAdminFacade.updateWebhookUrl(ENVIRONMENT_ID, "http://127.0.0.1/hook"))
                .isInstanceOf(ConfigurationException.class);

        verify(triggerSubscriptionWebhookSettingsAccessor, never()).saveWebhookUrl(any(), any());
    }

    @Test
    void testUpdateWebhookUrlSavesPublicUrl() {
        TriggerSubscriptionWebhookSettingsDTO settings =
            new TriggerSubscriptionWebhookSettingsDTO("https://93.184.216.34/hook", "whsec_abc");

        when(environmentService.getEnvironment(ENVIRONMENT_ID)).thenReturn(Environment.PRODUCTION);
        when(triggerSubscriptionWebhookSettingsAccessor.saveWebhookUrl(
            Environment.PRODUCTION, "https://93.184.216.34/hook")).thenReturn(settings);

        assertThat(triggerSubscriptionAdminFacade.updateWebhookUrl(ENVIRONMENT_ID, "https://93.184.216.34/hook"))
            .isEqualTo(settings);
    }

    @Test
    void testSendTestEventWithoutUrlReturnsFailure() {
        when(environmentService.getEnvironment(ENVIRONMENT_ID)).thenReturn(Environment.PRODUCTION);
        when(triggerSubscriptionWebhookSettingsAccessor.getSettings(Environment.PRODUCTION))
            .thenReturn(new TriggerSubscriptionWebhookSettingsDTO(null, null));

        TriggerSubscriptionTestEventResultDTO webhookTestResult =
            triggerSubscriptionAdminFacade.sendTestEvent(ENVIRONMENT_ID);

        assertThat(webhookTestResult.success()).isFalse();
        assertThat(webhookTestResult.error()).isNotBlank();

        verify(webhookDeliveryService, never()).sendTest(any());
    }

    @Test
    void testSendTestEventWithoutSigningSecretReturnsFailure() {
        when(environmentService.getEnvironment(ENVIRONMENT_ID)).thenReturn(Environment.PRODUCTION);
        when(triggerSubscriptionWebhookSettingsAccessor.getSettings(Environment.PRODUCTION))
            .thenReturn(new TriggerSubscriptionWebhookSettingsDTO("https://93.184.216.34/hook", null));

        TriggerSubscriptionTestEventResultDTO webhookTestResult =
            triggerSubscriptionAdminFacade.sendTestEvent(ENVIRONMENT_ID);

        assertThat(webhookTestResult.success()).isFalse();
        assertThat(webhookTestResult.error()).isNotBlank();

        verify(webhookDeliveryService, never()).sendTest(any());
    }

    @Test
    void testSendTestEventSendsSynchronously() {
        WebhookDelivery webhookDelivery = mock(WebhookDelivery.class);
        WebhookTestResult webhookTestResult = new WebhookTestResult(true, 200, "secret body", null);

        when(environmentService.getEnvironment(ENVIRONMENT_ID)).thenReturn(Environment.PRODUCTION);
        when(triggerSubscriptionWebhookSettingsAccessor.getSettings(Environment.PRODUCTION))
            .thenReturn(new TriggerSubscriptionWebhookSettingsDTO("https://93.184.216.34/hook", "whsec_abc"));
        when(triggerSubscriptionEventSender.createTestDelivery(
            Environment.PRODUCTION, "https://93.184.216.34/hook", "whsec_abc")).thenReturn(webhookDelivery);
        when(webhookDeliveryService.sendTest(webhookDelivery)).thenReturn(webhookTestResult);

        assertThat(triggerSubscriptionAdminFacade.sendTestEvent(ENVIRONMENT_ID))
            .isEqualTo(new TriggerSubscriptionTestEventResultDTO(true, 200, null));
    }

    @Test
    void testInvalidEnvironmentIdIsRejectedAsIllegalArgument() {
        TriggerSubscriptionAdminFacade facade = new TriggerSubscriptionAdminFacadeImpl(
            new ApplicationProperties(), connectedUserService, () -> List.of(Environment.values()),
            integrationInstanceService, triggerSubscriptionEventSender, triggerSubscriptionManager,
            triggerSubscriptionService,
            triggerSubscriptionWebhookSettingsAccessor, webhookDeliveryService);

        assertThatThrownBy(() -> facade.getWebhookSettings(-1L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> facade.getWebhookSettings(99L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> facade.sendTestEvent(99L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testEveryFacadeMethodRequiresTenantAdmin() throws NoSuchMethodException {
        List<String> ungatedMethodNames = new ArrayList<>();

        for (Method facadeMethod : TriggerSubscriptionAdminFacade.class.getMethods()) {
            Method implementationMethod = TriggerSubscriptionAdminFacadeImpl.class.getDeclaredMethod(
                facadeMethod.getName(), facadeMethod.getParameterTypes());

            PreAuthorize preAuthorize = implementationMethod.getAnnotation(PreAuthorize.class);

            if (preAuthorize == null || !"isTenantAdmin()".equals(preAuthorize.value())) {
                ungatedMethodNames.add(implementationMethod.toGenericString());
            }
        }

        assertThat(ungatedMethodNames).isEmpty();
    }

    private void mockConnectedUser(boolean enabled) {
        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.getId()).thenReturn(CONNECTED_USER_ID);
        when(connectedUser.isEnabled()).thenReturn(enabled);
        when(connectedUserService.getConnectedUser(CONNECTED_USER_ID)).thenReturn(connectedUser);
    }
}
