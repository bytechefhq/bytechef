/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.trigger.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.dto.TriggerSubscriptionWebhookSettingsDTO;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.repository.PropertyRepository;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.configuration.service.PropertyServiceImpl;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Runs the settings accessor against a real property table and forces a second call to start while a first-time URL
 * save sits between reading the missing secret and writing a new one.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = IntegrationIntTestConfiguration.class, properties = "spring.liquibase.contexts=configuration,user")
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class TriggerSubscriptionWebhookSettingsAccessorIntTest {

    private static final long COMPLETION_TIMEOUT_SECONDS = 30;
    private static final long CONCURRENT_CALL_WAIT_SECONDS = 3;
    private static final Environment ENVIRONMENT = Environment.STAGING;
    private static final String SIGNING_SECRET_KEY = "triggerSubscriptionSigningSecret";
    private static final String WEBHOOK_URL = "https://example.com/hook";
    private static final String WEBHOOK_URL_KEY = "triggerSubscriptionWebhookUrl";

    private final AtomicBoolean concurrentCallFinishedFirst = new AtomicBoolean();
    private final AtomicReference<Supplier<TriggerSubscriptionWebhookSettingsDTO>> concurrentCall =
        new AtomicReference<>();
    private final AtomicReference<CompletableFuture<TriggerSubscriptionWebhookSettingsDTO>> concurrentCallFuture =
        new AtomicReference<>();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private PropertyRepository propertyRepository;

    private TriggerSubscriptionWebhookSettingsAccessor triggerSubscriptionWebhookSettingsAccessor;

    @BeforeEach
    void beforeEach() {
        propertyRepository.deleteAll();

        PropertyService delegatePropertyService = new PropertyServiceImpl(propertyRepository);

        PropertyService propertyService = mock(PropertyService.class, delegatesTo(delegatePropertyService));

        doAnswer(invocation -> {
            Optional<Property> result = delegatePropertyService.fetchProperty(
                invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2),
                invocation.getArgument(3));

            Supplier<TriggerSubscriptionWebhookSettingsDTO> call = concurrentCall.getAndSet(null);

            if (call != null) {
                runConcurrently(call);
            }

            return result;
        }).when(propertyService)
            .fetchProperty(eq(SIGNING_SECRET_KEY), any(), any(), any());

        triggerSubscriptionWebhookSettingsAccessor = new TriggerSubscriptionWebhookSettingsAccessor(
            propertyService, jdbcTemplate, platformTransactionManager);
    }

    @AfterEach
    void afterEach() {
        propertyRepository.deleteAll();
    }

    @Test
    void testFirstWebhookUrlSaveDoesNotReplaceASecretRotatedWhileItRuns() throws Exception {
        concurrentCall.set(() -> triggerSubscriptionWebhookSettingsAccessor.rotateSigningSecret(ENVIRONMENT));

        triggerSubscriptionWebhookSettingsAccessor.saveWebhookUrl(ENVIRONMENT, WEBHOOK_URL);

        TriggerSubscriptionWebhookSettingsDTO rotated = awaitConcurrentCall();

        assertThat(concurrentCallFinishedFirst.get()).isFalse();
        assertThat(fetchStoredSigningSecret()).isEqualTo(rotated.signingSecret());
        assertThat(rotated.webhookUrl()).isEqualTo(WEBHOOK_URL);
    }

    @Test
    void testConcurrentFirstWebhookUrlSavesShareOneSecret() throws Exception {
        concurrentCall.set(
            () -> triggerSubscriptionWebhookSettingsAccessor.saveWebhookUrl(ENVIRONMENT, WEBHOOK_URL + "/other"));

        TriggerSubscriptionWebhookSettingsDTO first = triggerSubscriptionWebhookSettingsAccessor.saveWebhookUrl(
            ENVIRONMENT, WEBHOOK_URL);

        TriggerSubscriptionWebhookSettingsDTO second = awaitConcurrentCall();

        assertThat(concurrentCallFinishedFirst.get()).isFalse();
        assertThat(second.signingSecret()).isEqualTo(first.signingSecret());
        assertThat(fetchStoredSigningSecret()).isEqualTo(first.signingSecret());
        assertThat(countProperties(SIGNING_SECRET_KEY)).isEqualTo(1);
        assertThat(countProperties(WEBHOOK_URL_KEY)).isEqualTo(1);
    }

    private TriggerSubscriptionWebhookSettingsDTO awaitConcurrentCall()
        throws ExecutionException, InterruptedException, TimeoutException {

        CompletableFuture<TriggerSubscriptionWebhookSettingsDTO> future = concurrentCallFuture.get();

        return future.get(COMPLETION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private int countProperties(String key) {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM property WHERE key = ? AND scope = ? AND environment = ?", Integer.class, key,
                Property.Scope.EMBEDDED.ordinal(), ENVIRONMENT.ordinal()));
    }

    private String fetchStoredSigningSecret() {
        Optional<Property> propertyOptional = new PropertyServiceImpl(propertyRepository).fetchProperty(
            SIGNING_SECRET_KEY, Property.Scope.EMBEDDED, null, (long) ENVIRONMENT.ordinal());

        Property property = propertyOptional.orElseThrow();

        Map<String, ?> value = property.getValue();

        return (String) value.get("signingSecret");
    }

    private void runConcurrently(Supplier<TriggerSubscriptionWebhookSettingsDTO> call) {
        CompletableFuture<TriggerSubscriptionWebhookSettingsDTO> future = CompletableFuture.supplyAsync(call);

        concurrentCallFuture.set(future);

        try {
            future.get(CONCURRENT_CALL_WAIT_SECONDS, TimeUnit.SECONDS);

            concurrentCallFinishedFirst.set(true);
        } catch (TimeoutException timeoutException) {
            concurrentCallFinishedFirst.set(false);
        } catch (ExecutionException | InterruptedException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
