/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscription;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionDisableReason;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionStatus;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.TriggerSubscriptionService;
import com.bytechef.ee.embedded.configuration.trigger.subscription.TriggerSubscriptionEventSender;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs the whole connection-invalid chain: the credential status update, the connection after-save listener, the
 * credentials-invalidated event and this listener, against the real trigger subscription manager and Postgres.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@TriggerSubscriptionLifecycleIntTest
class TriggerSubscriptionConnectionEventListenerIntTest {

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private ConnectionDefinitionService connectionDefinitionService;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private EmbeddedPermissionEvaluator embeddedPermissionEvaluator;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private OAuth2Service oAuth2Service;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private PrincipalJobService principalJobService;

    @Autowired
    private TriggerDefinitionService triggerDefinitionService;

    @Autowired
    private TriggerLifecycleFacade triggerLifecycleFacade;

    @Autowired
    private TriggerSubscriptionEventSender triggerSubscriptionEventSender;

    @Autowired
    private TriggerSubscriptionManager triggerSubscriptionManager;

    @Autowired
    private TriggerSubscriptionService triggerSubscriptionService;

    private ConnectedUser connectedUser;
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void beforeEach() {
        TriggerSubscriptionLifecycleTestSupport.addConnectionForeignKey(jdbcClient);
        TriggerSubscriptionLifecycleTestSupport.stubCollaborators(
            connectionDefinitionService, embeddedPermissionEvaluator, environmentService, oAuth2Service,
            principalJobService, triggerDefinitionService);
        TriggerSubscriptionLifecycleTestSupport.authenticateAdmin();

        connectedUser = connectedUserService.createConnectedUser(
            "trigger-subscription-listener-user-" + UUID.randomUUID(), Environment.PRODUCTION);
        transactionTemplate = new TransactionTemplate(platformTransactionManager);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        TriggerSubscriptionLifecycleTestSupport.dropConnectionForeignKey(jdbcClient);
    }

    @Test
    void testInvalidCredentialsDisableTheTriggerSubscriptionsOnceTheTransactionCommits() {
        long connectionId = TriggerSubscriptionLifecycleTestSupport.createConnection(
            connectionService, AuthorizationType.OAUTH2_AUTHORIZATION_CODE);

        TriggerSubscription triggerSubscription = createSubscription(connectionId);

        clearInvocations(triggerLifecycleFacade, triggerSubscriptionEventSender);

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            connectionService.updateConnectionCredentialStatus(connectionId, Connection.CredentialStatus.INVALID);

            assertThat(getStatus(triggerSubscription)).isEqualTo(TriggerSubscriptionStatus.ENABLED);

            verify(triggerLifecycleFacade, never()).executeTriggerDisable(anyString(), any(), any(), anyMap(), any());
        });

        Connection connection = connectionService.getConnection(connectionId);

        assertThat(connection.getCredentialStatus()).isEqualTo(Connection.CredentialStatus.INVALID);
        assertThat(getStatus(triggerSubscription)).isEqualTo(TriggerSubscriptionStatus.CONNECTION_ERROR);

        verify(triggerLifecycleFacade, times(1)).executeTriggerDisable(anyString(), any(), any(), anyMap(), any());
        verify(triggerSubscriptionEventSender, times(1)).sendLifecycleEvent(
            argThat(curTriggerSubscription -> triggerSubscription.getId()
                .equals(curTriggerSubscription.getId())),
            eq("subscription.disabled"), eq(TriggerSubscriptionDisableReason.CONNECTION_INVALID));
        verify(triggerSubscriptionEventSender, times(1)).sendLifecycleEvent(any(), any(), any());
    }

    @Test
    void testInvalidCredentialsInARolledBackTransactionDisableNothing() {
        long connectionId = TriggerSubscriptionLifecycleTestSupport.createConnection(
            connectionService, AuthorizationType.OAUTH2_AUTHORIZATION_CODE);

        TriggerSubscription triggerSubscription = createSubscription(connectionId);

        clearInvocations(triggerLifecycleFacade, triggerSubscriptionEventSender);

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            connectionService.updateConnectionCredentialStatus(connectionId, Connection.CredentialStatus.INVALID);

            transactionStatus.setRollbackOnly();
        });

        Connection connection = connectionService.getConnection(connectionId);

        assertThat(connection.getCredentialStatus()).isEqualTo(Connection.CredentialStatus.VALID);
        assertThat(getStatus(triggerSubscription)).isEqualTo(TriggerSubscriptionStatus.ENABLED);

        verify(triggerLifecycleFacade, never()).executeTriggerDisable(anyString(), any(), any(), anyMap(), any());
        verify(triggerSubscriptionEventSender, never()).sendLifecycleEvent(any(), any(), any());
    }

    private TriggerSubscription createSubscription(long connectionId) {
        return triggerSubscriptionManager.create(
            connectedUser, null, connectionId, TriggerSubscriptionLifecycleTestSupport.WEBHOOK_COMPONENT_NAME, 1,
            TriggerSubscriptionLifecycleTestSupport.WEBHOOK_TRIGGER_NAME, Map.of(), null, null, null, 0);
    }

    private TriggerSubscriptionStatus getStatus(TriggerSubscription triggerSubscription) {
        TriggerSubscription currentTriggerSubscription = triggerSubscriptionService.getTriggerSubscription(
            triggerSubscription.getId());

        return currentTriggerSubscription.getStatus();
    }
}
