/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.LocalDateTimeUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.facade.AuditEventFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller exposing read-only access to persistent audit events.
 *
 * <p>
 * Authorization is enforced on {@link AuditEventFacade}, not here.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
public class AuditEventGraphQlController {

    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final int MAX_PAGE_SIZE = 200;

    private final AuditEventFacade auditEventFacade;
    private final boolean maskPrincipals;

    @SuppressFBWarnings("EI")
    public AuditEventGraphQlController(
        ApplicationProperties applicationProperties, AuditEventFacade auditEventFacade) {

        ApplicationProperties.Audit audit = applicationProperties.getAudit();

        this.auditEventFacade = auditEventFacade;
        this.maskPrincipals = audit.isMaskPrincipals();
    }

    @QueryMapping(name = "auditEvents")
    public AuditEventPage auditEvents(
        @Argument String principal, @Argument String eventType, @Argument Long fromDate, @Argument Long toDate,
        @Argument String dataSearch, @Argument Integer page, @Argument Integer size) {

        PageRequest pageRequest = PageRequest.of(
            page == null ? 0 : Math.max(0, page),
            size == null ? DEFAULT_PAGE_SIZE : Math.clamp(size, 1, MAX_PAGE_SIZE));

        AuditEventFilter auditEventFilter = new AuditEventFilter(
            principal, eventType, toLocalDateTime(fromDate), toLocalDateTime(toDate), dataSearch);

        Page<PersistentAuditEvent> persistentPage = auditEventFacade.fetchAuditEvents(auditEventFilter, pageRequest);

        List<AuditEvent> content = persistentPage.getContent()
            .stream()
            .map(event -> toAuditEvent(event, maskPrincipals))
            .toList();

        return new AuditEventPage(
            content, persistentPage.getNumber(), persistentPage.getSize(), persistentPage.getTotalElements(),
            persistentPage.getTotalPages());
    }

    @QueryMapping(name = "auditEventTypes")
    public List<String> auditEventTypes() {
        return auditEventFacade.fetchEventTypes();
    }

    private static LocalDateTime toLocalDateTime(Long epochMillis) {
        if (epochMillis == null) {
            return null;
        }

        // Event dates are written as zone-less timestamps in the JVM default zone (LocalDateTime.now() in the aspect
        // and
        // the job status listener, LocalDateTimeUtils in the actuator repository), so epoch millis are converted with
        // that same zone.
        return LocalDateTimeUtils.toLocalDateTime(Instant.ofEpochMilli(epochMillis));
    }

    private static AuditEvent toAuditEvent(PersistentAuditEvent persistentAuditEvent, boolean maskPrincipals) {
        Map<String, String> dataMap = persistentAuditEvent.getData();

        List<AuditEventDataEntry> data = dataMap.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> new AuditEventDataEntry(entry.getKey(), entry.getValue()))
            .toList();

        String principal = persistentAuditEvent.getPrincipal();

        return new AuditEvent(
            data,
            LocalDateTimeUtils.getTime(persistentAuditEvent.getEventDate()),
            persistentAuditEvent.getEventType(),
            persistentAuditEvent.getId(),
            toOutcome(dataMap.get("result")),
            maskPrincipals ? maskPrincipal(principal) : principal);
    }

    private static @Nullable AuditEventOutcome toOutcome(@Nullable String result) {
        if (result == null) {
            return null;
        }

        for (AuditEventOutcome auditEventOutcome : AuditEventOutcome.values()) {
            if (auditEventOutcome.name()
                .equals(result)) {

                return auditEventOutcome;
            }
        }

        return null;
    }

    static String maskPrincipal(String principal) {
        if (principal == null || principal.isBlank()) {
            return principal;
        }

        int atIndex = principal.indexOf('@');

        if (atIndex <= 0) {
            return principal.charAt(0) + "***";
        }

        return principal.charAt(0) + "***" + principal.substring(atIndex);
    }

    record AuditEvent(
        List<AuditEventDataEntry> data, Long eventDate, String eventType, Long id, @Nullable AuditEventOutcome outcome,
        String principal) {
    }

    record AuditEventDataEntry(String key, String value) {
    }

    enum AuditEventOutcome {
        ALLOWED, DENIED, ERROR, ROLLED_BACK, SUCCESS
    }

    record AuditEventPage(List<AuditEvent> content, int number, int size, long totalElements, int totalPages) {
    }
}
