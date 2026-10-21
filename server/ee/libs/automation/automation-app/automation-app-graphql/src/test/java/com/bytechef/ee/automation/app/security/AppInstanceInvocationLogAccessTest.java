/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.security;

import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.APP_INSTANCE;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.DEVELOPMENT_ADMIN;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.PRIVATE_APP_INSTANCE;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.TENANT_ADMIN;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.VIEWER;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.WORKSPACE_A;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.WORKSPACE_ADMIN;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.authenticate;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.userId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.facade.AppAuthorizationFixture;
import com.bytechef.ee.automation.app.facade.AppAuthorizationTestConfiguration;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.platform.external.invocation.log.security.ViewableAppInstances;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AppAuthorizationTestConfiguration.class, properties = "bytechef.edition=ee")
class AppInstanceInvocationLogAccessTest {

    private static final long DELETED_APP_INSTANCE = 999L;

    @Autowired
    private AppInstanceInvocationLogAccess appInstanceInvocationLogAccess;

    @Autowired
    private AppInstanceService appInstanceService;

    @Autowired
    private AppService appService;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private ResourceGrantService resourceGrantService;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkspaceConnectionRepository workspaceConnectionRepository;

    @Autowired
    private WorkspaceUserRepository workspaceUserRepository;

    @BeforeEach
    void beforeEach() {
        AppAuthorizationFixture.stubTenant(
            appInstanceService, appService, connectionService, userService, workspaceConnectionRepository,
            workspaceUserRepository);
        AppAuthorizationFixture.stubGrants(resourceGrantService);

        List<AppInstance> appInstances = List.of(
            appInstanceService.getAppInstance(APP_INSTANCE), appInstanceService.getAppInstance(PRIVATE_APP_INSTANCE));

        when(appInstanceService.getWorkspaceAppInstances(WORKSPACE_A, Environment.PRODUCTION))
            .thenReturn(appInstances);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testMemberWhoCannotViewAPrivateInstanceDoesNotGetIt() {
        authenticate(VIEWER);

        assertThat(getViewableAppInstances()).isEqualTo(ViewableAppInstances.only(Set.of(APP_INSTANCE)));
    }

    @Test
    void testOwnerOfAPrivateInstanceGetsIt() {
        authenticate(WORKSPACE_ADMIN);

        assertThat(getViewableAppInstances())
            .isEqualTo(ViewableAppInstances.only(Set.of(APP_INSTANCE, PRIVATE_APP_INSTANCE)));
    }

    @Test
    void testGranteeOfAPrivateInstanceGetsIt() {
        resourceGrantService.grant("AppInstance", PRIVATE_APP_INSTANCE, userId(VIEWER));

        authenticate(VIEWER);

        assertThat(getViewableAppInstances())
            .isEqualTo(ViewableAppInstances.only(Set.of(APP_INSTANCE, PRIVATE_APP_INSTANCE)));
    }

    @Test
    void testTenantAdminIsNotRestrictedToExistingInstances() {
        authenticate(TENANT_ADMIN);

        assertThat(getViewableAppInstances()).isEqualTo(ViewableAppInstances.all());
    }

    @Test
    void testTenantAdminIsNotRestrictedInAnEnvironmentWithoutInstances() {
        authenticate(TENANT_ADMIN);

        assertThat(appInstanceInvocationLogAccess.getViewableAppInstances(WORKSPACE_A, 9))
            .isEqualTo(ViewableAppInstances.all());
    }

    @Test
    void testWorkspaceAdminWhoIsNotATenantAdminIsRestrictedToExistingInstances() {
        authenticate(WORKSPACE_ADMIN);

        ViewableAppInstances viewableAppInstances = getViewableAppInstances();

        assertThat(viewableAppInstances.restricted()).isTrue();
        assertThat(viewableAppInstances.appInstanceIds())
            .containsExactlyInAnyOrder(APP_INSTANCE, PRIVATE_APP_INSTANCE)
            .doesNotContain(DELETED_APP_INSTANCE);
    }

    @Test
    void testMemberWithoutAppViewInTheEnvironmentGetsNone() {
        authenticate(DEVELOPMENT_ADMIN);

        assertThat(getViewableAppInstances()).isEqualTo(ViewableAppInstances.only(Set.of()));
    }

    @Test
    void testUnknownEnvironmentGetsNone() {
        authenticate(WORKSPACE_ADMIN);

        assertThat(appInstanceInvocationLogAccess.getViewableAppInstances(WORKSPACE_A, 9))
            .isEqualTo(ViewableAppInstances.only(Set.of()));
    }

    private ViewableAppInstances getViewableAppInstances() {
        return appInstanceInvocationLogAccess.getViewableAppInstances(
            WORKSPACE_A, Environment.PRODUCTION.ordinal());
    }
}
