/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.trigger.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.dto.TriggerSubscriptionWebhookSettingsDTO;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.tenant.TenantContext;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class TriggerSubscriptionWebhookSettingsAccessorTest {

    private static final String SIGNING_SECRET_KEY = "triggerSubscriptionSigningSecret";
    private static final String WEBHOOK_URL_KEY = "triggerSubscriptionWebhookUrl";

    private final Long environmentId = (long) Environment.DEVELOPMENT.ordinal();
    private JdbcTemplate jdbcTemplate;
    private PropertyService propertyService;
    private TriggerSubscriptionWebhookSettingsAccessor service;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate = mock(JdbcTemplate.class);
        propertyService = mock(PropertyService.class);

        service = new TriggerSubscriptionWebhookSettingsAccessor(
            propertyService, jdbcTemplate, mock(PlatformTransactionManager.class));

        mockProperty(SIGNING_SECRET_KEY, null);
        mockProperty(WEBHOOK_URL_KEY, null);
    }

    @Test
    void testGetSettingsWhenAbsentReturnsNulls() {
        TriggerSubscriptionWebhookSettingsDTO settings = service.getSettings(Environment.DEVELOPMENT);

        assertThat(settings.webhookUrl()).isNull();
        assertThat(settings.signingSecret()).isNull();
    }

    @Test
    void testGetSettingsReadsTheUrlAndTheSecretFromTheirOwnProperties() {
        mockProperty(SIGNING_SECRET_KEY, Map.of("signingSecret", "whsec_stored"));
        mockProperty(WEBHOOK_URL_KEY, Map.of("webhookUrl", "https://example.com/hook"));

        TriggerSubscriptionWebhookSettingsDTO settings = service.getSettings(Environment.DEVELOPMENT);

        assertThat(settings.webhookUrl()).isEqualTo("https://example.com/hook");
        assertThat(settings.signingSecret()).isEqualTo("whsec_stored");
    }

    @Test
    void testSaveWebhookUrlGeneratesSecretOnce() {
        TriggerSubscriptionWebhookSettingsDTO first = service.saveWebhookUrl(
            Environment.DEVELOPMENT, "https://example.com/hook");

        assertThat(first.webhookUrl()).isEqualTo("https://example.com/hook");
        assertThat(first.signingSecret()).startsWith("whsec_");
        assertThat(first.signingSecret()).hasSizeGreaterThan(40);

        mockProperty(SIGNING_SECRET_KEY, Map.of("signingSecret", first.signingSecret()));
        mockProperty(WEBHOOK_URL_KEY, Map.of("webhookUrl", first.webhookUrl()));

        TriggerSubscriptionWebhookSettingsDTO second = service.saveWebhookUrl(
            Environment.DEVELOPMENT, "https://example.com/other");

        assertThat(second.webhookUrl()).isEqualTo("https://example.com/other");
        assertThat(second.signingSecret()).isEqualTo(first.signingSecret());

        verify(propertyService, times(1)).save(
            eq(SIGNING_SECRET_KEY), any(), eq(Property.Scope.EMBEDDED), isNull(), eq(environmentId));
        verify(propertyService).save(
            WEBHOOK_URL_KEY, Map.of("webhookUrl", "https://example.com/other"), Property.Scope.EMBEDDED, null,
            environmentId);
    }

    @Test
    void testSaveWebhookUrlDoesNotWriteAnExistingSecret() {
        mockProperty(SIGNING_SECRET_KEY, Map.of("signingSecret", "whsec_rotated"));

        TriggerSubscriptionWebhookSettingsDTO settings = service.saveWebhookUrl(
            Environment.DEVELOPMENT, "https://example.com/hook");

        assertThat(settings.signingSecret()).isEqualTo("whsec_rotated");

        verify(propertyService, never()).save(
            eq(SIGNING_SECRET_KEY), any(), any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testRotateSigningSecretWritesOnlyTheSecret() {
        mockProperty(SIGNING_SECRET_KEY, Map.of("signingSecret", "whsec_old"));
        mockProperty(WEBHOOK_URL_KEY, Map.of("webhookUrl", "https://example.com/hook"));

        TriggerSubscriptionWebhookSettingsDTO rotated = service.rotateSigningSecret(Environment.DEVELOPMENT);

        assertThat(rotated.webhookUrl()).isEqualTo("https://example.com/hook");
        assertThat(rotated.signingSecret()).startsWith("whsec_");
        assertThat(rotated.signingSecret()).isNotEqualTo("whsec_old");

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);

        verify(propertyService).save(
            eq(SIGNING_SECRET_KEY), captor.capture(), eq(Property.Scope.EMBEDDED), isNull(), eq(environmentId));
        verify(propertyService, never()).save(eq(WEBHOOK_URL_KEY), any(), any(), any(), any());

        assertThat(captor.getValue()).containsExactlyEntriesOf(Map.of("signingSecret", rotated.signingSecret()));
    }

    @Test
    void testSaveWebhookUrlWithABlankValueClearsTheUrlAndKeepsTheSecret() {
        mockProperty(SIGNING_SECRET_KEY, Map.of("signingSecret", "whsec_stored"));
        mockProperty(WEBHOOK_URL_KEY, Map.of("webhookUrl", "https://example.com/hook"));

        TriggerSubscriptionWebhookSettingsDTO settings = service.saveWebhookUrl(Environment.DEVELOPMENT, " ");

        assertThat(settings.webhookUrl()).isNull();
        assertThat(settings.signingSecret()).isEqualTo("whsec_stored");

        verify(propertyService).delete(WEBHOOK_URL_KEY, Property.Scope.EMBEDDED, null, environmentId);
        verify(propertyService, never()).save(any(), any(), any(), any(), any());
    }

    @Test
    void testSaveWebhookUrlWithNullClearsTheUrl() {
        TriggerSubscriptionWebhookSettingsDTO settings = service.saveWebhookUrl(Environment.DEVELOPMENT, null);

        assertThat(settings.webhookUrl()).isNull();

        verify(propertyService).delete(WEBHOOK_URL_KEY, Property.Scope.EMBEDDED, null, environmentId);
    }

    @Test
    void testSaveWebhookUrlRejectsNonHttpUrl() {
        assertThatThrownBy(() -> service.saveWebhookUrl(Environment.DEVELOPMENT, "ftp://x"))
            .isInstanceOf(IllegalArgumentException.class);

        verify(propertyService, never()).save(
            any(), any(), any(), any(), any());
    }

    @Test
    void testSaveWebhookUrlAndRotateSigningSecretTakeTheEnvironmentLock() {
        long lockKey = TriggerSubscriptionWebhookSettingsAccessor.getLockKey(TenantContext.DEFAULT_TENANT_ID,
            Environment.DEVELOPMENT);

        service.saveWebhookUrl(Environment.DEVELOPMENT, "https://example.com/hook");

        verify(jdbcTemplate).queryForList("SELECT pg_advisory_xact_lock(?)", lockKey);

        reset(jdbcTemplate);

        service.rotateSigningSecret(Environment.DEVELOPMENT);

        verify(jdbcTemplate).queryForList("SELECT pg_advisory_xact_lock(?)", lockKey);

        reset(jdbcTemplate);

        service.getSettings(Environment.DEVELOPMENT);

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void testSaveWebhookUrlTakesALockOfTheCurrentTenant() {
        long tenant1LockKey = TriggerSubscriptionWebhookSettingsAccessor.getLockKey("tenant1", Environment.DEVELOPMENT);
        long tenant2LockKey = TriggerSubscriptionWebhookSettingsAccessor.getLockKey("tenant2", Environment.DEVELOPMENT);

        try {
            TenantContext.setCurrentTenantId("tenant1");

            service.saveWebhookUrl(Environment.DEVELOPMENT, "https://example.com/hook");

            TenantContext.setCurrentTenantId("tenant2");

            service.saveWebhookUrl(Environment.DEVELOPMENT, "https://example.com/hook");
        } finally {
            TenantContext.resetCurrentTenantId();
        }

        assertThat(tenant1LockKey).isNotEqualTo(tenant2LockKey);

        verify(jdbcTemplate).queryForList("SELECT pg_advisory_xact_lock(?)", tenant1LockKey);
        verify(jdbcTemplate).queryForList("SELECT pg_advisory_xact_lock(?)", tenant2LockKey);
    }

    @Test
    void testLockKeyDiffersPerEnvironment() {
        assertThat(TriggerSubscriptionWebhookSettingsAccessor.getLockKey(TenantContext.DEFAULT_TENANT_ID,
            Environment.DEVELOPMENT))
                .isEqualTo(TriggerSubscriptionWebhookSettingsAccessor.getLockKey(TenantContext.DEFAULT_TENANT_ID,
                    Environment.DEVELOPMENT))
                .isNotEqualTo(TriggerSubscriptionWebhookSettingsAccessor.getLockKey(TenantContext.DEFAULT_TENANT_ID,
                    Environment.PRODUCTION));
    }

    @Test
    void testLockKeyDiffersPerTenant() {
        assertThat(TriggerSubscriptionWebhookSettingsAccessor.getLockKey("tenant1", Environment.DEVELOPMENT))
            .isEqualTo(TriggerSubscriptionWebhookSettingsAccessor.getLockKey("tenant1", Environment.DEVELOPMENT))
            .isNotEqualTo(TriggerSubscriptionWebhookSettingsAccessor.getLockKey("tenant2", Environment.DEVELOPMENT));
    }

    private void mockProperty(String key, Map<String, Object> value) {
        if (value == null) {
            when(propertyService.fetchProperty(key, Property.Scope.EMBEDDED, null, environmentId))
                .thenReturn(Optional.empty());

            return;
        }

        Property property = mock(Property.class);

        doReturn(value).when(property)
            .getValue();

        when(propertyService.fetchProperty(key, Property.Scope.EMBEDDED, null, environmentId))
            .thenReturn(Optional.of(property));
    }
}
