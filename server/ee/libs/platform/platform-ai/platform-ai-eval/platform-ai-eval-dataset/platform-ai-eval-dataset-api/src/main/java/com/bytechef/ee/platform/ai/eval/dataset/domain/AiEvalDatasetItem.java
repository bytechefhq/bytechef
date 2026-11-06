/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.dataset.domain;

import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Table("ai_eval_dataset_item")
public final class AiEvalDatasetItem {

    @CreatedDate
    @Column("created_date")
    private Instant createdDate;

    @Column("dataset_id")
    private Long datasetId;

    @Column("dataset_version_id")
    private Long datasetVersionId;

    @Column("expected_output")
    private String expectedOutput;

    @Id
    private Long id;

    private String input;

    private String metadata;

    @Column("source_trace_id")
    private Long sourceTraceId;

    private AiEvalDatasetItem() {
    }

    public AiEvalDatasetItem(Long datasetId, Long datasetVersionId, String input) {
        Validate.notNull(datasetId, "datasetId must not be null");
        Validate.notNull(datasetVersionId, "datasetVersionId must not be null");
        Validate.isTrue(StringUtils.isNotBlank(input), "input must not be blank");

        this.datasetId = datasetId;
        this.datasetVersionId = datasetVersionId;
        this.input = input;
    }

    public static AiEvalDatasetItem newItem(
        Long datasetId, Long datasetVersionId, String input, String expectedOutput, String metadata) {

        AiEvalDatasetItem item = new AiEvalDatasetItem(datasetId, datasetVersionId, input);

        item.expectedOutput = expectedOutput;
        item.metadata = metadata;

        return item;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiEvalDatasetItem aiEvalDatasetItem)) {
            return false;
        }

        return Objects.equals(id, aiEvalDatasetItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Long getDatasetId() {
        return datasetId;
    }

    public Long getDatasetVersionId() {
        return datasetVersionId;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public Long getId() {
        return id;
    }

    public String getInput() {
        return input;
    }

    public String getMetadata() {
        return metadata;
    }

    public Long getSourceTraceId() {
        return sourceTraceId;
    }

    void setDatasetId(Long datasetId) {
        this.datasetId = datasetId;
    }

    void setDatasetVersionId(Long datasetVersionId) {
        this.datasetVersionId = datasetVersionId;
    }

    void setExpectedOutput(String expectedOutput) {
        assertNotPersisted("expectedOutput");

        this.expectedOutput = expectedOutput;
    }

    void setInput(String input) {
        assertNotPersisted("input");
        Validate.isTrue(StringUtils.isNotBlank(input), "input must not be blank");

        this.input = input;
    }

    void setMetadata(String metadata) {
        assertNotPersisted("metadata");

        this.metadata = metadata;
    }

    private void assertNotPersisted(String fieldName) {
        if (id != null) {
            throw new IllegalStateException(
                "Cannot mutate '" + fieldName + "' on persisted AiEvalDatasetItem id=" + id
                    + " — load the parent version, verify it is not frozen, and route the update through the service "
                    + "layer (delete-and-reinsert or stage on a fresh unfrozen version)");
        }
    }

    public void linkSourceTrace(Long traceId) {
        Validate.notNull(traceId, "traceId must not be null");

        if (this.sourceTraceId != null && !Objects.equals(this.sourceTraceId, traceId)) {
            throw new IllegalStateException(
                "AiEvalDatasetItem source trace is already linked to " + this.sourceTraceId
                    + "; cannot relink to " + traceId);
        }

        this.sourceTraceId = traceId;
    }

    void setSourceTraceId(Long sourceTraceId) {
        this.sourceTraceId = sourceTraceId;
    }

    @Override
    public String toString() {
        return "AiEvalDatasetItem{" +
            "id=" + id +
            ", datasetId=" + datasetId +
            ", datasetVersionId=" + datasetVersionId +
            ", sourceTraceId=" + sourceTraceId +
            '}';
    }
}
