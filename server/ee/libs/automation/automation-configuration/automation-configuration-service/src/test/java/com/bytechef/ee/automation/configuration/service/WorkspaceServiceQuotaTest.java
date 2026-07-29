/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.exception.QuotaLimitExceededException;
import com.bytechef.platform.plan.domain.PlanLimits;
import com.bytechef.platform.plan.domain.PlanTier;
import com.bytechef.platform.plan.provider.PlanLimitsProvider;
import com.bytechef.platform.ratelimit.PlanLimitRejectionCounter;
import com.bytechef.platform.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Pins the {@code maxWorkspaces} quota gate: an at-limit tenant is rejected with {@link QuotaLimitExceededException}
 * before any row is written, a below-limit tenant proceeds, and a null limit (or absent provider bean) means unlimited.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
public class WorkspaceServiceQuotaTest {

    @Mock
    private PermissionService permissionService;

    @Mock
    private UserService userService;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceUserRepository workspaceUserRepository;

    private ObjectProvider<PlanLimitsProvider> planLimitsProviderObjectProvider;
    private WorkspaceService workspaceService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void beforeEach() {
        ObjectProvider<PlanLimitRejectionCounter> planLimitRejectionCounterObjectProvider =
            (ObjectProvider<PlanLimitRejectionCounter>) mock(ObjectProvider.class);

        planLimitsProviderObjectProvider = (ObjectProvider<PlanLimitsProvider>) mock(ObjectProvider.class);

        workspaceService = new WorkspaceServiceImpl(
            permissionService, planLimitRejectionCounterObjectProvider, planLimitsProviderObjectProvider, userService,
            workspaceRepository, workspaceUserRepository);

        lenient()
            .when(workspaceRepository.save(any(Workspace.class)))
            .thenAnswer(invocation -> {
                Workspace workspace = invocation.getArgument(0);

                workspace.setId(100L);

                return workspace;
            });
    }

    @Test
    public void testCreateRejectedAtWorkspaceLimit() {
        stubMaxWorkspaces(3);

        when(workspaceRepository.count()).thenReturn(3L);

        assertThatThrownBy(() -> workspaceService.create(newWorkspace()))
            .isInstanceOf(QuotaLimitExceededException.class);

        verify(workspaceRepository, never()).save(any(Workspace.class));
    }

    @Test
    public void testCreateAllowedBelowWorkspaceLimit() {
        stubMaxWorkspaces(3);

        when(workspaceRepository.count()).thenReturn(2L);

        assertThatCode(() -> workspaceService.create(newWorkspace())).doesNotThrowAnyException();

        verify(workspaceRepository).save(any(Workspace.class));
    }

    @Test
    public void testNullLimitMeansUnlimited() {
        stubMaxWorkspaces(null);

        assertThatCode(() -> workspaceService.create(newWorkspace())).doesNotThrowAnyException();

        verify(workspaceRepository).save(any(Workspace.class));
        verify(workspaceRepository, never()).count();
    }

    private void stubMaxWorkspaces(Integer maxWorkspaces) {
        PlanLimits planLimits = new PlanLimits(
            PlanTier.FREE, null, null, null, null, PlanLimits.DEFAULT_BURST_MULTIPLIER, null, null, null,
            maxWorkspaces, null, null, null);

        when(planLimitsProviderObjectProvider.getIfAvailable()).thenReturn(tenantId -> planLimits);
    }

    private static Workspace newWorkspace() {
        Workspace workspace = new Workspace();

        workspace.setName("Quota Workspace");

        return workspace;
    }
}
