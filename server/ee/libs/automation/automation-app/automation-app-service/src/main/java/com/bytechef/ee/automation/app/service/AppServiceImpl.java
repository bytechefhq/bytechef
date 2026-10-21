/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.repository.AppRepository;
import com.bytechef.ee.automation.app.util.AppSecretKeys;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
public class AppServiceImpl implements AppService {

    private static final Logger log = LoggerFactory.getLogger(AppServiceImpl.class);
    private static final Duration LAST_USED_THROTTLE = Duration.ofMinutes(1);
    private static final int MAX_CAUSE_DEPTH = 16;
    private static final int MAX_NAME_LENGTH = 256;
    private static final int MAX_URL_LENGTH = 2048;
    private static final String NAME_UNIQUE_CONSTRAINT = "uk_app_workspace_environment_name";

    private final AppRepository appRepository;

    @SuppressFBWarnings("EI")
    public AppServiceImpl(AppRepository appRepository) {
        this.appRepository = appRepository;
    }

    @Override
    public CreatedApp create(App app) {
        checkLength("name", app.getName(), MAX_NAME_LENGTH);
        checkLength("url", app.getUrl(), MAX_URL_LENGTH);
        checkNameAvailable(app);

        AppSecretKeys.GeneratedKey generatedKey = AppSecretKeys.generate();

        app.setUuid(String.valueOf(UUID.randomUUID()));
        app.setSecretKeyHash(generatedKey.hash());
        app.setSecretKeyHint(generatedKey.hint());
        app.setStatus(AppStatus.ENABLED);

        return new CreatedApp(saveTranslatingNameConflict(app), generatedKey.key());
    }

    @Override
    public void delete(long id) {
        appRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<App> fetchApp(long id) {
        return appRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<App> fetchAppBySecretKey(String secretKey) {
        return appRepository.findBySecretKeyHash(AppSecretKeys.hash(secretKey));
    }

    @Override
    @Transactional(readOnly = true)
    public App getApp(long id) {
        return appRepository.findById(id)
            .orElseThrow(
                () -> new ConfigurationException("App %s not found".formatted(id), AppErrorType.APP_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<App> getApps(long workspaceId, Environment environment) {
        return appRepository.findAllByWorkspaceIdAndEnvironmentOrderByName(workspaceId, environment.ordinal());
    }

    @Override
    public RotatedKey rotateSecretKey(long id) {
        App app = getApp(id);

        AppSecretKeys.GeneratedKey generatedKey = AppSecretKeys.generate();

        app.setSecretKeyHash(generatedKey.hash());
        app.setSecretKeyHint(generatedKey.hint());
        app.setSecretKeyLastUsedDate(null);

        return new RotatedKey(appRepository.save(app), generatedKey.key());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void touchSecretKeyLastUsedDate(long id) {
        Instant now = Instant.now();

        try {
            appRepository.touchSecretKeyLastUsedDate(id, now, now.minus(LAST_USED_THROTTLE));
        } catch (RuntimeException runtimeException) {
            if (log.isWarnEnabled()) {
                log.warn(
                    "Failed to record the secret key last-used date of app {}: {}", id,
                    runtimeException.getMessage());
            }
        }
    }

    @Override
    public App update(
        long id, @Nullable String name, @Nullable String description, @Nullable AppHostingTarget hostingTarget,
        @Nullable String url, @Nullable Instant lastDeployedDate, @Nullable ResourceVisibility visibility) {

        checkLength("name", name, MAX_NAME_LENGTH);
        checkLength("url", url, MAX_URL_LENGTH);

        App current = getApp(id);

        if (name != null) {
            Validate.notBlank(name, "name");

            if (!Objects.equals(current.getName(), name)) {
                current.setName(name);

                checkNameAvailable(current);
            }
        }

        if (description != null) {
            current.setDescription(StringUtils.defaultIfEmpty(description, null));
        }

        if (hostingTarget != null) {
            current.setHostingTarget(hostingTarget);
        }

        if (url != null) {
            current.setUrl(StringUtils.defaultIfEmpty(url, null));
        }

        if (lastDeployedDate != null) {
            current.setLastDeployedDate(lastDeployedDate);
        }

        if (visibility != null) {
            current.setVisibility(visibility);
        }

        return saveTranslatingNameConflict(current);
    }

    @Override
    public App updateStatus(long id, AppStatus status) {
        App app = getApp(id);

        app.setStatus(status);

        return appRepository.save(app);
    }

    private static void checkLength(String fieldName, @Nullable String value, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new ConfigurationException(
                "App %s must be at most %d characters".formatted(fieldName, maxLength),
                AppErrorType.APP_FIELD_TOO_LONG);
        }
    }

    private void checkNameAvailable(App app) {
        Long workspaceId = Validate.notNull(app.getWorkspaceId(), "workspaceId");

        Validate.notBlank(app.getName(), "name");

        appRepository.findByWorkspaceIdAndEnvironmentAndName(workspaceId, app.getEnvironmentId(), app.getName())
            .filter(existing -> !Objects.equals(existing.getId(), app.getId()))
            .ifPresent(existing -> {
                throw nameAlreadyExists(app, null);
            });
    }

    private App saveTranslatingNameConflict(App app) {
        try {
            return appRepository.save(app);
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            if (isNameConflict(dataIntegrityViolationException)) {
                throw nameAlreadyExists(app, dataIntegrityViolationException);
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

    private static ConfigurationException nameAlreadyExists(App app, Throwable cause) {
        String message = "App '%s' already exists".formatted(app.getName());

        if (cause == null) {
            return new ConfigurationException(message, AppErrorType.APP_NAME_ALREADY_EXISTS);
        }

        return new ConfigurationException(message, cause, AppErrorType.APP_NAME_ALREADY_EXISTS);
    }
}
