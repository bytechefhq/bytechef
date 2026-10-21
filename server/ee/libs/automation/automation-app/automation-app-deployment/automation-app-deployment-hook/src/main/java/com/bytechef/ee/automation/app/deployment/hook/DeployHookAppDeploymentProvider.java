/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.deployment.hook;

import com.bytechef.component.definition.Context;
import com.bytechef.component.exception.ProviderException;
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
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
public class DeployHookAppDeploymentProvider implements AppDeploymentProvider {

    public static final String SETTINGS_CANCEL_URL = "cancelUrl";
    public static final String SETTINGS_DEPLOY_URL = "deployUrl";
    public static final String SETTINGS_STATUS_URL = "statusUrl";

    public static final String ID_PLACEHOLDER = "{id}";

    static final String COMPONENT_NAME = "httpClient";
    static final int COMPONENT_VERSION = 2;
    static final String GET_ACTION = "get";
    static final String POST_ACTION = "post";

    private static final String BODY_CONTENT_TYPE_JSON = "JSON";
    private static final int CONNECT_TIMEOUT_MILLIS = 10000;
    private static final String HEXADECIMAL_DIGITS = "0123456789ABCDEF";
    private static final String ID_SENTINEL = "bytechefdeploymentid0sentinel";
    private static final int HTTP_NOT_FOUND = 404;
    private static final int MAX_CAUSE_DEPTH = 10;
    private static final int MAX_LOGGED_STATUS_LENGTH = 40;
    private static final int MAX_PROVIDER_DEPLOYMENT_ID_LENGTH = 256;
    private static final int MAX_URL_LENGTH = 2048;
    private static final String RESPONSE_TYPE_JSON = "JSON";
    private static final String UNKNOWN_DEPLOYMENT_MESSAGE = "The deploy endpoint no longer knows this deployment";

    private static final String INPUT_BODY = "body";
    private static final String INPUT_BODY_CONTENT = "bodyContent";
    private static final String INPUT_BODY_CONTENT_TYPE = "bodyContentType";
    private static final String INPUT_FOLLOW_ALL_REDIRECTS = "followAllRedirects";
    private static final String INPUT_FOLLOW_REDIRECT = "followRedirect";
    private static final String INPUT_FULL_RESPONSE = "fullResponse";
    private static final String INPUT_HEADERS = "headers";
    private static final String INPUT_RESPONSE_TYPE = "responseType";
    private static final String INPUT_TIMEOUT = "timeout";
    private static final String INPUT_URI = "uri";

    private static final String REQUEST_ENVIRONMENT = "environment";
    private static final String REQUEST_GIT_COMMIT_SHA = "gitCommitSha";
    private static final String REQUEST_GIT_REF = "gitRef";
    private static final String REQUEST_VERSION = "version";

    private static final String RESPONSE_BODY = "body";
    private static final String RESPONSE_ERROR_MESSAGE = "errorMessage";
    private static final String RESPONSE_ID = "id";
    private static final String RESPONSE_STATUS = "status";
    private static final String RESPONSE_STATUS_CODE = "statusCode";
    private static final String RESPONSE_URL = "url";

    private static final String REQUEST_KIND_CANCEL = "cancel";
    private static final String REQUEST_KIND_DEPLOY = "deploy";
    private static final String REQUEST_KIND_STATUS = "status";

    private static final Logger log = LoggerFactory.getLogger(DeployHookAppDeploymentProvider.class);

    private final ObjectProvider<ActionDefinitionFacade> actionDefinitionFacadeProvider;

    @SuppressFBWarnings("EI")
    public DeployHookAppDeploymentProvider(ObjectProvider<ActionDefinitionFacade> actionDefinitionFacadeProvider) {
        this.actionDefinitionFacadeProvider = actionDefinitionFacadeProvider;
    }

    static AppDeploymentStatus toDeploymentStatus(@Nullable String status) {
        if (status == null) {
            return AppDeploymentStatus.BUILDING;
        }

        return switch (status.toUpperCase(Locale.ROOT)) {
            case "QUEUED" -> AppDeploymentStatus.QUEUED;
            case "BUILDING" -> AppDeploymentStatus.BUILDING;
            case "READY" -> AppDeploymentStatus.READY;
            case "FAILED" -> AppDeploymentStatus.FAILED;
            case "CANCELED", "CANCELLED" -> AppDeploymentStatus.CANCELED;
            default -> {
                log.warn(
                    "The deploy hook reported the unknown deployment status {}; treating it as building",
                    AppLogValues.toLoggableValue(status, MAX_LOGGED_STATUS_LENGTH));

                yield AppDeploymentStatus.BUILDING;
            }
        };
    }

    @Override
    public void cancel(ProviderDeploymentRef ref) {
        String cancelUrl = resolveDeploymentUrl(
            ref.deploymentSettings(), SETTINGS_CANCEL_URL, "/" + ID_PLACEHOLDER + "/cancel",
            ref.providerDeploymentId());

        executeRequest(
            REQUEST_KIND_CANCEL, POST_ACTION, toInputParameters(cancelUrl, Map.of()), ref.connectionId(), null);
    }

    @Override
    public ProviderDeployment deploy(ProviderDeploymentRequest request) {
        Map<String, ?> deploymentSettings = request.deploymentSettings();

        Environment environment = request.environment();

        validateDeploymentSettings(deploymentSettings, environment);

        String gitRef = StringUtils.trimToNull(request.gitRef());
        String gitCommitSha = StringUtils.trimToNull(request.gitCommitSha());

        if (gitRef == null && gitCommitSha == null) {
            throw new IllegalArgumentException(
                "A deploy hook deployment needs an app version with a git reference or a git commit");
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();

        requestBody.put(REQUEST_ENVIRONMENT, environment.name());

        if (gitRef != null) {
            requestBody.put(REQUEST_GIT_REF, gitRef);
        }

        if (gitCommitSha != null) {
            requestBody.put(REQUEST_GIT_COMMIT_SHA, gitCommitSha);
        }

        String versionName = StringUtils.trimToNull(request.versionName());

        if (versionName != null) {
            requestBody.put(REQUEST_VERSION, versionName);
        }

        String deployUrl = getString(deploymentSettings, SETTINGS_DEPLOY_URL);

        Map<?, ?> responseBody = Objects.requireNonNull(
            executeRequest(
                REQUEST_KIND_DEPLOY, POST_ACTION, toInputParameters(deployUrl, requestBody), request.connectionId(),
                environment));

        String providerDeploymentId = toProviderDeploymentId(responseBody.get(RESPONSE_ID));

        if (providerDeploymentId == null) {
            throw new DeployHookDeploymentException("The deploy hook answered the deployment request without an id");
        }

        return toProviderDeployment(providerDeploymentId, responseBody);
    }

    @Override
    public ProviderDeployment getDeployment(ProviderDeploymentRef ref) {
        String statusUrl = resolveDeploymentUrl(
            ref.deploymentSettings(), SETTINGS_STATUS_URL, "/" + ID_PLACEHOLDER, ref.providerDeploymentId());

        Map<?, ?> responseBody = executeRequest(
            REQUEST_KIND_STATUS, GET_ACTION, toInputParameters(statusUrl, null), ref.connectionId(), null);

        if (responseBody == null) {
            return new ProviderDeployment(
                ref.providerDeploymentId(), AppDeploymentStatus.FAILED, null, UNKNOWN_DEPLOYMENT_MESSAGE);
        }

        return toProviderDeployment(ref.providerDeploymentId(), responseBody);
    }

    @Override
    public String getDeploymentConnectionComponentName() {
        return COMPONENT_NAME;
    }

    @Override
    public AppHostingTarget getHostingTarget() {
        return AppHostingTarget.SELF_HOSTED;
    }

    @Override
    public void validateDeploymentSettings(Map<String, ?> deploymentSettings, Environment environment) {
        boolean httpAllowed = environment == Environment.DEVELOPMENT;

        validateUrl(getRequiredDeployUrl(deploymentSettings), SETTINGS_DEPLOY_URL, httpAllowed);

        for (String templateKey : new String[] {
            SETTINGS_STATUS_URL, SETTINGS_CANCEL_URL
        }) {
            if (deploymentSettings.get(templateKey) != null) {
                validateTemplate(deploymentSettings, templateKey, httpAllowed);
            }
        }
    }

    private static String encodePathSegment(String value) {
        StringBuilder sb = new StringBuilder();

        for (byte valueByte : value.getBytes(StandardCharsets.UTF_8)) {
            int character = valueByte & 0xFF;

            if ((character >= 'A' && character <= 'Z') || (character >= 'a' && character <= 'z') ||
                (character >= '0' && character <= '9') || character == '-' || character == '_' || character == '~') {

                sb.append((char) character);
            } else {
                sb.append('%');
                sb.append(HEXADECIMAL_DIGITS.charAt(character >> 4));
                sb.append(HEXADECIMAL_DIGITS.charAt(character & 0xF));
            }
        }

        return sb.toString();
    }

    private @Nullable Map<?, ?> executeRequest(
        String requestKind, String actionName, Map<String, Object> inputParameters, long connectionId,
        @Nullable Environment environment) {

        ActionDefinitionFacade actionDefinitionFacade = actionDefinitionFacadeProvider.getIfAvailable();

        if (actionDefinitionFacade == null) {
            throw new DeployHookDeploymentException(
                "The deploy hook deployment provider needs the component runtime, which this application does not " +
                    "run");
        }

        Long environmentId = environment == null ? null : (long) environment.ordinal();

        Object output;

        try {
            output = SecurityUtils.runAsSystem(() -> actionDefinitionFacade.executePerform(
                COMPONENT_NAME, COMPONENT_VERSION, actionName, null, null, null, null, null, inputParameters,
                Map.of(COMPONENT_NAME, connectionId), Map.of(), environmentId, PlatformType.AUTOMATION, false, null,
                null, null));
        } catch (RuntimeException exception) {
            if (requestKind.equals(REQUEST_KIND_STATUS) && isNotFound(exception)) {
                log.warn("The deploy hook status request was answered with HTTP status {}", HTTP_NOT_FOUND);

                return null;
            }

            log.warn("The deploy hook {} request failed ({})", requestKind, exception.getClass()
                .getName());

            throw new DeployHookDeploymentException("The deploy hook %s request failed".formatted(requestKind));
        }

        Integer statusCode = null;
        Object responseBody = null;

        if (output instanceof Context.Http.Response response) {
            statusCode = response.getStatusCode();
            responseBody = response.getBody();
        } else if (output instanceof Map<?, ?> outputMap &&
            outputMap.get(RESPONSE_STATUS_CODE) instanceof Number statusCodeNumber) {

            statusCode = statusCodeNumber.intValue();
            responseBody = outputMap.get(RESPONSE_BODY);
        }

        if (statusCode == null) {
            throw new DeployHookDeploymentException(
                "The deploy hook %s request was not answered with a JSON object".formatted(requestKind));
        }

        if (requestKind.equals(REQUEST_KIND_STATUS) && statusCode == HTTP_NOT_FOUND) {
            log.warn("The deploy hook status request was answered with HTTP status {}", statusCode);

            return null;
        }

        if (statusCode < 200 || statusCode > 299) {
            log.warn("The deploy hook {} request was answered with HTTP status {}", requestKind, statusCode);

            throw new DeployHookDeploymentException(
                "The deploy hook %s request was not answered with a 2xx status".formatted(requestKind));
        }

        if (requestKind.equals(REQUEST_KIND_CANCEL)) {
            return Map.of();
        }

        if (responseBody instanceof Map<?, ?> responseBodyMap) {
            return responseBodyMap;
        }

        throw new DeployHookDeploymentException(
            "The deploy hook %s request was not answered with a JSON object".formatted(requestKind));
    }

    private static String getRequiredDeployUrl(Map<String, ?> deploymentSettings) {
        Object value = deploymentSettings.get(SETTINGS_DEPLOY_URL);

        if (value == null || (value instanceof String string && StringUtils.isBlank(string))) {
            throw new AppDeploymentSettingsException(
                "The deploy hook deployment setting '%s' is required".formatted(SETTINGS_DEPLOY_URL));
        }

        if (!(value instanceof String string)) {
            throw notAnHttpUrl(SETTINGS_DEPLOY_URL);
        }

        return string.trim();
    }

    private static @Nullable String getString(Map<?, ?> values, String key) {
        if (values.get(key) instanceof String string) {
            return StringUtils.trimToNull(string);
        }

        return null;
    }

    private static boolean isNotFound(Throwable throwable) {
        Throwable current = throwable;

        for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (current instanceof ProviderException providerException &&
                Objects.equals(providerException.getStatusCode(), HTTP_NOT_FOUND)) {

                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private static boolean isStoredUrl(String url) {
        if (url.length() > MAX_URL_LENGTH) {
            return false;
        }

        URI uri;

        try {
            uri = new URI(url);
        } catch (URISyntaxException uriSyntaxException) {
            return false;
        }

        String scheme = uri.getScheme();

        return scheme != null && !uri.isOpaque() && uri.getHost() != null && uri.getRawUserInfo() == null &&
            (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));
    }

    private static AppDeploymentSettingsException notAnHttpUrl(String key) {
        return new AppDeploymentSettingsException(
            ("The deploy hook deployment setting '%s' must be an absolute http or https URL without credentials or " +
                "a fragment").formatted(key));
    }

    private static String resolveDeploymentUrl(
        Map<String, ?> deploymentSettings, String templateKey, String defaultSuffix, String providerDeploymentId) {

        validateUrl(getRequiredDeployUrl(deploymentSettings), SETTINGS_DEPLOY_URL, true);

        String template;

        if (deploymentSettings.get(templateKey) == null) {
            template = toDefaultTemplate(getRequiredDeployUrl(deploymentSettings), defaultSuffix);
        } else {
            template = validateTemplate(deploymentSettings, templateKey, true);
        }

        return template.replace(ID_PLACEHOLDER, encodePathSegment(providerDeploymentId));
    }

    private static String toDefaultTemplate(String deployUrl, String defaultSuffix) {
        String query = StringUtils.substringAfter(deployUrl, "?");
        String path = StringUtils.removeEnd(StringUtils.substringBefore(deployUrl, "?"), "/");

        if (query.isEmpty()) {
            return path + defaultSuffix;
        }

        return path + defaultSuffix + "?" + query;
    }

    private static Map<String, Object> toInputParameters(@Nullable String url, @Nullable Map<String, ?> requestBody) {
        Map<String, Object> inputParameters = new HashMap<>();

        inputParameters.put(INPUT_URI, url);
        inputParameters.put(INPUT_FULL_RESPONSE, true);
        inputParameters.put(INPUT_RESPONSE_TYPE, RESPONSE_TYPE_JSON);
        inputParameters.put(INPUT_FOLLOW_REDIRECT, false);
        inputParameters.put(INPUT_FOLLOW_ALL_REDIRECTS, false);
        inputParameters.put(INPUT_TIMEOUT, CONNECT_TIMEOUT_MILLIS);
        inputParameters.put(INPUT_HEADERS, Map.of("Accept", "application/json"));

        if (requestBody != null) {
            inputParameters.put(
                INPUT_BODY,
                Map.of(INPUT_BODY_CONTENT_TYPE, BODY_CONTENT_TYPE_JSON, INPUT_BODY_CONTENT, requestBody));
        }

        return inputParameters;
    }

    private static ProviderDeployment toProviderDeployment(String providerDeploymentId, Map<?, ?> responseBody) {
        AppDeploymentStatus status = toDeploymentStatus(getString(responseBody, RESPONSE_STATUS));

        String url = getString(responseBody, RESPONSE_URL);

        if (url != null && !isStoredUrl(url)) {
            url = null;
        }

        String errorMessage = null;

        if (status == AppDeploymentStatus.FAILED) {
            errorMessage = getString(responseBody, RESPONSE_ERROR_MESSAGE);
        }

        return new ProviderDeployment(providerDeploymentId, status, url, errorMessage);
    }

    private static @Nullable String toProviderDeploymentId(@Nullable Object id) {
        String providerDeploymentId = null;

        if (id instanceof String string) {
            providerDeploymentId = StringUtils.trimToNull(string);
        } else if (id instanceof Integer || id instanceof Long || id instanceof Short || id instanceof Byte ||
            id instanceof BigInteger) {

            providerDeploymentId = id.toString();
        }

        if (providerDeploymentId == null || providerDeploymentId.length() > MAX_PROVIDER_DEPLOYMENT_ID_LENGTH ||
            StringUtils.containsOnly(providerDeploymentId, '.')) {

            return null;
        }

        return providerDeploymentId;
    }

    private static String toOrigin(URI uri) {
        String scheme = StringUtils.lowerCase(uri.getScheme(), Locale.ROOT);
        String host = StringUtils.lowerCase(uri.getHost(), Locale.ROOT);

        int port = uri.getPort();

        if (port == -1) {
            port = scheme.equals("https") ? 443 : 80;
        }

        return scheme + "://" + host + ":" + port;
    }

    private static void validateUrl(String url, String key, boolean httpAllowed) {
        if (url.length() > MAX_URL_LENGTH) {
            throw notAnHttpUrl(key);
        }

        URI uri;

        try {
            uri = new URI(url);
        } catch (URISyntaxException uriSyntaxException) {
            throw notAnHttpUrl(key);
        }

        String scheme = uri.getScheme();

        if (scheme == null || uri.isOpaque() || uri.getHost() == null || uri.getRawUserInfo() != null ||
            uri.getRawFragment() != null) {

            throw notAnHttpUrl(key);
        }

        if (scheme.equalsIgnoreCase("https")) {
            return;
        }

        if (!scheme.equalsIgnoreCase("http")) {
            throw notAnHttpUrl(key);
        }

        if (!httpAllowed) {
            throw new AppDeploymentSettingsException(
                "The deploy hook deployment setting '%s' must use https outside the DEVELOPMENT environment".formatted(
                    key));
        }
    }

    private static String validateTemplate(Map<String, ?> deploymentSettings, String key, boolean httpAllowed) {
        if (!(deploymentSettings.get(key) instanceof String value) || StringUtils.isBlank(value)) {
            throw notAnHttpUrl(key);
        }

        String template = value.trim();

        if (!template.contains(ID_PLACEHOLDER)) {
            throw new AppDeploymentSettingsException(
                "The deploy hook deployment setting '%s' must contain %s".formatted(key, ID_PLACEHOLDER));
        }

        String sampleUrl = template.replace(ID_PLACEHOLDER, ID_SENTINEL);

        validateUrl(sampleUrl, key, httpAllowed);

        URI sampleUri = URI.create(sampleUrl);

        if (StringUtils.contains(sampleUri.getRawAuthority(), ID_SENTINEL)) {
            throw new AppDeploymentSettingsException(
                "The deploy hook deployment setting '%s' may use %s only in its path or query".formatted(
                    key, ID_PLACEHOLDER));
        }

        URI deployUri = URI.create(getRequiredDeployUrl(deploymentSettings));

        if (!toOrigin(sampleUri).equals(toOrigin(deployUri))) {
            throw new AppDeploymentSettingsException(
                "The deploy hook deployment setting '%s' must have the same scheme, host and port as '%s'".formatted(
                    key, SETTINGS_DEPLOY_URL));
        }

        return template;
    }
}
