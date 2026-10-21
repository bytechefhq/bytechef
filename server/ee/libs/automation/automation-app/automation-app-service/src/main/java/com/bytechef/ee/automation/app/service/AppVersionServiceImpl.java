/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import com.bytechef.ee.automation.app.domain.AppVersion;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.repository.AppVersionRepository;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
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
public class AppVersionServiceImpl implements AppVersionService {

    private static final int MAX_CAUSE_DEPTH = 16;
    private static final int MAX_DESCRIPTION_LENGTH = 4096;
    private static final int MAX_GIT_COMMIT_SHA_LENGTH = 64;
    private static final int MAX_GIT_REF_LENGTH = 256;
    private static final int MAX_NAME_LENGTH = 256;
    private static final String NAME_UNIQUE_CONSTRAINT = "uk_app_version_app_name";

    private final AppVersionRepository appVersionRepository;

    @SuppressFBWarnings("EI")
    public AppVersionServiceImpl(AppVersionRepository appVersionRepository) {
        this.appVersionRepository = appVersionRepository;
    }

    @Override
    public AppVersion create(AppVersion appVersion) {
        Validate.notNull(appVersion.getAppId(), "appId");
        Validate.notBlank(appVersion.getName(), "name");
        Validate.isTrue(appVersion.getId() == null, "id must be null");

        checkLength("name", appVersion.getName(), MAX_NAME_LENGTH);
        checkLength("description", appVersion.getDescription(), MAX_DESCRIPTION_LENGTH);
        checkLength("gitRef", appVersion.getGitRef(), MAX_GIT_REF_LENGTH);
        checkLength("gitCommitSha", appVersion.getGitCommitSha(), MAX_GIT_COMMIT_SHA_LENGTH);

        if (appVersionRepository.findByAppIdAndName(appVersion.getAppId(), appVersion.getName())
            .isPresent()) {

            throw nameAlreadyExists(appVersion, null);
        }

        try {
            return appVersionRepository.save(appVersion);
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            if (isNameConflict(dataIntegrityViolationException)) {
                throw nameAlreadyExists(appVersion, dataIntegrityViolationException);
            }

            throw dataIntegrityViolationException;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AppVersion> fetchAppVersion(long appId, String name) {
        return appVersionRepository.findByAppIdAndName(appId, name);
    }

    @Override
    @Transactional(readOnly = true)
    public AppVersion getAppVersion(long id) {
        return appVersionRepository.findById(id)
            .orElseThrow(() -> new ConfigurationException(
                "App version %s not found".formatted(id), AppErrorType.APP_VERSION_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppVersion> getAppVersions(long appId, int limit) {
        return appVersionRepository.findAllByAppIdOrderByIdDesc(appId, Limit.of(limit));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppVersion> getAppVersionsByIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        return appVersionRepository.findAllById(ids);
    }

    private static void checkLength(String fieldName, @Nullable String value, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new ConfigurationException(
                "App version %s must be at most %d characters".formatted(fieldName, maxLength),
                AppErrorType.APP_FIELD_TOO_LONG);
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

    private static ConfigurationException nameAlreadyExists(AppVersion appVersion, @Nullable Throwable cause) {
        String message = "App version '%s' already exists".formatted(appVersion.getName());

        if (cause == null) {
            return new ConfigurationException(message, AppErrorType.APP_VERSION_NAME_ALREADY_EXISTS);
        }

        return new ConfigurationException(message, cause, AppErrorType.APP_VERSION_NAME_ALREADY_EXISTS);
    }
}
