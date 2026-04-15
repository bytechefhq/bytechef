/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller exposing read-only access to persistent audit events.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
public class AuditEventGraphQlController {

    private final AuditEventService auditEventService;
    private final boolean maskPrincipals;

    @SuppressFBWarnings("EI")
    public AuditEventGraphQlController(
        AuditEventService auditEventService,
        @Value("${bytechef.audit.mask-principals:false}") boolean maskPrincipals) {

        this.auditEventService = auditEventService;
        this.maskPrincipals = maskPrincipals;
    }

    @QueryMapping(name = "auditEvents")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AuditEventPage auditEvents(
        @Argument String principal, @Argument String eventType, @Argument Long fromDate, @Argument Long toDate,
        @Argument String dataSearch, @Argument Integer page, @Argument Integer size) {

        PageRequest pageRequest = PageRequest.of(
            page == null ? 0 : page, size == null ? 25 : size, Sort.by(Sort.Direction.DESC, "eventDate"));

        Page<PersistentAuditEvent> persistentPage = auditEventService.fetchAuditEvents(
            principal, eventType, toLocalDateTime(fromDate), toLocalDateTime(toDate), dataSearch, pageRequest);

        List<AuditEvent> content = persistentPage.getContent()
            .stream()
            .map(event -> toAuditEvent(event, maskPrincipals))
            .toList();

        return new AuditEventPage(
            content, persistentPage.getNumber(), persistentPage.getSize(), persistentPage.getTotalElements(),
            persistentPage.getTotalPages());
    }

    @QueryMapping(name = "auditEventTypes")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<String> auditEventTypes() {
        return auditEventService.fetchEventTypes();
    }

    private static LocalDateTime toLocalDateTime(Long epochMillis) {
        if (epochMillis == null) {
            return null;
        }

        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
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
            persistentAuditEvent.getEventDate()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli(),
            persistentAuditEvent.getEventType(),
            persistentAuditEvent.getId(),
            maskPrincipals ? maskPrincipal(principal) : principal);
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

    record AuditEvent(List<AuditEventDataEntry> data, Long eventDate, String eventType, Long id, String principal) {
    }

    record AuditEventDataEntry(String key, String value) {
    }

    record AuditEventPage(List<AuditEvent> content, int number, int size, long totalElements, int totalPages) {
    }
}
