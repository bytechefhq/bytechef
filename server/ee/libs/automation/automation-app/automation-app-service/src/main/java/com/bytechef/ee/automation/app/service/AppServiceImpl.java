/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.repository.AppRepository;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.domain.ResourceVisibility;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
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

    private static final int MAX_CAUSE_DEPTH = 16;
    private static final int MAX_NAME_LENGTH = 256;
    private static final String NAME_UNIQUE_CONSTRAINT = "uk_app_workspace_name";

    private final AppRepository appRepository;

    @SuppressFBWarnings("EI")
    public AppServiceImpl(AppRepository appRepository) {
        this.appRepository = appRepository;
    }

    @Override
    public App create(App app) {
        checkLength("name", app.getName(), MAX_NAME_LENGTH);
        checkNameAvailable(app);

        app.setUuid(String.valueOf(UUID.randomUUID()));
        app.setStatus(AppStatus.DRAFT);

        return saveTranslatingNameConflict(app);
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
    public App getApp(long id) {
        return appRepository.findById(id)
            .orElseThrow(
                () -> new ConfigurationException("App %s not found".formatted(id), AppErrorType.APP_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<App> getApps(long workspaceId) {
        return appRepository.findAllByWorkspaceIdOrderByName(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<App> getAppsByIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        return appRepository.findAllById(ids);
    }

    @Override
    public App update(long id, @Nullable String name, @Nullable String description) {
        checkLength("name", name, MAX_NAME_LENGTH);

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

        return saveTranslatingNameConflict(current);
    }

    @Override
    public App updateCategory(long id, @Nullable Long categoryId) {
        App app = getApp(id);

        app.setCategoryId(categoryId);

        return appRepository.save(app);
    }

    @Override
    public App updateIcon(long id, @Nullable FileEntry icon) {
        App app = getApp(id);

        app.setIcon(icon);

        return appRepository.save(app);
    }

    @Override
    public App updatePortalDiscoverable(long id, boolean portalDiscoverable) {
        App app = getApp(id);

        app.setPortalDiscoverable(portalDiscoverable);

        return appRepository.save(app);
    }

    @Override
    public App updateStatus(long id, AppStatus status) {
        Validate.isTrue(status != AppStatus.DRAFT, "An app cannot return to DRAFT");

        App app = getApp(id);

        app.setStatus(status);

        return appRepository.save(app);
    }

    @Override
    public App updateVisibility(long id, ResourceVisibility visibility) {
        App app = getApp(id);

        app.setVisibility(visibility);

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

        appRepository.findByWorkspaceIdAndName(workspaceId, app.getName())
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

    private static ConfigurationException nameAlreadyExists(App app, @Nullable Throwable cause) {
        String message = "App '%s' already exists".formatted(app.getName());

        if (cause == null) {
            return new ConfigurationException(message, AppErrorType.APP_NAME_ALREADY_EXISTS);
        }

        return new ConfigurationException(message, cause, AppErrorType.APP_NAME_ALREADY_EXISTS);
    }
}
