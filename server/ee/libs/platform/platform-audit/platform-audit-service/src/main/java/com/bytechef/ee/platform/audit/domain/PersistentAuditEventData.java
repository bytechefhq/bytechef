/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.domain;

import java.util.Objects;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("persistent_audit_event_data")
public final class PersistentAuditEventData {

    @Column
    private String key;

    @Column
    private String value;

    public PersistentAuditEventData() {
    }

    public PersistentAuditEventData(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof PersistentAuditEventData that)) {
            return false;
        }

        return Objects.equals(key, that.key) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "PersistentAuditEventData{" +
            "key='" + key + '\'' +
            ", value='" + value + '\'' +
            '}';
    }
}
