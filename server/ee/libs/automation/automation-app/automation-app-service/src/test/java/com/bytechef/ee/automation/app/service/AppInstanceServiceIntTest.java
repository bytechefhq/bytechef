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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.bytechef.ee.automation.app.config.AppIntTestConfiguration;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.repository.AppInstanceRepository;
import com.bytechef.ee.automation.app.repository.AppRepository;
import com.bytechef.ee.automation.app.util.AppSecretKeys;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AppIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class AppInstanceServiceIntTest {

    @Autowired
    private AppApiCollectionService appApiCollectionService;

    @MockitoSpyBean
    private AppInstanceRepository appInstanceRepository;

    @Autowired
    private AppInstanceService appInstanceService;

    @Autowired
    private AppRepository appRepository;

    @Autowired
    private AppService appService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long appId;

    @BeforeEach
    void beforeEach() {
        App app = new App();

        app.setName("crm");
        app.setWorkspaceId(1L);
        app.setOwnerUserId(1L);
        app.setVisibility(ResourceVisibility.WORKSPACE);

        appId = appService.create(app)
            .getId();
    }

    @AfterEach
    void afterEach() {
        appRepository.deleteAll();
    }

    @Test
    void testCreateGeneratesKeyAndReturnsItOnce() {
        AppInstanceService.CreatedAppInstance created = create("Croatia", Environment.PRODUCTION);

        AppInstance stored = appInstanceRepository.findById(created.appInstance()
            .getId())
            .orElseThrow();

        assertThat(created.secretKey()).isNotBlank();
        assertThat(stored.getSecretKeyHash()).isEqualTo(AppSecretKeys.hash(created.secretKey()));
        assertThat(stored.getSecretKeyHash()).isNotEqualTo(created.secretKey());
        assertThat(stored.getSecretKeyHint()).isEqualTo(AppSecretKeys.hint(created.secretKey()));
        assertThat(stored.getStatus()).isEqualTo(AppInstanceStatus.ACTIVE);
        assertThat(stored.getUserAuthMode()).isEqualTo(AppUserAuthMode.NONE);
        assertThat(stored.getUuid()).isNotBlank();
        assertThat(stored.getAppId()).isEqualTo(appId);
        assertThat(stored.getOwnerUserId()).isEqualTo(7L);
        assertThat(appInstanceService.fetchAppInstanceBySecretKey(created.secretKey())).get()
            .extracting(AppInstance::getId)
            .isEqualTo(stored.getId());
        assertThat(appInstanceService.getAppInstance(stored.getId())
            .getSecretKeyHash()).isNotEqualTo(created.secretKey());
    }

    @Test
    void testCreateKeepsTheSignInModeOfTheGivenInstance() {
        AppInstance appInstance = newAppInstance("Croatia", Environment.PRODUCTION);

        appInstance.setUserAuthMode(AppUserAuthMode.REQUIRED);

        AppInstanceService.CreatedAppInstance createdAppInstance = TenantContext.callWithTenantId(
            "public", () -> appInstanceService.create(appInstance));

        AppInstance created = createdAppInstance.appInstance();

        assertThat(appInstanceService.getAppInstance(created.getId())
            .getUserAuthMode()).isEqualTo(AppUserAuthMode.REQUIRED);
    }

    @Test
    void testUpdateCurrentAppDeploymentPointsTheInstanceAtTheDeployment() {
        AppInstance created = create("Croatia", Environment.PRODUCTION).appInstance();

        jdbcTemplate.update(
            "INSERT INTO app_version (id, app_id, name, created_by, created_date) VALUES (?, ?, '1.0', 'system', " +
                "CURRENT_TIMESTAMP)",
            9001L, appId);
        jdbcTemplate.update(
            "INSERT INTO app_deployment (id, app_instance_id, app_version_id, provider, status, created_by, " +
                "created_date) VALUES (?, ?, ?, 1, 2, 'system', CURRENT_TIMESTAMP)",
            9002L, created.getId(), 9001L);

        AppInstance updated = appInstanceService.updateCurrentAppDeployment(created.getId(), 9002L);

        assertThat(updated.getCurrentAppDeploymentId()).isEqualTo(9002L);
        assertThat(appInstanceService.getAppInstance(created.getId())
            .getCurrentAppDeploymentId()).isEqualTo(9002L);
    }

    @Test
    void testUpdateUserAuthStoresTheModeAndTheClientIdAndFetchByUuidFindsTheInstance() {
        AppInstance created = create("Croatia", Environment.PRODUCTION).appInstance();

        appInstanceService.updateUserAuth(created.getId(), AppUserAuthMode.OPTIONAL, "app-" + created.getUuid());

        AppInstance stored = appInstanceService.fetchAppInstanceByUuid(created.getUuid())
            .orElseThrow();

        assertThat(stored.getUserAuthMode()).isEqualTo(AppUserAuthMode.OPTIONAL);
        assertThat(stored.getRegisteredClientId()).isEqualTo("app-" + created.getUuid());
        assertThat(appInstanceService.fetchAppInstanceByUuid("missing")).isEmpty();

        appInstanceService.updateUserAuth(created.getId(), AppUserAuthMode.NONE, null);

        assertThat(appInstanceService.getAppInstance(created.getId())
            .getRegisteredClientId()).isNull();
    }

    @Test
    void testTwoInstancesOfOneAppInOneEnvironment() {
        AppInstanceService.CreatedAppInstance croatia = create("Croatia", Environment.PRODUCTION);
        AppInstanceService.CreatedAppInstance germany = create("Germany", Environment.PRODUCTION);

        assertThat(appInstanceService.getAppInstances(appId))
            .extracting(AppInstance::getName)
            .containsExactly("Croatia", "Germany");
        assertThat(croatia.secretKey()).isNotEqualTo(germany.secretKey());
        assertThat(appInstanceService.fetchAppInstanceBySecretKey(germany.secretKey())).get()
            .extracting(AppInstance::getName)
            .isEqualTo("Germany");
    }

    @Test
    void testDuplicateNameInEnvironmentIsRejected() {
        create("Croatia", Environment.PRODUCTION);

        assertThatThrownBy(() -> create("Croatia", Environment.PRODUCTION))
            .cause()
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_INSTANCE_NAME_ALREADY_EXISTS.getErrorKey()));

        assertThat(create("Croatia", Environment.STAGING).appInstance()
            .getId()).isNotNull();
    }

    @Test
    void testCreateTranslatesTheNameConstraintViolationToNameAlreadyExists() {
        create("Croatia", Environment.PRODUCTION);

        doReturn(Optional.empty()).when(appInstanceRepository)
            .findByAppIdAndEnvironmentAndName(anyLong(), anyInt(), anyString());

        assertThatThrownBy(() -> create("Croatia", Environment.PRODUCTION))
            .cause()
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_INSTANCE_NAME_ALREADY_EXISTS.getErrorKey()))
            .hasCauseInstanceOf(DataIntegrityViolationException.class);

        assertThat(appInstanceService.getAppInstances(appId)).hasSize(1);
    }

    @Test
    void testRotateReplacesHash() {
        AppInstanceService.CreatedAppInstance created = create("Croatia", Environment.PRODUCTION);
        long id = created.appInstance()
            .getId();

        appInstanceService.touchSecretKeyLastUsedDate(id);

        AppInstanceService.RotatedKey rotated = TenantContext.callWithTenantId(
            "public", () -> appInstanceService.rotateSecretKey(id));

        AppInstance stored = appInstanceRepository.findById(id)
            .orElseThrow();

        assertThat(rotated.secretKey()).isNotEqualTo(created.secretKey());
        assertThat(stored.getSecretKeyHash()).isEqualTo(AppSecretKeys.hash(rotated.secretKey()));
        assertThat(stored.getSecretKeyLastUsedDate()).isNull();
        assertThat(appInstanceService.fetchAppInstanceBySecretKey(created.secretKey())).isEmpty();
        assertThat(appInstanceService.fetchAppInstanceBySecretKey(rotated.secretKey())).isPresent();
    }

    @Test
    void testDeletingAppCascadesToInstancesAndBindings() {
        long appInstanceId = create("Croatia", Environment.PRODUCTION).appInstance()
            .getId();

        long connectionId = Objects.requireNonNull(jdbcTemplate.queryForObject(
            """
                INSERT INTO connection (
                    name, component_name, environment, connection_version, parameters, credential_status, type,
                    created_date, created_by, last_modified_date, last_modified_by, version
                ) VALUES ('crm connection', 'crm', 2, 1, '{}', 0, 0, now(), 'test', now(), 'test', 0)
                RETURNING id
                """,
            Long.class));

        try {
            appApiCollectionService.bind(appInstanceId, 42L);

            jdbcTemplate.update(
                "INSERT INTO app_connection (app_instance_id, connection_id, binding_key) VALUES (?, ?, 'crm')",
                appInstanceId, connectionId);
            jdbcTemplate.update(
                "INSERT INTO app_workflow (app_instance_id, workflow_key, project_deployment_id, workflow_uuid) " +
                    "VALUES (?, 'approve_refund', 11, '0b0f3a3e-0000-4000-8000-000000000001')",
                appInstanceId);
            jdbcTemplate.update(
                "INSERT INTO app_workflow_run (run_id, app_instance_id, workflow_key, job_id, created_date) " +
                    "VALUES ('run-1', ?, 'approve_refund', 5, now())",
                appInstanceId);

            assertThat(appApiCollectionService.getBindings(appInstanceId)).hasSize(1);
            assertThat(countRowsOfInstance("app_connection", appInstanceId)).isEqualTo(1);
            assertThat(countRowsOfInstance("app_workflow", appInstanceId)).isEqualTo(1);
            assertThat(countRowsOfInstance("app_workflow_run", appInstanceId)).isEqualTo(1);

            appService.delete(appId);

            assertThat(appInstanceService.fetchAppInstance(appInstanceId)).isEmpty();
            assertThat(appInstanceService.getAppInstances(appId)).isEmpty();
            assertThat(appApiCollectionService.getBindings(appInstanceId)).isEmpty();
            assertThat(countRowsOfInstance("app_connection", appInstanceId)).isZero();
            assertThat(countRowsOfInstance("app_workflow", appInstanceId)).isZero();
            assertThat(countRowsOfInstance("app_workflow_run", appInstanceId)).isZero();
        } finally {
            jdbcTemplate.update("DELETE FROM app_connection WHERE connection_id = ?", connectionId);
            jdbcTemplate.update("DELETE FROM connection WHERE id = ?", connectionId);
        }
    }

    @Test
    void testGetAppInstancesByAppIdsReadsTheInstancesOfEveryGivenApp() {
        long firstId = create("Croatia", Environment.PRODUCTION).appInstance()
            .getId();
        long secondId = create("Germany", Environment.STAGING).appInstance()
            .getId();

        assertThat(appInstanceService.getAppInstancesByAppIds(List.of(appId)))
            .extracting(AppInstance::getId)
            .containsExactly(firstId, secondId);
        assertThat(appInstanceService.getAppInstancesByAppIds(List.of(appId + 1000))).isEmpty();
        assertThat(appInstanceService.getAppInstancesByAppIds(List.of())).isEmpty();
    }

    @Test
    void testGetWorkspaceAppInstancesReadsTheInstancesOfTheWorkspaceInTheEnvironment() {
        long croatiaId = create("Croatia", Environment.PRODUCTION).appInstance()
            .getId();

        create("Germany", Environment.STAGING);

        AppInstance foreignAppInstance = newAppInstance("Austria", Environment.PRODUCTION);

        foreignAppInstance.setWorkspaceId(2L);

        long austriaId = TenantContext.callWithTenantId("public", () -> appInstanceService.create(foreignAppInstance))
            .appInstance()
            .getId();

        assertThat(appInstanceService.getWorkspaceAppInstances(1L, Environment.PRODUCTION))
            .extracting(AppInstance::getId)
            .containsExactly(croatiaId);
        assertThat(appInstanceService.getWorkspaceAppInstances(2L, Environment.PRODUCTION))
            .extracting(AppInstance::getId)
            .containsExactly(austriaId);
        assertThat(appInstanceService.getWorkspaceAppInstances(2L, Environment.DEVELOPMENT)).isEmpty();
    }

    @Test
    void testATerminatedInstanceCannotLeaveTerminated() {
        long id = create("Croatia", Environment.PRODUCTION).appInstance()
            .getId();

        appInstanceService.updateStatus(id, AppInstanceStatus.TERMINATED);

        assertThatThrownBy(() -> appInstanceService.updateStatus(id, AppInstanceStatus.ACTIVE))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(AppErrorType.APP_INSTANCE_TERMINATED.getErrorKey());
        assertThat(appInstanceService.updateStatus(id, AppInstanceStatus.TERMINATED)
            .getStatus()).isEqualTo(AppInstanceStatus.TERMINATED);
    }

    @Test
    void testTerminateClearsTheDeploymentConnectionAndSettings() {
        long id = create("Croatia", Environment.PRODUCTION).appInstance()
            .getId();

        appInstanceService.updateDeploymentSettings(id, 31L, Map.of("projectId", "prj_1"));

        AppInstance terminated = appInstanceService.updateStatus(id, AppInstanceStatus.TERMINATED);

        assertThat(terminated.getDeploymentConnectionId()).isNull();
        assertThat(terminated.getDeploymentSettings()).isEmpty();
        assertThat(appInstanceService.getAppInstance(id)
            .getDeploymentConnectionId()).isNull();
    }

    @Test
    void testATerminatedInstanceRefusesKeyRotationConfigurationAndDeploymentSettings() {
        long id = create("Croatia", Environment.PRODUCTION).appInstance()
            .getId();

        appInstanceService.updateStatus(id, AppInstanceStatus.TERMINATED);

        for (ThrowingCallable refusedChange : List.<ThrowingCallable>of(
            () -> appInstanceService.rotateSecretKey(id),
            () -> appInstanceService.updateConfiguration(id, Map.of("region", "eu")),
            () -> appInstanceService.updateDeploymentSettings(id, 31L, Map.of()))) {

            assertThatThrownBy(refusedChange)
                .isInstanceOf(ConfigurationException.class)
                .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AppErrorType.APP_INSTANCE_TERMINATED.getErrorKey());
        }

        assertThat(appInstanceService.getAppInstance(id)
            .getDeploymentConnectionId()).isNull();
    }

    @Test
    void testSuspendKeepsTheDeploymentConnectionAndSettings() {
        long id = create("Croatia", Environment.PRODUCTION).appInstance()
            .getId();

        appInstanceService.updateDeploymentSettings(id, 31L, Map.of("projectId", "prj_1"));

        AppInstance suspended = appInstanceService.updateStatus(id, AppInstanceStatus.SUSPENDED);

        assertThat(suspended.getDeploymentConnectionId()).isEqualTo(31L);
        assertThat(suspended.getDeploymentSettings()).isEqualTo(Map.of("projectId", "prj_1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "crm.example.com", "ftp://crm.example.com", "javascript:alert(1)", "https://user:secret@crm.example.com",
        "https:///path-only"
    })
    void testUpdateRefusesAUrlThatIsNotAnAbsoluteHttpUrlWithoutUserInfo(String url) {
        long id = create("Croatia", Environment.PRODUCTION).appInstance()
            .getId();

        assertThatThrownBy(() -> appInstanceService.update(id, null, null, url))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(AppErrorType.APP_INSTANCE_URL_INVALID.getErrorKey());
        assertThat(appInstanceService.getAppInstance(id)
            .getUrl()).isNull();
    }

    @Test
    void testCreateRefusesAUrlThatIsNotAnAbsoluteHttpUrl() {
        AppInstance appInstance = newAppInstance("Croatia", Environment.PRODUCTION);

        appInstance.setUrl("ftp://crm.example.com");

        assertThatThrownBy(
            () -> TenantContext.callWithTenantId("public", () -> appInstanceService.create(appInstance)))
                .rootCause()
                .isInstanceOf(ConfigurationException.class)
                .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AppErrorType.APP_INSTANCE_URL_INVALID.getErrorKey());
    }

    @Test
    void testUpdateAcceptsAnHttpOrHttpsUrlAndClearsAnEmptyOne() {
        long id = create("Croatia", Environment.DEVELOPMENT).appInstance()
            .getId();

        assertThat(appInstanceService.update(id, null, null, "http://localhost:3000")
            .getUrl()).isEqualTo("http://localhost:3000");
        assertThat(appInstanceService.update(id, null, null, "https://crm.example.com/app")
            .getUrl()).isEqualTo("https://crm.example.com/app");
        assertThat(appInstanceService.update(id, null, null, "")
            .getUrl()).isNull();
    }

    @Test
    void testUpdateIsKeepWhenNull() {
        long id = create("Croatia", Environment.PRODUCTION).appInstance()
            .getId();

        appInstanceService.update(id, "Hrvatska", AppHostingTarget.SELF_HOSTED, "https://crm.test");

        AppInstance kept = appInstanceService.update(id, null, null, null);

        assertThat(kept.getName()).isEqualTo("Hrvatska");
        assertThat(kept.getHostingTarget()).isEqualTo(AppHostingTarget.SELF_HOSTED);
        assertThat(kept.getUrl()).isEqualTo("https://crm.test");
        assertThat(kept.getEnvironment()).isEqualTo(Environment.PRODUCTION);

        AppInstance cleared = appInstanceService.update(id, null, null, "");

        assertThat(cleared.getUrl()).isNull();
        assertThat(cleared.getName()).isEqualTo("Hrvatska");
    }

    @Test
    void testUpdateRejectsARenameToAnExistingNameAndAUrlLongerThanTheColumn() {
        create("Croatia", Environment.PRODUCTION);

        long id = create("Germany", Environment.PRODUCTION).appInstance()
            .getId();
        String url = "https://x.test/" + "a".repeat(2048);

        assertThatThrownBy(() -> appInstanceService.update(id, "Croatia", null, null))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_INSTANCE_NAME_ALREADY_EXISTS.getErrorKey()));
        assertThatThrownBy(() -> appInstanceService.update(id, null, null, url))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_FIELD_TOO_LONG.getErrorKey()));
        assertThatThrownBy(() -> appInstanceService.update(id, " ", null, null))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(appInstanceService.update(id, "Germany", null, null)
            .getName()).isEqualTo("Germany");
    }

    @Test
    void testOwnerUserIdIsRequired() {
        AppInstance appInstance = newAppInstance("Croatia", Environment.PRODUCTION);

        appInstance.setOwnerUserId(null);

        assertThatThrownBy(() -> TenantContext.callWithTenantId("public", () -> appInstanceService.create(appInstance)))
            .cause()
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("ownerUserId");

        assertThat(appInstanceService.getAppInstances(appId)).isEmpty();
    }

    @Test
    void testUpdateStatusChangesOnlyTheStatus() {
        long id = create("Croatia", Environment.PRODUCTION).appInstance()
            .getId();

        AppInstance suspended = appInstanceService.updateStatus(id, AppInstanceStatus.SUSPENDED);

        assertThat(suspended.getStatus()).isEqualTo(AppInstanceStatus.SUSPENDED);
        assertThat(suspended.getName()).isEqualTo("Croatia");
    }

    @Test
    void testUpdateVisibilityChangesOnlyTheVisibility() {
        long id = create("Croatia", Environment.PRODUCTION).appInstance()
            .getId();

        AppInstance updated = appInstanceService.updateVisibility(id, ResourceVisibility.PRIVATE);

        assertThat(updated.getVisibility()).isEqualTo(ResourceVisibility.PRIVATE);
        assertThat(updated.getName()).isEqualTo("Croatia");
        assertThat(appInstanceService.getAppInstance(id)
            .getVisibility()).isEqualTo(ResourceVisibility.PRIVATE);
    }

    @Test
    void testUpdateKeepsTheVisibilitySetThroughSharing() {
        long id = create("Croatia", Environment.PRODUCTION).appInstance()
            .getId();

        appInstanceService.updateVisibility(id, ResourceVisibility.PRIVATE);

        AppInstance updated = appInstanceService.update(id, "Hrvatska", AppHostingTarget.SELF_HOSTED, "https://x.test");

        assertThat(updated.getName()).isEqualTo("Hrvatska");
        assertThat(updated.getVisibility()).isEqualTo(ResourceVisibility.PRIVATE);
        assertThat(appInstanceService.getAppInstance(id)
            .getVisibility()).isEqualTo(ResourceVisibility.PRIVATE);
    }

    @Test
    void testUpdateConfigurationStoresTheConfigurationAndKeepsTheOtherFields() {
        long id = create("Croatia", Environment.PRODUCTION).appInstance()
            .getId();

        assertThat(appInstanceService.getAppInstance(id)
            .getConfiguration()).isEmpty();

        AppInstance updated = appInstanceService.updateConfiguration(
            id, Map.of("country", "HR", "limits", Map.of("seats", 5)));

        assertThat(updated.getName()).isEqualTo("Croatia");

        AppInstance reloaded = appInstanceService.getAppInstance(id);

        assertThat(reloaded.getConfiguration()).isEqualTo(Map.of("country", "HR", "limits", Map.of("seats", 5)));

        appInstanceService.updateConfiguration(id, Map.of());

        assertThat(appInstanceService.getAppInstance(id)
            .getConfiguration()).isEmpty();
    }

    @Test
    void testTouchLastUsedWritesOncePerMinute() {
        long id = create("Croatia", Environment.PRODUCTION).appInstance()
            .getId();

        appInstanceService.touchSecretKeyLastUsedDate(id);

        Instant first = appInstanceRepository.findById(id)
            .orElseThrow()
            .getSecretKeyLastUsedDate();

        appInstanceService.touchSecretKeyLastUsedDate(id);

        Instant second = appInstanceRepository.findById(id)
            .orElseThrow()
            .getSecretKeyLastUsedDate();

        assertThat(first).isNotNull();
        assertThat(second).isEqualTo(first);
    }

    @Test
    void testTouchLastUsedSwallowsRepositoryFailures() {
        long id = create("Croatia", Environment.PRODUCTION).appInstance()
            .getId();

        doThrow(new QueryTimeoutException("database unavailable")).when(appInstanceRepository)
            .touchSecretKeyLastUsedDate(eq(id), any(Instant.class), any(Instant.class));

        assertThatCode(() -> appInstanceService.touchSecretKeyLastUsedDate(id)).doesNotThrowAnyException();

        verify(appInstanceRepository).touchSecretKeyLastUsedDate(eq(id), any(Instant.class), any(Instant.class));

        assertThat(appInstanceRepository.findById(id)
            .orElseThrow()
            .getSecretKeyLastUsedDate()).isNull();
    }

    @Test
    void testGetAppInstanceFailsWhenMissing() {
        assertThatThrownBy(() -> appInstanceService.getAppInstance(-1L))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_INSTANCE_NOT_FOUND.getErrorKey()));
    }

    private AppInstanceService.CreatedAppInstance create(String name, Environment environment) {
        return TenantContext.callWithTenantId(
            "public", () -> appInstanceService.create(newAppInstance(name, environment)));
    }

    private AppInstance newAppInstance(String name, Environment environment) {
        AppInstance appInstance = new AppInstance();

        appInstance.setAppId(appId);
        appInstance.setName(name);
        appInstance.setWorkspaceId(1L);
        appInstance.setOwnerUserId(7L);
        appInstance.setEnvironment(environment);
        appInstance.setHostingTarget(AppHostingTarget.VERCEL);

        return appInstance;
    }

    private long countRowsOfInstance(String tableName, long appInstanceId) {
        String sql = switch (tableName) {
            case "app_connection" -> "SELECT COUNT(*) FROM app_connection WHERE app_instance_id = ?";
            case "app_workflow" -> "SELECT COUNT(*) FROM app_workflow WHERE app_instance_id = ?";
            case "app_workflow_run" -> "SELECT COUNT(*) FROM app_workflow_run WHERE app_instance_id = ?";
            default -> throw new IllegalArgumentException("Unexpected table " + tableName);
        };

        Long count = jdbcTemplate.queryForObject(sql, Long.class, appInstanceId);

        return Objects.requireNonNull(count);
    }
}
