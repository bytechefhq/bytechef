/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
import org.springframework.test.context.NestedTestConfiguration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings(
    value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT",
    justification = "Guarded calls inside assertThatThrownBy are expected to throw, so their return value is irrelevant")
class AuditAspectIntTest {

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @SpringBootTest(classes = AuditAspectIntTest.Config.class)
    @Import({
        AuditAspect.class, AuditMapperResolver.class
    })
    class ProxyChain {

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

            assertThatThrownBy(() -> guardedTestService.adminOnlyMethod())
                .isInstanceOf(AccessDeniedException.class);

            PersistentAuditEvent captured = captureSavedEvent();

            assertThat(captured.getData()).containsEntry("result", "DENIED");
            assertThat(captured.getPrincipal()).isEqualTo("bob");
        }

        private void authenticate(String principal, String authority) {
            SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                    principal, "n/a", List.of(new SimpleGrantedAuthority(authority))));
        }

        private PersistentAuditEvent captureSavedEvent() {
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<PersistentAuditEvent>> captor = ArgumentCaptor.forClass(List.class);

            verify(auditEventService, times(1)).saveAll(captor.capture());

            List<PersistentAuditEvent> savedEvents = captor.getValue();

            assertThat(savedEvents).hasSize(1);

            return savedEvents.getFirst();
        }
    }

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
