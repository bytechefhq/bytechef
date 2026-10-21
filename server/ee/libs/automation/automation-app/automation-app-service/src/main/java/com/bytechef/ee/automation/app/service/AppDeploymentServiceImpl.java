/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import com.bytechef.ee.automation.app.domain.AppDeployment;
import com.bytechef.ee.automation.app.domain.AppDeploymentStatus;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.repository.AppDeploymentRepository;
import com.bytechef.ee.automation.app.repository.AppInstanceRepository;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
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
public class AppDeploymentServiceImpl implements AppDeploymentService {

    private static final int MAX_PROVIDER_DEPLOYMENT_ID_LENGTH = 256;
    private static final int MAX_URL_LENGTH = 2048;
    private static final List<Integer> NON_TERMINAL_STATUSES = Arrays.stream(AppDeploymentStatus.values())
        .filter(status -> !status.isTerminal())
        .map(AppDeploymentStatus::ordinal)
        .toList();

    private final AppDeploymentRepository appDeploymentRepository;
    private final AppInstanceRepository appInstanceRepository;

    @SuppressFBWarnings("EI")
    public AppDeploymentServiceImpl(
        AppDeploymentRepository appDeploymentRepository, AppInstanceRepository appInstanceRepository) {

        this.appDeploymentRepository = appDeploymentRepository;
        this.appInstanceRepository = appInstanceRepository;
    }

    @Override
    public Optional<AppDeployment> cancel(long id, @Nullable String handledProviderDeploymentId) {
        AppDeployment appDeployment = getNonTerminalAppDeployment(id);

        String providerDeploymentId = appDeployment.getProviderDeploymentId();

        if (providerDeploymentId != null && !providerDeploymentId.equals(handledProviderDeploymentId)) {
            return Optional.empty();
        }

        appDeployment.setStatus(AppDeploymentStatus.CANCELED);
        appDeployment.setFinishedDate(Instant.now());

        return Optional.of(appDeploymentRepository.save(appDeployment));
    }

    @Override
    public void checkNoDeploymentInProgress(long appInstanceId) {
        appInstanceRepository.lockAppInstance(appInstanceId);

        if (appDeploymentRepository.existsByAppInstanceIdAndStatusIn(appInstanceId, NON_TERMINAL_STATUSES)) {
            throw new ConfigurationException(
                "App instance %s already has a deployment in progress".formatted(appInstanceId),
                AppErrorType.APP_DEPLOYMENT_IN_PROGRESS);
        }
    }

    @Override
    public AppDeployment create(AppDeployment appDeployment) {
        Validate.notNull(appDeployment.getAppInstanceId(), "appInstanceId");
        Validate.notNull(appDeployment.getAppVersionId(), "appVersionId");
        Validate.isTrue(appDeployment.getId() == null, "id must be null");

        checkLength("url", appDeployment.getUrl(), MAX_URL_LENGTH);
        checkLength(
            "providerDeploymentId", appDeployment.getProviderDeploymentId(), MAX_PROVIDER_DEPLOYMENT_ID_LENGTH);

        AppDeploymentStatus status = appDeployment.getStatus();

        if (status.isTerminal()) {
            if (appDeployment.getFinishedDate() == null) {
                appDeployment.setFinishedDate(Instant.now());
            }
        } else {
            checkNoDeploymentInProgress(appDeployment.getAppInstanceId());
        }

        return appDeploymentRepository.save(appDeployment);
    }

    @Override
    @Transactional(readOnly = true)
    public AppDeployment getAppDeployment(long id) {
        return appDeploymentRepository.findById(id)
            .orElseThrow(() -> new ConfigurationException(
                "App deployment %s not found".formatted(id), AppErrorType.APP_DEPLOYMENT_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppDeployment> getAppDeployments(long appInstanceId, int limit) {
        return appDeploymentRepository.findAllByAppInstanceIdOrderByIdDesc(appInstanceId, Limit.of(limit));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppDeployment> getNonTerminalAppDeployments() {
        return appDeploymentRepository.findAllByStatusInOrderById(NON_TERMINAL_STATUSES);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasReadyAppDeployment(long appInstanceId, long appVersionId) {
        return appDeploymentRepository.existsByAppInstanceIdAndAppVersionIdAndStatus(
            appInstanceId, appVersionId, AppDeploymentStatus.READY.ordinal());
    }

    @Override
    public AppDeployment updateStatus(
        long id, AppDeploymentStatus status, @Nullable String url, @Nullable String errorMessage) {

        return updateStatus(id, null, status, url, errorMessage);
    }

    @Override
    public AppDeployment updateStatus(
        long id, @Nullable String providerDeploymentId, AppDeploymentStatus status, @Nullable String url,
        @Nullable String errorMessage) {

        checkLength("url", url, MAX_URL_LENGTH);
        checkLength("providerDeploymentId", providerDeploymentId, MAX_PROVIDER_DEPLOYMENT_ID_LENGTH);

        AppDeployment appDeployment = getNonTerminalAppDeployment(id);

        appDeployment.setStatus(status);

        if (providerDeploymentId != null) {
            appDeployment.setProviderDeploymentId(providerDeploymentId);
        }

        if (url != null) {
            appDeployment.setUrl(url);
        }

        if (errorMessage != null) {
            appDeployment.setErrorMessage(errorMessage);
        }

        if (status.isTerminal()) {
            appDeployment.setFinishedDate(Instant.now());
        }

        return appDeploymentRepository.save(appDeployment);
    }

    private AppDeployment getNonTerminalAppDeployment(long id) {
        AppDeployment appDeployment = getAppDeployment(id);

        AppDeploymentStatus currentStatus = appDeployment.getStatus();

        if (currentStatus.isTerminal()) {
            throw new ConfigurationException(
                "App deployment %s is %s and cannot be changed".formatted(id, currentStatus),
                AppErrorType.APP_DEPLOYMENT_TERMINAL);
        }

        return appDeployment;
    }

    private static void checkLength(String fieldName, @Nullable String value, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new ConfigurationException(
                "App deployment %s must be at most %d characters".formatted(fieldName, maxLength),
                AppErrorType.APP_FIELD_TOO_LONG);
        }
    }
}
