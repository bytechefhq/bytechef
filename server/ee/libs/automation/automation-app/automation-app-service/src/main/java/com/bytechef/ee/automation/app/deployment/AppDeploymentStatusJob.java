/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.deployment;

import com.bytechef.async.config.AsyncConfiguration;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.app.domain.AppDeployment;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.service.AppDeploymentService;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Follows provider deployments that have not finished. Runs on {@code bytechef.app.deployment.poll-cron}; for every
 * deployment that is neither {@code READY}, {@code FAILED} nor {@code CANCELED} it asks the provider for the
 * deployment, on {@code appExecutor}, and applies the answer. A deployment still unfinished after
 * {@code bytechef.app.deployment.timeout} (30 minutes when that is unset or not positive) is marked {@code FAILED}
 * without asking the provider. Without {@code appExecutor} (it exists only when
 * {@code bytechef.app.execution.concurrency-limit} is set) the sweep does nothing.
 *
 * <p>
 * Every tenant is swept under its own context; a tenant whose sweep fails is logged and the remaining tenants are still
 * swept. A deployment without a tenant service sweeps the scheduler thread's current tenant.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AppDeploymentStatusJob {

    private static final Logger log = LoggerFactory.getLogger(AppDeploymentStatusJob.class);

    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(30);
    private static final long MAX_SWEEP_WAIT_MINUTES = 5;

    private final ObjectProvider<AsyncTaskExecutor> appExecutorProvider;
    private final ApplicationProperties applicationProperties;
    private final AppDeploymentService appDeploymentService;
    private final AppDeploymentSynchronizer appDeploymentSynchronizer;
    private final AppInstanceService appInstanceService;
    private final ObjectProvider<TenantService> tenantServiceProvider;

    @SuppressFBWarnings("EI")
    public AppDeploymentStatusJob(
        @Qualifier(AsyncConfiguration.APP_EXECUTOR) ObjectProvider<AsyncTaskExecutor> appExecutorProvider,
        ApplicationProperties applicationProperties, AppDeploymentService appDeploymentService,
        AppDeploymentSynchronizer appDeploymentSynchronizer, AppInstanceService appInstanceService,
        ObjectProvider<TenantService> tenantServiceProvider) {

        this.appExecutorProvider = appExecutorProvider;
        this.applicationProperties = applicationProperties;
        this.appDeploymentService = appDeploymentService;
        this.appDeploymentSynchronizer = appDeploymentSynchronizer;
        this.appInstanceService = appInstanceService;
        this.tenantServiceProvider = tenantServiceProvider;
    }

    @Scheduled(cron = "${bytechef.app.deployment.poll-cron:*/30 * * * * *}")
    public void pollDeployments() {
        AsyncTaskExecutor appExecutor = appExecutorProvider.getIfAvailable();

        if (appExecutor == null) {
            log.debug("App deployment status sweep skipped: there is no appExecutor");

            return;
        }

        ApplicationProperties.App.Deployment deployment = applicationProperties.getApp()
            .getDeployment();

        Duration configuredTimeout = deployment.getTimeout();
        Duration timeout = configuredTimeout == null || configuredTimeout.isZero() || configuredTimeout.isNegative()
            ? DEFAULT_TIMEOUT : configuredTimeout;

        TenantService tenantService = tenantServiceProvider.getIfAvailable();

        if (tenantService == null) {
            pollDeploymentsForCurrentTenant(appExecutor, timeout);

            return;
        }

        for (String tenantId : tenantService.getTenantIds()) {
            try {
                TenantContext.runWithTenantId(tenantId, () -> pollDeploymentsForCurrentTenant(appExecutor, timeout));
            } catch (RuntimeException exception) {
                log.warn("App deployment status sweep failed for tenant {}", tenantId, exception);
            }
        }
    }

    private static String describe(Duration timeout) {
        long seconds = timeout.toSeconds();

        if (seconds % 60 == 0) {
            return "%d minutes".formatted(seconds / 60);
        }

        return "%d seconds".formatted(seconds);
    }

    private static boolean isExpired(AppDeployment appDeployment, Duration timeout, Instant now) {
        Instant createdDate = appDeployment.getCreatedDate();

        if (createdDate == null) {
            return false;
        }

        Instant deadline = createdDate.plus(timeout);

        return deadline.isBefore(now);
    }

    private void failExpiredDeployment(AppDeployment appDeployment, Duration timeout) {
        Optional<AppDeployment> failedAppDeployment;

        try {
            failedAppDeployment = appDeploymentSynchronizer.fail(
                appDeployment.getId(), "The deployment did not finish within " + describe(timeout));
        } catch (RuntimeException exception) {
            log.warn(
                "Could not mark app deployment {} as timed out ({})", appDeployment.getId(), exception.getClass()
                    .getName());

            return;
        }

        failedAppDeployment.ifPresent(failed -> {
            String providerDeploymentId = failed.getProviderDeploymentId();

            if (providerDeploymentId != null) {
                appDeploymentSynchronizer.cancelAtProvider(failed, providerDeploymentId);
            }
        });
    }

    private void pollDeployment(AppDeployment appDeployment, String providerDeploymentId) {
        try {
            AppInstance appInstance = appInstanceService.getAppInstance(appDeployment.getAppInstanceId());

            Long deploymentConnectionId = appInstance.getDeploymentConnectionId();
            Optional<AppDeploymentProvider> appDeploymentProvider = appDeploymentSynchronizer.fetchProvider(
                appDeployment.getProvider());

            if (deploymentConnectionId == null || appDeploymentProvider.isEmpty()) {
                log.debug(
                    "App deployment {} cannot be followed: no provider or no deployment connection",
                    appDeployment.getId());

                return;
            }

            AppDeploymentProvider provider = appDeploymentProvider.get();

            if (!appDeploymentSynchronizer.isConnectionOfProviderComponent(provider, deploymentConnectionId)) {
                appDeploymentSynchronizer.fail(
                    appDeployment.getId(), AppDeploymentSynchronizer.CONNECTION_COMPONENT_MISMATCH_MESSAGE);

                return;
            }

            ProviderDeployment providerDeployment = provider.getDeployment(
                new ProviderDeploymentRef(
                    deploymentConnectionId, appInstance.getDeploymentSettings(), providerDeploymentId));

            appDeploymentSynchronizer.applyProviderDeployment(appDeployment.getId(), providerDeployment);
        } catch (RuntimeException exception) {
            log.warn(
                "Could not follow app deployment {} ({})", appDeployment.getId(), exception.getClass()
                    .getName());
        }
    }

    private void pollDeploymentsForCurrentTenant(AsyncTaskExecutor appExecutor, Duration timeout) {
        Instant now = Instant.now();
        String tenantId = TenantContext.getCurrentTenantId();
        List<CompletableFuture<Void>> pollFutures = new ArrayList<>();

        for (AppDeployment appDeployment : appDeploymentService.getNonTerminalAppDeployments()) {
            String providerDeploymentId = appDeployment.getProviderDeploymentId();

            if (isExpired(appDeployment, timeout, now)) {
                pollFutures.add(
                    CompletableFuture.runAsync(
                        () -> TenantContext.runWithTenantId(
                            tenantId, () -> failExpiredDeployment(appDeployment, timeout)),
                        appExecutor));
            } else if (providerDeploymentId != null) {
                pollFutures.add(
                    CompletableFuture.runAsync(
                        () -> TenantContext.runWithTenantId(
                            tenantId, () -> pollDeployment(appDeployment, providerDeploymentId)),
                        appExecutor));
            }
        }

        if (pollFutures.isEmpty()) {
            return;
        }

        try {
            CompletableFuture.allOf(pollFutures.toArray(CompletableFuture[]::new))
                .orTimeout(MAX_SWEEP_WAIT_MINUTES, TimeUnit.MINUTES)
                .join();
        } catch (CompletionException completionException) {
            log.warn(
                "App deployment status sweep for tenant {} did not finish within {} minutes", tenantId,
                MAX_SWEEP_WAIT_MINUTES);
        }
    }
}
