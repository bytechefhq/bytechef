/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscription;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionStatus;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = IntegrationIntTestConfiguration.class, properties = "spring.liquibase.contexts=configuration,user")
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class TriggerSubscriptionRepositoryIntTest {

    @Autowired
    private TriggerSubscriptionRepository triggerSubscriptionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void afterEach() {
        triggerSubscriptionRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM connected_user");
    }

    @Test
    void testSaveAndFindAllByConnectedUserIdAndEnvironment() {
        long connectedUserId = insertConnectedUser("user-1", Environment.DEVELOPMENT);

        TriggerSubscription first = createTriggerSubscription(connectedUserId, Environment.DEVELOPMENT);

        first.setParameters(Map.of("channel", "C1"));

        triggerSubscriptionRepository.save(first);
        triggerSubscriptionRepository.save(createTriggerSubscription(connectedUserId, Environment.DEVELOPMENT));
        triggerSubscriptionRepository.save(createTriggerSubscription(connectedUserId, Environment.PRODUCTION));

        List<TriggerSubscription> triggerSubscriptions = triggerSubscriptionRepository
            .findAllByConnectedUserIdAndEnvironment(connectedUserId, Environment.DEVELOPMENT.ordinal());

        assertThat(triggerSubscriptions).hasSize(2);
        assertThat(triggerSubscriptions)
            .anySatisfy(triggerSubscription -> {
                assertThat(triggerSubscription.getParameters()
                    .get("channel")).isEqualTo("C1");
                assertThat(triggerSubscription.getEnvironment()).isEqualTo(Environment.DEVELOPMENT);
                assertThat(triggerSubscription.getStatus()).isEqualTo(TriggerSubscriptionStatus.ENABLED);
            });
        assertThat(triggerSubscriptionRepository.findAllByConnectedUserId(connectedUserId)).hasSize(3);
    }

    @Test
    void testFindAllByConnectionId() {
        long connectedUserId = insertConnectedUser("user-1", Environment.DEVELOPMENT);

        TriggerSubscription matching = createTriggerSubscription(connectedUserId, Environment.DEVELOPMENT);

        matching.setConnectionId(77L);

        TriggerSubscription saved = triggerSubscriptionRepository.save(matching);

        triggerSubscriptionRepository.save(createTriggerSubscription(connectedUserId, Environment.DEVELOPMENT));

        assertThat(triggerSubscriptionRepository.findAllByConnectionId(77L))
            .extracting(TriggerSubscription::getId)
            .containsExactly(saved.getId());
    }

    @Test
    void testFindAllByIntegrationInstanceId() {
        long connectedUserId = insertConnectedUser("user-1", Environment.DEVELOPMENT);

        TriggerSubscription matching = createTriggerSubscription(connectedUserId, Environment.DEVELOPMENT);

        matching.setIntegrationInstanceId(88L);

        TriggerSubscription saved = triggerSubscriptionRepository.save(matching);

        triggerSubscriptionRepository.save(createTriggerSubscription(connectedUserId, Environment.DEVELOPMENT));

        assertThat(triggerSubscriptionRepository.findAllByIntegrationInstanceId(88L))
            .extracting(TriggerSubscription::getId)
            .containsExactly(saved.getId());
    }

    @Test
    void testUpdateDeliveryKeepsTheNewerEventWhenAnOlderEventIsRecordedLater() {
        long id = saveTriggerSubscription();
        Instant newerEventDate = Instant.parse("2026-11-12T10:00:05Z");

        triggerSubscriptionRepository.updateDelivery(id, newerEventDate, "SENT", null);
        triggerSubscriptionRepository.updateDelivery(id, Instant.parse("2026-11-12T10:00:00Z"), "FAILED", "HTTP 500");

        assertDelivery(id, newerEventDate, "SENT", null);
    }

    @Test
    void testUpdateDeliveryReplacesTheOutcomeOfAnEqualOrOlderEvent() {
        long id = saveTriggerSubscription();
        Instant eventDate = Instant.parse("2026-11-12T10:00:00Z");

        triggerSubscriptionRepository.updateDelivery(id, eventDate, "FAILED", "HTTP 500");
        triggerSubscriptionRepository.updateDelivery(id, eventDate, "SENT", null);

        assertDelivery(id, eventDate, "SENT", null);

        Instant newerEventDate = Instant.parse("2026-11-12T10:00:05Z");

        triggerSubscriptionRepository.updateDelivery(id, newerEventDate, "FAILED", "HTTP 502");

        assertDelivery(id, newerEventDate, "FAILED", "HTTP 502");
    }

    private void assertDelivery(
        long id, Instant expectedLastEventDate, String expectedLastDeliveryStatus,
        @Nullable String expectedLastDeliveryError) {

        TriggerSubscription triggerSubscription = triggerSubscriptionRepository.findById(id)
            .orElseThrow();

        assertThat(triggerSubscription.getLastEventDate()).isEqualTo(expectedLastEventDate);
        assertThat(triggerSubscription.getLastDeliveryStatus()).isEqualTo(expectedLastDeliveryStatus);
        assertThat(triggerSubscription.getLastDeliveryError()).isEqualTo(expectedLastDeliveryError);
    }

    private long saveTriggerSubscription() {
        long connectedUserId = insertConnectedUser("user-1", Environment.DEVELOPMENT);

        TriggerSubscription triggerSubscription = triggerSubscriptionRepository.save(
            createTriggerSubscription(connectedUserId, Environment.DEVELOPMENT));

        return Objects.requireNonNull(triggerSubscription.getId(), "id");
    }

    private static TriggerSubscription createTriggerSubscription(long connectedUserId, Environment environment) {
        TriggerSubscription triggerSubscription = new TriggerSubscription();

        triggerSubscription.setComponentName("slack");
        triggerSubscription.setComponentVersion(1);
        triggerSubscription.setConnectedUserId(connectedUserId);
        triggerSubscription.setEnvironment(environment);
        triggerSubscription.setStatus(TriggerSubscriptionStatus.ENABLED);
        triggerSubscription.setTriggerName("newMessage");

        return triggerSubscription;
    }

    private long insertConnectedUser(String externalId, Environment environment) {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                """
                    INSERT INTO connected_user
                        (external_id, enabled, environment, created_date, created_by, last_modified_date,
                         last_modified_by, version)
                    VALUES (?, true, ?, now(), 'test', now(), 'test', 0)
                    RETURNING id
                    """,
                Long.class, externalId, environment.ordinal()),
            "connected user id");
    }
}
