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
import static org.mockito.Mockito.verify;

import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import com.bytechef.platform.audit.Audited;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
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

/**
 * Pins that {@link AuditCaptureAspect} runs inside Spring Security's {@code @PreAuthorize} interceptor: a denied caller
 * must never trigger a mapper's before-state read.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AuditCaptureAspectIntTest.Config.class, properties = "bytechef.edition=ee")
@SuppressFBWarnings(
    value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT",
    justification = "Guarded calls inside assertThatThrownBy are expected to throw, so their return value is irrelevant")
class AuditCaptureAspectIntTest {

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private GuardedAuditedService guardedAuditedService;

    @Autowired
    private RecordingAuditMapper recordingAuditMapper;

    @BeforeEach
    void beforeEach() {
        reset(auditEventService);

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

        PersistentAuditEvent persistentAuditEvent = captureSavedEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo("TEST_EVENT");
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "DENIED")
            .doesNotContainKey("captured");
    }

    @Test
    void testAllowedCallCapturesOnceAndRecordsTheCapturedValue() {
        authenticate("ROLE_ADMIN");

        assertThat(guardedAuditedService.adminOnly(7L)).isEqualTo("ok");

        assertThat(recordingAuditMapper.captureCount.get()).isEqualTo(1);
        assertThat(captureSavedEvent().getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("captured", "before-7");
    }

    @Test
    void testThrowingCaptureStillRunsTheCallAndRecordsTheEventWithoutTheCapturedValue() {
        authenticate("ROLE_ADMIN");

        assertThat(guardedAuditedService.throwingCapture(7L)).isEqualTo("ok");

        assertThat(captureSavedEvent().getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("captureError", IllegalStateException.class.getName())
            .doesNotContainKey("captured")
            .doesNotContainKey("mapperError");
    }

    private void authenticate(String authority) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", "n/a",
                    List.of(new SimpleGrantedAuthority(authority))));
    }

    @SuppressWarnings("unchecked")
    private PersistentAuditEvent captureSavedEvent() {
        ArgumentCaptor<List<PersistentAuditEvent>> argumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(auditEventService).saveAll(argumentCaptor.capture());

        List<PersistentAuditEvent> persistentAuditEvents = argumentCaptor.getValue();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    @SpringBootConfiguration
    @EnableAspectJAutoProxy
    @EnableMethodSecurity
    @Import({
        AuditAspect.class, AuditCaptureAspect.class, AuditMapperResolver.class
    })
    static class Config {

        @Bean
        AuditEventService auditEventService() {
            return mock(AuditEventService.class);
        }

        @Bean
        RecordingAuditMapper recordingAuditMapper() {
            return new RecordingAuditMapper();
        }

        @Bean
        ThrowingCaptureAuditMapper throwingCaptureAuditMapper() {
            return new ThrowingCaptureAuditMapper();
        }

        @Bean
        GuardedAuditedService guardedAuditedService() {
            return new GuardedAuditedService();
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
