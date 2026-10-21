/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import com.bytechef.ee.automation.app.audit.AppAuditEvent;
import com.bytechef.ee.automation.app.audit.AppAuditPublisher;
import com.bytechef.ee.automation.app.deployment.AppDeploymentProvider;
import com.bytechef.ee.automation.app.deployment.AppDeploymentSynchronizer;
import com.bytechef.ee.automation.app.deployment.ProviderDeployment;
import com.bytechef.ee.automation.app.deployment.ProviderDeploymentRef;
import com.bytechef.ee.automation.app.deployment.ProviderDeploymentRequest;
import com.bytechef.ee.automation.app.domain.AppDeployment;
import com.bytechef.ee.automation.app.domain.AppDeploymentProviderType;
import com.bytechef.ee.automation.app.domain.AppDeploymentStatus;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.domain.AppVersion;
import com.bytechef.ee.automation.app.dto.AppDeploymentDTO;
import com.bytechef.ee.automation.app.dto.AppVersionDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.service.AppDeploymentService;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.app.service.AppVersionService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
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
public class AppDeploymentFacadeImpl implements AppDeploymentFacade {

    private static final String DEPLOYMENT_START_FAILED = "The deployment could not be started";
    private static final int MAX_CANCEL_ATTEMPTS = 3;
    private static final String NO_DEPLOYMENT_CONNECTION = "The app instance has no deployment connection";
    private static final String NO_DEPLOYMENT_PROVIDER =
        "No deployment provider is available for the app instance's hosting target";
    private static final String PROVIDER_RESPONSE_REJECTED =
        "The hosting provider returned a deployment that could not be stored";
    private static final String PROVIDER_START_FAILED = "The hosting provider could not start the deployment";

    private static final Logger log = LoggerFactory.getLogger(AppDeploymentFacadeImpl.class);

    private final AppAuditPublisher appAuditPublisher;
    private final AppDeploymentService appDeploymentService;
    private final AppDeploymentSynchronizer appDeploymentSynchronizer;
    private final AppInstanceService appInstanceService;
    private final AppService appService;
    private final AppVersionService appVersionService;

    @SuppressFBWarnings("EI")
    public AppDeploymentFacadeImpl(
        AppAuditPublisher appAuditPublisher, AppDeploymentService appDeploymentService,
        AppDeploymentSynchronizer appDeploymentSynchronizer, AppInstanceService appInstanceService,
        AppService appService, AppVersionService appVersionService) {

        this.appAuditPublisher = appAuditPublisher;
        this.appDeploymentService = appDeploymentService;
        this.appDeploymentSynchronizer = appDeploymentSynchronizer;
        this.appInstanceService = appInstanceService;
        this.appService = appService;
        this.appVersionService = appVersionService;
    }

    @Override
    @PreAuthorize("hasPermission(#appInstanceId, 'AppInstance', 'APP_DEPLOY')")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AppDeploymentDTO cancel(long appInstanceId, long appDeploymentId) {
        for (int attempt = 0; attempt < MAX_CANCEL_ATTEMPTS; attempt++) {
            AppDeployment appDeployment = appDeploymentService.getAppDeployment(appDeploymentId);

            if (appDeployment.getAppInstanceId() != appInstanceId) {
                throw new ConfigurationException(
                    "App deployment %s not found".formatted(appDeploymentId), AppErrorType.APP_DEPLOYMENT_NOT_FOUND);
            }

            AppDeploymentStatus status = appDeployment.getStatus();

            if (status.isTerminal()) {
                throw new ConfigurationException(
                    "App deployment %s is %s and cannot be canceled".formatted(appDeploymentId, status),
                    AppErrorType.APP_DEPLOYMENT_TERMINAL);
            }

            String providerDeploymentId = appDeployment.getProviderDeploymentId();

            if (providerDeploymentId != null) {
                cancelAtProvider(appInstanceId, appDeployment, providerDeploymentId);
            }

            Optional<AppDeployment> canceledAppDeployment;

            try {
                canceledAppDeployment = appDeploymentSynchronizer.cancel(appDeploymentId, providerDeploymentId);
            } catch (OptimisticLockingFailureException optimisticLockingFailureException) {
                log.debug("App deployment {} changed while it was being canceled", appDeploymentId);

                continue;
            }

            if (canceledAppDeployment.isPresent()) {
                AppDeployment canceled = canceledAppDeployment.get();

                AppVersion appVersion = appVersionService.getAppVersion(canceled.getAppVersionId());

                return AppDeploymentDTO.of(canceled, AppVersionDTO.of(appVersion), false);
            }
        }

        throw new ConfigurationException(
            "App deployment %s kept changing while it was being canceled".formatted(appDeploymentId),
            AppErrorType.APP_DEPLOYMENT_IN_PROGRESS);
    }

    @Override
    @PreAuthorize("hasPermission(#appInstanceId, 'AppInstance', 'APP_DEPLOY')")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AppDeploymentDTO deploy(long appInstanceId, long appVersionId) {
        AppInstance appInstance = getDeployableAppInstance(appInstanceId);

        checkProviderAvailable(appInstance);

        AppVersion appVersion = getAppVersion(appInstance, appVersionId);

        return startDeployment(appInstance, appVersion, false);
    }

    @Override
    @PreAuthorize("hasPermission(#appInstanceId, 'AppInstance', 'APP_DEPLOY')")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AppDeploymentDTO rollback(long appInstanceId, long appVersionId) {
        AppInstance appInstance = getDeployableAppInstance(appInstanceId);

        checkProviderAvailable(appInstance);

        AppVersion appVersion = getAppVersion(appInstance, appVersionId);

        if (!appDeploymentService.hasReadyAppDeployment(appInstanceId, appVersionId)) {
            throw new ConfigurationException(
                "App version %s was never deployed to app instance %s".formatted(appVersionId, appInstanceId),
                AppErrorType.APP_VERSION_NOT_FOUND);
        }

        return startDeployment(appInstance, appVersion, true);
    }

    @Override
    @PreAuthorize("hasPermission(#appId, 'App', 'APP_DEPLOY')")
    public AppVersionDTO createVersion(
        long appId, String name, @Nullable String gitRef, @Nullable String gitCommitSha,
        @Nullable String description) {

        String versionName = normalizeVersionName(name);

        appService.getApp(appId);

        AppVersion appVersion = new AppVersion();

        appVersion.setAppId(appId);
        appVersion.setName(versionName);
        appVersion.setGitRef(StringUtils.trimToNull(gitRef));
        appVersion.setGitCommitSha(StringUtils.trimToNull(gitCommitSha));
        appVersion.setDescription(StringUtils.isBlank(description) ? null : description);

        return AppVersionDTO.of(appVersionService.create(appVersion));
    }

    @Override
    @PreAuthorize("hasPermission(#appId, 'App', 'APP_VIEW')")
    @Transactional(readOnly = true)
    public Optional<AppVersionDTO> fetchVersion(long appId, String name) {
        String versionName = normalizeVersionName(name);

        return appVersionService.fetchAppVersion(appId, versionName)
            .map(AppVersionDTO::of);
    }

    @Override
    @PreAuthorize("hasPermission(#appInstanceId, 'AppInstance', 'APP_VIEW')")
    @Transactional(readOnly = true)
    public Optional<AppVersionDTO> fetchVersionForInstance(long appInstanceId, String name) {
        String versionName = normalizeVersionName(name);

        AppInstance appInstance = appInstanceService.getAppInstance(appInstanceId);

        return appVersionService.fetchAppVersion(appInstance.getAppId(), versionName)
            .map(AppVersionDTO::of);
    }

    @Override
    @PreAuthorize("hasPermission(#appInstanceId, 'AppInstance', 'APP_VIEW')")
    @Transactional(readOnly = true)
    public AppDeploymentDTO getDeployment(long appInstanceId, long appDeploymentId) {
        AppInstance appInstance = appInstanceService.getAppInstance(appInstanceId);

        AppDeployment appDeployment = appDeploymentService.getAppDeployment(appDeploymentId);

        if (appDeployment.getAppInstanceId() != appInstanceId) {
            throw new ConfigurationException(
                "App deployment %s not found".formatted(appDeploymentId), AppErrorType.APP_DEPLOYMENT_NOT_FOUND);
        }

        AppVersion appVersion = appVersionService.getAppVersion(appDeployment.getAppVersionId());

        return AppDeploymentDTO.of(
            appDeployment, AppVersionDTO.of(appVersion),
            Objects.equals(appDeployment.getId(), appInstance.getCurrentAppDeploymentId()));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public boolean isDeploymentProviderAvailable(AppHostingTarget hostingTarget) {
        Optional<AppDeploymentProvider> appDeploymentProvider = appDeploymentSynchronizer.fetchProvider(
            hostingTarget);

        return appDeploymentProvider.isPresent();
    }

    @Override
    @PreAuthorize("hasPermission(#appInstanceId, 'AppInstance', 'APP_VIEW')")
    @Transactional(readOnly = true)
    public Optional<AppDeploymentDTO> getCurrentDeployment(long appInstanceId) {
        AppInstance appInstance = appInstanceService.getAppInstance(appInstanceId);

        Long currentAppDeploymentId = appInstance.getCurrentAppDeploymentId();

        if (currentAppDeploymentId == null) {
            return Optional.empty();
        }

        AppDeployment appDeployment = appDeploymentService.getAppDeployment(currentAppDeploymentId);

        if (appDeployment.getAppInstanceId() != appInstanceId) {
            log.warn(
                "App instance {} points at deployment {} of app instance {}; treating it as no current deployment",
                appInstanceId, currentAppDeploymentId, appDeployment.getAppInstanceId());

            return Optional.empty();
        }

        AppVersion appVersion = appVersionService.getAppVersion(appDeployment.getAppVersionId());

        return Optional.of(AppDeploymentDTO.of(appDeployment, AppVersionDTO.of(appVersion), true));
    }

    @Override
    @PreAuthorize("hasPermission(#appInstanceId, 'AppInstance', 'APP_VIEW')")
    @Transactional(readOnly = true)
    public List<AppDeploymentDTO> getDeployments(long appInstanceId, int limit) {
        validateLimit(limit);

        AppInstance appInstance = appInstanceService.getAppInstance(appInstanceId);

        List<AppDeployment> appDeployments = appDeploymentService.getAppDeployments(appInstanceId, limit);

        if (appDeployments.isEmpty()) {
            return List.of();
        }

        List<Long> appVersionIds = appDeployments.stream()
            .map(AppDeployment::getAppVersionId)
            .distinct()
            .toList();

        Map<Long, AppVersionDTO> appVersions = appVersionService.getAppVersionsByIds(appVersionIds)
            .stream()
            .map(AppVersionDTO::of)
            .collect(Collectors.toMap(AppVersionDTO::id, Function.identity()));

        Long currentAppDeploymentId = appInstance.getCurrentAppDeploymentId();

        return appDeployments.stream()
            .map(appDeployment -> AppDeploymentDTO.of(
                appDeployment, appVersions.get(appDeployment.getAppVersionId()),
                Objects.equals(appDeployment.getId(), currentAppDeploymentId)))
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#appId, 'App', 'APP_VIEW')")
    @Transactional(readOnly = true)
    public List<AppVersionDTO> getVersions(long appId, int limit) {
        validateLimit(limit);

        return appVersionService.getAppVersions(appId, limit)
            .stream()
            .map(AppVersionDTO::of)
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#appInstanceId, 'AppInstance', 'APP_VIEW')")
    @Transactional(readOnly = true)
    public List<AppVersionDTO> getVersionsForInstance(long appInstanceId, int limit) {
        validateLimit(limit);

        AppInstance appInstance = appInstanceService.getAppInstance(appInstanceId);

        return appVersionService.getAppVersions(appInstance.getAppId(), limit)
            .stream()
            .map(AppVersionDTO::of)
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#appInstanceId, 'AppInstance', 'APP_DEPLOY')")
    public AppDeploymentDTO recordDeployment(
        long appInstanceId, String versionName, @Nullable String url, @Nullable String gitCommitSha) {

        String normalizedVersionName = normalizeVersionName(versionName);
        String deploymentUrl = validateUrl(StringUtils.trimToNull(url));
        String normalizedGitCommitSha = StringUtils.trimToNull(gitCommitSha);

        AppInstance appInstance = appInstanceService.getAppInstance(appInstanceId);

        checkNotTerminated(appInstance);

        long appId = appInstance.getAppId();

        AppVersion appVersion = resolveAppVersion(appId, normalizedVersionName, normalizedGitCommitSha);

        AppDeployment appDeployment = new AppDeployment();

        appDeployment.setAppInstanceId(appInstanceId);
        appDeployment.setAppVersionId(appVersion.getId());
        appDeployment.setProvider(AppDeploymentProviderType.EXTERNAL);
        appDeployment.setStatus(AppDeploymentStatus.READY);
        appDeployment.setUrl(deploymentUrl);

        AppDeployment recordedAppDeployment = appDeploymentService.create(appDeployment);

        appDeploymentSynchronizer.markCurrent(appId, recordedAppDeployment, appVersion);

        return AppDeploymentDTO.of(recordedAppDeployment, AppVersionDTO.of(appVersion), true);
    }

    private static void checkNotTerminated(AppInstance appInstance) {
        if (appInstance.getStatus() == AppInstanceStatus.TERMINATED) {
            throw new ConfigurationException(
                "App instance %s is terminated".formatted(appInstance.getId()), AppErrorType.APP_INSTANCE_TERMINATED);
        }
    }

    private static ConfigurationException providerUnavailable(String message) {
        return new ConfigurationException(message, AppErrorType.APP_DEPLOYMENT_PROVIDER_UNAVAILABLE);
    }

    private void cancelAtProvider(long appInstanceId, AppDeployment appDeployment, String providerDeploymentId) {
        AppInstance appInstance = appInstanceService.getAppInstance(appInstanceId);

        Optional<AppDeploymentProvider> optionalAppDeploymentProvider = appDeploymentSynchronizer.fetchProvider(
            appDeployment.getProvider());

        if (optionalAppDeploymentProvider.isEmpty()) {
            log.warn(
                "App deployment {} is canceled without its provider: no {} deployment provider is installed",
                appDeployment.getId(), appDeployment.getProvider());

            return;
        }

        AppDeploymentProvider appDeploymentProvider = optionalAppDeploymentProvider.get();
        Long deploymentConnectionId = appInstance.getDeploymentConnectionId();

        if (deploymentConnectionId == null ||
            !appDeploymentSynchronizer.isConnectionOfProviderComponent(appDeploymentProvider, deploymentConnectionId)) {

            log.warn(
                "App deployment {} is canceled without its provider: app instance {} has no deployment connection " +
                    "of the provider's component",
                appDeployment.getId(), appInstanceId);

            return;
        }

        try {
            appDeploymentProvider.cancel(
                new ProviderDeploymentRef(
                    deploymentConnectionId, appInstance.getDeploymentSettings(), providerDeploymentId));
        } catch (RuntimeException exception) {
            log.warn(
                "The deployment provider could not cancel app deployment {} ({})", appDeployment.getId(),
                exception.getClass()
                    .getName());

            throw new ConfigurationException(
                "The hosting provider could not cancel app deployment %s".formatted(appDeployment.getId()),
                AppErrorType.APP_DEPLOYMENT_PROVIDER_FAILED);
        }
    }

    private void checkProviderAvailable(AppInstance appInstance) {
        AppHostingTarget hostingTarget = appInstance.getHostingTarget();

        if (appDeploymentSynchronizer.fetchProvider(hostingTarget)
            .isEmpty()) {

            throw providerUnavailable(
                "No deployment provider is available for the %s hosting target".formatted(hostingTarget));
        }

        if (appInstance.getDeploymentConnectionId() == null) {
            throw providerUnavailable(
                "App instance %s has no deployment connection".formatted(appInstance.getId()));
        }
    }

    private AppVersion getAppVersion(AppInstance appInstance, long appVersionId) {
        AppVersion appVersion = appVersionService.getAppVersion(appVersionId);

        if (!Objects.equals(appVersion.getAppId(), appInstance.getAppId())) {
            throw new ConfigurationException(
                "App version %s not found".formatted(appVersionId), AppErrorType.APP_VERSION_NOT_FOUND);
        }

        return appVersion;
    }

    private AppInstance getDeployableAppInstance(long appInstanceId) {
        AppInstance appInstance = appInstanceService.getAppInstance(appInstanceId);

        checkNotTerminated(appInstance);

        return appInstance;
    }

    private AppDeploymentDTO startDeployment(AppInstance appInstance, AppVersion appVersion, boolean rollback) {
        AppDeploymentProviderType providerType = AppDeploymentSynchronizer.toProviderType(
            appInstance.getHostingTarget())
            .orElseThrow(() -> providerUnavailable(
                "The %s hosting target has no deployment provider".formatted(appInstance.getHostingTarget())));

        AppDeployment appDeployment = new AppDeployment();

        appDeployment.setAppInstanceId(appInstance.getId());
        appDeployment.setAppVersionId(appVersion.getId());
        appDeployment.setProvider(providerType);
        appDeployment.setStatus(AppDeploymentStatus.QUEUED);

        if (rollback) {
            appDeployment.setMetadata(Map.of("rollback", true));
        }

        AppDeployment queuedAppDeployment = appDeploymentService.create(appDeployment);

        long appDeploymentId = queuedAppDeployment.getId();

        ProviderStart providerStart = prepareProviderStart(
            appInstance, appVersion, appDeploymentId, providerType, rollback);

        String startFailureMessage = providerStart.failureMessage();

        if (startFailureMessage != null) {
            return toDeploymentDTO(failOrGet(appDeploymentId, startFailureMessage), appVersion);
        }

        AppDeploymentProvider appDeploymentProvider = Objects.requireNonNull(providerStart.appDeploymentProvider());

        ProviderDeployment providerDeployment;

        try {
            providerDeployment = appDeploymentProvider.deploy(Objects.requireNonNull(providerStart.request()));
        } catch (RuntimeException exception) {
            log.warn(
                "The deployment provider could not start app deployment {} ({})", appDeploymentId,
                exception.getClass()
                    .getName());

            return toDeploymentDTO(failOrGet(appDeploymentId, PROVIDER_START_FAILED), appVersion);
        }

        AppDeployment appliedAppDeployment;

        try {
            appliedAppDeployment = appDeploymentSynchronizer.applyProviderDeployment(
                appDeploymentId, providerDeployment);
        } catch (RuntimeException exception) {
            log.warn(
                "Could not store what the deployment provider returned for app deployment {} ({})", appDeploymentId,
                exception.getClass()
                    .getName());

            appliedAppDeployment = failOrGet(appDeploymentId, PROVIDER_RESPONSE_REJECTED);
        }

        String answeredProviderDeploymentId = providerDeployment.id();
        AppDeploymentStatus appliedStatus = appliedAppDeployment.getStatus();

        if (appliedStatus.isTerminal()
            && !answeredProviderDeploymentId.equals(appliedAppDeployment.getProviderDeploymentId())) {

            appDeploymentSynchronizer.cancelAtProvider(appliedAppDeployment, answeredProviderDeploymentId);
        }

        return toDeploymentDTO(appliedAppDeployment, appVersion);
    }

    private ProviderStart prepareProviderStart(
        AppInstance appInstance, AppVersion appVersion, long appDeploymentId, AppDeploymentProviderType providerType,
        boolean rollback) {

        try {
            Map<String, Object> auditData = new HashMap<>();

            auditData.put("appInstanceId", appInstance.getId());
            auditData.put("appDeploymentId", appDeploymentId);
            auditData.put("appVersionId", appVersion.getId());
            auditData.put("versionName", appVersion.getName());
            auditData.put("provider", providerType.name());

            if (rollback) {
                auditData.put("rollback", true);
            }

            appAuditPublisher.publish(AppAuditEvent.APP_DEPLOYMENT_INITIATED, appInstance.getAppId(), auditData);

            AppInstance deployingAppInstance = appInstanceService.getAppInstance(appInstance.getId());

            Long deploymentConnectionId = deployingAppInstance.getDeploymentConnectionId();

            if (deploymentConnectionId == null) {
                return ProviderStart.failed(NO_DEPLOYMENT_CONNECTION);
            }

            AppHostingTarget hostingTarget = deployingAppInstance.getHostingTarget();

            Optional<AppDeploymentProvider> appDeploymentProvider = appDeploymentSynchronizer.fetchProvider(
                hostingTarget);

            if (appDeploymentProvider.isEmpty() ||
                !Optional.of(providerType)
                    .equals(AppDeploymentSynchronizer.toProviderType(hostingTarget))) {

                return ProviderStart.failed(NO_DEPLOYMENT_PROVIDER);
            }

            if (!appDeploymentSynchronizer.isConnectionOfProviderComponent(
                appDeploymentProvider.get(), deploymentConnectionId)) {

                return ProviderStart.failed(AppDeploymentSynchronizer.CONNECTION_COMPONENT_MISMATCH_MESSAGE);
            }

            ProviderDeploymentRequest providerDeploymentRequest = new ProviderDeploymentRequest(
                deploymentConnectionId, deployingAppInstance.getDeploymentSettings(), appVersion.getGitRef(),
                appVersion.getGitCommitSha(), deployingAppInstance.getEnvironment(), appVersion.getName());

            return new ProviderStart(appDeploymentProvider.get(), providerDeploymentRequest, null);
        } catch (RuntimeException exception) {
            log.warn(
                "Could not prepare app deployment {} for its provider ({})", appDeploymentId, exception.getClass()
                    .getName());

            return ProviderStart.failed(DEPLOYMENT_START_FAILED);
        }
    }

    private AppDeployment failOrGet(long appDeploymentId, String errorMessage) {
        try {
            return appDeploymentSynchronizer.fail(appDeploymentId, errorMessage)
                .orElseGet(() -> appDeploymentService.getAppDeployment(appDeploymentId));
        } catch (RuntimeException exception) {
            log.warn(
                "Could not mark app deployment {} as failed ({})", appDeploymentId, exception.getClass()
                    .getName());

            throw exception;
        }
    }

    private static AppDeploymentDTO toDeploymentDTO(AppDeployment appDeployment, AppVersion appVersion) {
        return AppDeploymentDTO.of(
            appDeployment, AppVersionDTO.of(appVersion), appDeployment.getStatus() == AppDeploymentStatus.READY);
    }

    private static String normalizeVersionName(String versionName) {
        Validate.notBlank(versionName, "versionName");

        String normalizedVersionName = versionName.trim();

        for (int index = 0; index < normalizedVersionName.length(); index++) {
            if (Character.isISOControl(normalizedVersionName.charAt(index))) {
                throw new IllegalArgumentException("The version name must not contain control characters");
            }
        }

        return normalizedVersionName;
    }

    private static void validateLimit(int limit) {
        if (limit < 1 || limit > MAX_LIST_LIMIT) {
            throw new ConfigurationException(
                "The limit must be from 1 to %d".formatted(MAX_LIST_LIMIT), AppErrorType.APP_LIST_LIMIT_INVALID);
        }
    }

    private static @Nullable String validateUrl(@Nullable String url) {
        if (url == null) {
            return null;
        }

        if (!AppDeploymentSynchronizer.isHttpUrl(url)) {
            throw invalidUrl();
        }

        return url;
    }

    private static ConfigurationException invalidUrl() {
        return new ConfigurationException(
            "The deployment url must be an absolute http or https URL", AppErrorType.APP_DEPLOYMENT_URL_INVALID);
    }

    private AppVersion resolveAppVersion(long appId, String versionName, @Nullable String gitCommitSha) {
        Optional<AppVersion> appVersionOptional = appVersionService.fetchAppVersion(appId, versionName);

        if (appVersionOptional.isEmpty()) {
            AppVersion appVersion = new AppVersion();

            appVersion.setAppId(appId);
            appVersion.setName(versionName);
            appVersion.setGitCommitSha(gitCommitSha);

            return appVersionService.create(appVersion);
        }

        AppVersion appVersion = appVersionOptional.get();

        if (gitCommitSha != null && !StringUtils.equalsIgnoreCase(gitCommitSha, appVersion.getGitCommitSha())) {
            throw new ConfigurationException(
                "App version '%s' is commit %s, not %s".formatted(
                    versionName, Objects.toString(appVersion.getGitCommitSha(), "unknown"), gitCommitSha),
                AppErrorType.APP_VERSION_COMMIT_MISMATCH);
        }

        return appVersion;
    }

    private record ProviderStart(
        @Nullable AppDeploymentProvider appDeploymentProvider, @Nullable ProviderDeploymentRequest request,
        @Nullable String failureMessage) {

        static ProviderStart failed(String failureMessage) {
            return new ProviderStart(null, null, failureMessage);
        }
    }
}
