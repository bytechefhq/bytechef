/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.platform.security.constant.AuthorityConstants;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = PermissionServiceIntTest.Config.class, properties = "bytechef.edition=ee")
@MockitoBean(types = {
    CurrentUserResolver.class, PermissionScopeRegistry.class, ProjectRepository.class,
    WorkspaceScopeCacheService.class, WorkspaceUserRepository.class
})
class PermissionServiceIntTest {

    private static final long WORKSPACE_ID = 21L;

    @Autowired
    private PermissionService permissionService;

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testEePermissionServiceMyScopeReadsRequireAuthentication() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new AnonymousAuthenticationToken(
                    "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThatThrownBy(() -> permissionService.getMyWorkspaceScopes(WORKSPACE_ID))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> permissionService.getMyWorkspaceRole(WORKSPACE_ID))
            .isInstanceOf(AccessDeniedException.class);

        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> permissionService.getMyWorkspaceScopes(WORKSPACE_ID))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        assertThatThrownBy(() -> permissionService.getMyWorkspaceRole(WORKSPACE_ID))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class);

        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "user", "n/a", List.of(new SimpleGrantedAuthority(AuthorityConstants.USER))));

        assertThatCode(() -> permissionService.getMyWorkspaceScopes(WORKSPACE_ID)).doesNotThrowAnyException();
        assertThatCode(() -> permissionService.getMyWorkspaceRole(WORKSPACE_ID)).doesNotThrowAnyException();
    }

    @SpringBootConfiguration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import(PermissionServiceImpl.class)
    static class Config {
    }
}
