/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.domain;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table(name = "persistent_audit_event")
public class PersistentAuditEvent {

    @MappedCollection(idColumn = "persistent_audit_event_id")
    private Set<PersistentAuditEventData> data = new HashSet<>();

    @Column("event_date")
    private LocalDateTime eventDate;

    @Column("event_type")
    private String eventType;

    @Id
    private Long id;

    @Column
    private String principal;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof PersistentAuditEvent)) {
            return false;
        }

        return id != null && id.equals(((PersistentAuditEvent) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Map<String, String> getData() {
        return data.stream()
            .collect(Collectors.toMap(PersistentAuditEventData::getKey, PersistentAuditEventData::getValue));
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public String getEventType() {
        return eventType;
    }

    public Long getId() {
        return id;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setData(Map<String, String> data) {
        this.data = data.entrySet()
            .stream()
            .filter(entry -> entry.getValue() != null)
            .map(entry -> new PersistentAuditEventData(entry.getKey(), entry.getValue()))
            .collect(Collectors.toSet());
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    @Override
    public String toString() {
        return "PersistentAuditEvent{" +
            "id=" + id +
            ", principal='" + principal + '\'' +
            ", eventDate=" + eventDate +
            ", eventType='" + eventType + '\'' +
            ", data=" + data +
            '}';
    }
}
