/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.listener;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.audit.repository.CustomAuditEventRepository;
import com.bytechef.ee.platform.audit.util.AuditPlatformUtils;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.authentication.CredentialAuthenticationSuccessEvent;
import com.bytechef.platform.security.web.authentication.CredentialType;
import com.bytechef.platform.security.web.authentication.FailureLogRateLimiter;
import com.bytechef.platform.security.web.authentication.TenantPrincipal;
import com.bytechef.platform.security.web.mcp.McpApiKeyEntity;
import com.bytechef.tenant.TenantContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.context.ApplicationListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class CredentialUsageAuditListener implements ApplicationListener<CredentialAuthenticationSuccessEvent> {

    static final String CREDENTIAL_USED = "CREDENTIAL_USED";

    static final Duration FAILURE_LOG_INTERVAL = Duration.ofMinutes(1);

    static final int MAX_TRACKED_CREDENTIALS = 10_000;

    private static final String AUTHENTICATION_TOKEN_SUFFIX = "AuthenticationToken";

    private static final Logger log = LoggerFactory.getLogger(CredentialUsageAuditListener.class);

    private final AuditEventRepository auditEventRepository;
    private final Clock clock;
    private final FailureLogRateLimiter failureLogRateLimiter;
    private final @Nullable ConcurrentMap<CredentialKey, Long> recordedCredentials;

    @Autowired
    @SuppressFBWarnings("EI")
    public CredentialUsageAuditListener(
        ApplicationProperties applicationProperties, AuditEventRepository auditEventRepository) {

        this(applicationProperties, auditEventRepository, Clock.systemUTC(), MAX_TRACKED_CREDENTIALS);
    }

    @SuppressFBWarnings("EI")
    CredentialUsageAuditListener(
        ApplicationProperties applicationProperties, AuditEventRepository auditEventRepository, Clock clock,
        long maxTrackedCredentials) {

        ApplicationProperties.Audit audit = applicationProperties.getAudit();

        Duration credentialUsageInterval = audit.getCredentialUsageInterval();

        this.auditEventRepository = auditEventRepository;
        this.clock = clock;
        this.failureLogRateLimiter = new FailureLogRateLimiter(clock, FAILURE_LOG_INTERVAL);
        this.recordedCredentials = createRecordedCredentials(credentialUsageInterval, clock, maxTrackedCredentials);
    }

    @Override
    public void onApplicationEvent(CredentialAuthenticationSuccessEvent credentialAuthenticationSuccessEvent) {
        Authentication authentication = credentialAuthenticationSuccessEvent.getAuthentication();

        String credentialType = getCredentialType(authentication);
        PlatformType platformType = getPlatformType(authentication);
        String principal = getPrincipal(authentication);
        String tenantId = getTenantId(authentication);

        Long apiKeyId = null;
        Long connectedUserId = null;
        Long environmentId = null;

        if (authentication instanceof AbstractApiKeyAuthenticationToken abstractApiKeyAuthenticationToken) {
            apiKeyId = abstractApiKeyAuthenticationToken.getApiKeyId();
            connectedUserId = abstractApiKeyAuthenticationToken.getConnectedUserId();
            environmentId = abstractApiKeyAuthenticationToken.getEnvironmentId();
        } else if (authentication.getPrincipal() instanceof McpApiKeyEntity mcpApiKeyEntity) {
            Environment environment = mcpApiKeyEntity.getEnvironment();

            apiKeyId = mcpApiKeyEntity.getApiKeyId();
            environmentId = (long) environment.ordinal();
        }

        CredentialKey credentialKey = new CredentialKey(
            tenantId, credentialType, principal, environmentId, apiKeyId, connectedUserId);

        long nowMillis = clock.millis();

        if (!markRecorded(credentialKey, nowMillis)) {
            return;
        }

        Map<String, Object> data = new LinkedHashMap<>();

        data.put("credentialType", credentialType);

        if (apiKeyId != null) {
            data.put("apiKeyId", apiKeyId);
        }

        if (connectedUserId != null) {
            data.put("connectedUserId", connectedUserId);
        }

        if (environmentId != null) {
            data.put("environmentId", environmentId);
        }

        if (platformType != null) {
            data.put(CustomAuditEventRepository.PLATFORM_DATA_KEY, platformType);
        }

        if (tenantId != null) {
            data.put(CustomAuditEventRepository.TENANT_ID_DATA_KEY, tenantId);
        }

        try {
            auditEventRepository.add(new AuditEvent(clock.instant(), principal, CREDENTIAL_USED, data));
        } catch (RuntimeException exception) {
            if (recordedCredentials != null) {
                recordedCredentials.remove(credentialKey, nowMillis);
            }

            logPersistenceFailure(principal, credentialKey, exception);
        }
    }

    private void logPersistenceFailure(String principal, CredentialKey credentialKey, RuntimeException exception) {
        OptionalLong suppressedFailures = failureLogRateLimiter.tryAcquire();

        if (suppressedFailures.isEmpty()) {
            return;
        }

        log.error(
            "AUDIT PERSISTENCE FAILURE: {} for {} was not recorded; the request is unaffected; {} similar failures " +
                "suppressed since the last report: {}",
            CREDENTIAL_USED, principal, suppressedFailures.getAsLong(), credentialKey, exception);
    }

    private boolean markRecorded(CredentialKey credentialKey, long nowMillis) {
        if (recordedCredentials == null) {
            return true;
        }

        return recordedCredentials.putIfAbsent(credentialKey, nowMillis) == null;
    }

    private static @Nullable ConcurrentMap<CredentialKey, Long> createRecordedCredentials(
        Duration credentialUsageInterval, Clock clock, long maxTrackedCredentials) {

        if (credentialUsageInterval.isZero() || credentialUsageInterval.isNegative()) {
            return null;
        }

        Cache<CredentialKey, Long> cache = Caffeine.newBuilder()
            .executor(Runnable::run)
            .expireAfterWrite(credentialUsageInterval)
            .maximumSize(maxTrackedCredentials)
            .ticker(() -> TimeUnit.MILLISECONDS.toNanos(clock.millis()))
            .build();

        return cache.asMap();
    }

    private static String getCredentialType(Authentication authentication) {
        if (authentication instanceof AbstractApiKeyAuthenticationToken abstractApiKeyAuthenticationToken) {
            CredentialType credentialType = abstractApiKeyAuthenticationToken.getCredentialType();

            return credentialType.getValue();
        }

        Class<? extends Authentication> authenticationClass = authentication.getClass();

        String simpleName = authenticationClass.getSimpleName();

        if (simpleName.endsWith(AUTHENTICATION_TOKEN_SUFFIX)) {
            return simpleName.substring(0, simpleName.length() - AUTHENTICATION_TOKEN_SUFFIX.length());
        }

        return simpleName;
    }

    private static @Nullable PlatformType getPlatformType(Authentication authentication) {
        if (authentication instanceof AbstractApiKeyAuthenticationToken abstractApiKeyAuthenticationToken) {
            return abstractApiKeyAuthenticationToken.getPlatformType();
        }

        if (authentication.getPrincipal() instanceof McpApiKeyEntity mcpApiKeyEntity) {
            return mcpApiKeyEntity.getType();
        }

        Class<? extends Authentication> authenticationClass = authentication.getClass();

        return AuditPlatformUtils.resolvePlatform(authenticationClass.getName());
    }

    private static String getPrincipal(Authentication authentication) {
        if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        return authentication.getName();
    }

    private static @Nullable String getTenantId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof TenantPrincipal tenantPrincipal) {
            return tenantPrincipal.getTenantId();
        }

        if (authentication instanceof AbstractApiKeyAuthenticationToken abstractApiKeyAuthenticationToken) {
            String tenantId = abstractApiKeyAuthenticationToken.getTenantId();

            if (tenantId != null) {
                return tenantId;
            }
        }

        String currentTenantId = TenantContext.getCurrentTenantId();

        return Objects.equals(currentTenantId, TenantContext.DEFAULT_TENANT_ID) ? null : currentTenantId;
    }

    private record CredentialKey(
        @Nullable String tenantId, String credentialType, String principal, @Nullable Long environmentId,
        @Nullable Long apiKeyId, @Nullable Long connectedUserId) {
    }
}
