/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.repository.AppInstanceRepository;
import com.bytechef.ee.automation.app.util.AppSecretKeys;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class AppInstanceServiceImpl implements AppInstanceService {

    private static final Logger log = LoggerFactory.getLogger(AppInstanceServiceImpl.class);

    private static final Duration LAST_USED_THROTTLE = Duration.ofMinutes(1);
    private static final int MAX_CAUSE_DEPTH = 16;
    private static final int MAX_NAME_LENGTH = 256;
    private static final int MAX_URL_LENGTH = 2048;
    private static final String NAME_UNIQUE_CONSTRAINT = "uk_app_instance_app_environment_name";

    private final AppInstanceRepository appInstanceRepository;

    @SuppressFBWarnings("EI")
    public AppInstanceServiceImpl(AppInstanceRepository appInstanceRepository) {
        this.appInstanceRepository = appInstanceRepository;
    }

    @Override
    public CreatedAppInstance create(AppInstance appInstance) {
        Validate.notNull(appInstance.getOwnerUserId(), "ownerUserId");
        Validate.notNull(appInstance.getWorkspaceId(), "workspaceId");

        checkLength("name", appInstance.getName(), MAX_NAME_LENGTH);
        checkLength("url", appInstance.getUrl(), MAX_URL_LENGTH);
        checkUrl(appInstance.getUrl());
        checkNameAvailable(appInstance);

        AppSecretKeys.GeneratedKey generatedKey = AppSecretKeys.generate();

        appInstance.setUuid(String.valueOf(UUID.randomUUID()));
        appInstance.setSecretKeyHash(generatedKey.hash());
        appInstance.setSecretKeyHint(generatedKey.hint());
        appInstance.setStatus(AppInstanceStatus.ACTIVE);

        return new CreatedAppInstance(saveTranslatingNameConflict(appInstance), generatedKey.key());
    }

    @Override
    public void delete(long id) {
        appInstanceRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AppInstance> fetchAppInstance(long id) {
        return appInstanceRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AppInstance> fetchAppInstanceBySecretKey(String secretKey) {
        return appInstanceRepository.findBySecretKeyHash(AppSecretKeys.hash(secretKey));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AppInstance> fetchAppInstanceByUuid(String uuid) {
        return appInstanceRepository.findByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public AppInstance getAppInstance(long id) {
        return appInstanceRepository.findById(id)
            .orElseThrow(() -> new ConfigurationException(
                "App instance %s not found".formatted(id), AppErrorType.APP_INSTANCE_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppInstance> getAppInstances(long appId) {
        return appInstanceRepository.findAllByAppIdOrderById(appId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppInstance> getAppInstancesByAppIds(Collection<Long> appIds) {
        if (appIds.isEmpty()) {
            return List.of();
        }

        return appInstanceRepository.findAllByAppIdInOrderById(appIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppInstance> getAppInstancesByDeploymentConnectionId(long deploymentConnectionId) {
        return appInstanceRepository.findAllByDeploymentConnectionIdOrderById(deploymentConnectionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppInstance> getWorkspaceAppInstances(long workspaceId, Environment environment) {
        return appInstanceRepository.findAllByWorkspaceIdAndEnvironmentOrderById(workspaceId, environment.ordinal());
    }

    @Override
    public RotatedKey rotateSecretKey(long id) {
        AppInstance appInstance = getAppInstance(id);

        checkNotTerminated(appInstance);

        AppSecretKeys.GeneratedKey generatedKey = AppSecretKeys.generate();

        appInstance.setSecretKeyHash(generatedKey.hash());
        appInstance.setSecretKeyHint(generatedKey.hint());
        appInstance.setSecretKeyLastUsedDate(null);

        return new RotatedKey(appInstanceRepository.save(appInstance), generatedKey.key());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void touchSecretKeyLastUsedDate(long id) {
        Instant now = Instant.now();

        try {
            appInstanceRepository.touchSecretKeyLastUsedDate(id, now, now.minus(LAST_USED_THROTTLE));
        } catch (RuntimeException runtimeException) {
            if (log.isWarnEnabled()) {
                log.warn(
                    "Failed to record the secret key last-used date of app instance {}: {}", id,
                    runtimeException.getMessage());
            }
        }
    }

    @Override
    public AppInstance update(
        long id, @Nullable String name, @Nullable AppHostingTarget hostingTarget, @Nullable String url) {

        checkLength("name", name, MAX_NAME_LENGTH);
        checkLength("url", url, MAX_URL_LENGTH);

        if (url != null && !url.isEmpty()) {
            checkUrl(url);
        }

        AppInstance current = getAppInstance(id);

        if (name != null) {
            Validate.notBlank(name, "name");

            if (!Objects.equals(current.getName(), name)) {
                current.setName(name);

                checkNameAvailable(current);
            }
        }

        if (hostingTarget != null) {
            current.setHostingTarget(hostingTarget);
        }

        if (url != null) {
            current.setUrl(StringUtils.defaultIfEmpty(url, null));
        }

        return saveTranslatingNameConflict(current);
    }

    @Override
    public AppInstance updateStatus(long id, AppInstanceStatus status) {
        AppInstance appInstance = getAppInstance(id);

        if (appInstance.getStatus() == AppInstanceStatus.TERMINATED && status != AppInstanceStatus.TERMINATED) {
            throw new ConfigurationException(
                "App instance %s is terminated".formatted(id), AppErrorType.APP_INSTANCE_TERMINATED);
        }

        appInstance.setStatus(status);

        if (status == AppInstanceStatus.TERMINATED) {
            appInstance.setDeploymentConnectionId(null);
            appInstance.setDeploymentSettings(Map.of());
        }

        return appInstanceRepository.save(appInstance);
    }

    @Override
    public AppInstance updateCurrentAppDeployment(long id, long appDeploymentId) {
        AppInstance appInstance = getAppInstance(id);

        appInstance.setCurrentAppDeploymentId(appDeploymentId);

        return appInstanceRepository.save(appInstance);
    }

    @Override
    public AppInstance updateConfiguration(long id, Map<String, ?> configuration) {
        AppInstance appInstance = getAppInstance(id);

        checkNotTerminated(appInstance);

        appInstance.setConfiguration(configuration);

        return appInstanceRepository.save(appInstance);
    }

    @Override
    public AppInstance updateDeploymentSettings(
        long id, @Nullable Long deploymentConnectionId, Map<String, ?> deploymentSettings) {

        AppInstance appInstance = getAppInstance(id);

        checkNotTerminated(appInstance);

        appInstance.setDeploymentConnectionId(deploymentConnectionId);
        appInstance.setDeploymentSettings(deploymentSettings);

        return appInstanceRepository.save(appInstance);
    }

    @Override
    public AppInstance updateVisibility(long id, ResourceVisibility visibility) {
        AppInstance appInstance = getAppInstance(id);

        appInstance.setVisibility(visibility);

        return appInstanceRepository.save(appInstance);
    }

    @Override
    public AppInstance updateUserAuth(long id, AppUserAuthMode userAuthMode, @Nullable String registeredClientId) {
        AppInstance appInstance = getAppInstance(id);

        appInstance.setUserAuthMode(userAuthMode);
        appInstance.setRegisteredClientId(registeredClientId);

        return appInstanceRepository.save(appInstance);
    }

    private static void checkLength(String fieldName, @Nullable String value, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new ConfigurationException(
                "App instance %s must be at most %d characters".formatted(fieldName, maxLength),
                AppErrorType.APP_FIELD_TOO_LONG);
        }
    }

    private void checkNameAvailable(AppInstance appInstance) {
        Validate.notBlank(appInstance.getName(), "name");

        appInstanceRepository
            .findByAppIdAndEnvironmentAndName(
                appInstance.getAppId(), appInstance.getEnvironmentId(), appInstance.getName())
            .filter(existing -> !Objects.equals(existing.getId(), appInstance.getId()))
            .ifPresent(existing -> {
                throw nameAlreadyExists(appInstance, null);
            });
    }

    private AppInstance saveTranslatingNameConflict(AppInstance appInstance) {
        try {
            return appInstanceRepository.save(appInstance);
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            if (isNameConflict(dataIntegrityViolationException)) {
                throw nameAlreadyExists(appInstance, dataIntegrityViolationException);
            }

            throw dataIntegrityViolationException;
        }
    }

    private static boolean isNameConflict(DataIntegrityViolationException dataIntegrityViolationException) {
        Throwable cause = dataIntegrityViolationException;

        for (int depth = 0; cause != null && depth < MAX_CAUSE_DEPTH; depth++) {
            String message = cause.getMessage();

            if (message != null && StringUtils.containsIgnoreCase(message, NAME_UNIQUE_CONSTRAINT)) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }

    private static ConfigurationException nameAlreadyExists(AppInstance appInstance, @Nullable Throwable cause) {
        String message = "App instance '%s' already exists in %s".formatted(
            appInstance.getName(), appInstance.getEnvironment());

        if (cause == null) {
            return new ConfigurationException(message, AppErrorType.APP_INSTANCE_NAME_ALREADY_EXISTS);
        }

        return new ConfigurationException(message, cause, AppErrorType.APP_INSTANCE_NAME_ALREADY_EXISTS);
    }

    private static void checkNotTerminated(AppInstance appInstance) {
        if (appInstance.getStatus() == AppInstanceStatus.TERMINATED) {
            throw new ConfigurationException(
                "App instance %s is terminated".formatted(appInstance.getId()), AppErrorType.APP_INSTANCE_TERMINATED);
        }
    }

    private static void checkUrl(@Nullable String url) {
        if (url == null) {
            return;
        }

        URI uri;

        try {
            uri = new URI(url);
        } catch (URISyntaxException uriSyntaxException) {
            throw invalidUrl();
        }

        String scheme = uri.getScheme();

        if (scheme == null || uri.isOpaque() || uri.getHost() == null || uri.getRawUserInfo() != null ||
            !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {

            throw invalidUrl();
        }
    }

    private static ConfigurationException invalidUrl() {
        return new ConfigurationException(
            "The app instance url must be an absolute http or https URL without user info",
            AppErrorType.APP_INSTANCE_URL_INVALID);
    }
}
