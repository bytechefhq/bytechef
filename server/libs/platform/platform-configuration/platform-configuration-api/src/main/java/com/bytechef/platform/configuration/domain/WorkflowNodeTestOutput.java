/*
 * Copyright 2025 ByteChef
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
import com.bytechef.platform.domain.BaseProperty;
import com.bytechef.platform.domain.OutputResponse;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("workflow_node_test_output")
public class WorkflowNodeTestOutput {

    @Id
    private Long id;

    @Column("type_name")
    private String typeName;

    @Column("type_operation_name")
    private String typeOperationName;

    @Column("type_version")
    private int typeVersion;

    @Column("output_schema")
    private MapWrapper outputSchema;

    @Column("sample_output")
    private MapWrapper sampleOutput;

    @Column("workflow_node_name")
    private String workflowNodeName;

    @Column("workflow_id")
    private String workflowId;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Version
    private int version;

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

    public String getTypeName() {
        return typeName;
    }

    public String getTypeOperationName() {
        return typeOperationName;
    }

    public int getTypeVersion() {
        return typeVersion;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Long getId() {
        return id;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public OutputResponse getOutput(Class<? extends BaseProperty> typeClass) {
        return new OutputResponse(getOutputSchema(typeClass), getSampleOutput());
    }

    public BaseProperty getOutputSchema(Class<? extends BaseProperty> typeClass) {
        return MapUtils.get(outputSchema.getMap(), "outputSchema", typeClass);
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

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public void setTypeOperationName(String typeOperationName) {
        this.typeOperationName = typeOperationName;
    }

    public void setTypeVersion(int typeVersion) {
        this.typeVersion = typeVersion;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOutputSchema(BaseProperty outputSchema) {
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
            "typeName='" + typeName + '\'' +
            ", typeOperationName='" + typeOperationName + '\'' +
            ", typeVersion=" + typeVersion +
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
