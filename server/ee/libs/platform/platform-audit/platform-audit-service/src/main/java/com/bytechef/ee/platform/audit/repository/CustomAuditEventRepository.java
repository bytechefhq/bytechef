/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.repository;

import com.bytechef.commons.util.LocalDateTimeUtils;
import com.bytechef.ee.platform.audit.AuditEventConverter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.web.authentication.TenantWebAuthenticationDetails;
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

    /**
     * Should be the same as in Liquibase migration.
     */
    protected static final int EVENT_DATA_COLUMN_MAX_LENGTH = 255;

    protected static final int PRINCIPAL_COLUMN_MAX_LENGTH = 256;

    private final PersistenceAuditEventRepository persistenceAuditEventRepository;

    private final AuditEventConverter auditEventConverter;

    private final @Nullable TransactionTemplate requiresNewTransactionTemplate;

    private final ObjectProvider<TenantService> tenantServiceProvider;

    private static final Logger log = LoggerFactory.getLogger(CustomAuditEventRepository.class);

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public CustomAuditEventRepository(
        PersistenceAuditEventRepository persistenceAuditEventRepository, AuditEventConverter auditEventConverter,
        ObjectProvider<TenantService> tenantServiceProvider,
        ObjectProvider<PlatformTransactionManager> transactionManagerProvider) {

        this.persistenceAuditEventRepository = persistenceAuditEventRepository;
        this.auditEventConverter = auditEventConverter;
        this.tenantServiceProvider = tenantServiceProvider;

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

        Map<String, String> eventData = auditEventConverter.convertDataToStrings(event.getData());

        persistentAuditEvent.setData(truncate(eventData));

        String tenantId = resolveTenantId(event);

        if (tenantId == null) {
            save(persistentAuditEvent);
        } else {
            TenantContext.runWithTenantId(tenantId, () -> save(persistentAuditEvent));
        }
    }

    private @Nullable String resolveTenantId(AuditEvent event) {
        if (!Objects.equals(TenantContext.getCurrentTenantId(), TenantContext.DEFAULT_TENANT_ID)) {
            return null;
        }

        try {
            TenantService tenantService = tenantServiceProvider.getIfAvailable();

            if (tenantService == null || !tenantService.isMultiTenantEnabled()) {
                return null;
            }

            String detailsTenantId = getDetailsTenantId(event.getData(), tenantService);

            if (detailsTenantId != null) {
                return detailsTenantId;
            }

            return getPrincipalTenantId(event.getPrincipal(), tenantService);
        } catch (RuntimeException exception) {
            log.warn("Unable to resolve the tenant of an audit event principal", exception);

            return null;
        }
    }

    private static @Nullable String getDetailsTenantId(Map<String, Object> data, TenantService tenantService) {
        if (data.get(DETAILS) instanceof TenantWebAuthenticationDetails tenantWebAuthenticationDetails) {
            String tenantId = tenantWebAuthenticationDetails.getTenantId();

            if (tenantId != null && !Objects.equals(tenantId, TenantContext.DEFAULT_TENANT_ID)) {
                List<String> tenantIds = tenantService.getTenantIds();

                if (tenantIds.contains(tenantId)) {
                    return tenantId;
                }
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

        if (tenantIds == null || tenantIds.isEmpty()) {
            return null;
        }

        return tenantIds.getFirst();
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
