/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.facade.AuditEventFacade;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AuditEventGraphQlControllerTest {

    @Test
    void testMaskPrincipalReturnsNullForNull() {
        assertThat(AuditEventGraphQlController.maskPrincipal(null)).isNull();
    }

    @Test
    void testMaskPrincipalReturnsBlankUnchanged() {
        assertThat(AuditEventGraphQlController.maskPrincipal("")).isEmpty();
        assertThat(AuditEventGraphQlController.maskPrincipal("   ")).isEqualTo("   ");
    }

    @Test
    void testMaskPrincipalMasksEmail() {
        assertThat(AuditEventGraphQlController.maskPrincipal("alice@example.com")).isEqualTo("a***@example.com");
    }

    @Test
    void testMaskPrincipalMasksNonEmail() {
        assertThat(AuditEventGraphQlController.maskPrincipal("alice")).isEqualTo("a***");
    }

    @Test
    void testAuditEventsMaskPrincipalsWhenMaskingIsEnabled() {
        AuditEventGraphQlController.AuditEventPage auditEventPage = createController(true)
            .auditEvents(null, null, null, null, null, null, null);

        assertThat(auditEventPage.content())
            .singleElement()
            .satisfies(auditEvent -> assertThat(auditEvent.principal()).isEqualTo("a***@example.com"));
    }

    @Test
    void testAuditEventsReturnTheRecordedEventUnmaskedByDefault() {
        AuditEventGraphQlController.AuditEventPage auditEventPage = createController(false)
            .auditEvents(null, null, null, null, null, null, null);

        assertThat(auditEventPage.totalElements()).isEqualTo(1L);
        assertThat(auditEventPage.content())
            .singleElement()
            .satisfies(auditEvent -> {
                assertThat(auditEvent.principal()).isEqualTo("alice@example.com");
                assertThat(auditEvent.eventType()).isEqualTo("WORKSPACE_USER_REMOVED");
                assertThat(auditEvent.outcome()).isEqualTo(AuditEventGraphQlController.AuditEventOutcome.SUCCESS);
                assertThat(auditEvent.data())
                    .extracting(
                        AuditEventGraphQlController.AuditEventDataEntry::key,
                        AuditEventGraphQlController.AuditEventDataEntry::value)
                    .containsExactly(tuple("result", "SUCCESS"), tuple("userId", "2"), tuple("workspaceId", "1"));
            });
    }

    private static AuditEventGraphQlController createController(boolean maskPrincipals) {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        AuditEventFacade auditEventFacade = mock(AuditEventFacade.class);
        PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

        applicationProperties.getAudit()
            .setMaskPrincipals(maskPrincipals);

        persistentAuditEvent.setData(Map.of("workspaceId", "1", "userId", "2", "result", "SUCCESS"));
        persistentAuditEvent.setEventDate(LocalDateTime.now());
        persistentAuditEvent.setEventType("WORKSPACE_USER_REMOVED");
        persistentAuditEvent.setId(1L);
        persistentAuditEvent.setPrincipal("alice@example.com");

        when(auditEventFacade.fetchAuditEvents(any(), any())).thenReturn(new PageImpl<>(List.of(persistentAuditEvent)));

        return new AuditEventGraphQlController(applicationProperties, auditEventFacade);
    }
}
