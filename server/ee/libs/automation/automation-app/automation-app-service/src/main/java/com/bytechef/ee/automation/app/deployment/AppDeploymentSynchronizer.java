/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.deployment;

import com.bytechef.ee.automation.app.audit.AppAuditEvent;
import com.bytechef.ee.automation.app.audit.AppAuditPublisher;
import com.bytechef.ee.automation.app.domain.AppDeployment;
import com.bytechef.ee.automation.app.domain.AppDeploymentProviderType;
import com.bytechef.ee.automation.app.domain.AppDeploymentStatus;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppVersion;
import com.bytechef.ee.automation.app.service.AppConnectionService;
import com.bytechef.ee.automation.app.service.AppDeploymentService;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppVersionService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Applies what a deployment provider reports to a deployment row, for {@code AppDeploymentFacadeImpl} and
 * {@link AppDeploymentStatusJob} alike, so a {@code READY} deployment becomes the instance's current one in exactly one
 * place. A row in a terminal status is never written again.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@Transactional
@ConditionalOnEEVersion
public class AppDeploymentSynchronizer {

    public static final String CONNECTION_COMPONENT_MISMATCH_MESSAGE =
        "The deployment connection does not belong to the component the hosting provider uses";
    static final String DEPLOYMENT_COUNTER_NAME = "bytechef_app_deployment";
    static final String PROVIDER_FAILED_MESSAGE = "The hosting provider reported that the deployment failed";

    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("[\\p{Cc}\\p{Cf}\\p{Zl}\\p{Zp}]");
    private static final int MAX_PROVIDER_FAILURE_REASON_LENGTH = 500;

    private static final Logger log = LoggerFactory.getLogger(AppDeploymentSynchronizer.class);

    private final AppAuditPublisher appAuditPublisher;
    private final ObjectProvider<AppDeploymentProvider> appDeploymentProviders;
    private final AppConnectionService appConnectionService;
    private final AppDeploymentService appDeploymentService;
    private final AppInstanceService appInstanceService;
    private final AppVersionService appVersionService;
    private final @Nullable MeterRegistry meterRegistry;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public AppDeploymentSynchronizer(
        AppAuditPublisher appAuditPublisher, ObjectProvider<AppDeploymentProvider> appDeploymentProviders,
        AppDeploymentService appDeploymentService, AppConnectionService appConnectionService,
        AppInstanceService appInstanceService, AppVersionService appVersionService,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.appAuditPublisher = appAuditPublisher;
        this.appDeploymentProviders = appDeploymentProviders;
        this.appConnectionService = appConnectionService;
        this.appDeploymentService = appDeploymentService;
        this.appInstanceService = appInstanceService;
        this.appVersionService = appVersionService;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    public static boolean isHttpUrl(String url) {
        URI uri;

        try {
            uri = new URI(url);
        } catch (URISyntaxException uriSyntaxException) {
            return false;
        }

        String scheme = uri.getScheme();

        return scheme != null && !uri.isOpaque() && uri.getHost() != null &&
            (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));
    }

    public static Optional<AppDeploymentProviderType> toProviderType(AppHostingTarget hostingTarget) {
        return switch (hostingTarget) {
            case VERCEL -> Optional.of(AppDeploymentProviderType.VERCEL);
            case SELF_HOSTED -> Optional.of(AppDeploymentProviderType.DEPLOY_HOOK);
        };
    }

    public AppDeployment applyProviderDeployment(long appDeploymentId, ProviderDeployment providerDeployment) {
        AppDeployment appDeployment = appDeploymentService.getAppDeployment(appDeploymentId);

        if (appDeployment.getStatus()
            .isTerminal()) {

            log.debug("App deployment {} is already {}", appDeploymentId, appDeployment.getStatus());

            return appDeployment;
        }

        AppDeploymentStatus status = providerDeployment.status();
        String url = toUrl(appDeploymentId, providerDeployment.url());
        String providerDeploymentId = providerDeployment.id();

        if (status == appDeployment.getStatus() &&
            Objects.equals(providerDeploymentId, appDeployment.getProviderDeploymentId()) &&
            (url == null || url.equals(appDeployment.getUrl()))) {

            return appDeployment;
        }

        AppDeployment updatedAppDeployment = appDeploymentService.updateStatus(
            appDeploymentId, providerDeploymentId, status, url,
            status == AppDeploymentStatus.FAILED ? toFailedMessage(providerDeployment.errorMessage()) : null);

        if (status == AppDeploymentStatus.READY) {
            markCurrent(
                getAppId(updatedAppDeployment), updatedAppDeployment,
                appVersionService.getAppVersion(updatedAppDeployment.getAppVersionId()));
        } else if (status == AppDeploymentStatus.FAILED) {
            publish(AppAuditEvent.APP_DEPLOYMENT_FAILED, updatedAppDeployment);

            countTerminal(updatedAppDeployment, AppDeploymentStatus.FAILED);
        } else if (status == AppDeploymentStatus.CANCELED) {
            publish(AppAuditEvent.APP_DEPLOYMENT_CANCELED, updatedAppDeployment);

            countTerminal(updatedAppDeployment, AppDeploymentStatus.CANCELED);
        }

        return updatedAppDeployment;
    }

    public Optional<AppDeployment> cancel(long appDeploymentId, @Nullable String canceledProviderDeploymentId) {
        Optional<AppDeployment> canceledAppDeployment = appDeploymentService.cancel(
            appDeploymentId, canceledProviderDeploymentId);

        canceledAppDeployment.ifPresent(appDeployment -> {
            publish(AppAuditEvent.APP_DEPLOYMENT_CANCELED, appDeployment);

            countTerminal(appDeployment, AppDeploymentStatus.CANCELED);
        });

        return canceledAppDeployment;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void cancelAtProvider(AppDeployment appDeployment, String providerDeploymentId) {
        long appDeploymentId = appDeployment.getId();

        try {
            AppInstance appInstance = appInstanceService.getAppInstance(appDeployment.getAppInstanceId());

            Long deploymentConnectionId = appInstance.getDeploymentConnectionId();
            Optional<AppDeploymentProvider> appDeploymentProvider = fetchProvider(appDeployment.getProvider());

            if (deploymentConnectionId == null || appDeploymentProvider.isEmpty()) {
                log.warn(
                    "The provider deployment of app deployment {} cannot be canceled: no provider or no deployment " +
                        "connection",
                    appDeploymentId);

                return;
            }

            AppDeploymentProvider provider = appDeploymentProvider.get();

            if (!isConnectionOfProviderComponent(provider, deploymentConnectionId)) {
                log.warn(
                    "The provider deployment of app deployment {} cannot be canceled: the deployment connection " +
                        "does not belong to the provider's component",
                    appDeploymentId);

                return;
            }

            provider.cancel(
                new ProviderDeploymentRef(
                    deploymentConnectionId, appInstance.getDeploymentSettings(), providerDeploymentId));
        } catch (RuntimeException exception) {
            log.warn(
                "The deployment provider could not cancel the provider deployment of app deployment {} ({})",
                appDeploymentId, exception.getClass()
                    .getName());
        }
    }

    public boolean isConnectionOfProviderComponent(AppDeploymentProvider provider, long connectionId) {
        return appConnectionService.isConnectionOfComponent(
            connectionId, provider.getDeploymentConnectionComponentName());
    }

    public Optional<AppDeployment> fail(long appDeploymentId, String errorMessage) {
        AppDeployment appDeployment = appDeploymentService.getAppDeployment(appDeploymentId);

        if (appDeployment.getStatus()
            .isTerminal()) {

            return Optional.empty();
        }

        AppDeployment failedAppDeployment = appDeploymentService.updateStatus(
            appDeploymentId, AppDeploymentStatus.FAILED, null, errorMessage);

        publish(AppAuditEvent.APP_DEPLOYMENT_FAILED, failedAppDeployment);

        countTerminal(failedAppDeployment, AppDeploymentStatus.FAILED);

        return Optional.of(failedAppDeployment);
    }

    public Optional<AppDeploymentProvider> fetchProvider(AppDeploymentProviderType providerType) {
        return switch (providerType) {
            case VERCEL -> fetchProvider(AppHostingTarget.VERCEL);
            case DEPLOY_HOOK -> fetchProvider(AppHostingTarget.SELF_HOSTED);
            case EXTERNAL -> Optional.empty();
        };
    }

    public Optional<AppDeploymentProvider> fetchProvider(AppHostingTarget hostingTarget) {
        return appDeploymentProviders.orderedStream()
            .filter(appDeploymentProvider -> appDeploymentProvider.getHostingTarget() == hostingTarget)
            .findFirst();
    }

    public void markCurrent(long appId, AppDeployment appDeployment, AppVersion appVersion) {
        appInstanceService.updateCurrentAppDeployment(appDeployment.getAppInstanceId(), appDeployment.getId());

        AppDeploymentProviderType provider = appDeployment.getProvider();

        appAuditPublisher.publish(
            AppAuditEvent.APP_DEPLOYMENT_COMPLETED, appId,
            Map.of(
                "appInstanceId", appDeployment.getAppInstanceId(), "appDeploymentId", appDeployment.getId(),
                "appVersionId", appVersion.getId(), "versionName", appVersion.getName(), "provider",
                provider.name()));

        countTerminal(appDeployment, AppDeploymentStatus.READY);
    }

    private void countTerminal(AppDeployment appDeployment, AppDeploymentStatus status) {
        if (meterRegistry == null) {
            return;
        }

        MeterRegistry registry = meterRegistry;
        AppDeploymentProviderType provider = appDeployment.getProvider();

        Runnable increment = () -> {
            try {
                Counter.builder(DEPLOYMENT_COUNTER_NAME)
                    .tag("provider", provider.name())
                    .tag("status", status.name())
                    .description("App deployments that reached a terminal status, tagged by provider and status")
                    .register(registry)
                    .increment();
            } catch (RuntimeException runtimeException) {
                log.warn(
                    "The app deployment counter could not be incremented ({})", runtimeException.getClass()
                        .getName());
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

                @Override
                public void afterCommit() {
                    increment.run();
                }
            });
        } else {
            increment.run();
        }
    }

    private long getAppId(AppDeployment appDeployment) {
        AppInstance appInstance = appInstanceService.getAppInstance(appDeployment.getAppInstanceId());

        return appInstance.getAppId();
    }

    private void publish(AppAuditEvent appAuditEvent, AppDeployment appDeployment) {
        AppDeploymentProviderType provider = appDeployment.getProvider();

        Map<String, Object> data = new HashMap<>();

        data.put("appInstanceId", appDeployment.getAppInstanceId());
        data.put("appDeploymentId", appDeployment.getId());
        data.put("appVersionId", appDeployment.getAppVersionId());
        data.put("provider", provider.name());

        appAuditPublisher.publish(appAuditEvent, getAppId(appDeployment), data);
    }

    private static String toFailedMessage(@Nullable String providerErrorMessage) {
        if (providerErrorMessage == null) {
            return PROVIDER_FAILED_MESSAGE;
        }

        String reason = CONTROL_CHARACTERS.matcher(providerErrorMessage)
            .replaceAll(" ")
            .trim();

        if (reason.length() > MAX_PROVIDER_FAILURE_REASON_LENGTH) {
            int endIndex = MAX_PROVIDER_FAILURE_REASON_LENGTH;

            if (Character.isHighSurrogate(reason.charAt(endIndex - 1))) {
                endIndex--;
            }

            reason = reason.substring(0, endIndex)
                .trim();
        }

        if (reason.isEmpty()) {
            return PROVIDER_FAILED_MESSAGE;
        }

        return PROVIDER_FAILED_MESSAGE + ": " + reason;
    }

    private static @Nullable String toUrl(long appDeploymentId, @Nullable String url) {
        if (url == null || isHttpUrl(url)) {
            return url;
        }

        log.warn("Ignoring a deployment url that is not an absolute http or https url for app deployment {}",
            appDeploymentId);

        return null;
    }
}
