/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifies that an {@code @AuditAiHub}-annotated method lands a row in {@code persistent_audit_event} with the expected
 * event type and SpEL-evaluated data payload, exercising the full path through {@link AiHubAuditAspect},
 * {@link AiHubAuditPublisher}, Spring Boot Actuator's {@code AuditListener}, and {@code CustomAuditEventRepository}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AiHubAuditAspectIntTest.IntTestConfiguration.class)
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
@TestPropertySource(properties = "bytechef.edition=ee")
public class AiHubAuditAspectIntTest {

    @Autowired
    private AuditedBean auditedBean;

    @Autowired
    private PersistenceAuditEventRepository auditEventRepository;

    @AfterEach
    public void afterEach() {
        auditEventRepository.deleteAll();
    }

    @Test
    public void testAnnotatedMethodLandsAuditRow() {
        auditedBean.simulateUpdate(7L, 99L);

        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                List<PersistentAuditEvent> events = auditEventRepository.findAll();

                assertThat(events).hasSize(1);

                PersistentAuditEvent event = events.get(0);

                assertThat(event.getEventType()).isEqualTo("AI_HUB_PERSONAL_AGENT_UPDATED");
                assertThat(event.getData())
                    .containsEntry("workspaceId", "7")
                    .containsEntry("agentId", "99");
            });
    }

    @EnableAutoConfiguration
    @EnableAspectJAutoProxy
    @ComponentScan(basePackages = {
        "com.bytechef.ee.ai.hub.audit",
        "com.bytechef.ee.platform.audit"
    })
    @Import({
        LiquibaseConfiguration.class,
        AiHubAuditAspectIntTest.IntTestConfiguration.IntTestJdbcConfiguration.class
    })
    @Configuration
    public static class IntTestConfiguration {

        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        @Configuration
        public static class IntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
        }
    }
}
