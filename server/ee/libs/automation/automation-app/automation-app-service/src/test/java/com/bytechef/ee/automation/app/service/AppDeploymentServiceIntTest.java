/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.automation.app.config.AppIntTestConfiguration;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppDeployment;
import com.bytechef.ee.automation.app.domain.AppDeploymentProviderType;
import com.bytechef.ee.automation.app.domain.AppDeploymentStatus;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppVersion;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.repository.AppRepository;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AppIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class AppDeploymentServiceIntTest {

    @Autowired
    private AppDeploymentService appDeploymentService;

    @Autowired
    private AppInstanceService appInstanceService;

    @Autowired
    private AppRepository appRepository;

    @Autowired
    private AppService appService;

    @Autowired
    private AppVersionService appVersionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long appId;
    private long appInstanceId;
    private long appVersionId;

    @BeforeEach
    void beforeEach() {
        App app = new App();

        app.setName("crm");
        app.setWorkspaceId(1L);
        app.setOwnerUserId(1L);
        app.setVisibility(ResourceVisibility.WORKSPACE);

        appId = appService.create(app)
            .getId();

        AppInstance appInstance = new AppInstance();

        appInstance.setAppId(appId);
        appInstance.setName("Croatia");
        appInstance.setWorkspaceId(1L);
        appInstance.setOwnerUserId(7L);
        appInstance.setEnvironment(Environment.PRODUCTION);
        appInstance.setHostingTarget(AppHostingTarget.SELF_HOSTED);

        appInstanceId = appInstanceService.create(appInstance)
            .appInstance()
            .getId();

        AppVersion appVersion = new AppVersion();

        appVersion.setAppId(appId);
        appVersion.setName("1.0");

        appVersionId = appVersionService.create(appVersion)
            .getId();
    }

    @AfterEach
    void afterEach() {
        appRepository.deleteAll();
    }

    @Test
    void testCreateOfTerminalDeploymentStoresFinishedDate() {
        Instant before = Instant.now();

        AppDeployment created = appDeploymentService.create(
            newAppDeployment(AppDeploymentStatus.READY, "https://crm.example.com"));

        AppDeployment stored = appDeploymentService.getAppDeployment(created.getId());

        assertThat(stored.getAppInstanceId()).isEqualTo(appInstanceId);
        assertThat(stored.getAppVersionId()).isEqualTo(appVersionId);
        assertThat(stored.getProvider()).isEqualTo(AppDeploymentProviderType.EXTERNAL);
        assertThat(stored.getStatus()).isEqualTo(AppDeploymentStatus.READY);
        assertThat(stored.getUrl()).isEqualTo("https://crm.example.com");
        Map<String, ?> metadata = stored.getMetadata();

        assertThat(metadata.get("source")).isEqualTo("ci");
        assertThat(stored.getCreatedBy()).isEqualTo("system");
        assertThat(stored.getCreatedDate()).isNotNull();
        assertThat(stored.getFinishedDate()).isNotNull()
            .isAfterOrEqualTo(before.minusSeconds(1));
    }

    @Test
    void testCreateOfNonTerminalDeploymentHasNoFinishedDate() {
        AppDeployment created = appDeploymentService.create(newAppDeployment(AppDeploymentStatus.QUEUED, null));

        assertThat(appDeploymentService.getAppDeployment(created.getId())
            .getFinishedDate()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = AppDeploymentStatus.class, names = {
        "QUEUED", "BUILDING"
    })
    void testCreateRefusesASecondDeploymentWhileOneIsInProgress(AppDeploymentStatus inProgressStatus) {
        AppDeployment inProgress = appDeploymentService.create(newAppDeployment(AppDeploymentStatus.QUEUED, null));

        appDeploymentService.updateStatus(inProgress.getId(), inProgressStatus, null, null);

        assertThatThrownBy(() -> appDeploymentService.create(newAppDeployment(AppDeploymentStatus.QUEUED, null)))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_DEPLOYMENT_IN_PROGRESS.getErrorKey()));

        assertThat(countDeployments()).isOne();
    }

    @Test
    void testCreateAllowsARecordedDeploymentWhileOneIsInProgress() {
        appDeploymentService.create(newAppDeployment(AppDeploymentStatus.QUEUED, null));
        appDeploymentService.create(newAppDeployment(AppDeploymentStatus.READY, "https://crm.example.com"));

        assertThat(countDeployments()).isEqualTo(2);
    }

    @Test
    void testCreateAllowsANewDeploymentOnceTheLastOneFinished() {
        AppDeployment finished = appDeploymentService.create(newAppDeployment(AppDeploymentStatus.QUEUED, null));

        appDeploymentService.updateStatus(finished.getId(), AppDeploymentStatus.FAILED, null, "Build failed");
        appDeploymentService.create(newAppDeployment(AppDeploymentStatus.QUEUED, null));

        assertThat(countDeployments()).isEqualTo(2);
    }

    @ParameterizedTest
    @EnumSource(value = AppDeploymentStatus.class, names = {
        "QUEUED", "BUILDING"
    })
    void testCheckNoDeploymentInProgressRefusesWhileOneIsInProgress(AppDeploymentStatus inProgressStatus) {
        AppDeployment inProgress = appDeploymentService.create(newAppDeployment(AppDeploymentStatus.QUEUED, null));

        appDeploymentService.updateStatus(inProgress.getId(), inProgressStatus, null, null);

        assertThatThrownBy(() -> appDeploymentService.checkNoDeploymentInProgress(inProgress.getAppInstanceId()))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_DEPLOYMENT_IN_PROGRESS.getErrorKey()));
    }

    @Test
    void testCheckNoDeploymentInProgressAllowsOnceTheDeploymentIsTerminal() {
        AppDeployment finished = appDeploymentService.create(newAppDeployment(AppDeploymentStatus.QUEUED, null));

        appDeploymentService.updateStatus(finished.getId(), AppDeploymentStatus.CANCELED, null, null);

        assertThatCode(() -> appDeploymentService.checkNoDeploymentInProgress(finished.getAppInstanceId()))
            .doesNotThrowAnyException();
    }

    @Test
    void testCancelOfAQueuedRowWithoutAProviderIdWritesCanceled() {
        AppDeployment queued = appDeploymentService.create(newAppDeployment(AppDeploymentStatus.QUEUED, null));

        Optional<AppDeployment> canceled = appDeploymentService.cancel(queued.getId(), null);

        assertThat(canceled).hasValueSatisfying(appDeployment -> {
            assertThat(appDeployment.getStatus()).isEqualTo(AppDeploymentStatus.CANCELED);
            assertThat(appDeployment.getFinishedDate()).isNotNull();
        });
        assertThat(appDeploymentService.getAppDeployment(queued.getId())
            .getStatus()).isEqualTo(AppDeploymentStatus.CANCELED);
    }

    @Test
    void testCancelIsNotWrittenWhenAProviderIdTheCallerDidNotHandleWasCommitted() {
        AppDeployment queued = appDeploymentService.create(newAppDeployment(AppDeploymentStatus.QUEUED, null));

        jdbcTemplate.update(
            "UPDATE app_deployment SET provider_deployment_id = 'dpl_1', status = ?, version = version + 1 " +
                "WHERE id = ?",
            AppDeploymentStatus.BUILDING.ordinal(), queued.getId());

        Optional<AppDeployment> canceled = appDeploymentService.cancel(queued.getId(), null);

        assertThat(canceled).isEmpty();

        AppDeployment stored = appDeploymentService.getAppDeployment(queued.getId());

        assertThat(stored.getStatus()).isEqualTo(AppDeploymentStatus.BUILDING);
        assertThat(stored.getProviderDeploymentId()).isEqualTo("dpl_1");
        assertThat(stored.getFinishedDate()).isNull();
    }

    @Test
    void testCancelWritesCanceledWhenTheProviderIdIsTheOneTheCallerHandled() {
        AppDeployment queued = appDeploymentService.create(newAppDeployment(AppDeploymentStatus.QUEUED, null));

        appDeploymentService.updateStatus(queued.getId(), "dpl_1", AppDeploymentStatus.BUILDING, null, null);

        Optional<AppDeployment> canceled = appDeploymentService.cancel(queued.getId(), "dpl_1");

        assertThat(canceled).hasValueSatisfying(
            appDeployment -> assertThat(appDeployment.getStatus()).isEqualTo(AppDeploymentStatus.CANCELED));
    }

    @Test
    void testCancelOfATerminalRowIsRefused() {
        AppDeployment queued = appDeploymentService.create(newAppDeployment(AppDeploymentStatus.QUEUED, null));

        appDeploymentService.updateStatus(queued.getId(), AppDeploymentStatus.FAILED, null, "Build failed");

        assertThatThrownBy(() -> appDeploymentService.cancel(queued.getId(), null))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_DEPLOYMENT_TERMINAL.getErrorKey()));
    }

    @Test
    void testGetNonTerminalAppDeploymentsListsOnlyUnfinishedOnes() {
        appDeploymentService.create(newAppDeployment(AppDeploymentStatus.READY, null));

        AppDeployment queued = appDeploymentService.create(newAppDeployment(AppDeploymentStatus.QUEUED, null));

        assertThat(appDeploymentService.getNonTerminalAppDeployments())
            .extracting(AppDeployment::getId)
            .containsExactly(queued.getId());
    }

    @Test
    void testUpdateStatusStoresTheProviderDeploymentId() {
        AppDeployment queued = appDeploymentService.create(newAppDeployment(AppDeploymentStatus.QUEUED, null));

        appDeploymentService.updateStatus(
            queued.getId(), "dpl_1", AppDeploymentStatus.BUILDING, "https://crm.vercel.app", null);

        AppDeployment stored = appDeploymentService.getAppDeployment(queued.getId());

        assertThat(stored.getProviderDeploymentId()).isEqualTo("dpl_1");
        assertThat(stored.getStatus()).isEqualTo(AppDeploymentStatus.BUILDING);
        assertThat(stored.getUrl()).isEqualTo("https://crm.vercel.app");
    }

    @Test
    void testHasReadyAppDeploymentIsTrueOnlyForAVersionThatWasReadyOnTheInstance() {
        AppDeployment queued = appDeploymentService.create(newAppDeployment(AppDeploymentStatus.QUEUED, null));

        assertThat(appDeploymentService.hasReadyAppDeployment(appInstanceId, appVersionId)).isFalse();

        appDeploymentService.updateStatus(queued.getId(), AppDeploymentStatus.READY, null, null);

        assertThat(appDeploymentService.hasReadyAppDeployment(appInstanceId, appVersionId)).isTrue();
    }

    @Test
    void testCreateRefusesTooLongUrl() {
        assertThatThrownBy(() -> appDeploymentService.create(
            newAppDeployment(AppDeploymentStatus.READY, "https://" + "a".repeat(2048))))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> assertThat(exception.getErrorKey())
                        .isEqualTo(AppErrorType.APP_FIELD_TOO_LONG.getErrorKey()));
    }

    @ParameterizedTest
    @EnumSource(value = AppDeploymentStatus.class, names = {
        "READY", "FAILED", "CANCELED"
    })
    void testTerminalDeploymentCannotBeUpdated(AppDeploymentStatus terminalStatus) {
        AppDeployment created = appDeploymentService.create(newAppDeployment(AppDeploymentStatus.QUEUED, null));

        appDeploymentService.updateStatus(created.getId(), terminalStatus, "https://crm.example.com", null);

        AppDeployment terminal = appDeploymentService.getAppDeployment(created.getId());

        for (AppDeploymentStatus status : AppDeploymentStatus.values()) {
            assertThatThrownBy(
                () -> appDeploymentService.updateStatus(created.getId(), status, "https://other.example.com", "x"))
                    .isInstanceOfSatisfying(
                        ConfigurationException.class,
                        exception -> assertThat(exception.getErrorKey())
                            .isEqualTo(AppErrorType.APP_DEPLOYMENT_TERMINAL.getErrorKey()));
        }

        AppDeployment stored = appDeploymentService.getAppDeployment(created.getId());

        assertThat(stored.getStatus()).isEqualTo(terminalStatus);
        assertThat(stored.getUrl()).isEqualTo("https://crm.example.com");
        assertThat(stored.getErrorMessage()).isNull();
        assertThat(stored.getFinishedDate()).isEqualTo(terminal.getFinishedDate());
        assertThat(stored.getVersion()).isEqualTo(terminal.getVersion());
    }

    @Test
    void testNonTerminalDeploymentMovesToTerminalAndGetsFinishedDate() {
        AppDeployment created = appDeploymentService.create(newAppDeployment(AppDeploymentStatus.QUEUED, null));

        AppDeployment building = appDeploymentService.updateStatus(
            created.getId(), AppDeploymentStatus.BUILDING, null, null);

        assertThat(building.getFinishedDate()).isNull();

        AppDeployment failed = appDeploymentService.updateStatus(
            created.getId(), AppDeploymentStatus.FAILED, null, "Build failed");

        assertThat(failed.getStatus()).isEqualTo(AppDeploymentStatus.FAILED);
        assertThat(failed.getErrorMessage()).isEqualTo("Build failed");
        assertThat(failed.getFinishedDate()).isNotNull();
    }

    @Test
    void testRecordedDeploymentIsNeverUpdatable() {
        AppDeployment recorded = appDeploymentService.create(
            newAppDeployment(AppDeploymentStatus.READY, "https://crm.example.com"));

        assertThatThrownBy(
            () -> appDeploymentService.updateStatus(recorded.getId(), AppDeploymentStatus.FAILED, null, "x"))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> assertThat(exception.getErrorKey())
                        .isEqualTo(AppErrorType.APP_DEPLOYMENT_TERMINAL.getErrorKey()));
    }

    @Test
    void testGetAppDeploymentOfUnknownIdIsNotFound() {
        assertThatThrownBy(() -> appDeploymentService.getAppDeployment(Long.MAX_VALUE))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_DEPLOYMENT_NOT_FOUND.getErrorKey()));
    }

    @Test
    void testGetAppDeploymentsIsNewestFirst() {
        AppDeployment first = appDeploymentService.create(newAppDeployment(AppDeploymentStatus.READY, null));
        AppDeployment second = appDeploymentService.create(newAppDeployment(AppDeploymentStatus.READY, null));

        assertThat(appDeploymentService.getAppDeployments(appInstanceId, 50))
            .extracting(AppDeployment::getId)
            .containsExactly(second.getId(), first.getId());
    }

    @Test
    void testGetAppDeploymentsReturnsOnlyTheNewestLimitOfThem() {
        AppDeployment first = appDeploymentService.create(newAppDeployment(AppDeploymentStatus.READY, null));
        AppDeployment second = appDeploymentService.create(newAppDeployment(AppDeploymentStatus.READY, null));
        AppDeployment third = appDeploymentService.create(newAppDeployment(AppDeploymentStatus.READY, null));

        assertThat(appDeploymentService.getAppDeployments(appInstanceId, 2))
            .extracting(AppDeployment::getId)
            .containsExactly(third.getId(), second.getId())
            .doesNotContain(first.getId());
    }

    @Test
    void testDeletingInstanceDeletesItsDeploymentsEvenTheCurrentOne() {
        AppDeployment deployment = appDeploymentService.create(newAppDeployment(AppDeploymentStatus.READY, null));

        appInstanceService.updateCurrentAppDeployment(appInstanceId, deployment.getId());

        appInstanceService.delete(appInstanceId);

        assertThat(countDeployments()).isZero();
        assertThat(appVersionService.getAppVersion(appVersionId)
            .getName()).isEqualTo("1.0");
    }

    @Test
    void testDeletingAppDeletesItsVersionsAndDeployments() {
        AppDeployment deployment = appDeploymentService.create(newAppDeployment(AppDeploymentStatus.READY, null));

        appInstanceService.updateCurrentAppDeployment(appInstanceId, deployment.getId());

        appService.delete(appId);

        assertThat(countDeployments()).isZero();
        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM app_version WHERE app_id = ?", Long.class, appId)).isZero();
    }

    @Test
    void testVersionReferencedByDeploymentCannotBeDeletedOnItsOwn() {
        appDeploymentService.create(newAppDeployment(AppDeploymentStatus.READY, null));

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM app_version WHERE id = ?", appVersionId))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testCurrentDeploymentReferenceIsClearedWhenTheDeploymentRowGoes() {
        AppDeployment deployment = appDeploymentService.create(newAppDeployment(AppDeploymentStatus.READY, null));

        appInstanceService.updateCurrentAppDeployment(appInstanceId, deployment.getId());

        jdbcTemplate.update("DELETE FROM app_deployment WHERE id = ?", deployment.getId());

        assertThat(appInstanceService.getAppInstance(appInstanceId)
            .getCurrentAppDeploymentId()).isNull();
    }

    private long countDeployments() {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM app_deployment WHERE app_instance_id = ?", Long.class, appInstanceId);

        return count == null ? 0 : count;
    }

    private AppDeployment newAppDeployment(AppDeploymentStatus status, String url) {
        AppDeployment appDeployment = new AppDeployment();

        appDeployment.setAppInstanceId(appInstanceId);
        appDeployment.setAppVersionId(appVersionId);
        appDeployment.setProvider(AppDeploymentProviderType.EXTERNAL);
        appDeployment.setStatus(status);
        appDeployment.setUrl(url);
        appDeployment.setMetadata(Map.of("source", "ci"));

        return appDeployment;
    }
}
