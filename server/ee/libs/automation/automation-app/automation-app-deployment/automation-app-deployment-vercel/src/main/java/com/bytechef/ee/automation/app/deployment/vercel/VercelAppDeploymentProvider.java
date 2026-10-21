/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.deployment.vercel;

import com.bytechef.ee.automation.app.deployment.AppDeploymentProvider;
import com.bytechef.ee.automation.app.deployment.AppDeploymentSettingsException;
import com.bytechef.ee.automation.app.deployment.ProviderDeployment;
import com.bytechef.ee.automation.app.deployment.ProviderDeploymentRef;
import com.bytechef.ee.automation.app.deployment.ProviderDeploymentRequest;
import com.bytechef.ee.automation.app.domain.AppDeploymentStatus;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.util.AppLogValues;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class VercelAppDeploymentProvider implements AppDeploymentProvider {

    public static final String SETTINGS_PROJECT_ID = "projectId";
    public static final String SETTINGS_REPOSITORY = "repository";
    public static final String SETTINGS_TARGET = "target";
    public static final String SETTINGS_TEAM_ID = "teamId";

    public static final String TARGET_PREVIEW = "preview";
    public static final String TARGET_PRODUCTION = "production";

    static final String CANCEL_DEPLOYMENT_ACTION = "cancelDeployment";
    static final String COMPONENT_NAME = "vercel";
    static final int COMPONENT_VERSION = 1;
    static final String CREATE_DEPLOYMENT_ACTION = "createDeployment";
    static final String GET_DEPLOYMENT_ACTION = "getDeployment";

    private static final String INPUT_DEPLOYMENT_ID = "deploymentId";
    private static final String INPUT_ORG = "org";
    private static final String INPUT_PROJECT = "project";
    private static final String INPUT_REF = "ref";
    private static final String INPUT_REPO = "repo";
    private static final String INPUT_SHA = "sha";
    private static final String INPUT_TARGET = "target";
    private static final String INPUT_TEAM_ID = "teamId";

    private static final int MAX_LOGGED_STATE_LENGTH = 40;

    private static final String GIT_SUFFIX = ".git";

    private static final String OUTPUT_ERROR_CODE = "errorCode";
    private static final String OUTPUT_ERROR_MESSAGE = "errorMessage";
    private static final String OUTPUT_ID = "id";
    private static final String OUTPUT_STATE = "state";
    private static final String OUTPUT_URL = "url";

    private static final Logger log = LoggerFactory.getLogger(VercelAppDeploymentProvider.class);

    private final ObjectProvider<ActionDefinitionFacade> actionDefinitionFacadeProvider;

    @SuppressFBWarnings("EI")
    public VercelAppDeploymentProvider(ObjectProvider<ActionDefinitionFacade> actionDefinitionFacadeProvider) {
        this.actionDefinitionFacadeProvider = actionDefinitionFacadeProvider;
    }

    static AppDeploymentStatus toDeploymentStatus(@Nullable String readyState) {
        if (readyState == null) {
            return AppDeploymentStatus.BUILDING;
        }

        return switch (readyState.toUpperCase(Locale.ROOT)) {
            case "QUEUED" -> AppDeploymentStatus.QUEUED;
            case "INITIALIZING", "BUILDING" -> AppDeploymentStatus.BUILDING;
            case "READY" -> AppDeploymentStatus.READY;
            case "ERROR" -> AppDeploymentStatus.FAILED;
            case "CANCELED", "DELETED" -> AppDeploymentStatus.CANCELED;
            default -> {
                log.warn(
                    "Vercel reported the unknown deployment state {}; treating it as building",
                    AppLogValues.toLoggableValue(readyState, MAX_LOGGED_STATE_LENGTH));

                yield AppDeploymentStatus.BUILDING;
            }
        };
    }

    @Override
    public void cancel(ProviderDeploymentRef ref) {
        executeAction(
            CANCEL_DEPLOYMENT_ACTION, toDeploymentInputParameters(ref), ref.connectionId(), null);
    }

    @Override
    public ProviderDeployment deploy(ProviderDeploymentRequest request) {
        Map<String, ?> deploymentSettings = request.deploymentSettings();

        Environment environment = request.environment();

        validateDeploymentSettings(deploymentSettings, environment);

        String projectId = getRequiredSetting(deploymentSettings, SETTINGS_PROJECT_ID);
        String[] repositoryParts = splitRepository(getRequiredSetting(deploymentSettings, SETTINGS_REPOSITORY));

        String gitRef = StringUtils.trimToNull(request.gitRef());

        if (gitRef == null) {
            throw new IllegalArgumentException("A Vercel deployment needs an app version with a git reference");
        }

        Map<String, Object> inputParameters = new HashMap<>();

        inputParameters.put(INPUT_PROJECT, projectId);
        inputParameters.put(INPUT_ORG, repositoryParts[0]);
        inputParameters.put(INPUT_REPO, repositoryParts[1]);
        inputParameters.put(INPUT_REF, gitRef);
        inputParameters.put(INPUT_TARGET, resolveTarget(deploymentSettings, environment));

        String gitCommitSha = StringUtils.trimToNull(request.gitCommitSha());

        if (gitCommitSha != null) {
            inputParameters.put(INPUT_SHA, gitCommitSha);
        }

        putTeamId(inputParameters, deploymentSettings);

        Map<?, ?> output = executeAction(
            CREATE_DEPLOYMENT_ACTION, inputParameters, request.connectionId(), environment);

        String providerDeploymentId = getString(output, OUTPUT_ID);

        if (providerDeploymentId == null) {
            throw new VercelDeploymentException("Vercel answered the deployment request without an id");
        }

        return toProviderDeployment(providerDeploymentId, output);
    }

    @Override
    public ProviderDeployment getDeployment(ProviderDeploymentRef ref) {
        Map<?, ?> output = executeAction(
            GET_DEPLOYMENT_ACTION, toDeploymentInputParameters(ref), ref.connectionId(), null);

        String providerDeploymentId = getString(output, OUTPUT_ID);

        return toProviderDeployment(
            providerDeploymentId == null ? ref.providerDeploymentId() : providerDeploymentId, output);
    }

    @Override
    public String getDeploymentConnectionComponentName() {
        return COMPONENT_NAME;
    }

    @Override
    public AppHostingTarget getHostingTarget() {
        return AppHostingTarget.VERCEL;
    }

    @Override
    public void validateDeploymentSettings(Map<String, ?> deploymentSettings, Environment environment) {
        getRequiredSetting(deploymentSettings, SETTINGS_PROJECT_ID);
        splitRepository(getRequiredSetting(deploymentSettings, SETTINGS_REPOSITORY));
        resolveTarget(deploymentSettings, environment);
    }

    private Map<?, ?> executeAction(
        String actionName, Map<String, Object> inputParameters, long connectionId,
        @Nullable Environment environment) {

        ActionDefinitionFacade actionDefinitionFacade = actionDefinitionFacadeProvider.getIfAvailable();

        if (actionDefinitionFacade == null) {
            throw new VercelDeploymentException(
                "The Vercel deployment provider needs the component runtime, which this application does not run");
        }

        Long environmentId = environment == null ? null : (long) environment.ordinal();

        Object output;

        try {
            output = SecurityUtils.runAsSystem(() -> actionDefinitionFacade.executePerform(
                COMPONENT_NAME, COMPONENT_VERSION, actionName, null, null, null, null, null, inputParameters,
                Map.of(COMPONENT_NAME, connectionId), Map.of(), environmentId, PlatformType.AUTOMATION, false, null,
                null, null));
        } catch (RuntimeException exception) {
            log.warn("The Vercel {} action failed ({})", actionName, exception.getClass()
                .getName());

            throw new VercelDeploymentException("The Vercel %s action failed".formatted(actionName));
        }

        if (output instanceof Map<?, ?> outputMap) {
            return outputMap;
        }

        return Map.of();
    }

    private static String getRequiredSetting(Map<String, ?> deploymentSettings, String key) {
        String value = getString(deploymentSettings, key);

        if (value == null) {
            throw new AppDeploymentSettingsException(
                "The Vercel deployment setting '%s' is required".formatted(key));
        }

        return value;
    }

    private static @Nullable String getString(Map<?, ?> values, String key) {
        Object value = values.get(key);

        if (value == null) {
            return null;
        }

        return StringUtils.trimToNull(value.toString());
    }

    private static void putTeamId(Map<String, Object> inputParameters, Map<String, ?> deploymentSettings) {
        String teamId = getString(deploymentSettings, SETTINGS_TEAM_ID);

        if (teamId != null) {
            inputParameters.put(INPUT_TEAM_ID, teamId);
        }
    }

    private static String resolveTarget(Map<String, ?> deploymentSettings, Environment environment) {
        String target = getString(deploymentSettings, SETTINGS_TARGET);

        if (target == null) {
            return environment == Environment.PRODUCTION ? TARGET_PRODUCTION : TARGET_PREVIEW;
        }

        String normalizedTarget = target.toLowerCase(Locale.ROOT);

        if (!normalizedTarget.equals(TARGET_PRODUCTION) && !normalizedTarget.equals(TARGET_PREVIEW)) {
            throw new AppDeploymentSettingsException(
                "The Vercel deployment setting '%s' must be '%s' or '%s'".formatted(
                    SETTINGS_TARGET, TARGET_PRODUCTION, TARGET_PREVIEW));
        }

        if (normalizedTarget.equals(TARGET_PRODUCTION) && environment != Environment.PRODUCTION) {
            throw new AppDeploymentSettingsException(
                "Only a PRODUCTION app instance may deploy to the Vercel production target");
        }

        return normalizedTarget;
    }

    private static String[] splitRepository(String repository) {
        String repositoryOwner = StringUtils.trimToEmpty(StringUtils.substringBefore(repository, "/"));
        String repositoryName = StringUtils.trimToEmpty(StringUtils.substringAfter(repository, "/"));

        if (repositoryName.endsWith(GIT_SUFFIX)) {
            repositoryName = repositoryName.substring(0, repositoryName.length() - GIT_SUFFIX.length());
        }

        if (repositoryOwner.isEmpty() || repositoryName.isEmpty() || repositoryName.contains("/")) {
            throw new AppDeploymentSettingsException(
                "The Vercel deployment setting '%s' must be a repository in owner/name form".formatted(
                    SETTINGS_REPOSITORY));
        }

        return new String[] {
            repositoryOwner, repositoryName
        };
    }

    private static Map<String, Object> toDeploymentInputParameters(ProviderDeploymentRef ref) {
        Map<String, Object> inputParameters = new HashMap<>();

        inputParameters.put(INPUT_DEPLOYMENT_ID, ref.providerDeploymentId());

        putTeamId(inputParameters, ref.deploymentSettings());

        return inputParameters;
    }

    private static ProviderDeployment toProviderDeployment(String providerDeploymentId, Map<?, ?> output) {
        return new ProviderDeployment(
            providerDeploymentId, toDeploymentStatus(getString(output, OUTPUT_STATE)), getString(output, OUTPUT_URL),
            toFailureReason(output));
    }

    private static @Nullable String toFailureReason(Map<?, ?> output) {
        String errorCode = getString(output, OUTPUT_ERROR_CODE);
        String errorMessage = getString(output, OUTPUT_ERROR_MESSAGE);

        if (errorCode == null) {
            return errorMessage;
        }

        if (errorMessage == null) {
            return errorCode;
        }

        return errorCode + ": " + errorMessage;
    }
}
