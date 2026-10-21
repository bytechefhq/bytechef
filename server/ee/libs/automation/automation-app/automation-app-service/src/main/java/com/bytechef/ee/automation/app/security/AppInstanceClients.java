/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.security;

import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.oauth2.authorizationserver.domain.ClientRegistrationRequest;
import com.bytechef.platform.oauth2.authorizationserver.exception.RegisteredClientErrorType;
import com.bytechef.platform.oauth2.authorizationserver.service.OAuth2AuthorizationGrantService;
import com.bytechef.platform.oauth2.authorizationserver.service.RegisteredClientService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
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
public class AppInstanceClients {

    public static final String CALLBACK_PATH = "/auth/callback";
    public static final String CLIENT_ID_PREFIX = "app-";
    public static final List<String> DEVELOPMENT_REDIRECT_URIS = List.of("http://localhost:3000" + CALLBACK_PATH);
    public static final Set<String> SCOPES = Set.of("openid", "profile", "app:use");

    private static final Logger log = LoggerFactory.getLogger(AppInstanceClients.class);

    private static final String AUTHORIZATION_SERVER_PROPERTY = "bytechef.oauth2.authorization-server.enabled";
    private static final String HTTPS_SCHEME_PREFIX = "https://";

    private final ObjectProvider<OAuth2AuthorizationGrantService> authorizationGrantServiceProvider;
    private final ObjectProvider<RegisteredClientService> registeredClientServiceProvider;

    @SuppressFBWarnings("EI")
    public AppInstanceClients(
        ObjectProvider<OAuth2AuthorizationGrantService> authorizationGrantServiceProvider,
        ObjectProvider<RegisteredClientService> registeredClientServiceProvider) {

        this.authorizationGrantServiceProvider = authorizationGrantServiceProvider;
        this.registeredClientServiceProvider = registeredClientServiceProvider;
    }

    public static String getClientId(AppInstance appInstance) {
        return CLIENT_ID_PREFIX + appInstance.getUuid();
    }

    public static Optional<String> parseInstanceUuid(String clientId) {
        if (!clientId.startsWith(CLIENT_ID_PREFIX) || clientId.length() == CLIENT_ID_PREFIX.length()) {
            return Optional.empty();
        }

        return Optional.of(clientId.substring(CLIENT_ID_PREFIX.length()));
    }

    public void checkAvailable(AppUserAuthMode userAuthMode) {
        if (userAuthMode == AppUserAuthMode.NONE) {
            return;
        }

        if (!isAvailable()) {
            throw new ConfigurationException(
                "App instance sign-in mode %s needs the authorization server; set %s=true or use NONE".formatted(
                    userAuthMode, AUTHORIZATION_SERVER_PROPERTY),
                AppErrorType.APP_USER_AUTH_UNAVAILABLE);
        }
    }

    public AppUserAuthMode getDefaultUserAuthMode() {
        return isAvailable() ? AppUserAuthMode.REQUIRED : AppUserAuthMode.NONE;
    }

    public boolean isAvailable() {
        return registeredClientServiceProvider.getIfAvailable() != null;
    }

    public void deleteClient(String clientId) {
        RegisteredClientService registeredClientService = registeredClientServiceProvider.getIfAvailable();

        if (registeredClientService == null) {
            log.warn(
                "OAuth2 client {} of an app instance was not deleted because the authorization server is off",
                clientId);

            return;
        }

        registeredClientService.deleteClient(clientId);
    }

    public String registerClient(AppInstance appInstance, String appName) {
        checkAvailable(appInstance.getUserAuthMode());

        RegisteredClientService registeredClientService = registeredClientServiceProvider.getObject();

        try {
            registeredClientService.registerClient(toClientRegistrationRequest(appInstance, appName));
        } catch (ConfigurationException configurationException) {
            throw toUrlInvalidUnlessOtherError(configurationException);
        }

        return getClientId(appInstance);
    }

    public void revokeGrant(String clientId, String principalName) {
        Optional<RevocationTarget> revocationTargetOptional = fetchRevocationTarget(clientId);

        if (revocationTargetOptional.isEmpty()) {
            return;
        }

        RevocationTarget revocationTarget = revocationTargetOptional.get();

        try {
            revocationTarget.authorizationGrantService()
                .revokeGrant(revocationTarget.registeredClientId(), principalName);
        } catch (ConfigurationException configurationException) {
            rethrowUnlessGrantNotFound(configurationException);
        }
    }

    public void revokeGrants(String clientId) {
        Optional<RevocationTarget> revocationTargetOptional = fetchRevocationTarget(clientId);

        if (revocationTargetOptional.isEmpty()) {
            return;
        }

        RevocationTarget revocationTarget = revocationTargetOptional.get();

        try {
            revocationTarget.authorizationGrantService()
                .revokeGrants(revocationTarget.registeredClientId());
        } catch (ConfigurationException configurationException) {
            rethrowUnlessGrantNotFound(configurationException);
        }
    }

    public void updateClient(AppInstance appInstance, String appName) {
        checkAvailable(appInstance.getUserAuthMode());

        RegisteredClientService registeredClientService = registeredClientServiceProvider.getObject();

        try {
            registeredClientService.updateClient(toClientRegistrationRequest(appInstance, appName));
        } catch (ConfigurationException configurationException) {
            throw toUrlInvalidUnlessOtherError(configurationException);
        }
    }

    private Optional<RevocationTarget> fetchRevocationTarget(String clientId) {
        RegisteredClientService registeredClientService = registeredClientServiceProvider.getIfAvailable();
        OAuth2AuthorizationGrantService authorizationGrantService =
            authorizationGrantServiceProvider.getIfAvailable();

        if (registeredClientService == null || authorizationGrantService == null) {
            return Optional.empty();
        }

        Optional<String> registeredClientIdOptional = registeredClientService.fetchRegisteredClientId(clientId);

        if (registeredClientIdOptional.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new RevocationTarget(authorizationGrantService, registeredClientIdOptional.get()));
    }

    private static void rethrowUnlessGrantNotFound(ConfigurationException configurationException) {
        if (configurationException.getErrorKey() != RegisteredClientErrorType.GRANT_NOT_FOUND.getErrorKey() ||
            configurationException.getEntityClass() != RegisteredClientErrorType.GRANT_NOT_FOUND.getErrorClass()) {

            throw configurationException;
        }
    }

    private static ClientRegistrationRequest toClientRegistrationRequest(AppInstance appInstance, String appName) {
        String url = appInstance.getUrl();

        if (url == null) {
            throw new IllegalStateException("App instance %s has no url".formatted(appInstance.getId()));
        }

        if (appInstance.getEnvironment() != Environment.DEVELOPMENT &&
            !Strings.CI.startsWith(url, HTTPS_SCHEME_PREFIX)) {

            throw new ConfigurationException(
                "Sign-in outside DEVELOPMENT needs an https app instance url", AppErrorType.APP_INSTANCE_URL_INVALID);
        }

        List<String> redirectUris = new ArrayList<>();

        redirectUris.add(StringUtils.removeEnd(url, "/") + CALLBACK_PATH);

        if (appInstance.getEnvironment() == Environment.DEVELOPMENT) {
            redirectUris.addAll(DEVELOPMENT_REDIRECT_URIS);
        }

        return new ClientRegistrationRequest(
            getClientId(appInstance), toClientName(appName, appInstance.getName()), redirectUris, SCOPES, false);
    }

    private static ConfigurationException toUrlInvalidUnlessOtherError(
        ConfigurationException configurationException) {

        if (configurationException.getErrorKey() != RegisteredClientErrorType.INVALID_REDIRECT_URI.getErrorKey() ||
            configurationException.getEntityClass() != RegisteredClientErrorType.INVALID_REDIRECT_URI.getErrorClass()) {

            return configurationException;
        }

        return new ConfigurationException(
            "Sign-in needs an app instance url that is https, or http on a loopback host",
            AppErrorType.APP_INSTANCE_URL_INVALID);
    }

    private static String toClientName(String appName, @Nullable String instanceName) {
        if (instanceName == null || instanceName.isBlank()) {
            return appName;
        }

        return "%s (%s)".formatted(appName, instanceName);
    }

    private record RevocationTarget(
        OAuth2AuthorizationGrantService authorizationGrantService, String registeredClientId) {
    }
}
