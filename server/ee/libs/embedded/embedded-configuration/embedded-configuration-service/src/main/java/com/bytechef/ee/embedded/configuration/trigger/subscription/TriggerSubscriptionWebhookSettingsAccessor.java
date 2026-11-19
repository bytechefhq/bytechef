/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.trigger.subscription;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.ee.embedded.configuration.dto.TriggerSubscriptionWebhookSettingsDTO;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Stores, per environment, the destination URL and signing secret used to deliver trigger subscription events. A
 * property-backed settings helper for facades and the event sender, not a service: it reads and writes the settings
 * through {@link PropertyService}. Saving the URL and rotating the secret take a transaction-scoped advisory lock per
 * tenant and environment before they read, so a first-time URL save never replaces a secret rotated while it runs and
 * two first-time saves never generate two secrets. Advisory locks are shared by the whole database, so the key includes
 * the tenant: tenants keep their settings in their own schemas and must not wait on each other.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class TriggerSubscriptionWebhookSettingsAccessor {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final String LOCK_KEY_PREFIX = "trigger-subscription-webhook-settings:";
    private static final String SECRET_PREFIX = "whsec_";
    private static final int SECRET_BYTE_COUNT = 32;
    private static final String SIGNING_SECRET = "signingSecret";
    private static final String SIGNING_SECRET_KEY = "triggerSubscriptionSigningSecret";
    private static final String WEBHOOK_URL = "webhookUrl";
    private static final String WEBHOOK_URL_KEY = "triggerSubscriptionWebhookUrl";

    private final JdbcTemplate jdbcTemplate;
    private final PropertyService propertyService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final TransactionTemplate transactionTemplate;

    @SuppressFBWarnings("EI")
    public TriggerSubscriptionWebhookSettingsAccessor(
        PropertyService propertyService, JdbcTemplate jdbcTemplate,
        PlatformTransactionManager platformTransactionManager) {

        this.jdbcTemplate = jdbcTemplate;
        this.propertyService = propertyService;
        this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
    }

    public TriggerSubscriptionWebhookSettingsDTO getSettings(Environment environment) {
        return new TriggerSubscriptionWebhookSettingsDTO(
            fetchValue(WEBHOOK_URL_KEY, WEBHOOK_URL, environment),
            fetchValue(SIGNING_SECRET_KEY, SIGNING_SECRET, environment));
    }

    /**
     * Saves the environment webhook URL, or clears it when the URL is null or blank. The URL and the signing secret are
     * stored as separate properties, so saving the URL never overwrites a secret rotated at the same time; a secret is
     * generated only when none exists yet.
     */
    public TriggerSubscriptionWebhookSettingsDTO saveWebhookUrl(Environment environment, @Nullable String webhookUrl) {
        return Objects.requireNonNull(
            transactionTemplate.execute(status -> doSaveWebhookUrl(environment, webhookUrl)));
    }

    public TriggerSubscriptionWebhookSettingsDTO rotateSigningSecret(Environment environment) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> doRotateSigningSecret(environment)));
    }

    static long getLockKey(String tenantId, Environment environment) {
        MessageDigest messageDigest;

        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException(noSuchAlgorithmException);
        }

        String lockName = LOCK_KEY_PREFIX + tenantId + ":" + environment.ordinal();

        byte[] digest = messageDigest.digest(lockName.getBytes(StandardCharsets.UTF_8));

        ByteBuffer digestBuffer = ByteBuffer.wrap(digest);

        return digestBuffer.getLong();
    }

    private TriggerSubscriptionWebhookSettingsDTO doRotateSigningSecret(Environment environment) {
        lock(environment);

        String signingSecret = generateSigningSecret();

        save(SIGNING_SECRET_KEY, SIGNING_SECRET, signingSecret, environment);

        return new TriggerSubscriptionWebhookSettingsDTO(
            fetchValue(WEBHOOK_URL_KEY, WEBHOOK_URL, environment), signingSecret);
    }

    private TriggerSubscriptionWebhookSettingsDTO doSaveWebhookUrl(
        Environment environment, @Nullable String webhookUrl) {

        lock(environment);

        String signingSecret = fetchValue(SIGNING_SECRET_KEY, SIGNING_SECRET, environment);

        if (webhookUrl == null || webhookUrl.isBlank()) {
            propertyService.delete(WEBHOOK_URL_KEY, Property.Scope.EMBEDDED, null, (long) environment.ordinal());

            return new TriggerSubscriptionWebhookSettingsDTO(null, signingSecret);
        }

        validateWebhookUrl(webhookUrl);

        if (signingSecret == null) {
            signingSecret = generateSigningSecret();

            save(SIGNING_SECRET_KEY, SIGNING_SECRET, signingSecret, environment);
        }

        save(WEBHOOK_URL_KEY, WEBHOOK_URL, webhookUrl, environment);

        return new TriggerSubscriptionWebhookSettingsDTO(webhookUrl, signingSecret);
    }

    @Nullable
    private String fetchValue(String key, String valueKey, Environment environment) {
        Optional<Property> propertyOptional = propertyService.fetchProperty(
            key, Property.Scope.EMBEDDED, null, (long) environment.ordinal());

        if (propertyOptional.isEmpty()) {
            return null;
        }

        Property property = propertyOptional.get();

        Map<String, ?> value = property.getValue();

        return (String) value.get(valueKey);
    }

    private String generateSigningSecret() {
        byte[] bytes = new byte[SECRET_BYTE_COUNT];

        secureRandom.nextBytes(bytes);

        return SECRET_PREFIX + EncodingUtils.urlEncodeBase64ToString(bytes);
    }

    private void lock(Environment environment) {
        jdbcTemplate.queryForList(
            "SELECT pg_advisory_xact_lock(?)", getLockKey(TenantContext.getCurrentTenantId(), environment));
    }

    private void save(String key, String valueKey, String value, Environment environment) {
        propertyService.save(
            key, Map.of(valueKey, value), Property.Scope.EMBEDDED, null, (long) environment.ordinal());
    }

    private static void validateWebhookUrl(String webhookUrl) {
        URI uri;

        try {
            uri = new URI(webhookUrl);
        } catch (URISyntaxException uriSyntaxException) {
            throw new IllegalArgumentException("Webhook URL is not a valid URL", uriSyntaxException);
        }

        String scheme = uri.getScheme();

        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT)) || uri.getHost() == null) {
            throw new IllegalArgumentException("Webhook URL must be an absolute http or https URL");
        }
    }
}
