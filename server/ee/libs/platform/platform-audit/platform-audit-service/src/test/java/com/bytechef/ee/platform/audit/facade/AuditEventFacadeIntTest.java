/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import java.util.List;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Drives the real {@link AuditEventFacadeImpl} through Spring Security's method interceptor and the
 * {@link AuditAspect}: the {@code ADMIN} guard must actually deny a non-admin, a denied read is recorded, and a
 * successful read of the audit log must not append {@code PERMISSION_CHECK} rows to it.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AuditEventFacadeIntTest.Config.class, properties = "bytechef.edition=ee")
@Import({
    AuditEventFacadeImpl.class, AuditAspect.class, AuditMapperResolver.class
})
class AuditEventFacadeIntTest {

    private static final String BODY_REACHED = "body reached";

    @Autowired
    private AuditEventFacade auditEventFacade;

    @Autowired
    private AuditEventService auditEventService;

    @BeforeEach
    void setUp() {
        reset(auditEventService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void testFetchEventTypesIsDeniedForNonAdmin() {
        authenticate("bob", "ROLE_USER");

        assertThatThrownBy(() -> auditEventFacade.fetchEventTypes())
            .isInstanceOf(AccessDeniedException.class);

        verify(auditEventService, never()).fetchEventTypes();
    }

    @Test
    void testFetchAuditEventsIsDeniedForNonAdmin() {
        authenticate("bob", "ROLE_USER");

        assertThatThrownBy(() -> auditEventFacade.fetchAuditEvents(AuditEventFilter.empty(), null))
            .isInstanceOf(AccessDeniedException.class);

        verify(auditEventService, never()).fetchAuditEvents(any(), any());
    }

    @Test
    void testFetchEventTypesIsAllowedForAdmin() {
        authenticate("alice", "ROLE_ADMIN");

        when(auditEventService.fetchEventTypes()).thenThrow(new IllegalStateException(BODY_REACHED));

        assertThatThrownBy(() -> auditEventFacade.fetchEventTypes())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(BODY_REACHED);
    }

    @Test
    void testFetchAuditEventsIsAllowedForAdmin() {
        authenticate("alice", "ROLE_ADMIN");

        when(auditEventService.fetchAuditEvents(any(), any())).thenThrow(new IllegalStateException(BODY_REACHED));

        assertThatThrownBy(() -> auditEventFacade.fetchAuditEvents(AuditEventFilter.empty(), null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(BODY_REACHED);
    }

    @Test
    void testReadingAuditEventsDoesNotAppendToTheAuditLog() {
        authenticate("alice", "ROLE_ADMIN");

        when(auditEventService.fetchEventTypes()).thenReturn(List.of("PERMISSION_CHECK"));

        assertThat(auditEventFacade.fetchEventTypes()).containsExactly("PERMISSION_CHECK");

        verify(auditEventService, never()).saveAll(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDeniedReadOfTheAuditLogIsRecorded() {
        authenticate("bob", "ROLE_USER");

        assertThatThrownBy(() -> auditEventFacade.fetchEventTypes())
            .isInstanceOf(AccessDeniedException.class);

        ArgumentCaptor<List<PersistentAuditEvent>> argumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(auditEventService).saveAll(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue())
            .singleElement()
            .satisfies(persistentAuditEvent -> {
                assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("bob");
                assertThat(persistentAuditEvent.getData())
                    .containsEntry("result", "DENIED")
                    .containsEntry("method", AuditEventFacade.class.getName() + ".fetchEventTypes");
            });
    }

    private void authenticate(String principal, String authority) {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    @SpringBootConfiguration
    @EnableAspectJAutoProxy
    @EnableMethodSecurity
    static class Config {

        @Bean
        AuditEventService auditEventService() {
            return mock(AuditEventService.class);
        }
    }
}
