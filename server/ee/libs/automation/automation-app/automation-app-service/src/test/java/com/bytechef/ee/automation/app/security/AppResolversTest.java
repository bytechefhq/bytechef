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
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class AppResolversTest {

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
    void environmentComesFromTheRow() {
        App app = new App();

        app.setEnvironment(Environment.STAGING);

        when(appService.fetchApp(1L)).thenReturn(Optional.of(app));

        assertThat(new AppEnvironmentResolver(appService).fetchEnvironment(1L)).hasValue(Environment.STAGING);
        assertThat(new AppEnvironmentResolver(appService).fetchEnvironment("x")).isEmpty();
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
