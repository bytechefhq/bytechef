/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.dataset.domain;

import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Table("ai_eval_dataset_version")
public final class AiEvalDatasetVersion {

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate;

    @Column("dataset_id")
    private Long datasetId;

    private boolean frozen;

    @Id
    private Long id;

    private String label;

    @Column("version_number")
    private int versionNumber;

    private AiEvalDatasetVersion() {
    }

    public AiEvalDatasetVersion(Long datasetId, int versionNumber) {
        Validate.notNull(datasetId, "datasetId must not be null");
        Validate.isTrue(versionNumber > 0, "versionNumber must be positive, got: %d", versionNumber);

        this.datasetId = datasetId;
        this.versionNumber = versionNumber;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiEvalDatasetVersion aiEvalDatasetVersion)) {
            return false;
        }

        return Objects.equals(id, aiEvalDatasetVersion.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Long getDatasetId() {
        return datasetId;
    }

    public Long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setDatasetId(Long datasetId) {
        assertNotFrozen("datasetId");
        Validate.notNull(datasetId, "datasetId must not be null");

        this.datasetId = datasetId;
    }

    public void setLabel(String label) {
        assertNotFrozen("label");

        this.label = label;
    }

    public void setVersionNumber(int versionNumber) {
        assertNotFrozen("versionNumber");
        Validate.isTrue(versionNumber > 0, "versionNumber must be positive, got: %d", versionNumber);

        this.versionNumber = versionNumber;
    }

    private void assertNotFrozen(String fieldName) {
        if (frozen) {
            throw new IllegalStateException(
                "Cannot modify '" + fieldName + "' on frozen AiEvalDatasetVersion id=" + id);
        }
    }

    public void freeze() {
        this.frozen = true;
    }

    void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    @Override
    public String toString() {
        return "AiEvalDatasetVersion{" +
            "id=" + id +
            ", datasetId=" + datasetId +
            ", versionNumber=" + versionNumber +
            ", label='" + label + '\'' +
            ", frozen=" + frozen +
            '}';
    }
}
