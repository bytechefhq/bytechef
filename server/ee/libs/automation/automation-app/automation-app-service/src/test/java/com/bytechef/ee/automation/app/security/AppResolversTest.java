/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class AppResolversTest {

    private final AppInstanceService appInstanceService = mock(AppInstanceService.class);
    private final AppService appService = mock(AppService.class);

    @Test
    void ownershipResolvesWorkspaceAndOwnerAndFailsClosed() {
        App app = new App();

        app.setWorkspaceId(7L);
        app.setOwnerUserId(3L);

        when(appService.fetchApp(1L)).thenReturn(Optional.of(app));
        when(appService.fetchApp(2L)).thenReturn(Optional.empty());

        AppOwnershipResolver resolver = new AppOwnershipResolver(appService);

        ResourceOwner owner = resolver.resolveOwner(1L);

        assertThat(owner.workspaceId()).hasValue(7L);
        assertThat(owner.ownerUserId()).hasValue(3L);
        assertThat(resolver.resolveOwner(2L)).isEqualTo(ResourceOwner.unknown());
        assertThat(resolver.resourceType()).isEqualTo("App");
    }

    @Test
    void testInstanceOwnerIsNotAppOwnerForForeignInstance() {
        App app = new App();

        app.setId(1L);
        app.setWorkspaceId(7L);
        app.setOwnerUserId(3L);

        AppInstance appInstance = new AppInstance();

        appInstance.setId(20L);
        appInstance.setAppId(1L);
        appInstance.setWorkspaceId(8L);
        appInstance.setOwnerUserId(4L);

        when(appService.fetchApp(1L)).thenReturn(Optional.of(app));
        when(appInstanceService.fetchAppInstance(20L)).thenReturn(Optional.of(appInstance));
        when(appInstanceService.fetchAppInstance(21L)).thenReturn(Optional.empty());

        AppInstanceOwnershipResolver resolver = new AppInstanceOwnershipResolver(appInstanceService);

        ResourceOwner owner = resolver.resolveOwner(20L);

        assertThat(owner.workspaceId()).hasValue(8L);
        assertThat(owner.ownerUserId()).hasValue(4L);
        assertThat(resolver.resolveOwner(21L)).isEqualTo(ResourceOwner.unknown());
        assertThat(resolver.resourceType()).isEqualTo("AppInstance");
    }

    @Test
    void instanceEnvironmentComesFromTheInstanceRow() {
        AppInstance appInstance = new AppInstance();

        appInstance.setEnvironment(Environment.STAGING);

        when(appInstanceService.fetchAppInstance(20L)).thenReturn(Optional.of(appInstance));
        when(appInstanceService.fetchAppInstance(21L)).thenReturn(Optional.empty());

        AppInstanceEnvironmentResolver resolver = new AppInstanceEnvironmentResolver(appInstanceService);

        assertThat(resolver.fetchEnvironment(20L)).hasValue(Environment.STAGING);
        assertThat(resolver.fetchEnvironment(21L)).isEmpty();
        assertThat(resolver.fetchEnvironment("x")).isEmpty();
        assertThat(resolver.resourceType()).isEqualTo("AppInstance");
    }

    @Test
    void instanceVisibilityProviderReturnsTheInstanceRecordOrEmpty() {
        AppInstance appInstance = new AppInstance();

        appInstance.setId(20L);
        appInstance.setVisibility(ResourceVisibility.PRIVATE);
        appInstance.setCreatedBy("croatia-admin");

        when(appInstanceService.fetchAppInstance(20L)).thenReturn(Optional.of(appInstance));
        when(appInstanceService.fetchAppInstance(21L)).thenReturn(Optional.empty());

        AppInstanceVisibilityProvider provider = new AppInstanceVisibilityProvider(appInstanceService);

        assertThat(provider.fetchVisibility(20L))
            .hasValueSatisfying(record -> {
                assertThat(record.id()).isEqualTo(20L);
                assertThat(record.visibility()).isEqualTo(ResourceVisibility.PRIVATE);
                assertThat(record.createdBy()).isEqualTo("croatia-admin");
            });
        assertThat(provider.fetchVisibility(21L)).isEmpty();
        assertThat(provider.resourceType()).isEqualTo("AppInstance");
    }

    @Test
    void instancePolicyDefaultsToWorkspace() {
        AppInstanceVisibilityPolicy policy = new AppInstanceVisibilityPolicy();

        assertThat(policy.resourceType()).isEqualTo("AppInstance");
        assertThat(policy.defaultVisibility()).isEqualTo(ResourceVisibility.WORKSPACE);
        assertThat(policy.supportedVisibilities())
            .containsExactlyInAnyOrder(ResourceVisibility.PRIVATE, ResourceVisibility.WORKSPACE);
    }

    @Test
    void visibilityProviderReturnsRecordOrEmpty() {
        App app = new App();

        app.setId(1L);
        app.setVisibility(ResourceVisibility.WORKSPACE);
        app.setCreatedBy("admin");

        when(appService.fetchApp(1L)).thenReturn(Optional.of(app));

        assertThat(new AppVisibilityProvider(appService).fetchVisibility(1L))
            .hasValueSatisfying(record -> {
                assertThat(record.visibility()).isEqualTo(ResourceVisibility.WORKSPACE);
                assertThat(record.createdBy()).isEqualTo("admin");
            });
        assertThat(new AppVisibilityProvider(appService).fetchVisibility(9L)).isEmpty();
    }

    @Test
    void policyDefaultsToWorkspace() {
        AppVisibilityPolicy policy = new AppVisibilityPolicy();

        assertThat(policy.defaultVisibility()).isEqualTo(ResourceVisibility.WORKSPACE);
        assertThat(policy.supportedVisibilities())
            .containsExactlyInAnyOrder(ResourceVisibility.PRIVATE, ResourceVisibility.WORKSPACE);
    }
}
