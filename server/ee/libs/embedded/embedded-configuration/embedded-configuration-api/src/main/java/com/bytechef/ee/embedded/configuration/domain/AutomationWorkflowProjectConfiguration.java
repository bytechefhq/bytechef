/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.domain;

import com.bytechef.automation.configuration.domain.Project;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("automation_workflow_project_configuration")
public class AutomationWorkflowProjectConfiguration {

    @Column("automation_hub_visible")
    private boolean automationHubVisible = true;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column
    private boolean enabled;

    @Column("environment_id")
    private long environmentId;

    @Id
    private Long id;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column("project_id")
    private AggregateReference<Project, Long> projectId;

    @Column("project_version")
    private int projectVersion;

    @Version
    private int version;

    @MappedCollection(idColumn = "automation_workflow_project_configuration_id")
    private Set<AutomationWorkflowProjectConfigurationWorkflow> workflows = Collections.emptySet();

    public AutomationWorkflowProjectConfiguration() {
    }

    public AutomationWorkflowProjectConfiguration(
        long projectId, long environmentId, int projectVersion, boolean enabled, boolean automationHubVisible) {

        this.projectId = AggregateReference.to(projectId);
        this.environmentId = environmentId;
        this.projectVersion = projectVersion;
        this.enabled = enabled;
        this.automationHubVisible = automationHubVisible;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AutomationWorkflowProjectConfiguration that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public long getEnvironmentId() {
        return environmentId;
    }

    public Long getId() {
        return id;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Long getProjectId() {
        return projectId.getId();
    }

    public int getProjectVersion() {
        return projectVersion;
    }

    public int getVersion() {
        return version;
    }

    public Set<AutomationWorkflowProjectConfigurationWorkflow> getWorkflows() {
        return Collections.unmodifiableSet(workflows);
    }

    public boolean isAutomationHubVisible() {
        return automationHubVisible;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setAutomationHubVisible(boolean automationHubVisible) {
        this.automationHubVisible = automationHubVisible;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setProjectVersion(int projectVersion) {
        this.projectVersion = projectVersion;
    }

    public void setWorkflows(Set<AutomationWorkflowProjectConfigurationWorkflow> workflows) {
        this.workflows = workflows == null ? Collections.emptySet() : workflows;
    }

    /**
     * Returns the stored row of the workflow, if any. Rows are sparse: a workflow without a row is enabled and has no
     * provided connections or inputs.
     */
    public Optional<AutomationWorkflowProjectConfigurationWorkflow> fetchWorkflow(UUID workflowUuid) {
        return workflows.stream()
            .filter(workflow -> Objects.equals(workflow.getWorkflowUuid(), workflowUuid))
            .findFirst();
    }

    /**
     * Returns whether the workflow is enabled; a workflow without a stored row is enabled.
     */
    public boolean isWorkflowEnabled(UUID workflowUuid) {
        return fetchWorkflow(workflowUuid)
            .map(AutomationWorkflowProjectConfigurationWorkflow::isEnabled)
            .orElse(true);
    }

    @Override
    public String toString() {
        return "AutomationWorkflowProjectConfiguration{" +
            "id=" + id +
            ", projectId=" + projectId +
            ", environmentId=" + environmentId +
            ", projectVersion=" + projectVersion +
            ", enabled=" + enabled +
            ", automationHubVisible=" + automationHubVisible +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
