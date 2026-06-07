/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.domain;

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Domain class representing a single record stored in the Context Store replica. Records are touched on every sync run;
 * concurrency is constrained by Spring Batch's per-source job-instance uniqueness rather than optimistic locking, hence
 * no {@code @Version} field.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Table("context_store_record")
public class ContextStoreRecord {

    @Id
    private Long id;

    @Column("source_id")
    private Long sourceId;

    @Column("source_record_id")
    private String sourceRecordId;

    @Column
    private MapWrapper payload;

    @Column("payload_hash")
    private String payloadHash;

    @Column("last_seen_at")
    private Instant lastSeenAt;

    @Column("deleted_at")
    @Nullable
    private Instant deletedAt;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate;

    @LastModifiedDate
    @Column("last_modified_date")
    private Instant lastModifiedDate;

    public ContextStoreRecord() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceRecordId() {
        return sourceRecordId;
    }

    public void setSourceRecordId(String sourceRecordId) {
        this.sourceRecordId = sourceRecordId;
    }

    public Map<String, ?> getPayload() {
        return payload.getMap();
    }

    public void setPayload(Map<String, ?> payload) {
        this.payload = new MapWrapper(Objects.requireNonNull(payload, "payload"));
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public void setPayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    @Nullable
    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(@Nullable Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ContextStoreRecord that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
