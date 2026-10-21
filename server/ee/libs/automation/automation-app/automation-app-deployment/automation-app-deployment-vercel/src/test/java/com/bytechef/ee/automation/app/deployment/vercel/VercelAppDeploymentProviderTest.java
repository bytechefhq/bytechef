/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.deployment.vercel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.app.deployment.AppDeploymentSettingsException;
import com.bytechef.ee.automation.app.deployment.ProviderDeployment;
import com.bytechef.ee.automation.app.deployment.ProviderDeploymentRef;
import com.bytechef.ee.automation.app.deployment.ProviderDeploymentRequest;
import com.bytechef.ee.automation.app.domain.AppDeploymentStatus;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import java.util.HashMap;
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
class VercelAppDeploymentProviderTest {

    private static final long CONNECTION_ID = 42L;
    private static final String SECRET_TOKEN = "vercel_secret_token_123";

    private ActionDefinitionFacade actionDefinitionFacade;
    private ObjectProvider<ActionDefinitionFacade> actionDefinitionFacadeProvider;
    private VercelAppDeploymentProvider vercelAppDeploymentProvider;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() {
        actionDefinitionFacade = mock(ActionDefinitionFacade.class);
        actionDefinitionFacadeProvider = mock(ObjectProvider.class);

        when(actionDefinitionFacadeProvider.getIfAvailable()).thenReturn(actionDefinitionFacade);

        vercelAppDeploymentProvider = new VercelAppDeploymentProvider(actionDefinitionFacadeProvider);
    }

    @Test
    void testDeploymentConnectionMustBelongToTheVercelComponent() {
        assertThat(vercelAppDeploymentProvider.getDeploymentConnectionComponentName()).isEqualTo("vercel");
    }

    @Test
    void testHostingTargetIsVercel() {
        assertThat(vercelAppDeploymentProvider.getHostingTarget()).isEqualTo(AppHostingTarget.VERCEL);
    }

    @Test
    void testDeployRunsCreateDeploymentOnTheInstanceConnection() {
        stubAction(
            "createDeployment",
            deploymentOutput("dpl_1", "QUEUED", "https://web-abc.vercel.app", null, null));

        ProviderDeployment providerDeployment = vercelAppDeploymentProvider.deploy(
            new ProviderDeploymentRequest(
                CONNECTION_ID, settings("prj_1", "team_1", "acme/web", null), "main", "abc123",
                Environment.DEVELOPMENT));

        assertThat(providerDeployment.id()).isEqualTo("dpl_1");
        assertThat(providerDeployment.status()).isEqualTo(AppDeploymentStatus.QUEUED);
        assertThat(providerDeployment.url()).isEqualTo("https://web-abc.vercel.app");

        Map<String, Object> inputParameters = verifyAction("createDeployment", Environment.DEVELOPMENT);

        assertThat(inputParameters).containsOnly(
            Map.entry("project", "prj_1"), Map.entry("org", "acme"), Map.entry("repo", "web"),
            Map.entry("ref", "main"), Map.entry("sha", "abc123"), Map.entry("target", "preview"),
            Map.entry("teamId", "team_1"));
    }

    @Test
    void testDeployOmitsOptionalInputsThatAreAbsent() {
        stubAction("createDeployment", deploymentOutput("dpl_1", "QUEUED", null, null, null));

        vercelAppDeploymentProvider.deploy(
            new ProviderDeploymentRequest(
                CONNECTION_ID, settings("prj_1", null, "acme/web", null), "main", null, Environment.STAGING));

        Map<String, Object> inputParameters = verifyAction("createDeployment", Environment.STAGING);

        assertThat(inputParameters).containsOnly(
            Map.entry("project", "prj_1"), Map.entry("org", "acme"), Map.entry("repo", "web"),
            Map.entry("ref", "main"), Map.entry("target", "preview"));
    }

    @Test
    void testDeployRejectsSettingsWithoutProjectOrRepository() {
        assertThatThrownBy(() -> vercelAppDeploymentProvider.deploy(
            new ProviderDeploymentRequest(
                CONNECTION_ID, settings(null, null, "acme/web", null), "main", null, Environment.DEVELOPMENT)))
                    .isInstanceOf(AppDeploymentSettingsException.class)
                    .hasMessageContaining("projectId");

        assertThatThrownBy(() -> vercelAppDeploymentProvider.deploy(
            new ProviderDeploymentRequest(
                CONNECTION_ID, settings("prj_1", null, null, null), "main", null, Environment.DEVELOPMENT)))
                    .isInstanceOf(AppDeploymentSettingsException.class)
                    .hasMessageContaining("repository");

        assertThatThrownBy(() -> vercelAppDeploymentProvider.deploy(
            new ProviderDeploymentRequest(
                CONNECTION_ID, settings("prj_1", null, "web", null), "main", null, Environment.DEVELOPMENT)))
                    .isInstanceOf(AppDeploymentSettingsException.class)
                    .hasMessageContaining("owner/name");

        assertThatThrownBy(() -> vercelAppDeploymentProvider.deploy(
            new ProviderDeploymentRequest(
                CONNECTION_ID, settings("prj_1", null, "acme/web", null), null, "abc123", Environment.DEVELOPMENT)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("git reference");

        verifyNoInteractions(actionDefinitionFacade);
    }

    @Test
    void testProductionInstanceTargetsProduction() {
        stubAction("createDeployment", deploymentOutput("dpl_1", "QUEUED", null, null, null));

        vercelAppDeploymentProvider.deploy(
            new ProviderDeploymentRequest(
                CONNECTION_ID, settings("prj_1", null, "acme/web", null), "main", null, Environment.PRODUCTION));

        Map<String, Object> inputParameters = verifyAction("createDeployment", Environment.PRODUCTION);

        assertThat(inputParameters.get("target")).isEqualTo("production");
    }

    @Test
    void testProductionInstanceMayChoosePreview() {
        stubAction("createDeployment", deploymentOutput("dpl_1", "QUEUED", null, null, null));

        vercelAppDeploymentProvider.deploy(
            new ProviderDeploymentRequest(
                CONNECTION_ID, settings("prj_1", null, "acme/web", "preview"), "main", null,
                Environment.PRODUCTION));

        Map<String, Object> inputParameters = verifyAction("createDeployment", Environment.PRODUCTION);

        assertThat(inputParameters.get("target")).isEqualTo("preview");
    }

    @Test
    void testNonProductionInstanceNeverTargetsProduction() {
        for (Environment environment : new Environment[] {
            Environment.DEVELOPMENT, Environment.STAGING
        }) {
            assertThatThrownBy(() -> vercelAppDeploymentProvider.deploy(
                new ProviderDeploymentRequest(
                    CONNECTION_ID, settings("prj_1", null, "acme/web", "production"), "main", null, environment)))
                        .isInstanceOf(AppDeploymentSettingsException.class)
                        .hasMessageContaining("production");
        }

        assertThatThrownBy(() -> vercelAppDeploymentProvider.deploy(
            new ProviderDeploymentRequest(
                CONNECTION_ID, settings("prj_1", null, "acme/web", "staging"), "main", null,
                Environment.PRODUCTION)))
                    .isInstanceOf(AppDeploymentSettingsException.class)
                    .hasMessageContaining("target");

        verifyNoInteractions(actionDefinitionFacade);
    }

    @Test
    void testFailureReasonIsBuiltFromTheErrorCodeAndMessage() {
        stubAction("getDeployment", deploymentOutput("dpl_1", "ERROR", null, "BUILD_FAILED", null));

        ProviderDeploymentRef ref = new ProviderDeploymentRef(
            CONNECTION_ID, settings("prj_1", null, "acme/web", null), "dpl_1");

        assertThat(vercelAppDeploymentProvider.getDeployment(ref)
            .errorMessage()).isEqualTo("BUILD_FAILED");

        stubAction("getDeployment", deploymentOutput("dpl_1", "ERROR", null, null, "Build exceeded the time limit"));

        assertThat(vercelAppDeploymentProvider.getDeployment(ref)
            .errorMessage()).isEqualTo("Build exceeded the time limit");

        stubAction("getDeployment", deploymentOutput("dpl_1", "ERROR", null, "  ", null));

        assertThat(vercelAppDeploymentProvider.getDeployment(ref)
            .errorMessage()).isNull();
    }

    @Test
    void testValidateDeploymentSettingsAcceptsCompleteSettings() {
        vercelAppDeploymentProvider.validateDeploymentSettings(
            settings("prj_1", "team_1", "acme/web", "preview"), Environment.DEVELOPMENT);
        vercelAppDeploymentProvider.validateDeploymentSettings(
            settings("prj_1", null, "acme/web", "production"), Environment.PRODUCTION);
        vercelAppDeploymentProvider.validateDeploymentSettings(
            settings("prj_1", null, "acme/web.git", null), Environment.STAGING);

        verifyNoInteractions(actionDefinitionFacade);
    }

    @Test
    void testValidateDeploymentSettingsNamesTheKeyButNeverTheValue() {
        assertThatThrownBy(() -> vercelAppDeploymentProvider.validateDeploymentSettings(
            settings(null, null, "acme/web", null), Environment.DEVELOPMENT))
                .isInstanceOf(AppDeploymentSettingsException.class)
                .hasMessageContaining("projectId");

        assertThatThrownBy(() -> vercelAppDeploymentProvider.validateDeploymentSettings(
            settings("prj_1", null, null, null), Environment.DEVELOPMENT))
                .isInstanceOf(AppDeploymentSettingsException.class)
                .hasMessageContaining("repository");

        for (String malformedRepository : new String[] {
            "web", "/web", "acme/", "acme/web/extra", "acme/.git", " / "
        }) {
            assertThatThrownBy(() -> vercelAppDeploymentProvider.validateDeploymentSettings(
                settings("prj_1", null, malformedRepository, null), Environment.DEVELOPMENT))
                    .isInstanceOf(AppDeploymentSettingsException.class)
                    .hasMessageContaining("owner/name")
                    .hasMessageNotContainingAny("acme", "extra");
        }

        assertThatThrownBy(() -> vercelAppDeploymentProvider.validateDeploymentSettings(
            settings("prj_1", null, "acme/web", "secret-target"), Environment.PRODUCTION))
                .isInstanceOf(AppDeploymentSettingsException.class)
                .hasMessageContaining("target")
                .hasMessageNotContaining("secret-target");

        assertThatThrownBy(() -> vercelAppDeploymentProvider.validateDeploymentSettings(
            settings("prj_1", null, "acme/web", "production"), Environment.STAGING))
                .isInstanceOf(AppDeploymentSettingsException.class)
                .hasMessageContaining("production");
    }

    @Test
    void testDeployStripsATrailingGitFromTheRepositoryName() {
        stubAction("createDeployment", deploymentOutput("dpl_1", "QUEUED", null, null, null));

        vercelAppDeploymentProvider.deploy(
            new ProviderDeploymentRequest(
                CONNECTION_ID, settings("prj_1", null, "acme/web.git", null), "main", null, Environment.DEVELOPMENT));

        Map<String, Object> inputParameters = verifyAction("createDeployment", Environment.DEVELOPMENT);

        assertThat(inputParameters.get("org")).isEqualTo("acme");
        assertThat(inputParameters.get("repo")).isEqualTo("web");
    }

    @Test
    void testVercelStatesMapToDeploymentStatus() {
        assertThat(VercelAppDeploymentProvider.toDeploymentStatus("QUEUED")).isEqualTo(AppDeploymentStatus.QUEUED);
        assertThat(VercelAppDeploymentProvider.toDeploymentStatus("INITIALIZING"))
            .isEqualTo(AppDeploymentStatus.BUILDING);
        assertThat(VercelAppDeploymentProvider.toDeploymentStatus("BUILDING"))
            .isEqualTo(AppDeploymentStatus.BUILDING);
        assertThat(VercelAppDeploymentProvider.toDeploymentStatus("READY")).isEqualTo(AppDeploymentStatus.READY);
        assertThat(VercelAppDeploymentProvider.toDeploymentStatus("ERROR")).isEqualTo(AppDeploymentStatus.FAILED);
        assertThat(VercelAppDeploymentProvider.toDeploymentStatus("CANCELED"))
            .isEqualTo(AppDeploymentStatus.CANCELED);
        assertThat(VercelAppDeploymentProvider.toDeploymentStatus("DELETED"))
            .isEqualTo(AppDeploymentStatus.CANCELED);
        assertThat(VercelAppDeploymentProvider.toDeploymentStatus("ready")).isEqualTo(AppDeploymentStatus.READY);
        assertThat(VercelAppDeploymentProvider.toDeploymentStatus("SOMETHING_NEW"))
            .isEqualTo(AppDeploymentStatus.BUILDING);
        assertThat(VercelAppDeploymentProvider.toDeploymentStatus(null)).isEqualTo(AppDeploymentStatus.BUILDING);

        stubAction("getDeployment", deploymentOutput("dpl_1", "SOMETHING_NEW", null, null, null));

        ProviderDeployment providerDeployment = vercelAppDeploymentProvider.getDeployment(
            new ProviderDeploymentRef(CONNECTION_ID, settings("prj_1", null, "acme/web", null), "dpl_1"));

        assertThat(providerDeployment.status()).isEqualTo(AppDeploymentStatus.BUILDING);
        assertThat(providerDeployment.status()
            .isTerminal()).isFalse();
    }

    @Test
    void testGetDeploymentRunsGetDeployment() {
        stubAction("getDeployment", deploymentOutput("dpl_1", "READY", "https://web-abc.vercel.app", null, null));

        ProviderDeployment providerDeployment = vercelAppDeploymentProvider.getDeployment(
            new ProviderDeploymentRef(CONNECTION_ID, settings("prj_1", "team_1", "acme/web", null), "dpl_1"));

        assertThat(providerDeployment.id()).isEqualTo("dpl_1");
        assertThat(providerDeployment.status()).isEqualTo(AppDeploymentStatus.READY);
        assertThat(providerDeployment.url()).isEqualTo("https://web-abc.vercel.app");

        Map<String, Object> inputParameters = verifyAction("getDeployment", null);

        assertThat(inputParameters).containsOnly(Map.entry("deploymentId", "dpl_1"), Map.entry("teamId", "team_1"));
    }

    @Test
    void testCancelRunsCancelDeployment() {
        stubAction("cancelDeployment", deploymentOutput("dpl_1", "CANCELED", null, null, null));

        vercelAppDeploymentProvider.cancel(
            new ProviderDeploymentRef(CONNECTION_ID, settings("prj_1", null, "acme/web", null), "dpl_1"));

        Map<String, Object> inputParameters = verifyAction("cancelDeployment", null);

        assertThat(inputParameters).containsOnly(Map.entry("deploymentId", "dpl_1"));
    }

    @Test
    void testActionFailureBecomesFailedDeploymentWithMessageOnly() {
        stubAction(
            "createDeployment",
            deploymentOutput("dpl_1", "ERROR", null, "BUILD_FAILED", "Command \"npm run build\" exited with 1"));

        ProviderDeployment failedDeployment = vercelAppDeploymentProvider.deploy(
            new ProviderDeploymentRequest(
                CONNECTION_ID, settings("prj_1", null, "acme/web", null), "main", null, Environment.DEVELOPMENT));

        assertThat(failedDeployment.id()).isEqualTo("dpl_1");
        assertThat(failedDeployment.status()).isEqualTo(AppDeploymentStatus.FAILED);
        assertThat(failedDeployment.errorMessage())
            .isEqualTo("BUILD_FAILED: Command \"npm run build\" exited with 1");

        when(actionDefinitionFacade.executePerform(
            anyString(), anyInt(), eq("getDeployment"), any(), any(), any(), any(), any(), anyMap(), anyMap(),
            anyMap(), any(), any(), anyBoolean(), any(), any(), any()))
                .thenThrow(
                    new IllegalStateException(
                        "401 Unauthorized: Authorization: Bearer " + SECRET_TOKEN + " body={\"error\":\"forbidden\"}",
                        new RuntimeException(SECRET_TOKEN)));

        assertThatThrownBy(() -> vercelAppDeploymentProvider.getDeployment(
            new ProviderDeploymentRef(CONNECTION_ID, settings("prj_1", null, "acme/web", null), "dpl_1")))
                .isInstanceOf(VercelDeploymentException.class)
                .hasMessage("The Vercel getDeployment action failed")
                .hasNoCause()
                .satisfies(exception -> {
                    assertThat(exception.getSuppressed()).isEmpty();
                    assertThat(exception.toString()).doesNotContain(SECRET_TOKEN, "Bearer", "forbidden");
                });
    }

    @Test
    void testDeployFailsClearlyWithoutTheComponentRuntime() {
        when(actionDefinitionFacadeProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> vercelAppDeploymentProvider.deploy(
            new ProviderDeploymentRequest(
                CONNECTION_ID, settings("prj_1", null, "acme/web", null), "main", null, Environment.DEVELOPMENT)))
                    .isInstanceOf(VercelDeploymentException.class)
                    .hasMessageContaining("component runtime");
    }

    @Test
    void testDeployRejectsAnAnswerWithoutAnId() {
        stubAction("createDeployment", deploymentOutput(null, "QUEUED", null, null, null));

        assertThatThrownBy(() -> vercelAppDeploymentProvider.deploy(
            new ProviderDeploymentRequest(
                CONNECTION_ID, settings("prj_1", null, "acme/web", null), "main", null, Environment.DEVELOPMENT)))
                    .isInstanceOf(VercelDeploymentException.class)
                    .hasMessageContaining("without an id");
    }

    private static Map<String, Object> deploymentOutput(
        String id, String state, String url, String errorCode, String errorMessage) {

        Map<String, Object> output = new HashMap<>();

        output.put("id", id);
        output.put("url", url);
        output.put("state", state);
        output.put("errorCode", errorCode);
        output.put("errorMessage", errorMessage);

        return output;
    }

    private static Map<String, Object> settings(String projectId, String teamId, String repository, String target) {
        Map<String, Object> settings = new HashMap<>();

        if (projectId != null) {
            settings.put("projectId", projectId);
        }

        if (teamId != null) {
            settings.put("teamId", teamId);
        }

        if (repository != null) {
            settings.put("repository", repository);
        }

        if (target != null) {
            settings.put("target", target);
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

        verify(actionDefinitionFacade).executePerform(
            eq("vercel"), eq(1), eq(actionName), isNull(), isNull(), isNull(), isNull(), isNull(),
            inputParametersCaptor.capture(), eq(Map.of("vercel", CONNECTION_ID)), eq(Map.of()),
            environment == null ? isNull() : eq((long) environment.ordinal()), eq(PlatformType.AUTOMATION),
            eq(false), isNull(), isNull(), isNull());

        return inputParametersCaptor.getValue();
    }
}
