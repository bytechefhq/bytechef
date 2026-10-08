/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.AppEvent;
import com.bytechef.ee.embedded.configuration.repository.AppEventRepository;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        IntegrationIntTestConfiguration.class, AppEventFacadeIntTest.MethodSecurityConfiguration.class
    },
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class AppEventFacadeIntTest {

    @Autowired
    private AppEventFacade appEventFacade;

    @Autowired
    private AppEventRepository appEventRepository;

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        appEventRepository.deleteAll();
    }

    @Test
    void testTenantAdminReachesEveryOperation() {
        authenticate(tenantAdmin());

        AppEvent appEvent = appEventFacade.create(newAppEvent("orderCreated", "{\"type\":\"object\"}"));

        long appEventId = appEvent.getId();

        assertThat(appEventRepository.findById(appEventId))
            .get()
            .extracting(AppEvent::getName)
            .isEqualTo("orderCreated");

        appEvent.setName("orderUpdated");
        appEvent.setSchema("{\"type\":\"string\"}");

        AppEvent updatedAppEvent = appEventFacade.update(appEvent);

        assertThat(updatedAppEvent.getName()).isEqualTo("orderUpdated");

        AppEvent storedAppEvent = appEventRepository.findById(appEventId)
            .orElseThrow();

        assertThat(storedAppEvent.getName()).isEqualTo("orderUpdated");
        assertThat(storedAppEvent.getSchema()).isEqualTo("{\"type\":\"string\"}");

        assertThat(appEventFacade.getAppEvent(appEventId))
            .extracting(AppEvent::getName)
            .isEqualTo("orderUpdated");
        assertThat(appEventFacade.getAppEvents())
            .extracting(AppEvent::getId)
            .containsExactly(appEventId);

        appEventFacade.delete(appEventId);

        assertThat(appEventRepository.findById(appEventId)).isEmpty();
    }

    @Test
    void testNonAdminIsDeniedBeforeTheServiceIsCalled() {
        AppEvent existingAppEvent = appEventRepository.save(newAppEvent("orderCreated", "{\"type\":\"object\"}"));

        long existingAppEventId = existingAppEvent.getId();

        for (Authentication authentication : List.of(nonAdmin(), connectedUser())) {
            authenticate(authentication);

            AppEvent newAppEvent = newAppEvent("orderShipped", "{\"type\":\"object\"}");

            AppEvent changedAppEvent = appEventRepository.findById(existingAppEventId)
                .orElseThrow();

            changedAppEvent.setName("orderUpdated");

            assertThatThrownBy(() -> appEventFacade.create(newAppEvent))
                .as("create denied to %s", authentication.getName())
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> appEventFacade.update(changedAppEvent))
                .as("update denied to %s", authentication.getName())
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> appEventFacade.delete(existingAppEventId))
                .as("delete denied to %s", authentication.getName())
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> appEventFacade.getAppEvent(existingAppEventId))
                .as("getAppEvent denied to %s", authentication.getName())
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> appEventFacade.getAppEvents())
                .as("getAppEvents denied to %s", authentication.getName())
                .isInstanceOf(AccessDeniedException.class);
        }

        List<AppEvent> appEvents = appEventRepository.findAll();

        assertThat(appEvents)
            .extracting(AppEvent::getId)
            .containsExactly(existingAppEventId);
        assertThat(appEvents)
            .extracting(AppEvent::getName)
            .containsExactly("orderCreated");
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
    }

    private static Authentication connectedUser() {
        return new EmbeddedApiKeyAuthenticationToken(
            Environment.PRODUCTION.ordinal(), 1L, new User("external-user-1", "", List.of()), false);
    }

    private static AppEvent newAppEvent(String name, String schema) {
        AppEvent appEvent = new AppEvent();

        appEvent.setName(name);
        appEvent.setSchema(schema);

        return appEvent;
    }

    private static Authentication nonAdmin() {
        return new UsernamePasswordAuthenticationToken(
            "user", "n/a", List.of(new SimpleGrantedAuthority(AuthorityConstants.USER)));
    }

    private static Authentication tenantAdmin() {
        return new UsernamePasswordAuthenticationToken(
            "admin", "n/a",
            List.of(
                new SimpleGrantedAuthority(AuthorityConstants.ADMIN),
                new SimpleGrantedAuthority(AuthorityConstants.USER)));
    }

    @Configuration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    static class MethodSecurityConfiguration {

        @Bean("permissionService")
        PermissionService permissionService() {
            PermissionService permissionService = mock(PermissionService.class);

            when(permissionService.isTenantAdmin())
                .thenAnswer(invocation -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN));

            return permissionService;
        }
    }
}
