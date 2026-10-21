/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.domain;

import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Which app started which workflow run, under the public run id the app polls with.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("app_workflow_run")
public class AppWorkflowRun {

    @Column("app_id")
    private AggregateReference<App, Long> appId;

    @Column("created_date")
    private Instant createdDate;

    @Id
    private Long id;

    @Column("job_id")
    private Long jobId;

    @Column("run_id")
    private String runId;

    @Column("workflow_key")
    private String workflowKey;

    public AppWorkflowRun() {
    }

    public Long getAppId() {
        return appId.getId();
    }

    public void setAppId(Long appId) {
        this.appId = AggregateReference.to(appId);
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getWorkflowKey() {
        return workflowKey;
    }

    public void setWorkflowKey(String workflowKey) {
        this.workflowKey = workflowKey;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AppWorkflowRun that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
