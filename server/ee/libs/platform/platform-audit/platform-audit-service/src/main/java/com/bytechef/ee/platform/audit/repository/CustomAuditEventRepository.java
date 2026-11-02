/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.repository;

import com.bytechef.commons.util.LocalDateTimeUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.audit.AuditEventConverter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.TenantWebAuthenticationDetails;
import com.bytechef.platform.security.web.authentication.UnresolvedCredentialAuthenticationToken;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * An implementation of Spring Boot's AuditEventRepository.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
@ConditionalOnEEVersion
public class CustomAuditEventRepository implements AuditEventRepository {

    private static final String AUTHORIZATION_FAILURE = "AUTHORIZATION_FAILURE";
    private static final String DETAILS = "details";
    public static final String ANONYMOUS_USER = "anonymoususer";
    public static final String PLATFORM_DATA_KEY = CustomAuditEventRepository.class.getName() + ".platform";
    public static final String TENANT_ID_DATA_KEY = CustomAuditEventRepository.class.getName() + ".tenantId";

    /**
     * Should be the same as in Liquibase migration.
     */
    protected static final int EVENT_DATA_COLUMN_MAX_LENGTH = 256;

    protected static final int PRINCIPAL_COLUMN_MAX_LENGTH = 256;

    private final PersistenceAuditEventRepository persistenceAuditEventRepository;

    private final AuditEventConverter auditEventConverter;

    private final @Nullable TransactionTemplate requiresNewTransactionTemplate;

    private final ObjectProvider<TenantService> tenantServiceProvider;

    private final @Nullable String systemUsername;

    private static final Logger log = LoggerFactory.getLogger(CustomAuditEventRepository.class);

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public CustomAuditEventRepository(
        PersistenceAuditEventRepository persistenceAuditEventRepository, AuditEventConverter auditEventConverter,
        ObjectProvider<TenantService> tenantServiceProvider,
        ObjectProvider<PlatformTransactionManager> transactionManagerProvider,
        ObjectProvider<ApplicationProperties> applicationPropertiesProvider) {

        this.persistenceAuditEventRepository = persistenceAuditEventRepository;
        this.auditEventConverter = auditEventConverter;
        this.tenantServiceProvider = tenantServiceProvider;
        this.systemUsername = getSystemUsername(applicationPropertiesProvider.getIfAvailable());

        PlatformTransactionManager transactionManager = transactionManagerProvider.getIfAvailable();

        if (transactionManager == null) {
            this.requiresNewTransactionTemplate = null;
        } else {
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

            this.requiresNewTransactionTemplate = transactionTemplate;
        }
    }

    @Override
    public List<AuditEvent> find(String principal, Instant after, String type) {
        Iterable<PersistentAuditEvent> persistentAuditEvents;

        if (principal == null && after == null && type == null) {
            persistentAuditEvents = persistenceAuditEventRepository.findAll();
        } else if (after == null && type == null) {
            persistentAuditEvents = persistenceAuditEventRepository.findByPrincipal(principal);
        } else if (type == null) {
            persistentAuditEvents = persistenceAuditEventRepository.findByPrincipalAndEventDateAfter(principal,
                LocalDateTimeUtils.toLocalDateTime(after));
        } else {
            persistentAuditEvents = persistenceAuditEventRepository.findByPrincipalAndEventDateAfterAndEventType(
                principal, LocalDateTimeUtils.toLocalDateTime(after), type);
        }

        return auditEventConverter.convertToAuditEvent(persistentAuditEvents);
    }

    @Override
    public void add(AuditEvent event) {
        if (AUTHORIZATION_FAILURE.equals(event.getType()) || ANONYMOUS_USER.equals(event.getPrincipal())) {
            return;
        }

        PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

        persistentAuditEvent.setEventDate(LocalDateTimeUtils.toLocalDateTime(event.getTimestamp()));
        persistentAuditEvent.setEventType(event.getType());
        persistentAuditEvent.setPrincipal(StringUtils.truncate(event.getPrincipal(), PRINCIPAL_COLUMN_MAX_LENGTH));

        Map<String, Object> data = new HashMap<>(event.getData());

        Object eventTenantId = data.remove(TENANT_ID_DATA_KEY);

        if (data.remove(PLATFORM_DATA_KEY) instanceof PlatformType platformType) {
            persistentAuditEvent.setPlatform(platformType);
        }

        Map<String, String> eventData = auditEventConverter.convertDataToStrings(data);

        persistentAuditEvent.setData(truncate(eventData));

        String tenantId =
            resolveTenantId(event, eventTenantId instanceof String tenantIdString ? tenantIdString : null);

        if (tenantId == null) {
            save(persistentAuditEvent);
        } else {
            TenantContext.runWithTenantId(tenantId, () -> save(persistentAuditEvent));
        }
    }

    private @Nullable String resolveTenantId(AuditEvent event, @Nullable String eventTenantId) {
        if (!Objects.equals(TenantContext.getCurrentTenantId(), TenantContext.DEFAULT_TENANT_ID)) {
            return null;
        }

        try {
            TenantService tenantService = tenantServiceProvider.getIfAvailable();

            if (tenantService == null || !tenantService.isMultiTenantEnabled()) {
                return null;
            }

            if (Objects.equals(eventTenantId, TenantContext.DEFAULT_TENANT_ID)) {
                return null;
            }

            List<String> tenantIds = tenantService.getTenantIds();

            if (eventTenantId != null && tenantIds.contains(eventTenantId)) {
                return eventTenantId;
            }

            String detailsTenantId = getDetailsTenantId(event.getData(), tenantIds);

            if (detailsTenantId != null) {
                return detailsTenantId;
            }

            String principal = event.getPrincipal();

            if (Objects.equals(principal, systemUsername) ||
                Objects.equals(principal, UnresolvedCredentialAuthenticationToken.PRINCIPAL)) {

                return null;
            }

            return getPrincipalTenantId(principal, tenantService);
        } catch (RuntimeException exception) {
            log.warn("Unable to resolve the tenant of an audit event principal", exception);

            return null;
        }
    }

    private static @Nullable String getDetailsTenantId(Map<String, Object> data, List<String> tenantIds) {
        if (data.get(DETAILS) instanceof TenantWebAuthenticationDetails tenantWebAuthenticationDetails) {
            String tenantId = tenantWebAuthenticationDetails.getTenantId();

            if (tenantId != null && !Objects.equals(tenantId, TenantContext.DEFAULT_TENANT_ID) &&
                tenantIds.contains(tenantId)) {

                return tenantId;
            }
        }

        return null;
    }

    private static @Nullable String getPrincipalTenantId(String principal, TenantService tenantService) {
        if (StringUtils.isBlank(principal)) {
            return null;
        }

        EmailValidator emailValidator = EmailValidator.getInstance();

        List<String> tenantIds = emailValidator.isValid(principal)
            ? tenantService.getTenantIdsByUserEmail(principal)
            : tenantService.getTenantIdsByUserLogin(principal.toLowerCase(Locale.ENGLISH));

        if (tenantIds == null || tenantIds.size() != 1) {
            return null;
        }

        return tenantIds.getFirst();
    }

    private static @Nullable String getSystemUsername(@Nullable ApplicationProperties applicationProperties) {
        if (applicationProperties == null) {
            return null;
        }

        ApplicationProperties.Security security = applicationProperties.getSecurity();

        if (security == null) {
            return null;
        }

        ApplicationProperties.Security.System system = security.getSystem();

        String username = system.getUsername();

        return StringUtils.isBlank(username) ? null : username;
    }

    private void save(PersistentAuditEvent persistentAuditEvent) {
        if (requiresNewTransactionTemplate == null) {
            persistenceAuditEventRepository.save(persistentAuditEvent);
        } else {
            requiresNewTransactionTemplate.executeWithoutResult(
                transactionStatus -> persistenceAuditEventRepository.save(persistentAuditEvent));
        }
    }

    /**
     * Truncate event data that might exceed column length.
     */
    private Map<String, String> truncate(Map<String, String> data) {
        Map<String, String> results = new HashMap<>();

        if (data != null) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                String value = entry.getValue();

                if (value != null) {
                    int length = value.length();
                    if (length > EVENT_DATA_COLUMN_MAX_LENGTH) {
                        value = value.substring(0, EVENT_DATA_COLUMN_MAX_LENGTH);

                        log.warn(
                            "Event data for {} too long ({}) has been truncated to {}. " +
                                "Consider increasing column width.",
                            entry.getKey(), length,
                            EVENT_DATA_COLUMN_MAX_LENGTH);
                    }
                }

                results.put(entry.getKey(), value);
            }
        }

        return results;
    }
}
