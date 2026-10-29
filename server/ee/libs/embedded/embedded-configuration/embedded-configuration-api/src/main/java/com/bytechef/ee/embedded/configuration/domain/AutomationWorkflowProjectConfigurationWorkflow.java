/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.domain;

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("automation_workflow_project_configuration_workflow")
public class AutomationWorkflowProjectConfigurationWorkflow {

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column
    private boolean enabled;

    @Id
    private Long id;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column("provided_connections")
    private MapWrapper providedConnections = new MapWrapper();

    @Column("provided_inputs")
    private MapWrapper providedInputs = new MapWrapper();

    @Version
    private int version;

    @Column("workflow_uuid")
    private UUID workflowUuid;

    public AutomationWorkflowProjectConfigurationWorkflow() {
    }

    public AutomationWorkflowProjectConfigurationWorkflow(UUID workflowUuid, boolean enabled) {
        this.workflowUuid = workflowUuid;
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AutomationWorkflowProjectConfigurationWorkflow that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Long getId() {
        return id;
    }

    /**
     * Returns the connections the configuration provides to every connected user, keyed by the component name of the
     * workflow's connection slot.
     */
    public Map<String, Long> getProvidedConnections() {
        if (providedConnections == null) {
            return Map.of();
        }

        Map<String, ?> providedConnectionMap = providedConnections.getMap();
        Map<String, Long> connectionIds = new HashMap<>();

        for (Map.Entry<String, ?> entry : providedConnectionMap.entrySet()) {
            if (entry.getValue() instanceof Number connectionId) {
                connectionIds.put(entry.getKey(), connectionId.longValue());
            }
        }

        return Collections.unmodifiableMap(connectionIds);
    }

    /**
     * Returns the input values the configuration provides to every connected user, keyed by the workflow input name.
     */
    public Map<String, Object> getProvidedInputs() {
        if (providedInputs == null) {
            return Map.of();
        }

        return Collections.unmodifiableMap(new HashMap<>(providedInputs.getMap()));
    }

    public int getVersion() {
        return version;
    }

    public UUID getWorkflowUuid() {
        return workflowUuid;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setProvidedConnections(Map<String, Long> providedConnections) {
        this.providedConnections = new MapWrapper(providedConnections == null ? Map.of() : providedConnections);
    }

    public void setProvidedInputs(Map<String, ?> providedInputs) {
        this.providedInputs = new MapWrapper(providedInputs == null ? Map.of() : providedInputs);
    }

    @Override
    public String toString() {
        return "AutomationWorkflowProjectConfigurationWorkflow{" +
            "id=" + id +
            ", workflowUuid=" + workflowUuid +
            ", enabled=" + enabled +
            ", providedConnections=" + providedConnections +
            ", providedInputs=" + providedInputs +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
