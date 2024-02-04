/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.configuration.domain;

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.registry.domain.Output;
import com.bytechef.platform.component.registry.domain.Property;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("workflow_node_test_output")
public class WorkflowNodeTestOutput implements Persistable<Long> {

    @Column("component_name")
    private String componentName;

    @Column("component_operation_name")
    private String componentOperationName;

    @Column("component_version")
    private int componentVersion;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

    @Id
    private Long id;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @Column("output_schema")
    private MapWrapper outputSchema;

    @Column("sample_output")
    private MapWrapper sampleOutput;

    @Column("workflow_node_name")
    private String workflowNodeName;

    @Version
    private int version;

    @Column("workflow_id")
    private String workflowId;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WorkflowNodeTestOutput that = (WorkflowNodeTestOutput) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getComponentOperationName() {
        return componentOperationName;
    }

    public int getComponentVersion() {
        return componentVersion;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Output getOutput() {
        return new Output(getOutputSchema(), getSampleOutput());
    }

    public Property getOutputSchema() {
        return MapUtils.get(outputSchema.getMap(), "outputSchema", Property.class);
    }

    public Object getSampleOutput() {
        return MapUtils.get(sampleOutput.getMap(), "sampleOutput");
    }

    public int getVersion() {
        return version;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public String getWorkflowNodeName() {
        return workflowNodeName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public void setComponentOperationName(String componentOperationName) {
        this.componentOperationName = componentOperationName;
    }

    public void setComponentVersion(int componentVersion) {
        this.componentVersion = componentVersion;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOutputSchema(Property outputSchema) {
        this.outputSchema = new MapWrapper(Map.of("outputSchema", outputSchema));
    }

    public void setSampleOutput(Object sampleOutput) {
        this.sampleOutput = new MapWrapper(Map.of("sampleOutput", sampleOutput));
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public void setWorkflowNodeName(String workflowNodeName) {
        this.workflowNodeName = workflowNodeName;
    }

    @Override
    public String toString() {
        return "WorkflowTestNodeOutput{" +
            "componentName='" + componentName + '\'' +
            ", componentOperationName='" + componentOperationName + '\'' +
            ", componentVersion=" + componentVersion +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", id=" + id +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", outputSchema=" + outputSchema +
            ", sampleOutput=" + sampleOutput +
            ", workflowNodeName='" + workflowNodeName + '\'' +
            ", version=" + version +
            ", workflowId='" + workflowId + '\'' +
            '}';
    }
}
