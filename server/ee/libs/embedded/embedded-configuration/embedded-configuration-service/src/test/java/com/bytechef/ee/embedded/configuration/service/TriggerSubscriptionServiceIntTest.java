/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscription;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionStatus;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.util.Objects;
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
class TriggerSubscriptionServiceIntTest {

    private static final int ERROR_MAX_LENGTH = 1024;

    @Autowired
    private TriggerSubscriptionService triggerSubscriptionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void afterEach() {
        jdbcTemplate.update("DELETE FROM connected_user_trigger_subscription");
        jdbcTemplate.update("DELETE FROM connected_user");
    }

    @Test
    void testUpdateDeliveryDoesNotChangeVersion() {
        TriggerSubscription triggerSubscription = new TriggerSubscription();

        triggerSubscription.setComponentName("slack");
        triggerSubscription.setComponentVersion(1);
        triggerSubscription.setConnectedUserId(insertConnectedUser());
        triggerSubscription.setEnvironment(Environment.PRODUCTION);
        triggerSubscription.setStatus(TriggerSubscriptionStatus.ENABLED);
        triggerSubscription.setTriggerName("newMessage");

        TriggerSubscription created = triggerSubscriptionService.create(triggerSubscription);

        int versionBefore = triggerSubscriptionService.getTriggerSubscription(created.getId())
            .getVersion();

        triggerSubscriptionService.updateDelivery(created.getId(), Instant.now(), "SENT", null);

        TriggerSubscription reloaded = triggerSubscriptionService.getTriggerSubscription(created.getId());

        assertThat(reloaded.getVersion()).isEqualTo(versionBefore);
        assertThat(reloaded.getLastDeliveryStatus()).isEqualTo("SENT");
        assertThat(reloaded.getLastEventDate()).isNotNull();
        assertThat(reloaded.getLastDeliveryError()).isNull();
    }

    @Test
    void testUpdateKeepsTheDeliveryValuesWrittenAfterTheSubscriptionWasLoaded() {
        TriggerSubscription created = triggerSubscriptionService.create(createTriggerSubscription());

        TriggerSubscription loaded = triggerSubscriptionService.getTriggerSubscription(created.getId());

        triggerSubscriptionService.updateDelivery(created.getId(), Instant.now(), "FAILED", "HTTP 500");

        loaded.setName("renamed");

        triggerSubscriptionService.update(loaded);

        TriggerSubscription reloaded = triggerSubscriptionService.getTriggerSubscription(created.getId());

        assertThat(reloaded.getName()).isEqualTo("renamed");
        assertThat(reloaded.getLastDeliveryStatus()).isEqualTo("FAILED");
        assertThat(reloaded.getLastDeliveryError()).isEqualTo("HTTP 500");
        assertThat(reloaded.getLastEventDate()).isNotNull();
    }

    @Test
    void testUpdateDeliveryStoresAnOverLongErrorTruncated() {
        TriggerSubscription created = triggerSubscriptionService.create(createTriggerSubscription());

        triggerSubscriptionService.updateDelivery(created.getId(), Instant.now(), "FAILED", "x".repeat(2000));

        TriggerSubscription reloaded = triggerSubscriptionService.getTriggerSubscription(created.getId());

        assertThat(reloaded.getLastDeliveryError()).hasSize(ERROR_MAX_LENGTH);
    }

    @Test
    void testUpdateStoresAnOverLongErrorMessageTruncated() {
        TriggerSubscription created = triggerSubscriptionService.create(createTriggerSubscription());

        created.setErrorMessage("x".repeat(2000));

        triggerSubscriptionService.update(created);

        TriggerSubscription reloaded = triggerSubscriptionService.getTriggerSubscription(created.getId());

        assertThat(reloaded.getErrorMessage()).hasSize(ERROR_MAX_LENGTH);
    }

    private TriggerSubscription createTriggerSubscription() {
        TriggerSubscription triggerSubscription = new TriggerSubscription();

        triggerSubscription.setComponentName("slack");
        triggerSubscription.setComponentVersion(1);
        triggerSubscription.setConnectedUserId(insertConnectedUser());
        triggerSubscription.setEnvironment(Environment.PRODUCTION);
        triggerSubscription.setStatus(TriggerSubscriptionStatus.ENABLED);
        triggerSubscription.setTriggerName("newMessage");

        return triggerSubscription;
    }

    private long insertConnectedUser() {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                """
                    INSERT INTO connected_user
                        (external_id, enabled, environment, created_date, created_by, last_modified_date,
                         last_modified_by, version)
                    VALUES ('user-1', true, ?, now(), 'test', now(), 'test', 0)
                    RETURNING id
                    """,
                Long.class, Environment.PRODUCTION.ordinal()),
            "connected user id");
    }
}
