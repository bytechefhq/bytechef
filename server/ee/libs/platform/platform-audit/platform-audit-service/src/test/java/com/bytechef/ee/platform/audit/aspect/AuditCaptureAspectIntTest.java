/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import com.bytechef.platform.audit.Audited;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AuditIntTestConfiguration.class)
@ActiveProfiles("testint")
@Import({
    PostgreSQLContainerConfiguration.class, AuditCaptureAspectIntTest.CaptureServiceConfiguration.class
})
@SuppressFBWarnings(
    value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT",
    justification = "Guarded calls inside assertThatThrownBy are expected to throw, so their return value is irrelevant")
class AuditCaptureAspectIntTest {

    @Autowired
    private GuardedAuditedService guardedAuditedService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private RecordingAuditMapper recordingAuditMapper;

    @BeforeEach
    void beforeEach() {
        persistenceAuditEventRepository.deleteAll();

        recordingAuditMapper.captureCount.set(0);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDeniedCallNeverCaptures() {
        authenticate("ROLE_USER");

        assertThatThrownBy(() -> guardedAuditedService.adminOnly(7L)).isInstanceOf(AccessDeniedException.class);

        assertThat(recordingAuditMapper.captureCount.get()).isZero();

        PersistentAuditEvent persistentAuditEvent = findSingle();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo("TEST_EVENT");
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("alice");
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "DENIED")
            .doesNotContainKey("captured");
    }

    @Test
    void testAllowedCallCapturesOnceAndRecordsTheCapturedValue() {
        authenticate("ROLE_ADMIN");

        assertThat(guardedAuditedService.adminOnly(7L)).isEqualTo("ok");

        assertThat(recordingAuditMapper.captureCount.get()).isEqualTo(1);
        assertThat(findSingle().getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("captured", "before-7");
        assertThat(AuditFrame.current()).isNull();
    }

    @Test
    void testThrowingCaptureStillRunsTheCallAndRecordsTheEventWithoutTheCapturedValue() {
        authenticate("ROLE_ADMIN");

        assertThat(guardedAuditedService.throwingCapture(7L)).isEqualTo("ok");

        assertThat(findSingle().getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("captureError", IllegalStateException.class.getName())
            .doesNotContainKey("captured")
            .doesNotContainKey("mapperError");
    }

    private void authenticate(String authority) {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken("alice", "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    private PersistentAuditEvent findSingle() {
        Page<PersistentAuditEvent> page =
            persistenceAuditEventRepository.findAllFiltered(AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    @Configuration
    @EnableMethodSecurity
    static class CaptureServiceConfiguration {

        @Bean
        GuardedAuditedService guardedAuditedService() {
            return new GuardedAuditedService();
        }

        @Bean
        RecordingAuditMapper recordingAuditMapper() {
            return new RecordingAuditMapper();
        }

        @Bean
        ThrowingCaptureAuditMapper throwingCaptureAuditMapper() {
            return new ThrowingCaptureAuditMapper();
        }
    }

    static class GuardedAuditedService {

        @Audited(event = "TEST_EVENT", mapper = RecordingAuditMapper.class)
        @PreAuthorize("hasRole('ADMIN')")
        public String adminOnly(long userId) {
            return "ok";
        }

        @Audited(event = "TEST_EVENT", mapper = ThrowingCaptureAuditMapper.class)
        @PreAuthorize("hasRole('ADMIN')")
        public String throwingCapture(long userId) {
            return "ok";
        }
    }

    static class RecordingAuditMapper implements AuditMapper {

        private final AtomicInteger captureCount = new AtomicInteger();

        @Override
        public Object capture(AuditInvocation auditInvocation) {
            captureCount.incrementAndGet();

            return "before-" + auditInvocation.argument("userId");
        }

        @Override
        public Map<String, String> map(AuditInvocation auditInvocation) {
            if (auditInvocation.captured() == null) {
                return Map.of();
            }

            return Map.of("captured", String.valueOf(auditInvocation.captured()));
        }
    }

    static class ThrowingCaptureAuditMapper implements AuditMapper {

        @Override
        public Object capture(AuditInvocation auditInvocation) {
            throw new IllegalStateException("before-state read failed");
        }

        @Override
        public Map<String, String> map(AuditInvocation auditInvocation) {
            if (auditInvocation.captured() == null) {
                return Map.of();
            }

            return Map.of("captured", String.valueOf(auditInvocation.captured()));
        }
    }
}
