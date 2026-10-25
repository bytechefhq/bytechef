/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.audit.EmbeddedUserAuditEvents;
import com.bytechef.ee.automation.configuration.audit.EmbeddedUserAuditMapper;
import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.domain.EmbeddedUser;
import com.bytechef.ee.automation.configuration.exception.WorkspaceUserErrorType;
import com.bytechef.ee.automation.configuration.repository.CustomRoleRepository;
import com.bytechef.ee.automation.configuration.repository.EmbeddedUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScopeSide;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.security.access.AccessDeniedException;

/**
 * Covers the last-admin guard in {@link EmbeddedUserServiceImpl}: no environment that has an embedded admin may be left
 * without one.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedUserServiceTest {

    private static final int ADMIN = WorkspaceRole.ADMIN.ordinal();
    private static final long CUSTOM_ROLE_ID = 7L;
    private static final Set<String> EMBEDDED_SCOPE_NAMES = Set.of(
        "EMBEDDED_VIEW", "INTEGRATION_CREATE", "INTEGRATION_PUBLISH", "WORKFLOW_VIEW");
    private static final long USER_ID = 42L;

    private CustomRoleRepository customRoleRepository;
    private EmbeddedUserRepository embeddedUserRepository;
    private EmbeddedUserServiceImpl embeddedUserService;
    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        customRoleRepository = mock(CustomRoleRepository.class);
        embeddedUserRepository = mock(EmbeddedUserRepository.class);
        permissionService = mock(PermissionService.class);

        PermissionScopeRegistry permissionScopeRegistry = mock(PermissionScopeRegistry.class);

        when(permissionScopeRegistry.getAllScopeNames(PermissionScopeSide.EMBEDDED)).thenReturn(EMBEDDED_SCOPE_NAMES);
        when(permissionService.getMyEmbeddedScopes(any())).thenReturn(EMBEDDED_SCOPE_NAMES);
        when(permissionService.hasEmbeddedScopeInEveryEnvironment(anyString())).thenReturn(true);

        embeddedUserService = new EmbeddedUserServiceImpl(
            customRoleRepository, embeddedUserRepository, permissionScopeRegistry, permissionService,
            mock(UserService.class));
    }

    @Test
    void testDeleteEmbeddedMembershipsDeletesTheLastAdminAndEvictsTheCache() {
        givenRows(EmbeddedUser.forRole(USER_ID, WorkspaceRole.ADMIN));
        givenAdminCount(Environment.DEVELOPMENT, 1);

        embeddedUserService.deleteEmbeddedMemberships(USER_ID);

        verify(embeddedUserRepository).deleteByUserId(USER_ID);
        verify(permissionService).evictEmbeddedScopeCache(USER_ID);
    }

    @Test
    void testDeleteEmbeddedMembershipsIsAuditedAsAnEmbeddedRemoval() throws NoSuchMethodException {
        Audited audited = EmbeddedUserServiceImpl.class.getMethod("deleteEmbeddedMemberships", long.class)
            .getAnnotation(Audited.class);

        assertThat(audited.event()).isEqualTo(EmbeddedUserAuditEvents.EMBEDDED_USER_REMOVED);
        assertThat(audited.mapper()).isEqualTo(EmbeddedUserAuditMapper.class);
    }

    @Test
    void testRemoveEmbeddedUserRefusesTheLastAdmin() {
        givenRows(EmbeddedUser.forRole(USER_ID, WorkspaceRole.ADMIN));
        givenAdminCount(Environment.DEVELOPMENT, 1);

        assertLastAdminProtected(() -> embeddedUserService.removeEmbeddedUser(USER_ID));

        verify(embeddedUserRepository, never()).deleteByUserId(USER_ID);
    }

    @Test
    void testRemoveEmbeddedUserAllowsAnAdminWhenAnotherRemains() {
        givenRows(EmbeddedUser.forRole(USER_ID, WorkspaceRole.ADMIN));
        givenAdminCountEverywhere(2);

        assertThat(embeddedUserService.removeEmbeddedUser(USER_ID)).isTrue();

        verify(embeddedUserRepository).deleteByUserId(USER_ID);
    }

    @Test
    void testRemoveEmbeddedUserRefusesTheLastAdminOfOneEnvironment() {
        givenRows(
            EmbeddedUser.forRole(USER_ID, WorkspaceRole.VIEWER, Environment.DEVELOPMENT),
            EmbeddedUser.forRole(USER_ID, WorkspaceRole.ADMIN, Environment.PRODUCTION));
        givenAdminCountEverywhere(2);
        givenAdminCount(Environment.PRODUCTION, 1);

        assertLastAdminProtected(() -> embeddedUserService.removeEmbeddedUser(USER_ID));
    }

    @Test
    void testRemoveEmbeddedUserIgnoresANonAdmin() {
        givenRows(EmbeddedUser.forRole(USER_ID, WorkspaceRole.EDITOR));

        assertThat(embeddedUserService.removeEmbeddedUser(USER_ID)).isTrue();

        verify(embeddedUserRepository, never()).countUsersWithRoleForEnvironment(anyInt(), anyInt());
    }

    @Test
    void testRemoveEmbeddedUserLocksTheAdminsBeforeTheMember() {
        givenRows(EmbeddedUser.forRole(USER_ID, WorkspaceRole.EDITOR));

        embeddedUserService.removeEmbeddedUser(USER_ID);

        InOrder inOrder = inOrder(embeddedUserRepository);

        inOrder.verify(embeddedUserRepository)
            .lockEmbeddedUsersWithRole(ADMIN);
        inOrder.verify(embeddedUserRepository)
            .lockEmbeddedUsers(USER_ID);
    }

    @Test
    void testUpdateEmbeddedUserRoleRefusesDemotingTheLastAdmin() {
        EmbeddedUser embeddedUser = EmbeddedUser.forRole(USER_ID, WorkspaceRole.ADMIN);

        when(embeddedUserRepository.findByUserIdAndEnvironmentIsNull(USER_ID)).thenReturn(Optional.of(embeddedUser));
        givenAdminCount(Environment.STAGING, 1);

        assertLastAdminProtected(() -> embeddedUserService.updateEmbeddedUserRole(USER_ID, WorkspaceRole.EDITOR));

        verify(embeddedUserRepository, never()).save(any(EmbeddedUser.class));
    }

    @Test
    void testUpdateEmbeddedUserRoleAllowsStepDownWhenAnotherAdminRemains() {
        EmbeddedUser embeddedUser = EmbeddedUser.forRole(USER_ID, WorkspaceRole.ADMIN);

        when(embeddedUserRepository.findByUserIdAndEnvironmentIsNull(USER_ID)).thenReturn(Optional.of(embeddedUser));
        when(embeddedUserRepository.save(any(EmbeddedUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        givenAdminCountEverywhere(2);

        EmbeddedUser savedEmbeddedUser = embeddedUserService.updateEmbeddedUserRole(USER_ID, WorkspaceRole.EDITOR);

        assertThat(savedEmbeddedUser.getEmbeddedRole()).isEqualTo(WorkspaceRole.EDITOR);
    }

    @Test
    void testUpdateEmbeddedUserRoleKeepingAdminSkipsTheCount() {
        EmbeddedUser embeddedUser = EmbeddedUser.forRole(USER_ID, WorkspaceRole.ADMIN);

        when(embeddedUserRepository.findByUserIdAndEnvironmentIsNull(USER_ID)).thenReturn(Optional.of(embeddedUser));

        assertThatCode(() -> embeddedUserService.updateEmbeddedUserRole(USER_ID, WorkspaceRole.ADMIN))
            .doesNotThrowAnyException();

        verify(embeddedUserRepository, never()).countUsersWithRoleForEnvironment(anyInt(), anyInt());
    }

    @Test
    void testSetEnvironmentRoleRefusesDemotingTheLastAdminOfThatEnvironment() {
        givenRows(EmbeddedUser.forRole(USER_ID, WorkspaceRole.ADMIN));
        givenAdminCountEverywhere(2);
        givenAdminCount(Environment.PRODUCTION, 1);

        assertLastAdminProtected(
            () -> embeddedUserService.setEnvironmentRole(USER_ID, Environment.PRODUCTION, WorkspaceRole.VIEWER));

        verify(embeddedUserRepository, never()).delete(any(EmbeddedUser.class));
    }

    @Test
    void testSetEnvironmentRoleChecksOnlyTheChangedEnvironment() {
        givenRows(EmbeddedUser.forRole(USER_ID, WorkspaceRole.ADMIN));
        givenAdminCountEverywhere(1);
        givenAdminCount(Environment.PRODUCTION, 2);

        assertThatCode(
            () -> embeddedUserService.setEnvironmentRole(USER_ID, Environment.PRODUCTION, WorkspaceRole.VIEWER))
                .doesNotThrowAnyException();
    }

    @Test
    void testSetEnvironmentRoleGrantingAdminSkipsTheCount() {
        givenRows(EmbeddedUser.forRole(USER_ID, WorkspaceRole.ADMIN, Environment.PRODUCTION));

        assertThatCode(
            () -> embeddedUserService.setEnvironmentRole(USER_ID, Environment.PRODUCTION, WorkspaceRole.ADMIN))
                .doesNotThrowAnyException();

        verify(embeddedUserRepository, never()).countUsersWithRoleForEnvironment(anyInt(), anyInt());
    }

    @Test
    void testRemoveEnvironmentRoleRefusesTheLastAdminOfThatEnvironment() {
        EmbeddedUser productionAdmin = EmbeddedUser.forRole(USER_ID, WorkspaceRole.ADMIN, Environment.PRODUCTION);

        givenRows(EmbeddedUser.forRole(USER_ID, WorkspaceRole.VIEWER, Environment.DEVELOPMENT), productionAdmin);
        when(embeddedUserRepository.findByUserIdAndEnvironment(USER_ID, Environment.PRODUCTION.ordinal()))
            .thenReturn(Optional.of(productionAdmin));
        givenAdminCount(Environment.PRODUCTION, 1);

        assertLastAdminProtected(() -> embeddedUserService.removeEnvironmentRole(USER_ID, Environment.PRODUCTION));

        verify(embeddedUserRepository, never()).delete(any(EmbeddedUser.class));
    }

    @Test
    void testRemoveEnvironmentRoleAllowsAnAdminWhenAnotherRemains() {
        EmbeddedUser productionAdmin = EmbeddedUser.forRole(USER_ID, WorkspaceRole.ADMIN, Environment.PRODUCTION);

        givenRows(EmbeddedUser.forRole(USER_ID, WorkspaceRole.ADMIN, Environment.DEVELOPMENT), productionAdmin);
        when(embeddedUserRepository.findByUserIdAndEnvironment(USER_ID, Environment.PRODUCTION.ordinal()))
            .thenReturn(Optional.of(productionAdmin));
        givenAdminCountEverywhere(1);
        givenAdminCount(Environment.PRODUCTION, 2);

        embeddedUserService.removeEnvironmentRole(USER_ID, Environment.PRODUCTION);

        verify(embeddedUserRepository).delete(productionAdmin);
    }

    @Test
    void testRemoveEnvironmentRoleWideningTheLastRowKeepsTheAdmin() {
        EmbeddedUser productionAdmin = EmbeddedUser.forRole(USER_ID, WorkspaceRole.ADMIN, Environment.PRODUCTION);

        givenRows(productionAdmin);
        when(embeddedUserRepository.findByUserIdAndEnvironment(USER_ID, Environment.PRODUCTION.ordinal()))
            .thenReturn(Optional.of(productionAdmin));
        givenAdminCountEverywhere(1);

        assertThatCode(() -> embeddedUserService.removeEnvironmentRole(USER_ID, Environment.PRODUCTION))
            .doesNotThrowAnyException();

        verify(embeddedUserRepository, never()).countUsersWithRoleForEnvironment(anyInt(), anyInt());
    }

    @Test
    void testAddEmbeddedUserWithCustomRoleSavesACustomRoleMembership() {
        givenCustomRole("INTEGRATION_CREATE", "WORKFLOW_VIEW");
        when(embeddedUserRepository.save(any(EmbeddedUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmbeddedUser embeddedUser = embeddedUserService.addEmbeddedUser(USER_ID, null, CUSTOM_ROLE_ID);

        assertThat(embeddedUser.getCustomRoleId()).isEqualTo(CUSTOM_ROLE_ID);
        assertThat(embeddedUser.getEmbeddedRole()).isNull();
        assertThat(embeddedUser.getEnvironment()).isNull();
        verify(permissionService).evictEmbeddedScopeCache(USER_ID);
    }

    @Test
    void testAddEmbeddedUserRefusesBothABuiltInAndACustomRole() {
        givenCustomRole("INTEGRATION_CREATE");

        assertErrorType(
            () -> embeddedUserService.addEmbeddedUser(USER_ID, WorkspaceRole.EDITOR, CUSTOM_ROLE_ID),
            WorkspaceUserErrorType.ROLE_SELECTION_INVALID);

        verify(embeddedUserRepository, never()).save(any(EmbeddedUser.class));
    }

    @Test
    void testAddEmbeddedUserRefusesNeitherABuiltInNorACustomRole() {
        assertErrorType(
            () -> embeddedUserService.addEmbeddedUser(USER_ID, null, null),
            WorkspaceUserErrorType.ROLE_SELECTION_INVALID);

        verify(embeddedUserRepository, never()).save(any(EmbeddedUser.class));
    }

    @Test
    void testAddEmbeddedUserRefusesAnUnknownCustomRole() {
        when(customRoleRepository.findById(CUSTOM_ROLE_ID)).thenReturn(Optional.empty());

        assertErrorType(
            () -> embeddedUserService.addEmbeddedUser(USER_ID, null, CUSTOM_ROLE_ID),
            WorkspaceUserErrorType.CUSTOM_ROLE_NOT_FOUND);

        verify(embeddedUserRepository, never()).save(any(EmbeddedUser.class));
    }

    @Test
    void testAddEmbeddedUserRefusesAnAutomationCustomRole() {
        when(customRoleRepository.findById(CUSTOM_ROLE_ID))
            .thenReturn(Optional.of(new CustomRole("custom-role", Set.of("WORKFLOW_VIEW"))));

        assertErrorType(
            () -> embeddedUserService.addEmbeddedUser(USER_ID, null, CUSTOM_ROLE_ID),
            WorkspaceUserErrorType.CUSTOM_ROLE_SIDE_MISMATCH);

        verify(embeddedUserRepository, never()).save(any(EmbeddedUser.class));
    }

    @Test
    void testAddEmbeddedUserRefusesACustomRoleCarryingScopesTheCallerLacks() {
        givenCustomRole("INTEGRATION_CREATE", "INTEGRATION_PUBLISH");
        when(permissionService.getMyEmbeddedScopes(any())).thenReturn(Set.of("INTEGRATION_CREATE"));

        assertThatThrownBy(() -> embeddedUserService.addEmbeddedUser(USER_ID, null, CUSTOM_ROLE_ID))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("INTEGRATION_PUBLISH");

        verify(embeddedUserRepository, never()).save(any(EmbeddedUser.class));
    }

    @Test
    void testAddEmbeddedUserIgnoresTheCustomRoleScopesOutsideTheEmbeddedCatalogue() {
        givenCustomRole("INTEGRATION_CREATE", "PROJECT_CREATE");
        when(permissionService.getMyEmbeddedScopes(any())).thenReturn(Set.of("INTEGRATION_CREATE"));

        assertThatCode(() -> embeddedUserService.addEmbeddedUser(USER_ID, null, CUSTOM_ROLE_ID))
            .doesNotThrowAnyException();
    }

    @Test
    void testUpdateEmbeddedUserRoleAssignsACustomRole() {
        EmbeddedUser embeddedUser = EmbeddedUser.forRole(USER_ID, WorkspaceRole.EDITOR);

        givenCustomRole("INTEGRATION_CREATE");
        when(embeddedUserRepository.findByUserIdAndEnvironmentIsNull(USER_ID)).thenReturn(Optional.of(embeddedUser));
        when(embeddedUserRepository.save(any(EmbeddedUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmbeddedUser savedEmbeddedUser = embeddedUserService.updateEmbeddedUserRole(USER_ID, null, CUSTOM_ROLE_ID);

        assertThat(savedEmbeddedUser.getCustomRoleId()).isEqualTo(CUSTOM_ROLE_ID);
        assertThat(savedEmbeddedUser.getEmbeddedRole()).isNull();
    }

    @Test
    void testUpdateEmbeddedUserRoleToACustomRoleRefusesDemotingTheLastAdmin() {
        EmbeddedUser embeddedUser = EmbeddedUser.forRole(USER_ID, WorkspaceRole.ADMIN);

        givenCustomRole("INTEGRATION_CREATE");
        when(embeddedUserRepository.findByUserIdAndEnvironmentIsNull(USER_ID)).thenReturn(Optional.of(embeddedUser));
        givenAdminCount(Environment.STAGING, 1);

        assertLastAdminProtected(() -> embeddedUserService.updateEmbeddedUserRole(USER_ID, null, CUSTOM_ROLE_ID));

        verify(embeddedUserRepository, never()).save(any(EmbeddedUser.class));
    }

    @Test
    void testSetEnvironmentRoleToACustomRoleRefusesDemotingTheLastAdminOfThatEnvironment() {
        givenCustomRole("INTEGRATION_CREATE");
        givenRows(EmbeddedUser.forRole(USER_ID, WorkspaceRole.ADMIN));
        givenAdminCountEverywhere(2);
        givenAdminCount(Environment.PRODUCTION, 1);

        assertLastAdminProtected(
            () -> embeddedUserService.setEnvironmentRole(USER_ID, Environment.PRODUCTION, null, CUSTOM_ROLE_ID));

        verify(embeddedUserRepository, never()).delete(any(EmbeddedUser.class));
    }

    @Test
    void testSetEnvironmentRoleRefusesACustomRoleCarryingScopesTheCallerLacksInThatEnvironment() {
        givenCustomRole("INTEGRATION_PUBLISH");
        givenRows(EmbeddedUser.forRole(USER_ID, WorkspaceRole.VIEWER));
        when(permissionService.getMyEmbeddedScopes(Environment.PRODUCTION)).thenReturn(Set.of("INTEGRATION_CREATE"));

        assertThatThrownBy(
            () -> embeddedUserService.setEnvironmentRole(USER_ID, Environment.PRODUCTION, null, CUSTOM_ROLE_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("PRODUCTION");
    }

    @Test
    void testSetEnvironmentRoleSplitsAnEmbeddedWideCustomRoleAcrossTheOtherEnvironments() {
        EmbeddedUser implicitEmbeddedUser = EmbeddedUser.forCustomRole(USER_ID, CUSTOM_ROLE_ID);

        givenRows(implicitEmbeddedUser);
        when(embeddedUserRepository.findByUserIdAndEnvironmentIsNull(USER_ID))
            .thenReturn(Optional.of(implicitEmbeddedUser));
        when(embeddedUserRepository.findByUserIdAndEnvironment(USER_ID, Environment.PRODUCTION.ordinal()))
            .thenReturn(Optional.empty());

        embeddedUserService.setEnvironmentRole(USER_ID, Environment.PRODUCTION, WorkspaceRole.VIEWER, null);

        ArgumentCaptor<EmbeddedUser> embeddedUserArgumentCaptor = ArgumentCaptor.forClass(EmbeddedUser.class);

        verify(embeddedUserRepository).delete(implicitEmbeddedUser);
        verify(embeddedUserRepository, times(Environment.values().length))
            .save(embeddedUserArgumentCaptor.capture());

        assertThat(embeddedUserArgumentCaptor.getAllValues())
            .filteredOn(embeddedUser -> embeddedUser.getEnvironment() != Environment.PRODUCTION)
            .hasSize(Environment.values().length - 1)
            .allSatisfy(embeddedUser -> assertThat(embeddedUser.getCustomRoleId()).isEqualTo(CUSTOM_ROLE_ID));
        assertThat(embeddedUserArgumentCaptor.getAllValues())
            .filteredOn(embeddedUser -> embeddedUser.getEnvironment() == Environment.PRODUCTION)
            .singleElement()
            .satisfies(embeddedUser -> assertThat(embeddedUser.getEmbeddedRole()).isEqualTo(WorkspaceRole.VIEWER));
    }

    @Test
    void testRemoveEnvironmentRoleWideningTheLastCustomRoleRowKeepsTheCustomRole() {
        EmbeddedUser productionEmbeddedUser = EmbeddedUser.forCustomRole(
            USER_ID, CUSTOM_ROLE_ID, Environment.PRODUCTION);

        givenCustomRole("INTEGRATION_CREATE");
        givenRows(productionEmbeddedUser);
        when(embeddedUserRepository.findByUserIdAndEnvironment(USER_ID, Environment.PRODUCTION.ordinal()))
            .thenReturn(Optional.of(productionEmbeddedUser));

        embeddedUserService.removeEnvironmentRole(USER_ID, Environment.PRODUCTION);

        ArgumentCaptor<EmbeddedUser> embeddedUserArgumentCaptor = ArgumentCaptor.forClass(EmbeddedUser.class);

        verify(embeddedUserRepository).save(embeddedUserArgumentCaptor.capture());

        EmbeddedUser widenedEmbeddedUser = embeddedUserArgumentCaptor.getValue();

        assertThat(widenedEmbeddedUser.getEnvironment()).isNull();
        assertThat(widenedEmbeddedUser.getCustomRoleId()).isEqualTo(CUSTOM_ROLE_ID);
    }

    private void assertErrorType(ThrowingCallable callable, WorkspaceUserErrorType workspaceUserErrorType) {
        assertThatThrownBy(callable)
            .isInstanceOf(ConfigurationException.class)
            .extracting(throwable -> ((ConfigurationException) throwable).getErrorKey())
            .isEqualTo(workspaceUserErrorType.getErrorKey());
    }

    private void assertLastAdminProtected(ThrowingCallable callable) {
        assertThatThrownBy(callable)
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("last embedded admin")
            .extracting(throwable -> ((ConfigurationException) throwable).getErrorKey())
            .isEqualTo(WorkspaceUserErrorType.LAST_ADMIN_PROTECTED.getErrorKey());
    }

    private void givenCustomRole(String... scopeNames) {
        when(customRoleRepository.findById(CUSTOM_ROLE_ID))
            .thenReturn(Optional.of(new CustomRole("custom-role", PermissionScopeSide.EMBEDDED, Set.of(scopeNames))));
    }

    private void givenAdminCount(Environment environment, long count) {
        when(embeddedUserRepository.countUsersWithRoleForEnvironment(ADMIN, environment.ordinal())).thenReturn(count);
    }

    private void givenAdminCountEverywhere(long count) {
        when(embeddedUserRepository.countUsersWithRoleForEnvironment(eq(ADMIN), anyInt())).thenReturn(count);
    }

    private void givenRows(EmbeddedUser... embeddedUsers) {
        when(embeddedUserRepository.findAllByUserId(USER_ID)).thenReturn(List.of(embeddedUsers));
        when(embeddedUserRepository.existsByUserId(USER_ID)).thenReturn(embeddedUsers.length > 0);
    }
}
