/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.web.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.facade.AuditEventFacade;
import com.bytechef.ee.platform.audit.web.graphql.config.AuditGraphQlTestConfiguration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;

/**
 * Verifies the controller wires through to {@link AuditEventFacade} and maps results. Authorization is enforced on the
 * facade (see {@code AuditEventFacadeAuthorizationTest}), not on this controller, so this test no longer asserts
 * controller-level access denial.
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

        when(auditEventFacade.fetchAuditEvents(any(), any(), any(), any(), any(), any())).thenReturn(page);

        graphQlTester
            .document("""
                query {
                    auditEvents {
                        content {
                            id
                            principal
                            eventType
                        }
                        totalElements
                    }
                }
                """)
            .execute()
            .path("auditEvents.content[0].principal")
            .entity(String.class)
            .isEqualTo("alice")
            .path("auditEvents.content[0].eventType")
            .entity(String.class)
            .isEqualTo("PERMISSION_CHECK")
            .path("auditEvents.totalElements")
            .entity(Integer.class)
            .isEqualTo(1);
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

    @TestConfiguration
    static class TestConfig {

        @Bean
        AuditEventFacade auditEventFacade() {
            return mock(AuditEventFacade.class);
        }
    }
}
