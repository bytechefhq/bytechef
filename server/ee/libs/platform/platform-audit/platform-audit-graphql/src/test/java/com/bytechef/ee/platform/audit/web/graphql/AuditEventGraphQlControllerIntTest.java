/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.facade.AuditEventFacade;
import com.bytechef.ee.platform.audit.web.graphql.config.AuditGraphQlTestConfiguration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;

/**
 * Verifies the controller wires through to {@link AuditEventFacade} and maps results. Authorization is enforced on the
 * facade (see {@code AuditEventFacadeTest} and {@code AuditEventFacadeIntTest}), not on this controller.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AuditGraphQlTestConfiguration.class,
    AuditEventGraphQlController.class
})
@GraphQlTest(
    controllers = AuditEventGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@Import(AuditEventGraphQlControllerIntTest.TestConfig.class)
public class AuditEventGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private AuditEventFacade auditEventFacade;

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    public void testAuditEventsAsAdminReturnsPage() {
        PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

        persistentAuditEvent.setId(1L);
        persistentAuditEvent.setPrincipal("alice");
        persistentAuditEvent.setEventType("PERMISSION_CHECK");
        persistentAuditEvent.setEventDate(LocalDateTime.now());
        persistentAuditEvent.setData(Map.of("method", "m", "result", "ALLOWED"));

        Page<PersistentAuditEvent> page = new PageImpl<>(List.of(persistentAuditEvent));

        when(auditEventFacade.fetchAuditEvents(any(), any())).thenReturn(page);

        graphQlTester
            .document("""
                query {
                    auditEvents {
                        content {
                            id
                            outcome
                            principal
                            eventType
                        }
                        totalElements
                    }
                }
                """)
            .execute()
            .path("auditEvents.content[0].outcome")
            .entity(String.class)
            .isEqualTo("ALLOWED")
            .path("auditEvents.content[0].principal")
            .entity(String.class)
            .isEqualTo("alice")
            .path("auditEvents.content[0].eventType")
            .entity(String.class)
            .isEqualTo("PERMISSION_CHECK")
            .path("auditEvents.totalElements")
            .entity(Long.class)
            .isEqualTo(1L);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    public void testAuditEventWithoutAResultHasNoOutcome() {
        PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

        persistentAuditEvent.setId(1L);
        persistentAuditEvent.setPrincipal("SYSTEM");
        persistentAuditEvent.setEventType("JOB_COMPLETED");
        persistentAuditEvent.setEventDate(LocalDateTime.now());
        persistentAuditEvent.setData(Map.of("jobId", "7", "status", "COMPLETED"));

        when(auditEventFacade.fetchAuditEvents(any(), any()))
            .thenReturn(new PageImpl<>(List.of(persistentAuditEvent)));

        graphQlTester
            .document("""
                query {
                    auditEvents {
                        content {
                            outcome
                        }
                    }
                }
                """)
            .execute()
            .path("auditEvents.content[0].outcome")
            .valueIsNull();
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    public void testAuditEventsPassesEveryFilterArgumentToTheFacade() {
        when(auditEventFacade.fetchAuditEvents(any(), any())).thenReturn(Page.empty());

        graphQlTester
            .document("""
                query {
                    auditEvents(
                        principal: "alice", eventType: "WORKSPACE_USER_ADDED", fromDate: 1000, toDate: 2000,
                        dataSearch: "needle") {
                        totalElements
                    }
                }
                """)
            .execute()
            .path("auditEvents.totalElements")
            .entity(Long.class)
            .isEqualTo(0L);

        ArgumentCaptor<AuditEventFilter> auditEventFilterArgumentCaptor = ArgumentCaptor.forClass(
            AuditEventFilter.class);

        verify(auditEventFacade, atLeastOnce()).fetchAuditEvents(auditEventFilterArgumentCaptor.capture(), any());

        assertThat(auditEventFilterArgumentCaptor.getValue()).isEqualTo(
            new AuditEventFilter(
                "alice", "WORKSPACE_USER_ADDED", toLocalDateTime(1000), toLocalDateTime(2000), "needle"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    public void testAuditEventsClampsPageAndSize() {
        when(auditEventFacade.fetchAuditEvents(any(), any())).thenReturn(Page.empty());

        graphQlTester
            .document("""
                query {
                    auditEvents(page: -5, size: 100000) {
                        totalElements
                    }
                }
                """)
            .execute()
            .path("auditEvents.totalElements")
            .entity(Long.class)
            .isEqualTo(0L);

        ArgumentCaptor<Pageable> pageableArgumentCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(auditEventFacade, atLeastOnce()).fetchAuditEvents(any(), pageableArgumentCaptor.capture());

        Pageable pageable = pageableArgumentCaptor.getValue();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(200);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    public void testAuditEventDateRoundTripsAsTheInstantItWasRecordedAt() {
        long recordedAtMillis = System.currentTimeMillis();

        PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

        persistentAuditEvent.setId(1L);
        persistentAuditEvent.setPrincipal("alice");
        persistentAuditEvent.setEventType("PERMISSION_CHECK");

        // Stamped exactly as AuditAspect does it, in the JVM default zone
        persistentAuditEvent.setEventDate(LocalDateTime.now());

        when(auditEventFacade.fetchAuditEvents(any(), any()))
            .thenReturn(new PageImpl<>(List.of(persistentAuditEvent)));

        graphQlTester
            .document("""
                query {
                    auditEvents {
                        content {
                            eventDate
                        }
                    }
                }
                """)
            .execute()
            .path("auditEvents.content[0].eventDate")
            .entity(Long.class)
            .satisfies(eventDate -> assertThat(eventDate).isCloseTo(recordedAtMillis, within(60_000L)));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    public void testAuditEventTypesAsAdminReturnsTypes() {
        when(auditEventFacade.fetchEventTypes()).thenReturn(List.of("CONNECTION_CREATED", "PERMISSION_CHECK"));

        graphQlTester
            .document("""
                query {
                    auditEventTypes
                }
                """)
            .execute()
            .path("auditEventTypes")
            .entityList(String.class)
            .containsExactly("CONNECTION_CREATED", "PERMISSION_CHECK");
    }

    private static LocalDateTime toLocalDateTime(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        ApplicationProperties applicationProperties() {
            return new ApplicationProperties();
        }

        @Bean
        AuditEventFacade auditEventFacade() {
            return mock(AuditEventFacade.class);
        }
    }
}
