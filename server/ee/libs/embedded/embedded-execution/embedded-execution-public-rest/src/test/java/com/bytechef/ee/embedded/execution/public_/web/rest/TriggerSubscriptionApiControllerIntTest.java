/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionErrorCode;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionStatus;
import com.bytechef.ee.embedded.configuration.dto.ComponentTriggerDTO;
import com.bytechef.ee.embedded.configuration.dto.TriggerSubscriptionDTO;
import com.bytechef.ee.embedded.configuration.exception.AutomationProjectConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.exception.TriggerSubscriptionEnableException;
import com.bytechef.ee.embedded.configuration.exception.TriggerSubscriptionErrorType;
import com.bytechef.ee.embedded.configuration.facade.TriggerSubscriptionFacade;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.ee.embedded.security.web.configurer.EmbeddedApiKeySecurityConfigurer;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.tenant.domain.TenantKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    JacksonConfiguration.class, TriggerSubscriptionApiController.class
})
@TestPropertySource(properties = "bytechef.edition=ee")
@WebMvcTest(TriggerSubscriptionApiController.class)
@WithMockUser(username = TriggerSubscriptionApiControllerIntTest.EXTERNAL_USER_ID)
class TriggerSubscriptionApiControllerIntTest {

    static final String EXTERNAL_USER_ID = "external-user-1";

    private static final Instant CREATED_DATE = Instant.parse("2026-11-11T10:00:00Z");
    private static final Instant LAST_EVENT_DATE = Instant.parse("2026-11-11T12:00:00Z");
    private static final Instant LAST_MODIFIED_DATE = Instant.parse("2026-11-11T11:00:00Z");
    private static final long TRIGGER_SUBSCRIPTION_ID = 3L;

    @MockitoBean
    private EnvironmentService environmentService;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TriggerSubscriptionFacade triggerSubscriptionFacade;

    @BeforeEach
    void beforeEach() {
        when(environmentService.getEnvironment("STAGING")).thenReturn(Environment.STAGING);
        when(environmentService.getEnvironment((String) null)).thenReturn(Environment.PRODUCTION);
    }

    @Test
    void testCreateTriggerSubscriptionReturnsSubscription() throws Exception {
        when(
            triggerSubscriptionFacade.createTriggerSubscription(
                EXTERNAL_USER_ID, "github", 1, "newIssue", Map.of("repository", "bytechef"), "My subscription",
                "ext-1", "https://example.com/hooks", 5L, Environment.STAGING))
                    .thenReturn(createTriggerSubscriptionDTO(TriggerSubscriptionStatus.ENABLED, null, null));

        mockMvc
            .perform(
                post(
                    "/v1/{externalUserId}/components/{componentName}/versions/{componentVersion}/triggers/" +
                        "{triggerName}/subscriptions",
                    EXTERNAL_USER_ID, "github", 1, "newIssue")
                        .with(csrf())
                        .header("X-Environment", "STAGING")
                        .header("X-Instance-Id", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                                {
                                  "externalId": "ext-1",
                                  "input": {"repository": "bytechef"},
                                  "name": "My subscription",
                                  "webhookUrl": "https://example.com/hooks"
                                }
                                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(TRIGGER_SUBSCRIPTION_ID))
            .andExpect(jsonPath("$.componentName").value("github"))
            .andExpect(jsonPath("$.componentVersion").value(1))
            .andExpect(jsonPath("$.triggerName").value("newIssue"))
            .andExpect(jsonPath("$.name").value("My subscription"))
            .andExpect(jsonPath("$.externalId").value("ext-1"))
            .andExpect(jsonPath("$.input.repository").value("bytechef"))
            .andExpect(jsonPath("$.webhookUrl").value("https://example.com/hooks"))
            .andExpect(jsonPath("$.status").value("ENABLED"))
            .andExpect(jsonPath("$.error").doesNotExist())
            .andExpect(jsonPath("$.staticWebhookUrl").value("https://bytechef.example.com/webhooks/abc"))
            .andExpect(jsonPath("$.lastEventDate").value("2026-11-11T12:00:00Z"))
            .andExpect(jsonPath("$.lastDeliveryStatus").value("SUCCESS"))
            .andExpect(jsonPath("$.lastDeliveryError").doesNotExist())
            .andExpect(jsonPath("$.createdDate").value("2026-11-11T10:00:00Z"))
            .andExpect(jsonPath("$.lastModifiedDate").value("2026-11-11T11:00:00Z"));
    }

    @Test
    void testCreateTriggerSubscriptionWithoutRequiredConnectionReturns400() throws Exception {
        when(
            triggerSubscriptionFacade.createTriggerSubscription(
                EXTERNAL_USER_ID, "github", 1, "newIssue", Map.of(), null, null, null, null, Environment.PRODUCTION))
                    .thenThrow(
                        new ConfigurationException(
                            "Component github requires a connection", TriggerSubscriptionErrorType.USER_INPUT_ERROR));

        mockMvc
            .perform(
                post(
                    "/v1/{externalUserId}/components/{componentName}/versions/{componentVersion}/triggers/" +
                        "{triggerName}/subscriptions",
                    EXTERNAL_USER_ID, "github", 1, "newIssue")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\": {}}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].code").value("USER_INPUT_ERROR"))
            .andExpect(jsonPath("$.errors[0].problem_type").value("USER_INPUT_ERROR"))
            .andExpect(jsonPath("$.errors[0].status").value("400"))
            .andExpect(jsonPath("$.errors[0].detail").value("Component github requires a connection"))
            .andExpect(jsonPath("$.errors[0].id").isNotEmpty())
            .andExpect(jsonPath("$.errors[0].meta.origin").value("bytechef"))
            .andExpect(jsonPath("$.triggerSubscriptionId").doesNotExist());
    }

    @Test
    void testCreateTriggerSubscriptionOfUnknownTriggerReturns400() throws Exception {
        when(
            triggerSubscriptionFacade.createTriggerSubscription(
                EXTERNAL_USER_ID, "github", 1, "missing", Map.of(), null, null, null, null, Environment.PRODUCTION))
                    .thenThrow(
                        new ConfigurationException(
                            "Trigger github/v1/missing does not exist", TriggerSubscriptionErrorType.NO_TRIGGER));

        mockMvc
            .perform(
                post(
                    "/v1/{externalUserId}/components/{componentName}/versions/{componentVersion}/triggers/" +
                        "{triggerName}/subscriptions",
                    EXTERNAL_USER_ID, "github", 1, "missing")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\": {}}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].code").value("NO_TRIGGER"));
    }

    @Test
    void testCreateTriggerSubscriptionWithAnInternalConfigurationErrorReturns500() throws Exception {
        when(
            triggerSubscriptionFacade.createTriggerSubscription(
                EXTERNAL_USER_ID, "github", 1, "newIssue", Map.of(), null, null, null, null, Environment.PRODUCTION))
                    .thenThrow(
                        new ConfigurationException(
                            "Project version 3 of project id=12 is not published",
                            AutomationProjectConfigurationErrorType.PROJECT_VERSION_NOT_PUBLISHED));

        mockMvc
            .perform(
                post(
                    "/v1/{externalUserId}/components/{componentName}/versions/{componentVersion}/triggers/" +
                        "{triggerName}/subscriptions",
                    EXTERNAL_USER_ID, "github", 1, "newIssue")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\": {}}"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.errors[0].code").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.errors[0].status").value("500"))
            .andExpect(jsonPath("$.errors[0].detail").value("Internal error while processing the trigger subscription"))
            .andExpect(jsonPath("$.errors[0].meta.origin").value("bytechef"))
            .andExpect(jsonPath("$.triggerSubscriptionId").doesNotExist());
    }

    @Test
    void testGetTriggerSubscriptionReturnsFailedSubscriptionError() throws Exception {
        when(
            triggerSubscriptionFacade.getTriggerSubscription(
                EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, Environment.PRODUCTION))
                    .thenReturn(
                        createTriggerSubscriptionDTO(
                            TriggerSubscriptionStatus.FAILED, TriggerSubscriptionErrorCode.OAUTH_REFRESH,
                            "Unable to perform oauth token refresh"));

        mockMvc
            .perform(get("/v1/{externalUserId}/trigger-subscriptions/{id}", EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.error.code").value("OAUTH_REFRESH"))
            .andExpect(jsonPath("$.error.message").value("Unable to perform oauth token refresh"));
    }

    @Test
    void testGetSubscriptionOfOtherUserReturns404() throws Exception {
        when(
            triggerSubscriptionFacade.getTriggerSubscription(
                EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, Environment.PRODUCTION))
                    .thenThrow(
                        new ConfigurationException(
                            "Trigger subscription id=3 not found",
                            TriggerSubscriptionErrorType.TRIGGER_SUBSCRIPTION_NOT_FOUND));

        mockMvc
            .perform(get("/v1/{externalUserId}/trigger-subscriptions/{id}", EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.errors[0].status").value("404"));
    }

    @Test
    void testDeleteOfOtherUserSubscriptionReturns404() throws Exception {
        doThrow(
            new ConfigurationException(
                "Trigger subscription id=3 not found", TriggerSubscriptionErrorType.TRIGGER_SUBSCRIPTION_NOT_FOUND))
                    .when(triggerSubscriptionFacade)
                    .deleteTriggerSubscription(EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, Environment.PRODUCTION);

        mockMvc
            .perform(
                delete("/v1/{externalUserId}/trigger-subscriptions/{id}", EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID)
                    .with(csrf()))
            .andExpect(status().isNotFound());
    }

    @Test
    void testGetTriggerSubscriptions() throws Exception {
        when(triggerSubscriptionFacade.getTriggerSubscriptions(EXTERNAL_USER_ID, Environment.STAGING))
            .thenReturn(List.of(createTriggerSubscriptionDTO(TriggerSubscriptionStatus.DISABLED, null, null)));

        mockMvc
            .perform(
                get("/v1/{externalUserId}/trigger-subscriptions", EXTERNAL_USER_ID)
                    .header("X-Environment", "STAGING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(TRIGGER_SUBSCRIPTION_ID))
            .andExpect(jsonPath("$[0].status").value("DISABLED"));
    }

    @Test
    void testUpdateTriggerSubscription() throws Exception {
        when(
            triggerSubscriptionFacade.updateTriggerSubscription(
                EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, Map.of("repository", "other"), "renamed", null, null,
                Environment.PRODUCTION))
                    .thenReturn(createTriggerSubscriptionDTO(TriggerSubscriptionStatus.ENABLED, null, null));

        mockMvc
            .perform(
                put("/v1/{externalUserId}/trigger-subscriptions/{id}", EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"input\": {\"repository\": \"other\"}, \"name\": \"renamed\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(TRIGGER_SUBSCRIPTION_ID));
    }

    @Test
    void testDeleteReturns204() throws Exception {
        mockMvc
            .perform(
                delete("/v1/{externalUserId}/trigger-subscriptions/{id}", EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID)
                    .with(csrf()))
            .andExpect(status().isNoContent());

        verify(triggerSubscriptionFacade).deleteTriggerSubscription(
            EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, Environment.PRODUCTION);
    }

    @Test
    void testEnableDisableReturn204() throws Exception {
        mockMvc
            .perform(
                post(
                    "/v1/{externalUserId}/trigger-subscriptions/{id}/enable", EXTERNAL_USER_ID,
                    TRIGGER_SUBSCRIPTION_ID)
                        .with(csrf())
                        .header("X-Environment", "STAGING"))
            .andExpect(status().isNoContent());

        verify(triggerSubscriptionFacade).enableTriggerSubscription(
            EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, true, Environment.STAGING);
        verify(triggerSubscriptionFacade, never()).enableTriggerSubscription(
            eq(EXTERNAL_USER_ID), anyLong(), eq(false), eq(Environment.STAGING));

        mockMvc
            .perform(
                post(
                    "/v1/{externalUserId}/trigger-subscriptions/{id}/disable", EXTERNAL_USER_ID,
                    TRIGGER_SUBSCRIPTION_ID)
                        .with(csrf())
                        .header("X-Environment", "STAGING"))
            .andExpect(status().isNoContent());

        verify(triggerSubscriptionFacade).enableTriggerSubscription(
            EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, false, Environment.STAGING);
    }

    @Test
    void testGetComponentTriggers() throws Exception {
        when(triggerSubscriptionFacade.getComponentTriggers(EXTERNAL_USER_ID, "github", 1))
            .thenReturn(
                List.of(
                    new ComponentTriggerDTO(
                        "newIssue", "New Issue", "Triggers when a new issue is created.", "DYNAMIC_WEBHOOK",
                        "{\"type\":\"object\"}", null, false)));

        mockMvc
            .perform(
                get(
                    "/v1/{externalUserId}/components/{componentName}/versions/{componentVersion}/triggers",
                    EXTERNAL_USER_ID, "github", 1)
                        .header("X-Environment", "STAGING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("newIssue"))
            .andExpect(jsonPath("$[0].title").value("New Issue"))
            .andExpect(jsonPath("$[0].description").value("Triggers when a new issue is created."))
            .andExpect(jsonPath("$[0].type").value("DYNAMIC_WEBHOOK"))
            .andExpect(jsonPath("$[0].inputSchema").value("{\"type\":\"object\"}"))
            .andExpect(jsonPath("$[0].outputSchema").doesNotExist())
            .andExpect(jsonPath("$[0].hasDynamicOutput").value(false));
    }

    @Test
    void testCreateTriggerSubscriptionEnableFailureReturns499WithoutId() throws Exception {
        when(
            triggerSubscriptionFacade.createTriggerSubscription(
                EXTERNAL_USER_ID, "github", 1, "newIssue", Map.of(), null, null, null, null, Environment.PRODUCTION))
                    .thenThrow(
                        new TriggerSubscriptionEnableException(
                            null, TriggerSubscriptionErrorCode.API_ERROR, "Webhook registration rejected",
                            new IllegalStateException("Webhook registration rejected")));

        mockMvc
            .perform(
                post(
                    "/v1/{externalUserId}/components/{componentName}/versions/{componentVersion}/triggers/" +
                        "{triggerName}/subscriptions",
                    EXTERNAL_USER_ID, "github", 1, "newIssue")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\": {}}"))
            .andExpect(status().is(499))
            .andExpect(jsonPath("$.errors.length()").value(1))
            .andExpect(jsonPath("$.errors[0].code").value("API_ERROR"))
            .andExpect(jsonPath("$.errors[0].detail").value("Webhook registration rejected"))
            .andExpect(jsonPath("$.errors[0].status").value("499"))
            .andExpect(jsonPath("$.errors[0].meta.origin").value("remote-provider"))
            .andExpect(jsonPath("$.triggerSubscriptionId").doesNotExist());
    }

    @Test
    void testEnableFailureReturns499WithTheSubscriptionId() throws Exception {
        doThrow(
            new TriggerSubscriptionEnableException(
                TRIGGER_SUBSCRIPTION_ID, TriggerSubscriptionErrorCode.OAUTH_REFRESH,
                "Unable to perform oauth token refresh", new IllegalStateException("Token expired")))
                    .when(triggerSubscriptionFacade)
                    .enableTriggerSubscription(EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, true, Environment.PRODUCTION);

        mockMvc
            .perform(
                post(
                    "/v1/{externalUserId}/trigger-subscriptions/{id}/enable", EXTERNAL_USER_ID,
                    TRIGGER_SUBSCRIPTION_ID)
                        .with(csrf()))
            .andExpect(status().is(499))
            .andExpect(jsonPath("$.errors[0].code").value("OAUTH_REFRESH"))
            .andExpect(jsonPath("$.errors[0].detail").value("Unable to perform oauth token refresh"))
            .andExpect(jsonPath("$.triggerSubscriptionId").value(TRIGGER_SUBSCRIPTION_ID));
    }

    @Test
    void testUpdateInternalFailureReturns500WithTheSubscriptionId() throws Exception {
        when(
            triggerSubscriptionFacade.updateTriggerSubscription(
                EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, Map.of(), null, null, null, Environment.PRODUCTION))
                    .thenThrow(
                        new TriggerSubscriptionEnableException(
                            TRIGGER_SUBSCRIPTION_ID, TriggerSubscriptionErrorCode.INTERNAL_ERROR,
                            "Internal error while enabling the trigger subscription",
                            new IllegalStateException("relation \"secret\" does not exist")));

        mockMvc
            .perform(
                put("/v1/{externalUserId}/trigger-subscriptions/{id}", EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"input\": {}}"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.errors[0].code").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.errors[0].detail").value("Internal error while enabling the trigger subscription"))
            .andExpect(jsonPath("$.errors[0].status").value("500"))
            .andExpect(jsonPath("$.errors[0].meta.origin").value("bytechef"))
            .andExpect(jsonPath("$.triggerSubscriptionId").value(TRIGGER_SUBSCRIPTION_ID));
    }

    @Test
    void testEnableFailureFromMissingConnectionsReturns400WithTheSubscriptionId() throws Exception {
        doThrow(
            new TriggerSubscriptionEnableException(
                TRIGGER_SUBSCRIPTION_ID, TriggerSubscriptionErrorCode.USER_INPUT_ERROR,
                "Not all required connections are set for the trigger subscription",
                new IllegalStateException("Not all required connections are set")))
                    .when(triggerSubscriptionFacade)
                    .enableTriggerSubscription(EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, true, Environment.PRODUCTION);

        mockMvc
            .perform(
                post(
                    "/v1/{externalUserId}/trigger-subscriptions/{id}/enable", EXTERNAL_USER_ID,
                    TRIGGER_SUBSCRIPTION_ID)
                        .with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].code").value("USER_INPUT_ERROR"))
            .andExpect(jsonPath("$.errors[0].status").value("400"))
            .andExpect(jsonPath("$.errors[0].meta.origin").value("bytechef"))
            .andExpect(jsonPath("$.triggerSubscriptionId").value(TRIGGER_SUBSCRIPTION_ID));
    }

    @Test
    void testEnableWithDisabledIntegrationInstanceReturns400() throws Exception {
        doThrow(
            new ConfigurationException(
                "Integration instance 5 is disabled", TriggerSubscriptionErrorType.USER_INPUT_ERROR))
                    .when(triggerSubscriptionFacade)
                    .enableTriggerSubscription(EXTERNAL_USER_ID, TRIGGER_SUBSCRIPTION_ID, true, Environment.PRODUCTION);

        mockMvc
            .perform(
                post(
                    "/v1/{externalUserId}/trigger-subscriptions/{id}/enable", EXTERNAL_USER_ID,
                    TRIGGER_SUBSCRIPTION_ID)
                        .with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].code").value("USER_INPUT_ERROR"))
            .andExpect(jsonPath("$.errors[0].detail").value("Integration instance 5 is disabled"));
    }

    private static TriggerSubscriptionDTO createTriggerSubscriptionDTO(
        TriggerSubscriptionStatus status, TriggerSubscriptionErrorCode errorCode, String errorMessage) {

        return new TriggerSubscriptionDTO(
            TRIGGER_SUBSCRIPTION_ID, "github", 1, "newIssue", "My subscription", "ext-1",
            Map.of("repository", "bytechef"), "https://example.com/hooks", status, errorCode, errorMessage,
            "https://bytechef.example.com/webhooks/abc", LAST_EVENT_DATE, "SUCCESS", null, CREATED_DATE,
            LAST_MODIFIED_DATE);
    }

    @ContextConfiguration(
        classes = Security.SecurityFilterChainTestConfiguration.class)
    @ExtendWith(SpringExtension.class)
    @TestPropertySource(properties = "openapi.openAPIDefinition.base-path.embedded=/api/embedded")
    @WebAppConfiguration
    static class Security {

        private static final String API_SECRET_KEY = String.valueOf(TenantKey.of("public"));
        private static final long CONNECTED_USER_ID = 7L;
        private static final String TRIGGERS_PATH = "/api/embedded/v1/%s/components/github/versions/1/triggers";

        @Autowired
        private ApiKeyService apiKeyService;

        @Autowired
        private ConnectedUserService connectedUserService;

        @Autowired
        private TriggerSubscriptionFacade triggerSubscriptionFacade;

        @Autowired
        private WebApplicationContext webApplicationContext;

        private MockMvc mockMvc;

        @BeforeEach
        void beforeEach() {
            reset(apiKeyService, connectedUserService, triggerSubscriptionFacade);

            mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        }

        @Test
        void testListingTriggersAuthenticatesTheConnectedUserOfThePath() throws Exception {
            mockEmbeddedApiKey();

            AtomicReference<String> principalNameReference = new AtomicReference<>();

            when(triggerSubscriptionFacade.getComponentTriggers(EXTERNAL_USER_ID, "github", 1))
                .thenAnswer(invocation -> {
                    principalNameReference.set(getPrincipalName());

                    return List.of(
                        new ComponentTriggerDTO(
                            "newIssue", "New Issue", null, "DYNAMIC_WEBHOOK", "{\"type\":\"object\"}", null, false));
                });

            mockMvc
                .perform(
                    get(TRIGGERS_PATH.formatted(EXTERNAL_USER_ID))
                        .header("Authorization", "Bearer " + API_SECRET_KEY)
                        .header("X-Environment", "PRODUCTION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("newIssue"));

            assertThat(principalNameReference.get()).isEqualTo(EXTERNAL_USER_ID);
        }

        @Test
        void testListingTriggersWithoutApiKeyIsRejected() throws Exception {
            mockMvc
                .perform(get(TRIGGERS_PATH.formatted(EXTERNAL_USER_ID)))
                .andExpect(status().is4xxClientError());

            verify(triggerSubscriptionFacade, never()).getComponentTriggers(anyString(), anyString(), anyInt());
        }

        @Test
        void testListingTriggersWithoutExternalUserIdIsRejected() throws Exception {
            mockEmbeddedApiKey();

            mockMvc
                .perform(
                    get("/api/embedded/v1/components/github/versions/1/triggers")
                        .header("Authorization", "Bearer " + API_SECRET_KEY))
                .andExpect(status().isUnauthorized());

            verify(connectedUserService, never()).createConnectedUser(anyString(), anyLong());
        }

        @Test
        void testListingSubscriptionsAuthenticatesTheConnectedUserOfThePath() throws Exception {
            mockEmbeddedApiKey();

            AtomicReference<String> principalNameReference = new AtomicReference<>();

            when(triggerSubscriptionFacade.getTriggerSubscriptions(EXTERNAL_USER_ID, Environment.PRODUCTION))
                .thenAnswer(invocation -> {
                    principalNameReference.set(getPrincipalName());

                    return List.of();
                });

            mockMvc
                .perform(
                    get("/api/embedded/v1/{externalUserId}/trigger-subscriptions", EXTERNAL_USER_ID)
                        .header("Authorization", "Bearer " + API_SECRET_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

            assertThat(principalNameReference.get()).isEqualTo(EXTERNAL_USER_ID);
        }

        @Test
        void testDisablingSubscriptionWithApiKeyNeedsNoCsrfToken() throws Exception {
            mockEmbeddedApiKey();

            mockMvc
                .perform(
                    post(
                        "/api/embedded/v1/{externalUserId}/trigger-subscriptions/{id}/disable", EXTERNAL_USER_ID, 3L)
                            .header("Authorization", "Bearer " + API_SECRET_KEY)
                            .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

            verify(triggerSubscriptionFacade).enableTriggerSubscription(
                EXTERNAL_USER_ID, 3L, false, Environment.PRODUCTION);
        }

        @Test
        void testAutomationApiKeyIsRejected() throws Exception {
            mockApiKey(PlatformType.AUTOMATION);

            mockMvc
                .perform(
                    get("/api/embedded/v1/{externalUserId}/trigger-subscriptions", EXTERNAL_USER_ID)
                        .header("Authorization", "Bearer " + API_SECRET_KEY))
                .andExpect(status().isUnauthorized());
        }

        private static String getPrincipalName() {
            Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

            return authentication == null ? null : authentication.getName();
        }

        private void mockApiKey(PlatformType platformType) {
            ApiKey apiKey = new ApiKey();

            apiKey.setId(1L);
            apiKey.setType(platformType);

            when(apiKeyService.fetchApiKey(API_SECRET_KEY, Environment.PRODUCTION.ordinal()))
                .thenReturn(Optional.of(apiKey));
        }

        private void mockEmbeddedApiKey() {
            mockApiKey(PlatformType.EMBEDDED);

            ConnectedUser connectedUser = new ConnectedUser();

            connectedUser.setEnabled(true);
            connectedUser.setEnvironment(Environment.PRODUCTION);
            connectedUser.setExternalId(EXTERNAL_USER_ID);

            ReflectionTestUtils.setField(connectedUser, "id", CONNECTED_USER_ID);

            when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_ID, Environment.PRODUCTION.ordinal()))
                .thenReturn(Optional.of(connectedUser));
        }

        @Configuration
        @EnableWebMvc
        @EnableWebSecurity
        static class SecurityFilterChainTestConfiguration {

            @Bean
            ApiKeyService apiKeyService() {
                return mock(ApiKeyService.class);
            }

            @Bean
            ConnectedUserService connectedUserService() {
                return mock(ConnectedUserService.class);
            }

            @Bean
            EnvironmentService environmentService() {
                return () -> List.of(Environment.values());
            }

            @Bean
            SecurityFilterChain securityFilterChain(
                HttpSecurity http, ApiKeyService apiKeyService, ConnectedUserService connectedUserService,
                EnvironmentService environmentService) throws Exception {

                return http
                    .authorizeHttpRequests(authorize -> authorize.anyRequest()
                        .authenticated())
                    .with(
                        new EmbeddedApiKeySecurityConfigurer(
                            apiKeyService, connectedUserService, environmentService, mock(JwtTokenService.class),
                            mock(SigningKeyService.class)),
                        Customizer.withDefaults())
                    .build();
            }

            @Bean
            TriggerSubscriptionApiController triggerSubscriptionApiController(
                EnvironmentService environmentService, TriggerSubscriptionFacade triggerSubscriptionFacade) {

                return new TriggerSubscriptionApiController(environmentService, triggerSubscriptionFacade);
            }

            @Bean
            TriggerSubscriptionFacade triggerSubscriptionFacade() {
                return mock(TriggerSubscriptionFacade.class);
            }
        }
    }
}
