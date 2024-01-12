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

package com.bytechef.platform.workflow.test.domain;

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.registry.domain.Property;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("workflow_test_component_definition")
public class WorkflowTestComponentDefinition implements Persistable<Long> {

    @Id
    private Long id;

    @Column("component_name")
    private String componentName;

    @Column("component_operation_name")
    private String componentOperationName;

    @Column("component_version")
    private int componentVersion;

    @Column("operation_name")
    private String operationName;

    @Column("outputSchema")
    private MapWrapper outputSchema;

    @Column("sampleOutput")
    private MapWrapper sampleOutput;

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

        WorkflowTestComponentDefinition that = (WorkflowTestComponentDefinition) o;

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

    @Override
    public Long getId() {
        return id;
    }

    public String getOperationName() {
        return operationName;
    }

    public Property getOutputSchema() {
        return MapUtils.get(outputSchema.getMap(), "outputSchema", Property.class);
    }

    public Object getSampleOutput() {
        return MapUtils.get(outputSchema.getMap(), "sampleOutput");
    }

    public int getVersion() {
        return version;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setId(Long id) {
        this.id = id;
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

    public void setOperationName(String operationName) {
        this.operationName = operationName;
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

    @Override
    public String toString() {
        return "DynamicComponentDefinition{" +
            "id=" + id +
            ", componentName='" + componentName + '\'' +
            ", componentOperationName='" + componentOperationName + '\'' +
            ", componentVersion=" + componentVersion +
            ", operationName='" + operationName + '\'' +
            ", outputSchema='" + outputSchema + '\'' +
            ", sampleOutput='" + sampleOutput + '\'' +
            ", workflowId='" + workflowId + '\'' +
            '}';
    }
}
