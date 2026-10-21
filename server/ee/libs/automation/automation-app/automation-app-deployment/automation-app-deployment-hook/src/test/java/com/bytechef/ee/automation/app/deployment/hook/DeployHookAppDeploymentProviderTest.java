/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.deployment.hook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.ee.automation.app.deployment.AppDeploymentSettingsException;
import com.bytechef.ee.automation.app.deployment.ProviderDeployment;
import com.bytechef.ee.automation.app.deployment.ProviderDeploymentRef;
import com.bytechef.ee.automation.app.deployment.ProviderDeploymentRequest;
import com.bytechef.ee.automation.app.domain.AppDeploymentStatus;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class DeployHookAppDeploymentProviderTest {

    private static final long CONNECTION_ID = 42L;
    private static final String DEPLOY_URL = "https://deploy.example.com/hooks/crm";
    private static final String SECRET_TOKEN = "deploy_hook_secret_token_123";

    private ActionDefinitionFacade actionDefinitionFacade;
    private ObjectProvider<ActionDefinitionFacade> actionDefinitionFacadeProvider;
    private DeployHookAppDeploymentProvider deployHookAppDeploymentProvider;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() {
        actionDefinitionFacade = mock(ActionDefinitionFacade.class);
        actionDefinitionFacadeProvider = mock(ObjectProvider.class);

        when(actionDefinitionFacadeProvider.getIfAvailable()).thenReturn(actionDefinitionFacade);

        deployHookAppDeploymentProvider = new DeployHookAppDeploymentProvider(actionDefinitionFacadeProvider);
    }

    @Test
    void testDeploymentConnectionMustBelongToTheHttpClientComponent() {
        assertThat(deployHookAppDeploymentProvider.getDeploymentConnectionComponentName()).isEqualTo("httpClient");
    }

    @Test
    void testHostingTargetIsSelfHosted() {
        assertThat(deployHookAppDeploymentProvider.getHostingTarget()).isEqualTo(AppHostingTarget.SELF_HOSTED);
    }

    @Test
    void testDeployPostsTheVersionToTheDeployUrlOnTheInstanceConnection() {
        stubAction("post", response(201, deploymentBody("dpl_1", "QUEUED", "https://crm.example.com", null)));

        ProviderDeployment providerDeployment = deployHookAppDeploymentProvider.deploy(
            new ProviderDeploymentRequest(
                CONNECTION_ID, settings(DEPLOY_URL, null, null), "release/1.2", "abc123", Environment.PRODUCTION,
                "1.2"));

        assertThat(providerDeployment.id()).isEqualTo("dpl_1");
        assertThat(providerDeployment.status()).isEqualTo(AppDeploymentStatus.QUEUED);
        assertThat(providerDeployment.url()).isEqualTo("https://crm.example.com");
        assertThat(providerDeployment.errorMessage()).isNull();

        Map<String, Object> inputParameters = verifyAction("post", Environment.PRODUCTION);

        assertThat(inputParameters).containsEntry("uri", DEPLOY_URL)
            .containsEntry("fullResponse", true)
            .containsEntry("responseType", "JSON")
            .containsEntry("followRedirect", false)
            .containsEntry("followAllRedirects", false)
            .containsEntry(
                "body",
                Map.of(
                    "bodyContentType", "JSON",
                    "bodyContent", Map.of(
                        "environment", "PRODUCTION", "gitRef", "release/1.2", "gitCommitSha", "abc123", "version",
                        "1.2")));
    }

    @Test
    void testDeployOmitsTheFieldsTheVersionDoesNotCarry() {
        stubAction("post", response(200, deploymentBody(7, "BUILDING", null, null)));

        ProviderDeployment providerDeployment = deployHookAppDeploymentProvider.deploy(
            new ProviderDeploymentRequest(
                CONNECTION_ID, settings(DEPLOY_URL, null, null), null, "abc123", Environment.STAGING));

        assertThat(providerDeployment.id()).isEqualTo("7");

        Map<String, Object> inputParameters = verifyAction("post", Environment.STAGING);

        assertThat(inputParameters.get("body")).isEqualTo(
            Map.of(
                "bodyContentType", "JSON",
                "bodyContent", Map.of("environment", "STAGING", "gitCommitSha", "abc123")));
    }

    @Test
    void testDeployNeedsAGitReferenceOrCommit() {
        assertThatThrownBy(() -> deployHookAppDeploymentProvider.deploy(
            new ProviderDeploymentRequest(
                CONNECTION_ID, settings(DEPLOY_URL, null, null), " ", null, Environment.PRODUCTION)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("git reference");

        verifyNoInteractions(actionDefinitionFacade);
    }

    @Test
    void testStatusAndCancelUseTheDefaultUrlsWithTheIdEncoded() {
        stubAction("get", response(200, deploymentBody("a b/c", "READY", null, null)));
        stubAction("post", response(204, null));

        ProviderDeploymentRef ref = new ProviderDeploymentRef(
            CONNECTION_ID, settings(DEPLOY_URL + "/", null, null), "a b/c");

        ProviderDeployment providerDeployment = deployHookAppDeploymentProvider.getDeployment(ref);

        assertThat(providerDeployment.id()).isEqualTo("a b/c");
        assertThat(providerDeployment.status()).isEqualTo(AppDeploymentStatus.READY);
        assertThat(verifyAction("get", null)).containsEntry("uri", DEPLOY_URL + "/a%20b%2Fc")
            .doesNotContainKey("body");

        deployHookAppDeploymentProvider.cancel(ref);

        assertThat(verifyAction("post", null)).containsEntry("uri", DEPLOY_URL + "/a%20b%2Fc/cancel")
            .containsEntry("body", Map.of("bodyContentType", "JSON", "bodyContent", Map.of()));
    }

    @Test
    void testDefaultUrlsKeepTheQueryOfTheDeployUrl() {
        stubAction("get", response(200, deploymentBody("dpl_1", "READY", null, null)));

        deployHookAppDeploymentProvider.getDeployment(
            new ProviderDeploymentRef(CONNECTION_ID, settings(DEPLOY_URL + "?app=crm", null, null), "dpl_1"));

        assertThat(verifyAction("get", null)).containsEntry("uri", DEPLOY_URL + "/dpl_1?app=crm");
    }

    @Test
    void testStatusAndCancelUseTheTemplatedUrls() {
        stubAction("get", response(200, deploymentBody("dpl_1", "BUILDING", null, null)));
        stubAction("post", response(202, Map.of()));

        ProviderDeploymentRef ref = new ProviderDeploymentRef(
            CONNECTION_ID,
            settings(
                DEPLOY_URL, "https://deploy.example.com:443/deployments/{id}?verbose=true",
                "HTTPS://DEPLOY.example.com/deployments/{id}/stop"),
            "dpl/1");

        deployHookAppDeploymentProvider.getDeployment(ref);

        assertThat(verifyAction("get", null))
            .containsEntry("uri", "https://deploy.example.com:443/deployments/dpl%2F1?verbose=true");

        deployHookAppDeploymentProvider.cancel(ref);

        assertThat(verifyAction("post", null)).containsEntry("uri",
            "HTTPS://DEPLOY.example.com/deployments/dpl%2F1/stop");
    }

    @Test
    void testStatusValuesMapToDeploymentStatus() {
        assertThat(DeployHookAppDeploymentProvider.toDeploymentStatus("QUEUED")).isEqualTo(AppDeploymentStatus.QUEUED);
        assertThat(DeployHookAppDeploymentProvider.toDeploymentStatus("building"))
            .isEqualTo(AppDeploymentStatus.BUILDING);
        assertThat(DeployHookAppDeploymentProvider.toDeploymentStatus("Ready")).isEqualTo(AppDeploymentStatus.READY);
        assertThat(DeployHookAppDeploymentProvider.toDeploymentStatus("FAILED")).isEqualTo(AppDeploymentStatus.FAILED);
        assertThat(DeployHookAppDeploymentProvider.toDeploymentStatus("CANCELED"))
            .isEqualTo(AppDeploymentStatus.CANCELED);
        assertThat(DeployHookAppDeploymentProvider.toDeploymentStatus("cancelled"))
            .isEqualTo(AppDeploymentStatus.CANCELED);
        assertThat(DeployHookAppDeploymentProvider.toDeploymentStatus("SOMETHING_NEW"))
            .isEqualTo(AppDeploymentStatus.BUILDING);
        assertThat(DeployHookAppDeploymentProvider.toDeploymentStatus(null)).isEqualTo(AppDeploymentStatus.BUILDING);

        Map<String, Object> body = new HashMap<>();

        body.put("id", "dpl_1");
        body.put("status", List.of("READY"));

        stubAction("get", response(200, body));

        ProviderDeployment providerDeployment = deployHookAppDeploymentProvider.getDeployment(
            new ProviderDeploymentRef(CONNECTION_ID, settings(DEPLOY_URL, null, null), "dpl_1"));

        assertThat(providerDeployment.status()).isEqualTo(AppDeploymentStatus.BUILDING);
        assertThat(providerDeployment.status()
            .isTerminal()).isFalse();
    }

    @Test
    void testUrlIsDroppedUnlessItIsAnAbsoluteHttpUrl() {
        ProviderDeploymentRef ref = new ProviderDeploymentRef(CONNECTION_ID, settings(DEPLOY_URL, null, null), "d1");

        for (String rejectedUrl : new String[] {
            "javascript:alert(1)", "/relative/path", "ftp://crm.example.com", "https://user:pass@crm.example.com",
            "crm.example.com", "https://" + "a".repeat(2100) + ".example.com"
        }) {
            stubAction("get", response(200, deploymentBody("d1", "READY", rejectedUrl, null)));

            assertThat(deployHookAppDeploymentProvider.getDeployment(ref)
                .url()).isNull();
        }

        stubAction("get", response(200, deploymentBody("d1", "READY", "http://crm.internal:8080/app", null)));

        assertThat(deployHookAppDeploymentProvider.getDeployment(ref)
            .url()).isEqualTo("http://crm.internal:8080/app");

        Map<String, Object> body = new HashMap<>();

        body.put("id", "d1");
        body.put("status", "READY");
        body.put("url", 42);

        stubAction("get", response(200, body));

        assertThat(deployHookAppDeploymentProvider.getDeployment(ref)
            .url()).isNull();
    }

    @Test
    void testErrorMessageIsPassedThroughOnlyForFailed() {
        ProviderDeploymentRef ref = new ProviderDeploymentRef(CONNECTION_ID, settings(DEPLOY_URL, null, null), "d1");

        stubAction("get", response(200, deploymentBody("d1", "FAILED", null, "Exit code 1")));

        assertThat(deployHookAppDeploymentProvider.getDeployment(ref)
            .errorMessage()).isEqualTo("Exit code 1");

        for (String status : new String[] {
            "READY", "BUILDING", "CANCELED", "QUEUED", "UNKNOWN"
        }) {
            stubAction("get", response(200, deploymentBody("d1", status, null, "leaked detail")));

            assertThat(deployHookAppDeploymentProvider.getDeployment(ref)
                .errorMessage()).isNull();
        }
    }

    @Test
    void testStatusAnswerKeepsTheStoredId() {
        stubAction("get", response(200, deploymentBody("other", "READY", null, null)));

        ProviderDeployment providerDeployment = deployHookAppDeploymentProvider.getDeployment(
            new ProviderDeploymentRef(CONNECTION_ID, settings(DEPLOY_URL, null, null), "d1"));

        assertThat(providerDeployment.id()).isEqualTo("d1");
    }

    @Test
    void testAMapShapedResponseIsUnderstood() {
        stubAction(
            "get",
            Map.of("statusCode", 200, "body", deploymentBody("d1", "READY", null, null), "headers", Map.of()));

        assertThat(deployHookAppDeploymentProvider.getDeployment(
            new ProviderDeploymentRef(CONNECTION_ID, settings(DEPLOY_URL, null, null), "d1"))
            .status()).isEqualTo(AppDeploymentStatus.READY);

        stubAction("get", Map.of("statusCode", 500, "body", deploymentBody("d1", "READY", null, null)));

        assertThatThrownBy(() -> deployHookAppDeploymentProvider.getDeployment(
            new ProviderDeploymentRef(CONNECTION_ID, settings(DEPLOY_URL, null, null), "d1")))
                .isInstanceOf(DeployHookDeploymentException.class);
    }

    @Test
    void testUnusableAnswersBecomeACredentialFreeFailure() {
        Map<String, Object> secretBody = deploymentBody("d1", "READY", null, SECRET_TOKEN);

        assertFailure(
            "post", response(500, secretBody), "The deploy hook deploy request was not answered with a 2xx status");
        assertFailure(
            "post", response(302, secretBody), "The deploy hook deploy request was not answered with a 2xx status");
        assertFailure(
            "post", response(200, List.of(secretBody)),
            "The deploy hook deploy request was not answered with a JSON object");
        assertFailure(
            "post", response(200, SECRET_TOKEN), "The deploy hook deploy request was not answered with a JSON object");
        assertFailure(
            "post", response(200, null), "The deploy hook deploy request was not answered with a JSON object");
        assertFailure("post", SECRET_TOKEN, "The deploy hook deploy request was not answered with a JSON object");
        assertFailure(
            "post", response(200, deploymentBody(null, "QUEUED", null, SECRET_TOKEN)),
            "The deploy hook answered the deployment request without an id");
        assertFailure(
            "post", response(200, deploymentBody(Map.of("value", SECRET_TOKEN), "QUEUED", null, null)),
            "The deploy hook answered the deployment request without an id");
        assertFailure(
            "post", response(200, deploymentBody("..", "QUEUED", null, null)),
            "The deploy hook answered the deployment request without an id");
        assertFailure(
            "post", response(200, deploymentBody("x".repeat(257), "QUEUED", null, null)),
            "The deploy hook answered the deployment request without an id");
    }

    @Test
    void testActionFailureBecomesACredentialFreeFailure() {
        when(actionDefinitionFacade.executePerform(
            anyString(), anyInt(), eq("get"), any(), any(), any(), any(), any(), anyMap(), anyMap(), anyMap(), any(),
            any(), anyBoolean(), any(), any(), any()))
                .thenThrow(
                    new IllegalStateException(
                        "401 Unauthorized: Authorization: Bearer " + SECRET_TOKEN + " body={\"error\":\"forbidden\"}",
                        new RuntimeException(SECRET_TOKEN)));

        assertThatThrownBy(() -> deployHookAppDeploymentProvider.getDeployment(
            new ProviderDeploymentRef(CONNECTION_ID, settings(DEPLOY_URL, null, null), "d1")))
                .isInstanceOf(DeployHookDeploymentException.class)
                .hasMessage("The deploy hook status request failed")
                .hasNoCause()
                .satisfies(exception -> {
                    assertThat(exception.getSuppressed()).isEmpty();
                    assertThat(exception.toString()).doesNotContain(SECRET_TOKEN, "Bearer", "forbidden");
                });
    }

    @Test
    void testCancelFailsOnANon2xxAnswer() {
        stubAction("post", response(404, Map.of("error", SECRET_TOKEN)));

        assertThatThrownBy(() -> deployHookAppDeploymentProvider.cancel(
            new ProviderDeploymentRef(CONNECTION_ID, settings(DEPLOY_URL, null, null), "d1")))
                .isInstanceOf(DeployHookDeploymentException.class)
                .hasMessage("The deploy hook cancel request was not answered with a 2xx status")
                .hasNoCause();
    }

    @Test
    void testDeployFailsClearlyWithoutTheComponentRuntime() {
        when(actionDefinitionFacadeProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> deployHookAppDeploymentProvider.deploy(
            new ProviderDeploymentRequest(
                CONNECTION_ID, settings(DEPLOY_URL, null, null), "main", null, Environment.PRODUCTION)))
                    .isInstanceOf(DeployHookDeploymentException.class)
                    .hasMessageContaining("component runtime");
    }

    @Test
    void testValidateDeploymentSettingsAcceptsCompleteSettings() {
        deployHookAppDeploymentProvider.validateDeploymentSettings(
            settings(DEPLOY_URL, null, null), Environment.PRODUCTION);
        deployHookAppDeploymentProvider.validateDeploymentSettings(
            settings("HTTPS://deploy.example.com", "https://deploy.example.com/s/{id}",
                "https://deploy.example.com/{id}/c"),
            Environment.STAGING);
        deployHookAppDeploymentProvider.validateDeploymentSettings(
            settings("http://agent.local.test:8787", "http://agent.local.test:8787/{id}", null),
            Environment.DEVELOPMENT);

        verifyNoInteractions(actionDefinitionFacade);
    }

    @Test
    void testHttpIsAllowedOnlyForDevelopment() {
        for (Environment environment : new Environment[] {
            Environment.STAGING, Environment.PRODUCTION
        }) {
            assertSettingsRejected(settings("http://agent.local.test:8787", null, null), environment, "deployUrl",
                "https");
            assertSettingsRejected(
                settings(DEPLOY_URL, "http://agent.local.test:8787/{id}", null), environment, "statusUrl", "https");
            assertSettingsRejected(
                settings(DEPLOY_URL, null, "http://agent.local.test:8787/{id}/cancel"), environment, "cancelUrl",
                "https");
        }
    }

    @Test
    void testValidateDeploymentSettingsNamesTheKeyButNeverTheValue() {
        assertSettingsRejected(settings(null, null, null), Environment.DEVELOPMENT, "deployUrl", "required");
        assertSettingsRejected(settings("  ", null, null), Environment.DEVELOPMENT, "deployUrl", "required");

        for (String malformedUrl : new String[] {
            "secret-host.example.com/deploy", "ftp://secret-host.example.com", "https://user:secret@host.example.com",
            "https://secret-host.example.com/deploy#frag", "https:///secret-path", "mailto:secret@example.com",
            "https://secret host.example.com"
        }) {
            assertSettingsRejected(settings(malformedUrl, null, null), Environment.DEVELOPMENT, "deployUrl", null);
            assertSettingsRejected(
                settings(DEPLOY_URL, malformedUrl + "/{id}", null), Environment.DEVELOPMENT, "statusUrl", null);
        }

        assertSettingsRejected(
            settings(DEPLOY_URL, "https://secret-host.example.com/status", null), Environment.DEVELOPMENT, "statusUrl",
            "{id}");
        assertSettingsRejected(
            settings(DEPLOY_URL, null, "https://secret-host.example.com/cancel"), Environment.DEVELOPMENT, "cancelUrl",
            "{id}");
        assertSettingsRejected(
            settings(DEPLOY_URL, "https://{id}.secret-host.example.com/status", null), Environment.DEVELOPMENT,
            "statusUrl", "{id}");

        Map<String, Object> numericSettings = new HashMap<>();

        numericSettings.put("deployUrl", 12345);

        assertSettingsRejected(numericSettings, Environment.DEVELOPMENT, "deployUrl", null);

        verifyNoInteractions(actionDefinitionFacade);
    }

    @Test
    void testStatusAndCancelUrlsMustShareTheOriginOfTheDeployUrl() {
        for (String otherOrigin : new String[] {
            "https://secret-other.example.com/{id}", "https://deploy.example.com:8443/{id}",
            "https://secret.deploy.example.com/{id}"
        }) {
            assertSettingsRejected(
                settings(DEPLOY_URL, otherOrigin, null), Environment.PRODUCTION, "statusUrl", "deployUrl");
            assertSettingsRejected(
                settings(DEPLOY_URL, null, otherOrigin), Environment.PRODUCTION, "cancelUrl", "deployUrl");
        }

        assertSettingsRejected(
            settings("http://agent.local.test:8787", "https://agent.local.test:8787/{id}", null),
            Environment.DEVELOPMENT, "statusUrl", "deployUrl");
        assertSettingsRejected(
            settings("http://agent.local.test", "http://agent.local.test:8080/{id}", null), Environment.DEVELOPMENT,
            "statusUrl", "deployUrl");

        deployHookAppDeploymentProvider.validateDeploymentSettings(
            settings("http://agent.local.test", "http://agent.local.test:80/{id}", null), Environment.DEVELOPMENT);

        assertThatThrownBy(() -> deployHookAppDeploymentProvider.getDeployment(
            new ProviderDeploymentRef(
                CONNECTION_ID, settings(DEPLOY_URL, "https://secret-other.example.com/{id}", null), "d1")))
                    .isInstanceOf(AppDeploymentSettingsException.class)
                    .hasMessageContaining("statusUrl");
        assertThatThrownBy(() -> deployHookAppDeploymentProvider.cancel(
            new ProviderDeploymentRef(
                CONNECTION_ID, settings(DEPLOY_URL, null, "https://secret-other.example.com/{id}"), "d1")))
                    .isInstanceOf(AppDeploymentSettingsException.class)
                    .hasMessageContaining("cancelUrl");

        verifyNoInteractions(actionDefinitionFacade);
    }

    @Test
    void testAnyJsonIntegerIsAnId() {
        stubAction("post", response(200, deploymentBody(new BigInteger("123456789012345678901234567890"), "QUEUED",
            null, null)));

        assertThat(deployHookAppDeploymentProvider.deploy(
            new ProviderDeploymentRequest(
                CONNECTION_ID, settings(DEPLOY_URL, null, null), "main", null, Environment.PRODUCTION))
            .id()).isEqualTo("123456789012345678901234567890");

        stubAction("post", response(200, deploymentBody(9_000_000_000L, "QUEUED", null, null)));

        assertThat(deployHookAppDeploymentProvider.deploy(
            new ProviderDeploymentRequest(
                CONNECTION_ID, settings(DEPLOY_URL, null, null), "main", null, Environment.PRODUCTION))
            .id()).isEqualTo("9000000000");

        assertFailure(
            "post", response(200, deploymentBody(7.5, "QUEUED", null, null)),
            "The deploy hook answered the deployment request without an id");
        assertFailure(
            "post", response(200, deploymentBody(new BigDecimal("7.25"), "QUEUED", null, null)),
            "The deploy hook answered the deployment request without an id");
        assertFailure(
            "post", response(200, deploymentBody(true, "QUEUED", null, null)),
            "The deploy hook answered the deployment request without an id");
    }

    @Test
    void testStatusAnsweredNotFoundFailsTheDeployment() {
        ProviderDeploymentRef ref = new ProviderDeploymentRef(CONNECTION_ID, settings(DEPLOY_URL, null, null), "d1");

        stubAction("get", response(404, Map.of("error", SECRET_TOKEN)));

        ProviderDeployment providerDeployment = deployHookAppDeploymentProvider.getDeployment(ref);

        assertThat(providerDeployment.id()).isEqualTo("d1");
        assertThat(providerDeployment.status()).isEqualTo(AppDeploymentStatus.FAILED);
        assertThat(providerDeployment.url()).isNull();
        assertThat(providerDeployment.errorMessage())
            .isEqualTo("The deploy endpoint no longer knows this deployment");

        when(actionDefinitionFacade.executePerform(
            anyString(), anyInt(), eq("get"), any(), any(), any(), any(), any(), anyMap(), anyMap(), anyMap(), any(),
            any(), anyBoolean(), any(), any(), any()))
                .thenThrow(new ProviderException.NotFoundException("404: " + SECRET_TOKEN))
                .thenThrow(new IllegalStateException(new ProviderException(404, "404: " + SECRET_TOKEN)))
                .thenThrow(new ProviderException(500, "500: " + SECRET_TOKEN));

        assertThat(deployHookAppDeploymentProvider.getDeployment(ref)
            .errorMessage()).isEqualTo("The deploy endpoint no longer knows this deployment");
        assertThat(deployHookAppDeploymentProvider.getDeployment(ref)
            .status()).isEqualTo(AppDeploymentStatus.FAILED);
        assertThatThrownBy(() -> deployHookAppDeploymentProvider.getDeployment(ref))
            .isInstanceOf(DeployHookDeploymentException.class)
            .hasMessage("The deploy hook status request failed")
            .hasNoCause();
    }

    @Test
    void testDeployAnsweredNotFoundStaysAFailure() {
        assertFailure(
            "post", response(404, Map.of()), "The deploy hook deploy request was not answered with a 2xx status");
    }

    @Test
    void testDeployValidatesTheSettingsAgain() {
        assertThatThrownBy(() -> deployHookAppDeploymentProvider.deploy(
            new ProviderDeploymentRequest(
                CONNECTION_ID, settings("http://deploy.example.com", null, null), "main", null,
                Environment.PRODUCTION)))
                    .isInstanceOf(AppDeploymentSettingsException.class)
                    .hasMessageContaining("deployUrl");

        verifyNoInteractions(actionDefinitionFacade);
    }

    @Test
    void testStatusRejectsAStructurallyInvalidTemplate() {
        assertThatThrownBy(() -> deployHookAppDeploymentProvider.getDeployment(
            new ProviderDeploymentRef(
                CONNECTION_ID, settings(DEPLOY_URL, "https://deploy.example.com/no-placeholder", null), "d1")))
                    .isInstanceOf(AppDeploymentSettingsException.class)
                    .hasMessageContaining("statusUrl");

        verifyNoInteractions(actionDefinitionFacade);
    }

    private void assertFailure(String actionName, Object output, String expectedMessage) {
        stubAction(actionName, output);

        assertThatThrownBy(() -> deployHookAppDeploymentProvider.deploy(
            new ProviderDeploymentRequest(
                CONNECTION_ID, settings(DEPLOY_URL, null, null), "main", null, Environment.PRODUCTION)))
                    .isInstanceOf(DeployHookDeploymentException.class)
                    .hasMessage(expectedMessage)
                    .hasNoCause()
                    .satisfies(exception -> assertThat(exception.toString()).doesNotContain(SECRET_TOKEN));
    }

    private void assertSettingsRejected(
        Map<String, ?> settings, Environment environment, String expectedKey, String expectedText) {

        assertThatThrownBy(() -> deployHookAppDeploymentProvider.validateDeploymentSettings(settings, environment))
            .isInstanceOf(AppDeploymentSettingsException.class)
            .hasMessageContaining(expectedKey)
            .satisfies(exception -> {
                if (expectedText != null) {
                    assertThat(exception.getMessage()).contains(expectedText);
                }

                assertThat(exception.getMessage()).doesNotContain(
                    "secret", "agent.local.test", "deploy.example.com", "12345");
            });
    }

    private static Map<String, Object> deploymentBody(Object id, String status, String url, String errorMessage) {
        Map<String, Object> body = new HashMap<>();

        body.put("id", id);
        body.put("status", status);
        body.put("url", url);
        body.put("errorMessage", errorMessage);

        return body;
    }

    private static Context.Http.Response response(int statusCode, Object body) {
        return new TestResponse(statusCode, body);
    }

    private static Map<String, Object> settings(String deployUrl, String statusUrl, String cancelUrl) {
        Map<String, Object> settings = new HashMap<>();

        if (deployUrl != null) {
            settings.put("deployUrl", deployUrl);
        }

        if (statusUrl != null) {
            settings.put("statusUrl", statusUrl);
        }

        if (cancelUrl != null) {
            settings.put("cancelUrl", cancelUrl);
        }

        return settings;
    }

    private void stubAction(String actionName, Object output) {
        when(actionDefinitionFacade.executePerform(
            anyString(), anyInt(), eq(actionName), any(), any(), any(), any(), any(), anyMap(), anyMap(), anyMap(),
            any(), any(), anyBoolean(), any(), any(), any()))
                .thenReturn(output);
    }

    @SuppressWarnings({
        "unchecked", "rawtypes"
    })
    private Map<String, Object> verifyAction(String actionName, Environment environment) {
        ArgumentCaptor<Map> inputParametersCaptor = ArgumentCaptor.forClass(Map.class);

        verify(actionDefinitionFacade, atLeastOnce()).executePerform(
            eq("httpClient"), eq(2), eq(actionName), isNull(), isNull(), isNull(), isNull(), isNull(),
            inputParametersCaptor.capture(), eq(Map.of("httpClient", CONNECTION_ID)), eq(Map.of()),
            environment == null ? isNull() : eq((long) environment.ordinal()), eq(PlatformType.AUTOMATION),
            eq(false), isNull(), isNull(), isNull());

        return inputParametersCaptor.getValue();
    }

    private record TestResponse(int statusCode, Object body) implements Context.Http.Response {

        @Override
        public Map<String, List<String>> getHeaders() {
            return Map.of("Authorization", List.of("Bearer " + SECRET_TOKEN));
        }

        @Override
        public Object getBody() {
            return body;
        }

        @Override
        public <T> T getBody(Class<T> valueType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T getBody(TypeReference<T> valueTypeRef) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getFirstHeader(String name) {
            return null;
        }

        @Override
        public List<String> getHeader(String name) {
            return List.of();
        }

        @Override
        public int getStatusCode() {
            return statusCode;
        }
    }
}
