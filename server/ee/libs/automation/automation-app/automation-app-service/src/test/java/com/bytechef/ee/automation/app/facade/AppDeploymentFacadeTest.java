/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.app.audit.AppAuditDataAssertions;
import com.bytechef.ee.automation.app.audit.AppAuditEvent;
import com.bytechef.ee.automation.app.audit.AppAuditPublisher;
import com.bytechef.ee.automation.app.deployment.AppDeploymentProvider;
import com.bytechef.ee.automation.app.deployment.AppDeploymentSynchronizer;
import com.bytechef.ee.automation.app.deployment.ProviderDeployment;
import com.bytechef.ee.automation.app.deployment.ProviderDeploymentRef;
import com.bytechef.ee.automation.app.deployment.ProviderDeploymentRequest;
import com.bytechef.ee.automation.app.domain.App;
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
import com.bytechef.ee.automation.app.service.AppConnectionService;
import com.bytechef.ee.automation.app.service.AppDeploymentService;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.app.service.AppVersionService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AppDeploymentFacadeTest {

    private static final long APP_ID = 1L;
    private static final long APP_INSTANCE_ID = 11L;
    private static final long APP_VERSION_ID = 21L;
    private static final long APP_DEPLOYMENT_ID = 31L;
    private static final long DEPLOYMENT_CONNECTION_ID = 41L;
    private static final long EARLIER_APP_VERSION_ID = 20L;
    private static final String PROVIDER_SECRET = "vercel_token_s3cr3t";
    private static final String URL = "https://crm.example.com";
    private static final Map<String, Object> DEPLOYMENT_AUDIT_DATA = Map.of(
        "appInstanceId", APP_INSTANCE_ID, "appDeploymentId", APP_DEPLOYMENT_ID, "appVersionId", APP_VERSION_ID,
        "provider", "VERCEL");

    @Mock
    private AppAuditPublisher appAuditPublisher;

    @Mock
    private AppConnectionService appConnectionService;

    @Mock
    private AppDeploymentService appDeploymentService;

    @Mock
    private AppInstanceService appInstanceService;

    @Mock
    private AppService appService;

    @Mock
    private AppVersionService appVersionService;

    private AppDeploymentFacade appDeploymentFacade;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final FakeAppDeploymentProvider fakeAppDeploymentProvider = new FakeAppDeploymentProvider(
        AppHostingTarget.VERCEL);

    private final FakeAppDeploymentProvider fakeSelfHostedAppDeploymentProvider = new FakeAppDeploymentProvider(
        AppHostingTarget.SELF_HOSTED);

    @BeforeEach
    void beforeEach() {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

        beanFactory.addBean("fakeAppDeploymentProvider", fakeAppDeploymentProvider);
        beanFactory.addBean("fakeSelfHostedAppDeploymentProvider", fakeSelfHostedAppDeploymentProvider);

        AppDeploymentSynchronizer appDeploymentSynchronizer = new AppDeploymentSynchronizer(
            appAuditPublisher, beanFactory.getBeanProvider(AppDeploymentProvider.class), appDeploymentService,
            appConnectionService, appInstanceService, appVersionService, meterRegistryProvider(meterRegistry));

        lenient().when(appConnectionService.isConnectionOfComponent(DEPLOYMENT_CONNECTION_ID, "vercel"))
            .thenReturn(true);
        lenient().when(appConnectionService.isConnectionOfComponent(DEPLOYMENT_CONNECTION_ID, "httpClient"))
            .thenReturn(true);

        appDeploymentFacade = new AppDeploymentFacadeImpl(
            appAuditPublisher, appDeploymentService, appDeploymentSynchronizer, appInstanceService, appService,
            appVersionService);
    }

    @AfterEach
    void afterEach() {
        AppAuditDataAssertions.assertNoSecrets(appAuditPublisher, PROVIDER_SECRET, "prj_1");
    }

    @Test
    void testGuardsAreTheBriefsGuards() throws NoSuchMethodException {
        assertThat(guardOf("getCurrentDeployment", long.class))
            .isEqualTo("hasPermission(#appInstanceId, 'AppInstance', 'APP_VIEW')");
        assertThat(guardOf("getDeployments", long.class, int.class))
            .isEqualTo("hasPermission(#appInstanceId, 'AppInstance', 'APP_VIEW')");
        assertThat(guardOf("getVersions", long.class, int.class))
            .isEqualTo("hasPermission(#appId, 'App', 'APP_VIEW')");
        assertThat(guardOf("createVersion", long.class, String.class, String.class, String.class, String.class))
            .isEqualTo("hasPermission(#appId, 'App', 'APP_DEPLOY')");
        assertThat(guardOf("recordDeployment", long.class, String.class, String.class, String.class))
            .isEqualTo("hasPermission(#appInstanceId, 'AppInstance', 'APP_DEPLOY')");
        assertThat(guardOf("deploy", long.class, long.class))
            .isEqualTo("hasPermission(#appInstanceId, 'AppInstance', 'APP_DEPLOY')");
        assertThat(guardOf("rollback", long.class, long.class))
            .isEqualTo("hasPermission(#appInstanceId, 'AppInstance', 'APP_DEPLOY')");
        assertThat(guardOf("cancel", long.class, long.class))
            .isEqualTo("hasPermission(#appInstanceId, 'AppInstance', 'APP_DEPLOY')");
        assertThat(guardOf("getDeployment", long.class, long.class))
            .isEqualTo("hasPermission(#appInstanceId, 'AppInstance', 'APP_VIEW')");
        assertThat(guardOf("fetchVersion", long.class, String.class))
            .isEqualTo("hasPermission(#appId, 'App', 'APP_VIEW')");
        assertThat(guardOf("getVersionsForInstance", long.class, int.class))
            .isEqualTo("hasPermission(#appInstanceId, 'AppInstance', 'APP_VIEW')");
        assertThat(guardOf("fetchVersionForInstance", long.class, String.class))
            .isEqualTo("hasPermission(#appInstanceId, 'AppInstance', 'APP_VIEW')");
        assertThat(guardOf("isDeploymentProviderAvailable", AppHostingTarget.class))
            .isEqualTo("isAuthenticated()");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "deploy", "rollback", "cancel"
    })
    void testProviderCallsRunOutsideATransaction(String methodName) throws NoSuchMethodException {
        Method method = AppDeploymentFacadeImpl.class.getMethod(methodName, long.class, long.class);

        Transactional transactional = Objects.requireNonNull(method.getAnnotation(Transactional.class));

        assertThat(transactional.propagation()).isEqualTo(Propagation.NOT_SUPPORTED);
    }

    @Test
    void testProviderCancelOfTheSynchronizerRunsOutsideATransaction() throws NoSuchMethodException {
        Method method = AppDeploymentSynchronizer.class.getMethod(
            "cancelAtProvider", AppDeployment.class, String.class);

        Transactional transactional = Objects.requireNonNull(method.getAnnotation(Transactional.class));

        assertThat(transactional.propagation()).isEqualTo(Propagation.NOT_SUPPORTED);
    }

    @Test
    void testDeployWritesQueuedRowAndCallsProvider() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");

        Map<Long, AppDeployment> appDeployments = stubDeploymentStore();

        fakeAppDeploymentProvider.nextDeployment = new ProviderDeployment(
            "dpl_1", AppDeploymentStatus.BUILDING, "https://crm-abc.vercel.app");

        AppDeploymentDTO appDeploymentDTO = appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID);

        ArgumentCaptor<AppDeployment> appDeploymentCaptor = ArgumentCaptor.forClass(AppDeployment.class);

        verify(appDeploymentService).create(appDeploymentCaptor.capture());

        assertThat(appDeploymentCaptor.getValue()
            .getProvider()).isEqualTo(AppDeploymentProviderType.VERCEL);
        assertThat(fakeAppDeploymentProvider.queuedStatusAtDeploy).isEqualTo(AppDeploymentStatus.QUEUED);
        assertThat(fakeAppDeploymentProvider.deployRequests)
            .singleElement()
            .satisfies(request -> {
                assertThat(request.connectionId()).isEqualTo(DEPLOYMENT_CONNECTION_ID);
                assertThat(request.deploymentSettings()).isEqualTo(Map.of("projectId", "prj_1"));
                assertThat(request.gitRef()).isEqualTo("release/1.2");
                assertThat(request.gitCommitSha()).isEqualTo("abc123");
                assertThat(request.environment()).isEqualTo(Environment.PRODUCTION);
            });

        assertThat(appDeploymentDTO.id()).isEqualTo(APP_DEPLOYMENT_ID);
        assertThat(appDeploymentDTO.status()).isEqualTo(AppDeploymentStatus.BUILDING);
        assertThat(appDeploymentDTO.providerDeploymentId()).isEqualTo("dpl_1");
        assertThat(appDeploymentDTO.url()).isEqualTo("https://crm-abc.vercel.app");
        assertThat(appDeploymentDTO.current()).isFalse();
        assertThat(appDeployments.get(APP_DEPLOYMENT_ID)
            .getStatus()).isEqualTo(AppDeploymentStatus.BUILDING);

        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_DEPLOYMENT_INITIATED, APP_ID,
            Map.of(
                "appInstanceId", APP_INSTANCE_ID, "appDeploymentId", APP_DEPLOYMENT_ID, "appVersionId",
                APP_VERSION_ID, "versionName", "1.2", "provider", "VERCEL"));
        verify(appInstanceService, never()).updateCurrentAppDeployment(anyLong(), anyLong());
    }

    @Test
    void testDeployOfADeploymentTheProviderReportsReadySetsTheCurrentDeployment() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");
        stubDeploymentStore();

        fakeAppDeploymentProvider.nextDeployment = new ProviderDeployment(
            "dpl_1", AppDeploymentStatus.READY, "https://crm.vercel.app");

        AppDeploymentDTO appDeploymentDTO = appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID);

        assertThat(appDeploymentDTO.status()).isEqualTo(AppDeploymentStatus.READY);
        assertThat(appDeploymentDTO.current()).isTrue();

        verify(appInstanceService).updateCurrentAppDeployment(APP_INSTANCE_ID, APP_DEPLOYMENT_ID);
        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_DEPLOYMENT_COMPLETED, APP_ID,
            Map.of(
                "appInstanceId", APP_INSTANCE_ID, "appDeploymentId", APP_DEPLOYMENT_ID, "appVersionId",
                APP_VERSION_ID, "versionName", "1.2", "provider", "VERCEL"));
    }

    @Test
    void testSelfHostedHostingTargetResolvesTheDeployHookProviderType() {
        assertThat(AppDeploymentSynchronizer.toProviderType(AppHostingTarget.SELF_HOSTED))
            .contains(AppDeploymentProviderType.DEPLOY_HOOK);
        assertThat(AppDeploymentSynchronizer.toProviderType(AppHostingTarget.VERCEL))
            .contains(AppDeploymentProviderType.VERCEL);
    }

    @Test
    void testSelfHostedInstanceWithADeploymentConnectionDeploysThroughTheDeployHookProvider() {
        stubDeployableInstance(AppHostingTarget.SELF_HOSTED, DEPLOYMENT_CONNECTION_ID);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");
        stubDeploymentStore();

        fakeSelfHostedAppDeploymentProvider.nextDeployment = new ProviderDeployment(
            "hook_1", AppDeploymentStatus.BUILDING, null);

        AppDeploymentDTO appDeploymentDTO = appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID);

        ArgumentCaptor<AppDeployment> appDeploymentCaptor = ArgumentCaptor.forClass(AppDeployment.class);

        verify(appDeploymentService).create(appDeploymentCaptor.capture());

        assertThat(appDeploymentCaptor.getValue()
            .getProvider()).isEqualTo(AppDeploymentProviderType.DEPLOY_HOOK);
        assertThat(appDeploymentDTO.status()).isEqualTo(AppDeploymentStatus.BUILDING);
        assertThat(appDeploymentDTO.providerDeploymentId()).isEqualTo("hook_1");
        assertThat(fakeAppDeploymentProvider.deployRequests).isEmpty();
        assertThat(fakeSelfHostedAppDeploymentProvider.deployRequests)
            .singleElement()
            .satisfies(request -> {
                assertThat(request.connectionId()).isEqualTo(DEPLOYMENT_CONNECTION_ID);
                assertThat(request.gitRef()).isEqualTo("release/1.2");
                assertThat(request.gitCommitSha()).isEqualTo("abc123");
                assertThat(request.versionName()).isEqualTo("1.2");
                assertThat(request.environment()).isEqualTo(Environment.PRODUCTION);
            });
    }

    @Test
    void testSelfHostedInstanceWithoutADeploymentConnectionAnswersProviderUnavailable() {
        stubDeployableInstance(AppHostingTarget.SELF_HOSTED, null);

        assertThatThrownBy(() -> appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_DEPLOYMENT_PROVIDER_UNAVAILABLE.getErrorKey()));

        assertThat(fakeAppDeploymentProvider.deployRequests).isEmpty();
        assertThat(fakeSelfHostedAppDeploymentProvider.deployRequests).isEmpty();

        verify(appDeploymentService, never()).create(any());
    }

    @Test
    void testInstanceWithoutADeploymentConnectionAnswersProviderUnavailable() {
        stubDeployableInstance(AppHostingTarget.VERCEL, null);

        assertThatThrownBy(() -> appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_DEPLOYMENT_PROVIDER_UNAVAILABLE.getErrorKey()));

        assertThat(fakeAppDeploymentProvider.deployRequests).isEmpty();

        verify(appDeploymentService, never()).create(any());
    }

    @Test
    void testRecordStillWorksOnASelfHostedInstance() {
        AppInstance appInstance = stubInstance(AppInstanceStatus.ACTIVE);

        appInstance.setHostingTarget(AppHostingTarget.SELF_HOSTED);

        when(appVersionService.fetchAppVersion(APP_ID, "1.1"))
            .thenReturn(Optional.of(appVersion("1.1", "abc123")));
        stubDeploymentCreate();

        AppDeploymentDTO appDeploymentDTO = appDeploymentFacade.recordDeployment(
            APP_INSTANCE_ID, "1.1", URL, "abc123");

        assertThat(appDeploymentDTO.status()).isEqualTo(AppDeploymentStatus.READY);
        assertThat(appDeploymentDTO.provider()).isEqualTo(AppDeploymentProviderType.EXTERNAL);
        assertThat(fakeSelfHostedAppDeploymentProvider.deployRequests).isEmpty();
        assertThat(fakeSelfHostedAppDeploymentProvider.cancelAttempts).isZero();
    }

    @Test
    void testDeployOnTerminatedInstanceIsRefused() {
        AppInstance appInstance = stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);

        appInstance.setStatus(AppInstanceStatus.TERMINATED);

        assertThatThrownBy(() -> appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_INSTANCE_TERMINATED.getErrorKey()));

        assertThat(fakeAppDeploymentProvider.deployRequests).isEmpty();

        verifyNoInteractions(appDeploymentService);
    }

    @Test
    void testDeployOfAVersionOfAnotherAppIsNotFound() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);

        AppVersion foreignAppVersion = stubVersion(APP_VERSION_ID, "1.2", null, "abc123");

        foreignAppVersion.setAppId(APP_ID + 1);

        assertThatThrownBy(() -> appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_VERSION_NOT_FOUND.getErrorKey()));

        verifyNoInteractions(appDeploymentService);
    }

    @Test
    void testSecondDeployWhileBuildingIsRefused() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");

        when(appDeploymentService.create(any())).thenThrow(
            new ConfigurationException(
                "App instance 11 already has a deployment in progress", AppErrorType.APP_DEPLOYMENT_IN_PROGRESS));

        assertThatThrownBy(() -> appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_DEPLOYMENT_IN_PROGRESS.getErrorKey()));

        assertThat(fakeAppDeploymentProvider.deployRequests).isEmpty();

        verify(appDeploymentService, never()).updateStatus(anyLong(), any(), any(), any(), any());
        verifyNoInteractions(appAuditPublisher);
    }

    @Test
    void testDeployLeavesTheRowFailedWithoutTheProviderMessageWhenTheProviderThrows() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");

        Map<Long, AppDeployment> appDeployments = stubDeploymentStore();

        fakeAppDeploymentProvider.deployFailure = new IllegalStateException(
            "401 from https://api.vercel.com with Authorization: Bearer " + PROVIDER_SECRET + " body {\"error\"}");

        AppDeploymentDTO appDeploymentDTO = appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID);

        AppDeployment storedAppDeployment = appDeployments.get(APP_DEPLOYMENT_ID);

        assertThat(appDeploymentDTO.status()).isEqualTo(AppDeploymentStatus.FAILED);
        assertThat(storedAppDeployment.getStatus()).isEqualTo(AppDeploymentStatus.FAILED);
        assertThat(storedAppDeployment.getErrorMessage())
            .isNotBlank()
            .doesNotContain(PROVIDER_SECRET)
            .doesNotContain("Bearer")
            .doesNotContain("error");
        assertThat(storedAppDeployment.getProviderDeploymentId()).isNull();
        assertThat(storedAppDeployment.getFinishedDate()).isNotNull();
        assertThat(appDeploymentDTO.current()).isFalse();

        verify(appInstanceService, never()).updateCurrentAppDeployment(anyLong(), anyLong());
        verify(appAuditPublisher).publish(AppAuditEvent.APP_DEPLOYMENT_FAILED, APP_ID, DEPLOYMENT_AUDIT_DATA);
    }

    @Test
    void testAnswerArrivingAfterACancelCancelsTheProviderDeployment() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");

        Map<Long, AppDeployment> appDeployments = stubDeploymentStore();

        fakeAppDeploymentProvider.duringDeploy = () -> {
            AppDeployment queuedAppDeployment = appDeployments.get(APP_DEPLOYMENT_ID);

            queuedAppDeployment.setStatus(AppDeploymentStatus.CANCELED);
            queuedAppDeployment.setFinishedDate(Instant.now());
        };
        fakeAppDeploymentProvider.nextDeployment = new ProviderDeployment(
            "dpl_1", AppDeploymentStatus.BUILDING, null);

        AppDeploymentDTO appDeploymentDTO = appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID);

        AppDeployment storedAppDeployment = appDeployments.get(APP_DEPLOYMENT_ID);

        assertThat(fakeAppDeploymentProvider.canceledRefs)
            .singleElement()
            .satisfies(ref -> {
                assertThat(ref.providerDeploymentId()).isEqualTo("dpl_1");
                assertThat(ref.connectionId()).isEqualTo(DEPLOYMENT_CONNECTION_ID);
                assertThat(ref.deploymentSettings()).isEqualTo(Map.of("projectId", "prj_1"));
            });
        assertThat(appDeploymentDTO.status()).isEqualTo(AppDeploymentStatus.CANCELED);
        assertThat(storedAppDeployment.getStatus()).isEqualTo(AppDeploymentStatus.CANCELED);
        assertThat(storedAppDeployment.getProviderDeploymentId()).isNull();

        verify(appInstanceService, never()).updateCurrentAppDeployment(anyLong(), anyLong());
    }

    @Test
    void testAnswerArrivingAfterACancelKeepsTheRowWhenTheProviderCannotCancel() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");

        Map<Long, AppDeployment> appDeployments = stubDeploymentStore();

        fakeAppDeploymentProvider.duringDeploy = () -> {
            AppDeployment queuedAppDeployment = appDeployments.get(APP_DEPLOYMENT_ID);

            queuedAppDeployment.setStatus(AppDeploymentStatus.CANCELED);
            queuedAppDeployment.setFinishedDate(Instant.now());
        };
        fakeAppDeploymentProvider.cancelFailure = new IllegalStateException("Bearer " + PROVIDER_SECRET);

        AppDeploymentDTO appDeploymentDTO = appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID);

        assertThat(appDeploymentDTO.status()).isEqualTo(AppDeploymentStatus.CANCELED);
        assertThat(fakeAppDeploymentProvider.cancelAttempts).isEqualTo(1);
    }

    @Test
    void testRejectedProviderAnswerFailsTheRowAndCancelsTheProviderDeployment() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");

        Map<Long, AppDeployment> appDeployments = stubDeploymentStore();

        doThrow(new ConfigurationException("too long", AppErrorType.APP_FIELD_TOO_LONG))
            .when(appDeploymentService)
            .updateStatus(anyLong(), any(), any(), any(), any());

        fakeAppDeploymentProvider.nextDeployment = new ProviderDeployment(
            "dpl_1", AppDeploymentStatus.BUILDING, null);

        AppDeploymentDTO appDeploymentDTO = appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID);

        assertThat(appDeploymentDTO.status()).isEqualTo(AppDeploymentStatus.FAILED);
        assertThat(appDeployments.get(APP_DEPLOYMENT_ID)
            .getErrorMessage()).isEqualTo("The hosting provider returned a deployment that could not be stored");
        assertThat(fakeAppDeploymentProvider.canceledRefs)
            .extracting(ProviderDeploymentRef::providerDeploymentId)
            .containsExactly("dpl_1");
    }

    @Test
    void testDeployOfAnAnswerTheProviderReportsFailedDoesNotCancelIt() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");
        stubDeploymentStore();

        fakeAppDeploymentProvider.nextDeployment = new ProviderDeployment(
            "dpl_1", AppDeploymentStatus.FAILED, null);

        AppDeploymentDTO appDeploymentDTO = appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID);

        assertThat(appDeploymentDTO.status()).isEqualTo(AppDeploymentStatus.FAILED);
        assertThat(fakeAppDeploymentProvider.canceledRefs).isEmpty();
    }

    @Test
    void testDeployOfAnAnswerTheProviderReportsFailedStoresTheSanitizedReason() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");

        Map<Long, AppDeployment> appDeployments = stubDeploymentStore();

        fakeAppDeploymentProvider.nextDeployment = new ProviderDeployment(
            "dpl_1", AppDeploymentStatus.FAILED, null, "  BUILD_FAILED: Command \"npm run build\"\r\nexited\t1  ");

        appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID);

        assertThat(appDeployments.get(APP_DEPLOYMENT_ID)
            .getErrorMessage()).isEqualTo(
                "The hosting provider reported that the deployment failed: " +
                    "BUILD_FAILED: Command \"npm run build\"  exited 1");
    }

    @Test
    void testDeployOfAnAnswerTheProviderReportsFailedTruncatesALongReason() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");

        Map<Long, AppDeployment> appDeployments = stubDeploymentStore();

        fakeAppDeploymentProvider.nextDeployment = new ProviderDeployment(
            "dpl_1", AppDeploymentStatus.FAILED, null, "line one\nline two " + "x".repeat(2000));

        appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID);

        String errorMessage = appDeployments.get(APP_DEPLOYMENT_ID)
            .getErrorMessage();

        assertThat(errorMessage)
            .startsWith("The hosting provider reported that the deployment failed: line one line two x")
            .doesNotContain("\n")
            .hasSize("The hosting provider reported that the deployment failed: ".length() + 500);
    }

    @Test
    void testDeployOfAnAnswerTheProviderReportsFailedRemovesInvisibleAndControlCharactersFromTheReason() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");

        Map<Long, AppDeployment> appDeployments = stubDeploymentStore();

        fakeAppDeploymentProvider.nextDeployment = new ProviderDeployment(
            "dpl_1", AppDeploymentStatus.FAILED, null, "a\u0085b\u202Ec\u2028d\u200Be\u2029f");

        appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID);

        assertThat(appDeployments.get(APP_DEPLOYMENT_ID)
            .getErrorMessage()).isEqualTo("The hosting provider reported that the deployment failed: a b c d e f");
    }

    @Test
    void testDeployOfAnAnswerTheProviderReportsFailedDoesNotSplitASurrogatePairWhenTruncating() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");

        Map<Long, AppDeployment> appDeployments = stubDeploymentStore();

        String emoji = new String(Character.toChars(0x1F600));

        fakeAppDeploymentProvider.nextDeployment = new ProviderDeployment(
            "dpl_1", AppDeploymentStatus.FAILED, null, "x".repeat(499) + emoji + "tail");

        appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID);

        String errorMessage = appDeployments.get(APP_DEPLOYMENT_ID)
            .getErrorMessage();

        assertThat(errorMessage)
            .isEqualTo("The hosting provider reported that the deployment failed: " + "x".repeat(499));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", "   ", "\n\t\r"
    })
    void testDeployOfAnAnswerTheProviderReportsFailedFallsBackWhenTheReasonIsBlank(String reason) {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");

        Map<Long, AppDeployment> appDeployments = stubDeploymentStore();

        fakeAppDeploymentProvider.nextDeployment = new ProviderDeployment(
            "dpl_1", AppDeploymentStatus.FAILED, null, reason);

        appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID);

        assertThat(appDeployments.get(APP_DEPLOYMENT_ID)
            .getErrorMessage()).isEqualTo("The hosting provider reported that the deployment failed");
    }

    @Test
    void testDeployOfAnAnswerTheProviderReportsFailedWithoutAReasonKeepsTheGenericMessage() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");

        Map<Long, AppDeployment> appDeployments = stubDeploymentStore();

        fakeAppDeploymentProvider.nextDeployment = new ProviderDeployment(
            "dpl_1", AppDeploymentStatus.FAILED, null);

        appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID);

        assertThat(appDeployments.get(APP_DEPLOYMENT_ID)
            .getErrorMessage()).isEqualTo("The hosting provider reported that the deployment failed");
    }

    @Test
    void testDeployWithAConnectionOfAnotherComponentFailsTheRowAndCallsNoProvider() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");

        Map<Long, AppDeployment> appDeployments = stubDeploymentStore();

        when(appConnectionService.isConnectionOfComponent(DEPLOYMENT_CONNECTION_ID, "vercel")).thenReturn(false);

        AppDeploymentDTO appDeploymentDTO = appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID);

        assertThat(appDeploymentDTO.status()).isEqualTo(AppDeploymentStatus.FAILED);
        assertThat(appDeployments.get(APP_DEPLOYMENT_ID)
            .getErrorMessage())
                .isEqualTo("The deployment connection does not belong to the component the hosting provider uses");
        assertThat(fakeAppDeploymentProvider.deployRequests).isEmpty();
        assertThat(fakeAppDeploymentProvider.canceledRefs).isEmpty();
    }

    @Test
    void testCancelWithAConnectionOfAnotherComponentCancelsLocallyWithoutTheProvider() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");

        Map<Long, AppDeployment> appDeployments = stubDeploymentStore();

        appDeployments.put(APP_DEPLOYMENT_ID, buildingDeployment());

        when(appConnectionService.isConnectionOfComponent(DEPLOYMENT_CONNECTION_ID, "vercel")).thenReturn(false);

        AppDeploymentDTO appDeploymentDTO = appDeploymentFacade.cancel(APP_INSTANCE_ID, APP_DEPLOYMENT_ID);

        assertThat(fakeAppDeploymentProvider.canceledRefs).isEmpty();
        assertThat(appDeploymentDTO.status()).isEqualTo(AppDeploymentStatus.CANCELED);
        assertThat(appDeployments.get(APP_DEPLOYMENT_ID)
            .getStatus()).isEqualTo(AppDeploymentStatus.CANCELED);

        verify(appAuditPublisher).publish(AppAuditEvent.APP_DEPLOYMENT_CANCELED, APP_ID, DEPLOYMENT_AUDIT_DATA);
    }

    @Test
    void testCancelWithoutADeploymentConnectionCancelsLocallyWithoutTheProvider() {
        stubDeployableInstance(AppHostingTarget.VERCEL, null);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");

        Map<Long, AppDeployment> appDeployments = stubDeploymentStore();

        appDeployments.put(APP_DEPLOYMENT_ID, buildingDeployment());

        AppDeploymentDTO appDeploymentDTO = appDeploymentFacade.cancel(APP_INSTANCE_ID, APP_DEPLOYMENT_ID);

        assertThat(fakeAppDeploymentProvider.canceledRefs).isEmpty();
        assertThat(appDeploymentDTO.status()).isEqualTo(AppDeploymentStatus.CANCELED);
        assertThat(appDeployments.get(APP_DEPLOYMENT_ID)
            .getStatus()).isEqualTo(AppDeploymentStatus.CANCELED);

        verify(appAuditPublisher).publish(AppAuditEvent.APP_DEPLOYMENT_CANCELED, APP_ID, DEPLOYMENT_AUDIT_DATA);
    }

    @Test
    void testCancelWhenTheProviderIsNotInstalledCancelsLocallyWithoutTheProvider() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");

        Map<Long, AppDeployment> appDeployments = stubDeploymentStore();

        appDeployments.put(APP_DEPLOYMENT_ID, buildingDeployment());

        AppDeploymentSynchronizer appDeploymentSynchronizerWithoutProviders = new AppDeploymentSynchronizer(
            appAuditPublisher, new StaticListableBeanFactory().getBeanProvider(AppDeploymentProvider.class),
            appDeploymentService, appConnectionService, appInstanceService, appVersionService,
            meterRegistryProvider(meterRegistry));

        AppDeploymentFacade appDeploymentFacadeWithoutProviders = new AppDeploymentFacadeImpl(
            appAuditPublisher, appDeploymentService, appDeploymentSynchronizerWithoutProviders, appInstanceService,
            appService, appVersionService);

        AppDeploymentDTO appDeploymentDTO = appDeploymentFacadeWithoutProviders.cancel(
            APP_INSTANCE_ID, APP_DEPLOYMENT_ID);

        assertThat(fakeAppDeploymentProvider.canceledRefs).isEmpty();
        assertThat(appDeploymentDTO.status()).isEqualTo(AppDeploymentStatus.CANCELED);
        assertThat(appDeployments.get(APP_DEPLOYMENT_ID)
            .getStatus()).isEqualTo(AppDeploymentStatus.CANCELED);

        verify(appAuditPublisher).publish(AppAuditEvent.APP_DEPLOYMENT_CANCELED, APP_ID, DEPLOYMENT_AUDIT_DATA);
    }

    @Test
    void testGetDeploymentReturnsTheInstancesDeploymentMarkedCurrentWhenItIsThePointer() {
        AppInstance appInstance = stubInstance(AppInstanceStatus.ACTIVE);

        appInstance.setCurrentAppDeploymentId(APP_DEPLOYMENT_ID);

        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");

        when(appDeploymentService.getAppDeployment(APP_DEPLOYMENT_ID)).thenReturn(buildingDeployment());

        AppDeploymentDTO appDeploymentDTO = appDeploymentFacade.getDeployment(APP_INSTANCE_ID, APP_DEPLOYMENT_ID);

        assertThat(appDeploymentDTO.id()).isEqualTo(APP_DEPLOYMENT_ID);
        assertThat(appDeploymentDTO.status()).isEqualTo(AppDeploymentStatus.BUILDING);
        assertThat(appDeploymentDTO.appVersion()
            .name()).isEqualTo("1.2");
        assertThat(appDeploymentDTO.current()).isTrue();
    }

    @Test
    void testGetDeploymentOfAnotherInstanceIsNotFound() {
        stubInstance(AppInstanceStatus.ACTIVE);

        AppDeployment foreignAppDeployment = buildingDeployment();

        foreignAppDeployment.setAppInstanceId(APP_INSTANCE_ID + 1);

        when(appDeploymentService.getAppDeployment(APP_DEPLOYMENT_ID)).thenReturn(foreignAppDeployment);

        assertThatThrownBy(() -> appDeploymentFacade.getDeployment(APP_INSTANCE_ID, APP_DEPLOYMENT_ID))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_DEPLOYMENT_NOT_FOUND.getErrorKey()));

        verify(appVersionService, never()).getAppVersion(anyLong());
    }

    @Test
    void testFetchVersionFindsTheVersionByItsTrimmedName() {
        AppVersion appVersion = appVersion("1.2", "abc123");

        appVersion.setId(APP_VERSION_ID);

        when(appVersionService.fetchAppVersion(APP_ID, "1.2")).thenReturn(Optional.of(appVersion));

        assertThat(appDeploymentFacade.fetchVersion(APP_ID, " 1.2\n"))
            .map(AppVersionDTO::id)
            .contains(APP_VERSION_ID);
    }

    @Test
    void testFetchVersionOfAnUnknownNameIsEmpty() {
        when(appVersionService.fetchAppVersion(APP_ID, "9.9")).thenReturn(Optional.empty());

        assertThat(appDeploymentFacade.fetchVersion(APP_ID, "9.9")).isEmpty();
    }

    @Test
    void testFetchVersionRefusesABlankName() {
        assertThatThrownBy(() -> appDeploymentFacade.fetchVersion(APP_ID, " "))
            .isInstanceOf(IllegalArgumentException.class);

        verify(appVersionService, never()).fetchAppVersion(anyLong(), any());
    }

    @Test
    void testDeploymentProviderIsAvailableOnlyForAHostingTargetWithAProvider() {
        assertThat(appDeploymentFacade.isDeploymentProviderAvailable(AppHostingTarget.VERCEL)).isTrue();
        assertThat(appDeploymentFacade.isDeploymentProviderAvailable(AppHostingTarget.SELF_HOSTED)).isTrue();

        AppDeploymentSynchronizer appDeploymentSynchronizer = new AppDeploymentSynchronizer(
            appAuditPublisher, new StaticListableBeanFactory().getBeanProvider(AppDeploymentProvider.class),
            appDeploymentService, appConnectionService, appInstanceService, appVersionService,
            meterRegistryProvider(meterRegistry));

        AppDeploymentFacade appDeploymentFacadeWithoutProviders = new AppDeploymentFacadeImpl(
            appAuditPublisher, appDeploymentService, appDeploymentSynchronizer, appInstanceService, appService,
            appVersionService);

        assertThat(appDeploymentFacadeWithoutProviders.isDeploymentProviderAvailable(AppHostingTarget.VERCEL))
            .isFalse();
    }

    @Test
    void testCancelWhoseRowGainedAProviderIdTellsTheProvider() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");

        Map<Long, AppDeployment> appDeployments = stubDeploymentStore();

        AppDeployment storedAppDeployment = buildingDeployment();

        appDeployments.put(APP_DEPLOYMENT_ID, storedAppDeployment);

        AppDeployment queuedSnapshot = buildingDeployment();

        queuedSnapshot.setProviderDeploymentId(null);
        queuedSnapshot.setStatus(AppDeploymentStatus.QUEUED);

        List<AppDeployment> answers = new ArrayList<>(List.of(queuedSnapshot));

        when(appDeploymentService.getAppDeployment(APP_DEPLOYMENT_ID)).thenAnswer(
            invocation -> answers.isEmpty() ? storedAppDeployment : answers.removeFirst());

        AppDeploymentDTO appDeploymentDTO = appDeploymentFacade.cancel(APP_INSTANCE_ID, APP_DEPLOYMENT_ID);

        assertThat(fakeAppDeploymentProvider.canceledRefs)
            .extracting(ProviderDeploymentRef::providerDeploymentId)
            .containsExactly("dpl_1");
        assertThat(appDeploymentDTO.status()).isEqualTo(AppDeploymentStatus.CANCELED);
        assertThat(storedAppDeployment.getStatus()).isEqualTo(AppDeploymentStatus.CANCELED);

        verify(appAuditPublisher).publish(AppAuditEvent.APP_DEPLOYMENT_CANCELED, APP_ID, DEPLOYMENT_AUDIT_DATA);
    }

    @Test
    void testCancelDecidesOnTheRowItWritesInOneServiceCall() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");

        Map<Long, AppDeployment> appDeployments = stubDeploymentStore();

        appDeployments.put(APP_DEPLOYMENT_ID, buildingDeployment());

        appDeploymentFacade.cancel(APP_INSTANCE_ID, APP_DEPLOYMENT_ID);

        verify(appDeploymentService).cancel(APP_DEPLOYMENT_ID, "dpl_1");
        verify(appDeploymentService, never()).updateStatus(
            anyLong(), eq(AppDeploymentStatus.CANCELED), any(), any());
        verify(appDeploymentService, never()).updateStatus(
            anyLong(), any(), eq(AppDeploymentStatus.CANCELED), any(), any());
    }

    @Test
    void testDeployFailsTheQueuedRowWhenTheInstanceCannotBeReadAfterTheInsert() {
        AppInstance appInstance = stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);

        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");

        Map<Long, AppDeployment> appDeployments = stubDeploymentStore();

        when(appInstanceService.getAppInstance(APP_INSTANCE_ID))
            .thenReturn(appInstance)
            .thenThrow(new IllegalStateException("connection refused for " + PROVIDER_SECRET))
            .thenReturn(appInstance);

        AppDeploymentDTO appDeploymentDTO = appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID);

        AppDeployment storedAppDeployment = appDeployments.get(APP_DEPLOYMENT_ID);

        assertThat(appDeploymentDTO.status()).isEqualTo(AppDeploymentStatus.FAILED);
        assertThat(storedAppDeployment.getStatus()).isEqualTo(AppDeploymentStatus.FAILED);
        assertThat(storedAppDeployment.getErrorMessage())
            .isEqualTo("The deployment could not be started")
            .doesNotContain(PROVIDER_SECRET);
        assertThat(fakeAppDeploymentProvider.deployRequests).isEmpty();
        assertThat(fakeAppDeploymentProvider.cancelAttempts).isZero();
    }

    @Test
    void testDeployFailsTheQueuedRowWhenTheConnectionWasClearedAfterTheInsert() {
        AppInstance appInstance = stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);

        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");

        Map<Long, AppDeployment> appDeployments = stubDeploymentStore();

        AppInstance clearedAppInstance = new AppInstance();

        clearedAppInstance.setId(APP_INSTANCE_ID);
        clearedAppInstance.setAppId(APP_ID);
        clearedAppInstance.setStatus(AppInstanceStatus.ACTIVE);
        clearedAppInstance.setEnvironment(Environment.PRODUCTION);
        clearedAppInstance.setHostingTarget(AppHostingTarget.VERCEL);

        when(appInstanceService.getAppInstance(APP_INSTANCE_ID))
            .thenReturn(appInstance)
            .thenReturn(clearedAppInstance);

        AppDeploymentDTO appDeploymentDTO = appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID);

        AppDeployment storedAppDeployment = appDeployments.get(APP_DEPLOYMENT_ID);

        assertThat(appDeploymentDTO.status()).isEqualTo(AppDeploymentStatus.FAILED);
        assertThat(storedAppDeployment.getErrorMessage())
            .isEqualTo("The app instance has no deployment connection");
        assertThat(fakeAppDeploymentProvider.deployRequests).isEmpty();
    }

    @Test
    void testDeployFailsTheQueuedRowWhenTheHostingTargetHasNoProviderAfterTheInsert() {
        AppInstance appInstance = stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);

        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");

        Map<Long, AppDeployment> appDeployments = stubDeploymentStore();

        AppInstance selfHostedAppInstance = new AppInstance();

        selfHostedAppInstance.setId(APP_INSTANCE_ID);
        selfHostedAppInstance.setAppId(APP_ID);
        selfHostedAppInstance.setStatus(AppInstanceStatus.ACTIVE);
        selfHostedAppInstance.setEnvironment(Environment.PRODUCTION);
        selfHostedAppInstance.setHostingTarget(AppHostingTarget.SELF_HOSTED);
        selfHostedAppInstance.setDeploymentConnectionId(DEPLOYMENT_CONNECTION_ID);

        when(appInstanceService.getAppInstance(APP_INSTANCE_ID))
            .thenReturn(appInstance)
            .thenReturn(selfHostedAppInstance);

        AppDeploymentDTO appDeploymentDTO = appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID);

        assertThat(appDeploymentDTO.status()).isEqualTo(AppDeploymentStatus.FAILED);
        assertThat(appDeployments.get(APP_DEPLOYMENT_ID)
            .getErrorMessage()).isEqualTo("No deployment provider is available for the app instance's hosting target");
        assertThat(fakeAppDeploymentProvider.deployRequests).isEmpty();
    }

    @Test
    void testDeployFailsTheQueuedRowWhenTheInitiatedAuditCannotBePublished() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");

        Map<Long, AppDeployment> appDeployments = stubDeploymentStore();

        doThrow(new IllegalStateException("audit store unavailable"))
            .when(appAuditPublisher)
            .publish(eq(AppAuditEvent.APP_DEPLOYMENT_INITIATED), anyLong(), any());

        AppDeploymentDTO appDeploymentDTO = appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID);

        assertThat(appDeploymentDTO.status()).isEqualTo(AppDeploymentStatus.FAILED);
        assertThat(appDeployments.get(APP_DEPLOYMENT_ID)
            .getErrorMessage()).isEqualTo("The deployment could not be started");
        assertThat(fakeAppDeploymentProvider.deployRequests).isEmpty();
    }

    @Test
    void testDeployRethrowsWhenTheQueuedRowCannotBeMarkedFailed() {
        AppInstance appInstance = stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);

        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");
        stubDeploymentStore();

        when(appInstanceService.getAppInstance(APP_INSTANCE_ID))
            .thenReturn(appInstance)
            .thenThrow(new IllegalStateException("instance gone"));

        IllegalStateException databaseDown = new IllegalStateException("database down");

        doThrow(databaseDown).when(appDeploymentService)
            .updateStatus(anyLong(), eq(AppDeploymentStatus.FAILED), any(), any());

        assertThatThrownBy(() -> appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID))
            .isSameAs(databaseDown);
        assertThat(fakeAppDeploymentProvider.deployRequests).isEmpty();
    }

    @Test
    void testCancelOfAQueuedRowWithoutAProviderIdDoesNotCallTheProvider() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");

        Map<Long, AppDeployment> appDeployments = stubDeploymentStore();

        AppDeployment queuedAppDeployment = buildingDeployment();

        queuedAppDeployment.setProviderDeploymentId(null);
        queuedAppDeployment.setStatus(AppDeploymentStatus.QUEUED);

        appDeployments.put(APP_DEPLOYMENT_ID, queuedAppDeployment);

        AppDeploymentDTO appDeploymentDTO = appDeploymentFacade.cancel(APP_INSTANCE_ID, APP_DEPLOYMENT_ID);

        assertThat(appDeploymentDTO.status()).isEqualTo(AppDeploymentStatus.CANCELED);
        assertThat(fakeAppDeploymentProvider.cancelAttempts).isZero();
    }

    @Test
    void testRollbackCreatesNewRowForEarlierVersion() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(EARLIER_APP_VERSION_ID, "1.1", "release/1.1", "def456");

        Map<Long, AppDeployment> appDeployments = stubDeploymentStore();

        AppDeployment earlierAppDeployment = appDeployment(APP_DEPLOYMENT_ID - 10);

        earlierAppDeployment.setAppVersionId(EARLIER_APP_VERSION_ID);
        earlierAppDeployment.setProvider(AppDeploymentProviderType.VERCEL);

        appDeployments.put(earlierAppDeployment.getId(), earlierAppDeployment);

        when(appDeploymentService.hasReadyAppDeployment(APP_INSTANCE_ID, EARLIER_APP_VERSION_ID)).thenReturn(true);

        fakeAppDeploymentProvider.nextDeployment = new ProviderDeployment(
            "dpl_2", AppDeploymentStatus.READY, "https://crm.vercel.app");

        AppDeploymentDTO appDeploymentDTO = appDeploymentFacade.rollback(APP_INSTANCE_ID, EARLIER_APP_VERSION_ID);

        ArgumentCaptor<AppDeployment> appDeploymentCaptor = ArgumentCaptor.forClass(AppDeployment.class);

        verify(appDeploymentService).create(appDeploymentCaptor.capture());

        AppDeployment rolledBackAppDeployment = appDeploymentCaptor.getValue();

        assertThat(rolledBackAppDeployment.getId()).isEqualTo(APP_DEPLOYMENT_ID);
        assertThat(rolledBackAppDeployment.getAppVersionId()).isEqualTo(EARLIER_APP_VERSION_ID);
        Map<String, ?> metadata = rolledBackAppDeployment.getMetadata();

        assertThat(metadata.get("rollback")).isEqualTo(true);
        assertThat(appDeploymentDTO.id()).isEqualTo(APP_DEPLOYMENT_ID);
        assertThat(appDeploymentDTO.appVersion()
            .name()).isEqualTo("1.1");
        assertThat(appDeploymentDTO.current()).isTrue();
        assertThat(fakeAppDeploymentProvider.deployRequests)
            .singleElement()
            .satisfies(request -> assertThat(request.gitCommitSha()).isEqualTo("def456"));

        verify(appDeploymentService, never()).updateStatus(
            eq(earlierAppDeployment.getId()), any(), any(), any(), any());
        verify(appInstanceService).updateCurrentAppDeployment(APP_INSTANCE_ID, APP_DEPLOYMENT_ID);
    }

    @Test
    void testRollbackToAVersionNeverReadyOnTheInstanceIsRefused() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(EARLIER_APP_VERSION_ID, "1.1", "release/1.1", "def456");

        when(appDeploymentService.hasReadyAppDeployment(APP_INSTANCE_ID, EARLIER_APP_VERSION_ID)).thenReturn(false);

        assertThatThrownBy(() -> appDeploymentFacade.rollback(APP_INSTANCE_ID, EARLIER_APP_VERSION_ID))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_VERSION_NOT_FOUND.getErrorKey()));

        assertThat(fakeAppDeploymentProvider.deployRequests).isEmpty();

        verify(appDeploymentService, never()).create(any());
    }

    @Test
    void testCancelAsksTheProviderAndMarksTheDeploymentCanceled() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");

        Map<Long, AppDeployment> appDeployments = stubDeploymentStore();

        appDeployments.put(APP_DEPLOYMENT_ID, buildingDeployment());

        AppDeploymentDTO appDeploymentDTO = appDeploymentFacade.cancel(APP_INSTANCE_ID, APP_DEPLOYMENT_ID);

        assertThat(fakeAppDeploymentProvider.canceledRefs)
            .singleElement()
            .satisfies(ref -> {
                assertThat(ref.providerDeploymentId()).isEqualTo("dpl_1");
                assertThat(ref.connectionId()).isEqualTo(DEPLOYMENT_CONNECTION_ID);
            });
        assertThat(appDeploymentDTO.status()).isEqualTo(AppDeploymentStatus.CANCELED);
        assertThat(appDeployments.get(APP_DEPLOYMENT_ID)
            .getStatus()).isEqualTo(AppDeploymentStatus.CANCELED);

        verify(appAuditPublisher).publish(AppAuditEvent.APP_DEPLOYMENT_CANCELED, APP_ID, DEPLOYMENT_AUDIT_DATA);
    }

    @ParameterizedTest
    @EnumSource(value = AppDeploymentStatus.class, names = {
        "READY", "FAILED", "CANCELED"
    })
    void testCancelOfATerminalDeploymentIsRefusedAndNeverWritten(AppDeploymentStatus terminalStatus) {
        AppDeployment terminalAppDeployment = buildingDeployment();

        terminalAppDeployment.setStatus(terminalStatus);

        when(appDeploymentService.getAppDeployment(APP_DEPLOYMENT_ID)).thenReturn(terminalAppDeployment);

        assertThatThrownBy(() -> appDeploymentFacade.cancel(APP_INSTANCE_ID, APP_DEPLOYMENT_ID))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_DEPLOYMENT_TERMINAL.getErrorKey()));

        assertThat(fakeAppDeploymentProvider.canceledRefs).isEmpty();

        verify(appDeploymentService, never()).updateStatus(anyLong(), any(), any(), any());
        verify(appDeploymentService, never()).updateStatus(anyLong(), any(), any(), any(), any());
    }

    @Test
    void testCancelOfADeploymentOfAnotherInstanceIsNotFound() {
        AppDeployment foreignAppDeployment = buildingDeployment();

        foreignAppDeployment.setAppInstanceId(APP_INSTANCE_ID + 1);

        when(appDeploymentService.getAppDeployment(APP_DEPLOYMENT_ID)).thenReturn(foreignAppDeployment);

        assertThatThrownBy(() -> appDeploymentFacade.cancel(APP_INSTANCE_ID, APP_DEPLOYMENT_ID))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_DEPLOYMENT_NOT_FOUND.getErrorKey()));

        assertThat(fakeAppDeploymentProvider.canceledRefs).isEmpty();
    }

    @Test
    void testCancelKeepsTheDeploymentWhenTheProviderFails() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);

        when(appDeploymentService.getAppDeployment(APP_DEPLOYMENT_ID)).thenReturn(buildingDeployment());

        fakeAppDeploymentProvider.cancelFailure = new IllegalStateException("Bearer " + PROVIDER_SECRET);

        assertThatThrownBy(() -> appDeploymentFacade.cancel(APP_INSTANCE_ID, APP_DEPLOYMENT_ID))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> {
                    assertThat(exception.getErrorKey())
                        .isEqualTo(AppErrorType.APP_DEPLOYMENT_PROVIDER_FAILED.getErrorKey());
                    assertThat(exception.getMessage()).doesNotContain(PROVIDER_SECRET);
                });

        verify(appDeploymentService, never()).updateStatus(anyLong(), any(), any(), any());
        verify(appDeploymentService, never()).updateStatus(anyLong(), any(), any(), any(), any());
    }

    @Test
    void testEveryFacadeMethodIsGuarded() throws NoSuchMethodException {
        for (Method method : AppDeploymentFacade.class.getMethods()) {
            Method implementation = AppDeploymentFacadeImpl.class.getMethod(
                method.getName(), method.getParameterTypes());

            assertThat(implementation.getAnnotation(PreAuthorize.class))
                .as(method.getName())
                .isNotNull();
        }
    }

    @Test
    void testRecordDeploymentRunsInOneWritableTransaction() throws NoSuchMethodException {
        Method method = AppDeploymentFacadeImpl.class.getMethod(
            "recordDeployment", long.class, String.class, String.class, String.class);

        Transactional methodTransactional = method.getAnnotation(Transactional.class);
        Transactional classTransactional = AppDeploymentFacadeImpl.class.getAnnotation(Transactional.class);

        assertThat(methodTransactional).isNull();
        assertThat(classTransactional).isNotNull();
        assertThat(classTransactional.readOnly()).isFalse();
    }

    @Test
    void testRecordCreatesVersionWhenNameIsNew() {
        stubInstance(AppInstanceStatus.ACTIVE);

        when(appVersionService.fetchAppVersion(APP_ID, "1.1")).thenReturn(Optional.empty());
        when(appVersionService.create(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        stubDeploymentCreate();

        AppDeploymentDTO appDeploymentDTO = appDeploymentFacade.recordDeployment(
            APP_INSTANCE_ID, "1.1", URL, "abc123");

        ArgumentCaptor<AppVersion> appVersionCaptor = ArgumentCaptor.forClass(AppVersion.class);

        verify(appVersionService).create(appVersionCaptor.capture());

        AppVersion createdAppVersion = appVersionCaptor.getValue();

        assertThat(createdAppVersion.getAppId()).isEqualTo(APP_ID);
        assertThat(createdAppVersion.getName()).isEqualTo("1.1");
        assertThat(createdAppVersion.getGitCommitSha()).isEqualTo("abc123");

        AppVersionDTO appVersionDTO = appDeploymentDTO.appVersion();

        assertThat(appVersionDTO.id()).isEqualTo(APP_VERSION_ID);
        assertThat(appVersionDTO.name()).isEqualTo("1.1");
    }

    @Test
    void testRecordReusesExistingVersionOfTheSameCommit() {
        stubInstance(AppInstanceStatus.ACTIVE);

        when(appVersionService.fetchAppVersion(APP_ID, "1.1"))
            .thenReturn(Optional.of(appVersion("1.1", "ABC123")));
        stubDeploymentCreate();

        appDeploymentFacade.recordDeployment(APP_INSTANCE_ID, "1.1", URL, "abc123");

        verify(appVersionService, never()).create(any());
        verify(appDeploymentService).create(any());
    }

    @Test
    void testRecordReusesExistingVersionWhenNoCommitIsGiven() {
        stubInstance(AppInstanceStatus.ACTIVE);

        when(appVersionService.fetchAppVersion(APP_ID, "1.1"))
            .thenReturn(Optional.of(appVersion("1.1", "abc123")));
        stubDeploymentCreate();

        assertThat(appDeploymentFacade.recordDeployment(APP_INSTANCE_ID, "1.1", URL, null)
            .appVersion()
            .gitCommitSha()).isEqualTo("abc123");

        verify(appVersionService, never()).create(any());
    }

    @Test
    void testRecordRefusesExistingVersionNameOfAnotherCommit() {
        stubInstance(AppInstanceStatus.ACTIVE);

        when(appVersionService.fetchAppVersion(APP_ID, "1.1"))
            .thenReturn(Optional.of(appVersion("1.1", "abc123")));

        assertThatThrownBy(() -> appDeploymentFacade.recordDeployment(APP_INSTANCE_ID, "1.1", URL, "def456"))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_VERSION_COMMIT_MISMATCH.getErrorKey()));

        verify(appDeploymentService, never()).create(any());
        verify(appInstanceService, never()).updateCurrentAppDeployment(anyLong(), anyLong());
        verifyNoInteractions(appAuditPublisher);
    }

    @Test
    void testRecordRefusesExistingVersionWithoutCommitWhenACommitIsGiven() {
        stubInstance(AppInstanceStatus.ACTIVE);

        when(appVersionService.fetchAppVersion(APP_ID, "1.1")).thenReturn(Optional.of(appVersion("1.1", null)));

        assertThatThrownBy(() -> appDeploymentFacade.recordDeployment(APP_INSTANCE_ID, "1.1", URL, "def456"))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_VERSION_COMMIT_MISMATCH.getErrorKey()));
    }

    @Test
    void testRecordSetsCurrentDeployment() {
        stubInstance(AppInstanceStatus.ACTIVE);

        when(appVersionService.fetchAppVersion(APP_ID, "1.1"))
            .thenReturn(Optional.of(appVersion("1.1", "abc123")));
        stubDeploymentCreate();

        AppDeploymentDTO appDeploymentDTO = appDeploymentFacade.recordDeployment(
            APP_INSTANCE_ID, "1.1", URL, "abc123");

        ArgumentCaptor<AppDeployment> appDeploymentCaptor = ArgumentCaptor.forClass(AppDeployment.class);

        verify(appDeploymentService).create(appDeploymentCaptor.capture());

        AppDeployment recorded = appDeploymentCaptor.getValue();

        assertThat(recorded.getAppInstanceId()).isEqualTo(APP_INSTANCE_ID);
        assertThat(recorded.getAppVersionId()).isEqualTo(APP_VERSION_ID);
        assertThat(recorded.getProvider()).isEqualTo(AppDeploymentProviderType.EXTERNAL);
        assertThat(recorded.getStatus()).isEqualTo(AppDeploymentStatus.READY);
        assertThat(recorded.getUrl()).isEqualTo(URL);

        verify(appInstanceService).updateCurrentAppDeployment(APP_INSTANCE_ID, APP_DEPLOYMENT_ID);

        assertThat(appDeploymentDTO.id()).isEqualTo(APP_DEPLOYMENT_ID);
        assertThat(appDeploymentDTO.current()).isTrue();
        assertThat(appDeploymentDTO.status()).isEqualTo(AppDeploymentStatus.READY);
        assertThat(appDeploymentDTO.provider()).isEqualTo(AppDeploymentProviderType.EXTERNAL);
    }

    @Test
    void testRecordPublishesDeploymentCompleted() {
        stubInstance(AppInstanceStatus.ACTIVE);

        when(appVersionService.fetchAppVersion(APP_ID, "1.1"))
            .thenReturn(Optional.of(appVersion("1.1", "abc123")));
        stubDeploymentCreate();

        appDeploymentFacade.recordDeployment(APP_INSTANCE_ID, "1.1", URL, "abc123");

        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_DEPLOYMENT_COMPLETED, APP_ID,
            Map.of(
                "appInstanceId", APP_INSTANCE_ID, "appDeploymentId", APP_DEPLOYMENT_ID, "appVersionId",
                APP_VERSION_ID, "versionName", "1.1", "provider", "EXTERNAL"));
    }

    @Test
    void testRecordCountsAnExternalReadyDeploymentOnce() {
        stubInstance(AppInstanceStatus.ACTIVE);

        when(appVersionService.fetchAppVersion(APP_ID, "1.1"))
            .thenReturn(Optional.of(appVersion("1.1", "abc123")));
        stubDeploymentCreate();

        appDeploymentFacade.recordDeployment(APP_INSTANCE_ID, "1.1", URL, "abc123");

        assertThat(deploymentCount("EXTERNAL", "READY")).isEqualTo(1.0);
        assertThat(totalDeploymentCount()).isEqualTo(1.0);
    }

    @Test
    void testDeployCountsTheProviderAnswerOnce() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");
        stubDeploymentStore();

        fakeAppDeploymentProvider.nextDeployment = new ProviderDeployment(
            "dpl_1", AppDeploymentStatus.READY, "https://crm.vercel.app");

        appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID);

        assertThat(deploymentCount("VERCEL", "READY")).isEqualTo(1.0);
        assertThat(totalDeploymentCount()).isEqualTo(1.0);
    }

    @Test
    void testDeployThatIsStillBuildingCountsNothing() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");
        stubDeploymentStore();

        appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID);

        assertThat(totalDeploymentCount()).isZero();
    }

    @Test
    void testDeployThatFailsAtTheProviderCountsFailedOnce() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");
        stubDeploymentStore();

        fakeAppDeploymentProvider.deployFailure = new IllegalStateException("provider unavailable");

        appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID);

        assertThat(deploymentCount("VERCEL", "FAILED")).isEqualTo(1.0);
        assertThat(totalDeploymentCount()).isEqualTo(1.0);
    }

    @Test
    void testCancelCountsCanceledOnce() {
        stubDeployableInstance(AppHostingTarget.VERCEL, DEPLOYMENT_CONNECTION_ID);
        stubVersion(APP_VERSION_ID, "1.2", "release/1.2", "abc123");

        Map<Long, AppDeployment> appDeployments = stubDeploymentStore();

        appDeployments.put(APP_DEPLOYMENT_ID, buildingDeployment());

        appDeploymentFacade.cancel(APP_INSTANCE_ID, APP_DEPLOYMENT_ID);

        assertThat(deploymentCount("VERCEL", "CANCELED")).isEqualTo(1.0);
        assertThat(totalDeploymentCount()).isEqualTo(1.0);
    }

    @Test
    void testRecordOnTerminatedInstanceIsRefused() {
        stubInstance(AppInstanceStatus.TERMINATED);

        assertThatThrownBy(() -> appDeploymentFacade.recordDeployment(APP_INSTANCE_ID, "1.1", URL, "abc123"))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_INSTANCE_TERMINATED.getErrorKey()));

        verifyNoInteractions(appVersionService, appDeploymentService, appAuditPublisher);
    }

    @ParameterizedTest
    @EnumSource(value = AppInstanceStatus.class, names = {
        "ACTIVE", "SUSPENDED"
    })
    void testRecordOnActiveOrSuspendedInstanceIsAllowed(AppInstanceStatus status) {
        stubInstance(status);

        when(appVersionService.fetchAppVersion(APP_ID, "1.1"))
            .thenReturn(Optional.of(appVersion("1.1", "abc123")));
        stubDeploymentCreate();

        assertThat(appDeploymentFacade.recordDeployment(APP_INSTANCE_ID, "1.1", URL, "abc123")
            .id()).isEqualTo(APP_DEPLOYMENT_ID);
    }

    @Test
    void testRecordRefusesBlankVersionName() {
        assertThatThrownBy(() -> appDeploymentFacade.recordDeployment(APP_INSTANCE_ID, " ", URL, "abc123"))
            .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(appDeploymentService, appAuditPublisher);
    }

    @Test
    void testGetDeploymentsOfTerminatedInstanceListsHistoryAndMarksCurrent() {
        AppInstance appInstance = stubInstance(AppInstanceStatus.TERMINATED);

        appInstance.setCurrentAppDeploymentId(APP_DEPLOYMENT_ID);

        AppDeployment newer = appDeployment(APP_DEPLOYMENT_ID);
        AppDeployment older = appDeployment(APP_DEPLOYMENT_ID - 1);

        when(appDeploymentService.getAppDeployments(APP_INSTANCE_ID, 50)).thenReturn(List.of(newer, older));
        when(appVersionService.getAppVersionsByIds(List.of(APP_VERSION_ID)))
            .thenReturn(List.of(appVersion("1.1", "abc123")));

        assertThat(appDeploymentFacade.getDeployments(APP_INSTANCE_ID, 50))
            .extracting(AppDeploymentDTO::id, AppDeploymentDTO::current)
            .containsExactly(
                tuple(APP_DEPLOYMENT_ID, true),
                tuple(APP_DEPLOYMENT_ID - 1, false));
    }

    @Test
    void testGetVersionsMapsTheAppsVersions() {
        when(appVersionService.getAppVersions(APP_ID, 50)).thenReturn(List.of(appVersion("1.1", "abc123")));

        assertThat(appDeploymentFacade.getVersions(APP_ID, 50))
            .singleElement()
            .satisfies(appVersionDTO -> {
                assertThat(appVersionDTO.appId()).isEqualTo(APP_ID);
                assertThat(appVersionDTO.name()).isEqualTo("1.1");
                assertThat(appVersionDTO.gitCommitSha()).isEqualTo("abc123");
            });
    }

    @Test
    void testGetVersionsForInstanceListsTheVersionsOfTheInstancesApp() {
        stubInstance(AppInstanceStatus.ACTIVE);

        when(appVersionService.getAppVersions(APP_ID, 50)).thenReturn(List.of(appVersion("1.1", "abc123")));

        assertThat(appDeploymentFacade.getVersionsForInstance(APP_INSTANCE_ID, 50))
            .singleElement()
            .satisfies(appVersionDTO -> {
                assertThat(appVersionDTO.appId()).isEqualTo(APP_ID);
                assertThat(appVersionDTO.name()).isEqualTo("1.1");
            });
    }

    @Test
    void testGetVersionsForInstanceRefusesALimitOutsideOneToTwoHundred() {
        assertThatThrownBy(() -> appDeploymentFacade.getVersionsForInstance(APP_INSTANCE_ID, 0))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                configurationException -> assertThat(configurationException.getErrorKey())
                    .isEqualTo(AppErrorType.APP_LIST_LIMIT_INVALID.getErrorKey()));

        verify(appVersionService, never()).getAppVersions(anyLong(), anyInt());
    }

    @Test
    void testFetchVersionForInstanceFindsTheVersionOfTheInstancesAppByItsTrimmedName() {
        stubInstance(AppInstanceStatus.ACTIVE);

        AppVersion appVersion = appVersion("1.2", "abc123");

        appVersion.setId(APP_VERSION_ID);

        when(appVersionService.fetchAppVersion(APP_ID, "1.2")).thenReturn(Optional.of(appVersion));

        assertThat(appDeploymentFacade.fetchVersionForInstance(APP_INSTANCE_ID, " 1.2\n"))
            .map(AppVersionDTO::id)
            .contains(APP_VERSION_ID);
    }

    @Test
    void testTheDefaultListLimitIsFiftyAndTheMaximumTwoHundred() {
        assertThat(AppDeploymentFacade.DEFAULT_LIST_LIMIT).isEqualTo(50);
        assertThat(AppDeploymentFacade.MAX_LIST_LIMIT).isEqualTo(200);
    }

    @ParameterizedTest
    @ValueSource(ints = {
        1, 7, 200
    })
    void testListsPassTheLimitToTheService(int limit) {
        stubInstance(AppInstanceStatus.ACTIVE);

        when(appDeploymentService.getAppDeployments(APP_INSTANCE_ID, limit)).thenReturn(List.of());
        when(appVersionService.getAppVersions(APP_ID, limit)).thenReturn(List.of());

        assertThat(appDeploymentFacade.getDeployments(APP_INSTANCE_ID, limit)).isEmpty();
        assertThat(appDeploymentFacade.getVersions(APP_ID, limit)).isEmpty();

        verify(appDeploymentService).getAppDeployments(APP_INSTANCE_ID, limit);
        verify(appVersionService).getAppVersions(APP_ID, limit);
    }

    @ParameterizedTest
    @ValueSource(ints = {
        Integer.MIN_VALUE, -1, 0, 201, Integer.MAX_VALUE
    })
    void testListsRefuseALimitOutsideOneToTwoHundred(int limit) {
        assertThatThrownBy(() -> appDeploymentFacade.getDeployments(APP_INSTANCE_ID, limit))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                configurationException -> assertThat(configurationException.getErrorKey())
                    .isEqualTo(AppErrorType.APP_LIST_LIMIT_INVALID.getErrorKey()));
        assertThatThrownBy(() -> appDeploymentFacade.getVersions(APP_ID, limit))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                configurationException -> assertThat(configurationException.getErrorKey())
                    .isEqualTo(AppErrorType.APP_LIST_LIMIT_INVALID.getErrorKey()));

        verifyNoInteractions(appInstanceService, appDeploymentService, appVersionService);
    }

    @Test
    void testGetCurrentDeploymentReadsOnlyTheCurrentDeploymentAndItsVersion() {
        AppInstance appInstance = stubInstance(AppInstanceStatus.ACTIVE);

        appInstance.setCurrentAppDeploymentId(APP_DEPLOYMENT_ID);

        when(appDeploymentService.getAppDeployment(APP_DEPLOYMENT_ID)).thenReturn(appDeployment(APP_DEPLOYMENT_ID));
        when(appVersionService.getAppVersion(APP_VERSION_ID)).thenReturn(appVersion("1.1", "abc123"));

        assertThat(appDeploymentFacade.getCurrentDeployment(APP_INSTANCE_ID))
            .hasValueSatisfying(appDeploymentDTO -> {
                assertThat(appDeploymentDTO.id()).isEqualTo(APP_DEPLOYMENT_ID);
                assertThat(appDeploymentDTO.current()).isTrue();
                assertThat(appDeploymentDTO.appVersion())
                    .extracting(AppVersionDTO::name)
                    .isEqualTo("1.1");
            });

        verify(appDeploymentService, never()).getAppDeployments(anyLong(), anyInt());
        verify(appVersionService, never()).getAppVersionsByIds(any());
    }

    @Test
    void testGetCurrentDeploymentOfAnInstanceNeverDeployedIsEmptyWithoutADeploymentRead() {
        stubInstance(AppInstanceStatus.ACTIVE);

        assertThat(appDeploymentFacade.getCurrentDeployment(APP_INSTANCE_ID)).isEmpty();

        verifyNoInteractions(appDeploymentService, appVersionService);
    }

    @Test
    void testCreateVersionTrimsTheName() {
        when(appService.getApp(APP_ID)).thenReturn(new App());
        when(appVersionService.create(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        AppVersionDTO appVersionDTO = appDeploymentFacade.createVersion(APP_ID, " 2.0\n", null, null, null);

        assertThat(appVersionDTO.name()).isEqualTo("2.0");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "1.\u00001", "1.\n1", "1.\r1", "1.\t1", "1.\u001b[0m", "1.\u007f1"
    })
    void testCreateVersionRefusesANameWithControlCharacters(String name) {
        assertThatThrownBy(() -> appDeploymentFacade.createVersion(APP_ID, name, null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("control characters");

        verifyNoInteractions(appService, appVersionService);
    }

    @Test
    void testRecordTrimsTheVersionNameBeforeLookingItUp() {
        stubInstance(AppInstanceStatus.ACTIVE);

        when(appVersionService.fetchAppVersion(APP_ID, "1.1")).thenReturn(Optional.of(appVersion("1.1", "abc123")));
        stubDeploymentCreate();

        assertThat(appDeploymentFacade.recordDeployment(APP_INSTANCE_ID, " 1.1 ", URL, "abc123")
            .appVersion()
            .name()).isEqualTo("1.1");

        verify(appVersionService, never()).create(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "1.\u00001", "1.\n1", "1.\r1", "1.\t1", "1.\u001b[0m", "1.\u007f1"
    })
    void testRecordRefusesAVersionNameWithControlCharacters(String name) {
        assertThatThrownBy(() -> appDeploymentFacade.recordDeployment(APP_INSTANCE_ID, name, URL, "abc123"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("control characters");

        verifyNoInteractions(appDeploymentService, appVersionService, appAuditPublisher);
    }

    @Test
    void testGetCurrentDeploymentOfADeploymentOfAnotherInstanceIsEmpty() {
        AppInstance appInstance = stubInstance(AppInstanceStatus.ACTIVE);

        appInstance.setCurrentAppDeploymentId(APP_DEPLOYMENT_ID);

        AppDeployment foreignAppDeployment = appDeployment(APP_DEPLOYMENT_ID);

        foreignAppDeployment.setAppInstanceId(APP_INSTANCE_ID + 1);

        when(appDeploymentService.getAppDeployment(APP_DEPLOYMENT_ID)).thenReturn(foreignAppDeployment);

        assertThat(appDeploymentFacade.getCurrentDeployment(APP_INSTANCE_ID)).isEmpty();

        verifyNoInteractions(appVersionService);
    }

    @Test
    void testCreateVersionStoresTheGivenFieldsOnTheApp() {
        when(appService.getApp(APP_ID)).thenReturn(new App());
        when(appVersionService.create(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        AppVersionDTO appVersionDTO = appDeploymentFacade.createVersion(
            APP_ID, "2.0", "release/2.0", "fff000", "Second release");

        assertThat(appVersionDTO.id()).isEqualTo(APP_VERSION_ID);
        assertThat(appVersionDTO.appId()).isEqualTo(APP_ID);
        assertThat(appVersionDTO.name()).isEqualTo("2.0");
        assertThat(appVersionDTO.gitRef()).isEqualTo("release/2.0");
        assertThat(appVersionDTO.gitCommitSha()).isEqualTo("fff000");
        assertThat(appVersionDTO.description()).isEqualTo("Second release");

        verifyNoInteractions(appAuditPublisher);
    }

    @Test
    void testCreateVersionOfUnknownAppIsNotFound() {
        when(appService.getApp(APP_ID))
            .thenThrow(new ConfigurationException("App 1 not found", AppErrorType.APP_NOT_FOUND));

        assertThatThrownBy(() -> appDeploymentFacade.createVersion(APP_ID, "2.0", null, null, null))
            .isInstanceOf(ConfigurationException.class);

        verify(appVersionService, never()).create(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", " ", "\t\n"
    })
    void testCreateVersionTreatsBlankOptionalFieldsAsAbsent(String blank) {
        when(appService.getApp(APP_ID)).thenReturn(new App());
        when(appVersionService.create(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        AppVersionDTO appVersionDTO = appDeploymentFacade.createVersion(APP_ID, "2.0", blank, blank, blank);

        assertThat(appVersionDTO.gitRef()).isNull();
        assertThat(appVersionDTO.gitCommitSha()).isNull();
        assertThat(appVersionDTO.description()).isNull();
    }

    @Test
    void testCreateVersionTrimsTheCommitAndTheRef() {
        when(appService.getApp(APP_ID)).thenReturn(new App());
        when(appVersionService.create(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        AppVersionDTO appVersionDTO = appDeploymentFacade.createVersion(
            APP_ID, "2.0", " release/2.0\n", "fff000\n", "Second release");

        assertThat(appVersionDTO.gitRef()).isEqualTo("release/2.0");
        assertThat(appVersionDTO.gitCommitSha()).isEqualTo("fff000");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", " ", "\n"
    })
    void testRecordTreatsABlankCommitAsAbsentAndReusesTheExistingVersion(String blank) {
        stubInstance(AppInstanceStatus.ACTIVE);

        when(appVersionService.fetchAppVersion(APP_ID, "1.1"))
            .thenReturn(Optional.of(appVersion("1.1", "abc123")));
        stubDeploymentCreate();

        AppDeploymentDTO appDeploymentDTO = appDeploymentFacade.recordDeployment(
            APP_INSTANCE_ID, "1.1", URL, blank);

        assertThat(appDeploymentDTO.appVersion()
            .gitCommitSha()).isEqualTo("abc123");

        verify(appVersionService, never()).create(any());
    }

    @Test
    void testRecordStoresNoCommitForANewVersionWhenTheCommitIsBlank() {
        stubInstance(AppInstanceStatus.ACTIVE);

        when(appVersionService.fetchAppVersion(APP_ID, "1.1")).thenReturn(Optional.empty());
        when(appVersionService.create(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        stubDeploymentCreate();

        appDeploymentFacade.recordDeployment(APP_INSTANCE_ID, "1.1", " ", " ");

        ArgumentCaptor<AppVersion> appVersionCaptor = ArgumentCaptor.forClass(AppVersion.class);
        ArgumentCaptor<AppDeployment> appDeploymentCaptor = ArgumentCaptor.forClass(AppDeployment.class);

        verify(appVersionService).create(appVersionCaptor.capture());
        verify(appDeploymentService).create(appDeploymentCaptor.capture());

        AppVersion createdAppVersion = appVersionCaptor.getValue();
        AppDeployment recordedAppDeployment = appDeploymentCaptor.getValue();

        assertThat(createdAppVersion.getGitCommitSha()).isNull();
        assertThat(recordedAppDeployment.getUrl()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://crm.example.com", "http://localhost:3000/path?query=1", "HTTPS://CRM.EXAMPLE.COM"
    })
    void testRecordAcceptsAnHttpOrHttpsUrl(String url) {
        stubInstance(AppInstanceStatus.ACTIVE);

        when(appVersionService.fetchAppVersion(APP_ID, "1.1"))
            .thenReturn(Optional.of(appVersion("1.1", "abc123")));
        stubDeploymentCreate();

        assertThat(appDeploymentFacade.recordDeployment(APP_INSTANCE_ID, "1.1", url, null)
            .url()).isEqualTo(url);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "javascript:alert(1)", "JavaScript://crm.example.com/%0Aalert(1)", "data:text/html,<script>",
        "ftp://crm.example.com", "crm.example.com", "/relative/path", "https://", "https://exa mple.com",
        "mailto:someone@example.com"
    })
    void testRecordRefusesAUrlThatIsNotHttpOrHttps(String url) {
        assertThatThrownBy(() -> appDeploymentFacade.recordDeployment(APP_INSTANCE_ID, "1.1", url, "abc123"))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_DEPLOYMENT_URL_INVALID.getErrorKey()));

        verifyNoInteractions(appInstanceService, appVersionService, appDeploymentService, appAuditPublisher);
    }

    private double deploymentCount(String provider, String status) {
        Counter counter = meterRegistry.find("bytechef_app_deployment")
            .tag("provider", provider)
            .tag("status", status)
            .counter();

        return counter == null ? 0.0 : counter.count();
    }

    private double totalDeploymentCount() {
        return meterRegistry.find("bytechef_app_deployment")
            .counters()
            .stream()
            .mapToDouble(Counter::count)
            .sum();
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<MeterRegistry> meterRegistryProvider(MeterRegistry registry) {
        ObjectProvider<MeterRegistry> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(registry);

        return objectProvider;
    }

    private AppInstance stubInstance(AppInstanceStatus status) {
        AppInstance appInstance = new AppInstance();

        appInstance.setId(APP_INSTANCE_ID);
        appInstance.setAppId(APP_ID);
        appInstance.setStatus(status);

        when(appInstanceService.getAppInstance(APP_INSTANCE_ID)).thenReturn(appInstance);

        return appInstance;
    }

    private AppInstance stubDeployableInstance(
        AppHostingTarget hostingTarget, @Nullable Long deploymentConnectionId) {

        AppInstance appInstance = stubInstance(AppInstanceStatus.ACTIVE);

        appInstance.setEnvironment(Environment.PRODUCTION);
        appInstance.setHostingTarget(hostingTarget);
        appInstance.setDeploymentConnectionId(deploymentConnectionId);
        appInstance.setDeploymentSettings(Map.of("projectId", "prj_1"));

        return appInstance;
    }

    private AppVersion stubVersion(
        long appVersionId, String name, @Nullable String gitRef, @Nullable String gitCommitSha) {

        AppVersion appVersion = appVersion(name, gitCommitSha);

        appVersion.setId(appVersionId);
        appVersion.setGitRef(gitRef);

        lenient().when(appVersionService.getAppVersion(appVersionId))
            .thenReturn(appVersion);

        return appVersion;
    }

    private Map<Long, AppDeployment> stubDeploymentStore() {
        Map<Long, AppDeployment> appDeployments = new HashMap<>();

        lenient().when(appDeploymentService.create(any()))
            .thenAnswer(invocation -> {
                AppDeployment appDeployment = invocation.getArgument(0);

                appDeployment.setId(APP_DEPLOYMENT_ID);
                appDeployment.setCreatedBy("editor");
                appDeployment.setCreatedDate(Instant.now());

                appDeployments.put(APP_DEPLOYMENT_ID, appDeployment);

                return appDeployment;
            });
        lenient().when(appDeploymentService.getAppDeployment(anyLong()))
            .thenAnswer(invocation -> Objects.requireNonNull(appDeployments.get(invocation.<Long>getArgument(0))));
        lenient().when(appDeploymentService.updateStatus(anyLong(), any(), any(), any()))
            .thenAnswer(invocation -> updateStoredDeployment(
                appDeployments, invocation.getArgument(0), null, invocation.getArgument(1),
                invocation.getArgument(2), invocation.getArgument(3)));
        lenient().when(appDeploymentService.updateStatus(anyLong(), any(), any(), any(), any()))
            .thenAnswer(invocation -> updateStoredDeployment(
                appDeployments, invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2),
                invocation.getArgument(3), invocation.getArgument(4)));
        lenient().when(appDeploymentService.cancel(anyLong(), any()))
            .thenAnswer(invocation -> {
                AppDeployment appDeployment = Objects.requireNonNull(
                    appDeployments.get(invocation.<Long>getArgument(0)));

                String providerDeploymentId = appDeployment.getProviderDeploymentId();

                if (providerDeploymentId != null && !providerDeploymentId.equals(invocation.getArgument(1))) {
                    return Optional.empty();
                }

                return Optional.of(
                    updateStoredDeployment(
                        appDeployments, appDeployment.getId(), null, AppDeploymentStatus.CANCELED, null, null));
            });

        return appDeployments;
    }

    private AppDeployment updateStoredDeployment(
        Map<Long, AppDeployment> appDeployments, long id, @Nullable String providerDeploymentId,
        AppDeploymentStatus status, @Nullable String url, @Nullable String errorMessage) {

        AppDeployment appDeployment = Objects.requireNonNull(appDeployments.get(id));

        assertThat(appDeployment.getStatus()
            .isTerminal()).isFalse();

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

        return appDeployment;
    }

    private static AppDeployment buildingDeployment() {
        AppDeployment appDeployment = appDeployment(APP_DEPLOYMENT_ID);

        appDeployment.setProvider(AppDeploymentProviderType.VERCEL);
        appDeployment.setProviderDeploymentId("dpl_1");
        appDeployment.setStatus(AppDeploymentStatus.BUILDING);

        return appDeployment;
    }

    private void stubDeploymentCreate() {
        when(appDeploymentService.create(any())).thenAnswer(invocation -> {
            AppDeployment appDeployment = invocation.getArgument(0);

            appDeployment.setId(APP_DEPLOYMENT_ID);
            appDeployment.setCreatedBy("editor");
            appDeployment.setCreatedDate(Instant.now());
            appDeployment.setFinishedDate(Instant.now());

            return appDeployment;
        });
        when(appInstanceService.updateCurrentAppDeployment(eq(APP_INSTANCE_ID), eq(APP_DEPLOYMENT_ID)))
            .thenReturn(new AppInstance());
    }

    private static AppDeployment appDeployment(long id) {
        AppDeployment appDeployment = new AppDeployment();

        appDeployment.setId(id);
        appDeployment.setAppInstanceId(APP_INSTANCE_ID);
        appDeployment.setAppVersionId(APP_VERSION_ID);
        appDeployment.setProvider(AppDeploymentProviderType.EXTERNAL);
        appDeployment.setStatus(AppDeploymentStatus.READY);
        appDeployment.setCreatedBy("editor");
        appDeployment.setCreatedDate(Instant.now());

        return appDeployment;
    }

    private static AppVersion appVersion(String name, String gitCommitSha) {
        AppVersion appVersion = new AppVersion();

        appVersion.setId(APP_VERSION_ID);
        appVersion.setAppId(APP_ID);
        appVersion.setName(name);
        appVersion.setGitCommitSha(gitCommitSha);
        appVersion.setCreatedBy("editor");
        appVersion.setCreatedDate(Instant.now());

        return appVersion;
    }

    private static AppVersion withId(AppVersion appVersion) {
        appVersion.setId(APP_VERSION_ID);
        appVersion.setCreatedBy("editor");
        appVersion.setCreatedDate(Instant.now());

        return appVersion;
    }

    private final class FakeAppDeploymentProvider implements AppDeploymentProvider {

        private final AppHostingTarget hostingTarget;
        private final List<ProviderDeploymentRef> canceledRefs = new ArrayList<>();
        private final List<ProviderDeploymentRequest> deployRequests = new ArrayList<>();
        private @Nullable RuntimeException cancelFailure;
        private int cancelAttempts;
        private @Nullable RuntimeException deployFailure;
        private @Nullable Runnable duringDeploy;
        private ProviderDeployment nextDeployment = new ProviderDeployment(
            "dpl_1", AppDeploymentStatus.BUILDING, null);
        private @Nullable AppDeploymentStatus queuedStatusAtDeploy;

        private FakeAppDeploymentProvider(AppHostingTarget hostingTarget) {
            this.hostingTarget = hostingTarget;
        }

        @Override
        public AppHostingTarget getHostingTarget() {
            return hostingTarget;
        }

        @Override
        public String getDeploymentConnectionComponentName() {
            return hostingTarget == AppHostingTarget.VERCEL ? "vercel" : "httpClient";
        }

        @Override
        public ProviderDeployment deploy(ProviderDeploymentRequest request) {
            deployRequests.add(request);

            AppDeployment appDeployment = appDeploymentService.getAppDeployment(APP_DEPLOYMENT_ID);

            queuedStatusAtDeploy = appDeployment.getStatus();

            if (duringDeploy != null) {
                duringDeploy.run();
            }

            if (deployFailure != null) {
                throw deployFailure;
            }

            return nextDeployment;
        }

        @Override
        public ProviderDeployment getDeployment(ProviderDeploymentRef ref) {
            return nextDeployment;
        }

        @Override
        public void cancel(ProviderDeploymentRef ref) {
            cancelAttempts++;

            if (cancelFailure != null) {
                throw cancelFailure;
            }

            canceledRefs.add(ref);
        }
    }

    private static String guardOf(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = AppDeploymentFacadeImpl.class.getMethod(methodName, parameterTypes);

        PreAuthorize preAuthorize = Objects.requireNonNull(method.getAnnotation(PreAuthorize.class));

        return preAuthorize.value();
    }
}
