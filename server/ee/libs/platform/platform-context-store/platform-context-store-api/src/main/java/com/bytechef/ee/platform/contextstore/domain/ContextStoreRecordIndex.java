/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Sidecar typed-column index row for a single indexed field of a {@link ContextStoreRecord}. One row per (record,
 * field_name) tuple; only the value-column matching the field's declared type is populated.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Table("context_store_record_index")
public class ContextStoreRecordIndex {

    @Id
    private Long id;

    @Column("record_id")
    private Long recordId;

    @Column("field_name")
    private String fieldName;

    @Column("value_text")
    @Nullable
    private String valueText;

    @Column("value_numeric")
    @Nullable
    private BigDecimal valueNumeric;

    @Column("value_timestamp")
    @Nullable
    private Instant valueTimestamp;

    public ContextStoreRecordIndex() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    @Nullable
    public String getValueText() {
        return valueText;
    }

    public void setValueText(@Nullable String valueText) {
        this.valueText = valueText;
    }

    @Nullable
    public BigDecimal getValueNumeric() {
        return valueNumeric;
    }

    public void setValueNumeric(@Nullable BigDecimal valueNumeric) {
        this.valueNumeric = valueNumeric;
    }

    @Nullable
    public Instant getValueTimestamp() {
        return valueTimestamp;
    }

    public void setValueTimestamp(@Nullable Instant valueTimestamp) {
        this.valueTimestamp = valueTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ContextStoreRecordIndex that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
