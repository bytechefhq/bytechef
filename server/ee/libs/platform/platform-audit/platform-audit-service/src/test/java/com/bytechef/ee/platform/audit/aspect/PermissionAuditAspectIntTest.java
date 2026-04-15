/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Integration test that boots a minimal Spring context with method security enabled and verifies
 * {@link PermissionAuditAspect} actually wraps Spring Security's {@code AuthorizationManagerBeforeMethodInterceptor}.
 * The unit test {@code PermissionAuditAspectTest#testAspectIsOrderedHighestPrecedence} only confirms the
 * {@code @Order(HIGHEST_PRECEDENCE)} annotation is present; this test confirms the resulting interceptor chain actually
 * fires this aspect <em>around</em> the security advisor so DENIED events are recorded.
 *
 * <p>
 * If a future Spring Security upgrade reorders the authorization advisor (e.g., to {@code HIGHEST_PRECEDENCE - 1}),
 * this test fails — without it, the regression would surface as silently missing DENIED audit rows in production with
 * no failing unit test.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PermissionAuditAspectIntTest.Config.class)
@Import(PermissionAuditAspect.class)
class PermissionAuditAspectIntTest {

    @Autowired
    private GuardedTestService guardedTestService;

    @Autowired
    private AuditEventService auditEventService;

    @BeforeEach
    void setUp() {
        reset(auditEventService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void testAllowedPathRecordsAllowedAuditEventThroughProxyChain() {
        authenticate("alice", "ROLE_ADMIN");

        String result = guardedTestService.adminOnlyMethod();

        assertThat(result).isEqualTo("ok");

        PersistentAuditEvent captured = captureSavedEvent();

        assertThat(captured.getData()).containsEntry("result", "ALLOWED");
        assertThat(captured.getPrincipal()).isEqualTo("alice");
    }

    @Test
    void testDeniedPathRecordsDeniedAuditEventEvenThoughSecurityRejects() {
        authenticate("bob", "ROLE_USER");

        // Without the aspect ordering, Spring Security's authorization advisor would throw BEFORE this aspect's
        // proceed() runs and the DENIED branch would never fire. If this assertion fails, the aspect ordering is
        // wrong — fix the @Order on PermissionAuditAspect.
        assertThatThrownBy(() -> guardedTestService.adminOnlyMethod())
            .isInstanceOf(AccessDeniedException.class);

        PersistentAuditEvent captured = captureSavedEvent();

        assertThat(captured.getData()).containsEntry("result", "DENIED");
        assertThat(captured.getPrincipal()).isEqualTo("bob");
    }

    private void authenticate(String principal, String authority) {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, "n/a", java.util.List.of(new SimpleGrantedAuthority(authority))));
    }

    private PersistentAuditEvent captureSavedEvent() {
        ArgumentCaptor<PersistentAuditEvent> captor = ArgumentCaptor.forClass(PersistentAuditEvent.class);

        verify(auditEventService, atLeastOnce()).save(captor.capture());

        return captor.getValue();
    }

    // @SpringBootConfiguration (not @TestConfiguration): Spring ignores @TestConfiguration when it is passed
    // directly via @SpringBootTest(classes = ...), producing "Unable to find a @SpringBootConfiguration".
    // @SpringBootConfiguration is a @Configuration specialization that Spring accepts as the primary source.
    @SpringBootConfiguration
    @EnableAspectJAutoProxy
    @EnableMethodSecurity
    static class Config {

        @Bean
        AuditEventService auditEventService() {
            return mock(AuditEventService.class);
        }

        @Bean
        GuardedTestService guardedTestService() {
            return new GuardedTestService();
        }
    }

    @Service
    static class GuardedTestService {

        @PreAuthorize("hasRole('ADMIN')")
        public String adminOnlyMethod() {
            return "ok";
        }
    }
}
