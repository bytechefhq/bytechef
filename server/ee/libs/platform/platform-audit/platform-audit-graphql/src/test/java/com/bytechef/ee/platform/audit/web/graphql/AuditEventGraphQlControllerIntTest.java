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
import com.bytechef.platform.constant.PlatformType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
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
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration;

/**
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
    public void testAuditEventPlatformIsReturned() {
        PersistentAuditEvent embeddedAuditEvent = new PersistentAuditEvent();

        embeddedAuditEvent.setId(1L);
        embeddedAuditEvent.setPrincipal("alice");
        embeddedAuditEvent.setEventType("PERMISSION_CHECK");
        embeddedAuditEvent.setEventDate(LocalDateTime.now());
        embeddedAuditEvent.setPlatform(PlatformType.EMBEDDED);
        embeddedAuditEvent.setData(Map.of("result", "ALLOWED"));

        PersistentAuditEvent platformlessAuditEvent = new PersistentAuditEvent();

        platformlessAuditEvent.setId(2L);
        platformlessAuditEvent.setPrincipal("alice");
        platformlessAuditEvent.setEventType("AUTHENTICATION_SUCCESS");
        platformlessAuditEvent.setEventDate(LocalDateTime.now());
        platformlessAuditEvent.setData(Map.of());

        when(auditEventFacade.fetchAuditEvents(any(), any()))
            .thenReturn(new PageImpl<>(List.of(embeddedAuditEvent, platformlessAuditEvent)));

        graphQlTester
            .document("""
                query {
                    auditEvents {
                        content {
                            platform
                        }
                    }
                }
                """)
            .execute()
            .path("auditEvents.content[0].platform")
            .entity(String.class)
            .isEqualTo("EMBEDDED")
            .path("auditEvents.content[1].platform")
            .valueIsNull();
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    public void testAuditEventsPlatformArgumentIsOptional() {
        when(auditEventFacade.fetchAuditEvents(any(), any())).thenReturn(Page.empty());

        assertThat(fetchAuditEventFilter("auditEvents(platform: AUTOMATION)").platform())
            .isEqualTo(PlatformType.AUTOMATION);
        assertThat(fetchAuditEventFilter("auditEvents").platform()).isNull();
    }

    private AuditEventFilter fetchAuditEventFilter(String auditEventsField) {
        graphQlTester
            .document("query { " + auditEventsField + " { totalElements } }")
            .execute()
            .path("auditEvents.totalElements")
            .entity(Long.class)
            .isEqualTo(0L);

        ArgumentCaptor<AuditEventFilter> auditEventFilterArgumentCaptor = ArgumentCaptor.forClass(
            AuditEventFilter.class);

        verify(auditEventFacade, atLeastOnce()).fetchAuditEvents(auditEventFilterArgumentCaptor.capture(), any());

        return auditEventFilterArgumentCaptor.getValue();
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    public void testAuditEventsPassesEveryFilterArgumentToTheFacade() {
        when(auditEventFacade.fetchAuditEvents(any(), any())).thenReturn(Page.empty());

        graphQlTester
            .document("""
                query {
                    auditEvents(
                        eventType: "WORKSPACE_USER_ADDED", fromDate: 1000, toDate: 2000, search: "needle",
                        platform: EMBEDDED) {
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
            new AuditEventFilter("WORKSPACE_USER_ADDED", toLocalDateTime(1000), toLocalDateTime(2000), "needle", true,
                PlatformType.EMBEDDED));
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

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    public void testAuditEventsReturnPrincipalsUnmaskedByDefault() {
        when(auditEventFacade.fetchAuditEvents(any(), any()))
            .thenReturn(new PageImpl<>(List.of(createWorkspaceUserRemovedEvent("alice@example.com"))));

        graphQlTester
            .document("""
                query {
                    auditEvents {
                        content {
                            principal
                            outcome
                            data {
                                key
                                value
                            }
                        }
                    }
                }
                """)
            .execute()
            .path("auditEvents.content[0].principal")
            .entity(String.class)
            .isEqualTo("alice@example.com")
            .path("auditEvents.content[0].outcome")
            .entity(String.class)
            .isEqualTo("SUCCESS")
            .path("auditEvents.content[0].data[*].key")
            .entityList(String.class)
            .containsExactly("result", "userId", "workspaceId")
            .path("auditEvents.content[0].data[*].value")
            .entityList(String.class)
            .containsExactly("SUCCESS", "2", "1");
    }

    private static LocalDateTime toLocalDateTime(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }

    private static PersistentAuditEvent createWorkspaceUserRemovedEvent(String principal) {
        PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

        persistentAuditEvent.setData(Map.of("workspaceId", "1", "userId", "2", "result", "SUCCESS"));
        persistentAuditEvent.setEventDate(LocalDateTime.now());
        persistentAuditEvent.setEventType("WORKSPACE_USER_REMOVED");
        persistentAuditEvent.setId(1L);
        persistentAuditEvent.setPrincipal(principal);

        return persistentAuditEvent;
    }

    @Nested
    @NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
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
    @Import(MaskedPrincipals.MaskedPrincipalsTestConfig.class)
    class MaskedPrincipals {

        @Autowired
        private GraphQlTester graphQlTester;

        @Autowired
        private AuditEventFacade auditEventFacade;

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        public void testAuditEventsMaskPrincipals() {
            when(auditEventFacade.fetchAuditEvents(any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                    createWorkspaceUserRemovedEvent("alice@example.com"), createWorkspaceUserRemovedEvent("alice"),
                    createWorkspaceUserRemovedEvent(" "))));

            graphQlTester
                .document("""
                    query {
                        auditEvents {
                            content {
                                principal
                            }
                        }
                    }
                    """)
                .execute()
                .path("auditEvents.content[*].principal")
                .entityList(String.class)
                .containsExactly("a***@example.com", "a***", " ");
        }

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        public void testAuditEventsSearchExcludesPrincipalsWhenMasked() {
            when(auditEventFacade.fetchAuditEvents(any(), any())).thenReturn(Page.empty());

            graphQlTester
                .document("""
                    query {
                        auditEvents(search: "alice") {
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

            assertThat(auditEventFilterArgumentCaptor.getValue())
                .isEqualTo(new AuditEventFilter(null, null, null, "alice", false, null));
        }

        @TestConfiguration
        static class MaskedPrincipalsTestConfig {

            @Bean
            ApplicationProperties applicationProperties() {
                ApplicationProperties applicationProperties = new ApplicationProperties();

                ApplicationProperties.Audit audit = applicationProperties.getAudit();

                audit.setMaskPrincipals(true);

                return applicationProperties;
            }

            @Bean
            AuditEventFacade auditEventFacade() {
                return mock(AuditEventFacade.class);
            }
        }
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
