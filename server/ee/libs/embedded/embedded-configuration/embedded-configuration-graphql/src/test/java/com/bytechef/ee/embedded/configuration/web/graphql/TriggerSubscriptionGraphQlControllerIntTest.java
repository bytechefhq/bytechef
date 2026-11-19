/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.UrlValidationException;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionErrorCode;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionStatus;
import com.bytechef.ee.embedded.configuration.dto.TriggerSubscriptionDTO;
import com.bytechef.ee.embedded.configuration.dto.TriggerSubscriptionTestEventResultDTO;
import com.bytechef.ee.embedded.configuration.dto.TriggerSubscriptionWebhookSettingsDTO;
import com.bytechef.ee.embedded.configuration.exception.TriggerSubscriptionEnableException;
import com.bytechef.ee.embedded.configuration.exception.TriggerSubscriptionErrorType;
import com.bytechef.ee.embedded.configuration.facade.TriggerSubscriptionAdminFacade;
import com.bytechef.ee.embedded.configuration.web.graphql.config.EmbeddedConfigurationGraphQlConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.web.graphql.config.EmbeddedConfigurationGraphQlTestConfiguration;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    EmbeddedConfigurationGraphQlTestConfiguration.class,
    TriggerSubscriptionGraphQlController.class,
    TriggerSubscriptionGraphQlControllerIntTest.GraphQlErrorConfiguration.class
})
@GraphQlTest(
    controllers = TriggerSubscriptionGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@EmbeddedConfigurationGraphQlConfigurationSharedMocks
@ExtendWith(ObjectMapperSetupExtension.class)
class TriggerSubscriptionGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private TriggerSubscriptionAdminFacade triggerSubscriptionAdminFacade;

    @Test
    void testConnectedUserTriggerSubscriptions() {
        Instant createdDate = Instant.ofEpochMilli(1_700_000_000_000L);

        when(triggerSubscriptionAdminFacade.getConnectedUserTriggerSubscriptions(7L))
            .thenReturn(
                List.of(
                    new TriggerSubscriptionDTO(
                        3L, "github", 1, "newIssue", "My sub", "ext-1", Map.of(), "https://example.com/hook",
                        TriggerSubscriptionStatus.ENABLED, null, null, "https://bytechef.example.com/webhooks/abc",
                        null, "SENT", null, createdDate, createdDate)));

        this.graphQlTester
            .document("""
                query {
                    connectedUserTriggerSubscriptions(connectedUserId: "7") {
                        id
                        componentName
                        componentVersion
                        triggerName
                        name
                        externalId
                        status
                        errorCode
                        webhookUrl
                        staticWebhookUrl
                        lastDeliveryStatus
                        createdDate
                    }
                }
                """)
            .execute()
            .path("connectedUserTriggerSubscriptions[0].id")
            .entity(String.class)
            .isEqualTo("3")
            .path("connectedUserTriggerSubscriptions[0].componentName")
            .entity(String.class)
            .isEqualTo("github")
            .path("connectedUserTriggerSubscriptions[0].status")
            .entity(String.class)
            .isEqualTo("ENABLED")
            .path("connectedUserTriggerSubscriptions[0].errorCode")
            .valueIsNull()
            .path("connectedUserTriggerSubscriptions[0].staticWebhookUrl")
            .entity(String.class)
            .isEqualTo("https://bytechef.example.com/webhooks/abc")
            .path("connectedUserTriggerSubscriptions[0].createdDate")
            .entity(Long.class)
            .isEqualTo(1_700_000_000_000L);
    }

    @Test
    void testEnableTriggerSubscription() {
        this.graphQlTester
            .document("""
                mutation {
                    enableTriggerSubscription(id: "3", enable: true)
                }
                """)
            .execute()
            .path("enableTriggerSubscription")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(triggerSubscriptionAdminFacade).enableTriggerSubscription(3L, true);
    }

    @Test
    void testEnableTriggerSubscriptionFailureReachesClientWithMessage() {
        doThrow(
            new TriggerSubscriptionEnableException(
                3L, TriggerSubscriptionErrorCode.API_ERROR, "Provider rejected the subscription",
                new IllegalStateException("boom")))
                    .when(triggerSubscriptionAdminFacade)
                    .enableTriggerSubscription(3L, true);

        this.graphQlTester
            .document("""
                mutation {
                    enableTriggerSubscription(id: "3", enable: true)
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> {
                assertThat(errors).hasSize(1);
                assertThat(errors.getFirst()
                    .getMessage()).isEqualTo("Provider rejected the subscription");
                assertThat(errors.getFirst()
                    .getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                assertThat(errors.getFirst()
                    .getExtensions()).containsEntry("errorCode", "API_ERROR");
            });
    }

    @Test
    void testEnableTriggerSubscriptionInternalFailureHidesItsCause() {
        doThrow(
            new TriggerSubscriptionEnableException(
                3L, TriggerSubscriptionErrorCode.INTERNAL_ERROR, "relation \"secret\" does not exist",
                new IllegalStateException("relation \"secret\" does not exist")))
                    .when(triggerSubscriptionAdminFacade)
                    .enableTriggerSubscription(3L, true);

        this.graphQlTester
            .document("""
                mutation {
                    enableTriggerSubscription(id: "3", enable: true)
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> {
                assertThat(errors).hasSize(1);
                assertThat(errors.getFirst()
                    .getMessage()).isEqualTo("Internal error while enabling the trigger subscription");
                assertThat(errors.getFirst()
                    .getErrorType()).isEqualTo(ErrorType.INTERNAL_ERROR);
                assertThat(errors.getFirst()
                    .getExtensions()).containsEntry("errorCode", "INTERNAL_ERROR");
            });
    }

    @Test
    void testDeleteTriggerSubscription() {
        this.graphQlTester
            .document("""
                mutation {
                    deleteTriggerSubscription(id: "3")
                }
                """)
            .execute()
            .path("deleteTriggerSubscription")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(triggerSubscriptionAdminFacade).deleteTriggerSubscription(3L);
    }

    @Test
    void testTriggerSubscriptionWebhookSettings() {
        when(triggerSubscriptionAdminFacade.getWebhookSettings(1L))
            .thenReturn(new TriggerSubscriptionWebhookSettingsDTO("https://example.com/hook", "whsec_abc"));

        this.graphQlTester
            .document("""
                query {
                    triggerSubscriptionWebhookSettings(environmentId: "1") {
                        webhookUrl
                        signingSecret
                    }
                }
                """)
            .execute()
            .path("triggerSubscriptionWebhookSettings.webhookUrl")
            .entity(String.class)
            .isEqualTo("https://example.com/hook")
            .path("triggerSubscriptionWebhookSettings.signingSecret")
            .entity(String.class)
            .isEqualTo("whsec_abc");
    }

    @Test
    void testUpdateWebhookUrlReturnsSecret() {
        when(triggerSubscriptionAdminFacade.updateWebhookUrl(1L, "https://example.com/hook"))
            .thenReturn(new TriggerSubscriptionWebhookSettingsDTO("https://example.com/hook", "whsec_new"));

        this.graphQlTester
            .document("""
                mutation {
                    updateTriggerSubscriptionWebhookUrl(environmentId: "1", webhookUrl: "https://example.com/hook") {
                        webhookUrl
                        signingSecret
                    }
                }
                """)
            .execute()
            .path("updateTriggerSubscriptionWebhookUrl.webhookUrl")
            .entity(String.class)
            .isEqualTo("https://example.com/hook")
            .path("updateTriggerSubscriptionWebhookUrl.signingSecret")
            .entity(String.class)
            .isEqualTo("whsec_new");
    }

    @Test
    void testRotateTriggerSubscriptionWebhookSecret() {
        when(triggerSubscriptionAdminFacade.rotateSigningSecret(1L))
            .thenReturn(new TriggerSubscriptionWebhookSettingsDTO("https://example.com/hook", "whsec_rotated"));

        this.graphQlTester
            .document("""
                mutation {
                    rotateTriggerSubscriptionWebhookSecret(environmentId: "1") {
                        signingSecret
                    }
                }
                """)
            .execute()
            .path("rotateTriggerSubscriptionWebhookSecret.signingSecret")
            .entity(String.class)
            .isEqualTo("whsec_rotated");
    }

    @Test
    void testSendTestEvent() {
        when(triggerSubscriptionAdminFacade.sendTestEvent(1L))
            .thenReturn(new TriggerSubscriptionTestEventResultDTO(false, 500, "Server error"));

        this.graphQlTester
            .document("""
                mutation {
                    sendTriggerSubscriptionTestEvent(environmentId: "1") {
                        success
                        statusCode
                        error
                    }
                }
                """)
            .execute()
            .path("sendTriggerSubscriptionTestEvent.success")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("sendTriggerSubscriptionTestEvent.statusCode")
            .entity(Integer.class)
            .isEqualTo(500)
            .path("sendTriggerSubscriptionTestEvent.error")
            .entity(String.class)
            .isEqualTo("Server error");
    }

    @Test
    void testUpdateWebhookUrlRejectionReachesClientWithItsReason() {
        String webhookUrl = "https://unresolvable.invalid/hook";

        when(triggerSubscriptionAdminFacade.updateWebhookUrl(1L, webhookUrl))
            .thenThrow(
                new ConfigurationException(
                    "Webhook URL is not allowed: Cannot resolve URL host: " + webhookUrl,
                    new UrlValidationException("Cannot resolve URL host: " + webhookUrl),
                    TriggerSubscriptionErrorType.USER_INPUT_ERROR));

        this.graphQlTester
            .document("""
                mutation {
                    updateTriggerSubscriptionWebhookUrl(
                        environmentId: "1", webhookUrl: "https://unresolvable.invalid/hook") {

                        webhookUrl
                    }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> {
                assertThat(errors).hasSize(1);
                assertThat(errors.getFirst()
                    .getMessage()).isEqualTo("Webhook URL is not allowed: Cannot resolve URL host: " + webhookUrl);
                assertThat(errors.getFirst()
                    .getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            });
    }

    @Test
    void testEnableTriggerSubscriptionRejectionReachesClientWithItsReason() {
        doThrow(
            new ConfigurationException("Connected user 7 is disabled", TriggerSubscriptionErrorType.USER_INPUT_ERROR))
                .when(triggerSubscriptionAdminFacade)
                .enableTriggerSubscription(3L, true);

        this.graphQlTester
            .document("""
                mutation {
                    enableTriggerSubscription(id: "3", enable: true)
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> {
                assertThat(errors).hasSize(1);
                assertThat(errors.getFirst()
                    .getMessage()).isEqualTo("Connected user 7 is disabled");
                assertThat(errors.getFirst()
                    .getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            });
    }

    @Test
    void testRotateWebhookSecretInternalFailureHidesItsCause() {
        when(triggerSubscriptionAdminFacade.rotateSigningSecret(1L))
            .thenThrow(new IllegalStateException("relation \"property\" does not exist"));

        this.graphQlTester
            .document("""
                mutation {
                    rotateTriggerSubscriptionWebhookSecret(environmentId: "1") {
                        signingSecret
                    }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> {
                assertThat(errors).hasSize(1);
                assertThat(errors.getFirst()
                    .getMessage()).doesNotContain("relation");
                assertThat(errors.getFirst()
                    .getErrorType()).isEqualTo(ErrorType.INTERNAL_ERROR);
            });
    }

    @Configuration
    @ComponentScan("com.bytechef.graphql.error")
    static class GraphQlErrorConfiguration {
    }
}
