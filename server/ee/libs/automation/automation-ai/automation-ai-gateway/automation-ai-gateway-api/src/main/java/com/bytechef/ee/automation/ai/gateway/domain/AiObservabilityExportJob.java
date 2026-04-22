/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_observability_export_job")
public class AiObservabilityExportJob {

    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("cron_expression")
    private String cronExpression;

    @Column("error_message")
    private String errorMessage;

    @Column("file_path")
    private String filePath;

    @Column
    private String filters;

    @Column
    private int format;

    @Id
    private Long id;

    @Column("project_id")
    private Long projectId;

    @Column("record_count")
    private Integer recordCount;

    @Column
    private int scope;

    @Column
    private int status;

    @Column
    private int type;

    @Column("workspace_id")
    private Long workspaceId;

    private AiObservabilityExportJob() {
    }

    public AiObservabilityExportJob(
        Long workspaceId, AiObservabilityExportJobType type, AiObservabilityExportFormat format,
        AiObservabilityExportScope scope, String createdBy) {

        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notNull(type, "type must not be null");
        Validate.notNull(format, "format must not be null");
        Validate.notNull(scope, "scope must not be null");
        Validate.notNull(createdBy, "createdBy must not be null");

        this.createdBy = createdBy;
        this.format = format.ordinal();
        this.scope = scope.ordinal();
        this.status = AiObservabilityExportJobStatus.PENDING.ordinal();
        this.type = type.ordinal();
        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiObservabilityExportJob aiObservabilityExportJob)) {
            return false;
        }

        return Objects.equals(id, aiObservabilityExportJob.id);
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

    public String getCronExpression() {
        return cronExpression;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFilters() {
        return filters;
    }

    /**
     * Typed view of the filters JSON. The export executor should switch on the sealed subtype rather than re-parse
     * ad-hoc — a malformed filter surfaces as a parse exception at read time instead of producing an unfiltered
     * (overly-broad) export.
     */
    public AiObservabilityExportFilter getTypedFilter() {
        return AiObservabilityExportFilter.fromJson(filters);
    }

    public void setTypedFilter(AiObservabilityExportFilter filter) {
        this.filters = AiObservabilityExportFilter.toJson(filter);
    }

    public AiObservabilityExportFormat getFormat() {
        return AiObservabilityExportFormat.values()[format];
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Integer getRecordCount() {
        return recordCount;
    }

    public AiObservabilityExportScope getScope() {
        return AiObservabilityExportScope.values()[scope];
    }

    public AiObservabilityExportJobStatus getStatus() {
        return AiObservabilityExportJobStatus.values()[status];
    }

    public AiObservabilityExportJobType getType() {
        return AiObservabilityExportJobType.values()[type];
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setFilters(String filters) {
        // Round-trip through the sealed AiObservabilityExportFilter hierarchy so a malformed or unknown-discriminator
        // filter JSON fails loudly at write time, AND the stored form is the canonical normalized JSON. Without the
        // re-serialization, extra fields or casing drift survived verbatim into the row — which then no longer
        // round-tripped identically. Matches the behavior of AiObservabilityAlertRule.setFilters.
        if (filters != null && !filters.isBlank()) {
            AiObservabilityExportFilter parsed = AiObservabilityExportFilter.fromJson(filters);

            this.filters = AiObservabilityExportFilter.toJson(parsed);
        } else {
            this.filters = filters;
        }
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public void setRecordCount(Integer recordCount) {
        this.recordCount = recordCount;
    }

    public void setStatus(AiObservabilityExportJobStatus status) {
        Validate.notNull(status, "status must not be null");

        this.status = status.ordinal();
    }

    @Override
    public String toString() {
        return "AiObservabilityExportJob{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", type=" + getType() +
            ", format=" + getFormat() +
            ", scope=" + getScope() +
            ", status=" + getStatus() +
            ", createdDate=" + createdDate +
            '}';
    }
}
