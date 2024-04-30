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

package com.bytechef.embedded.configuration.domain;

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("integration_instance_workflow")
public class IntegrationInstanceWorkflow {

    @Column("integration_instance_configuration_workflow_id")
    private AggregateReference<IntegrationInstanceConfigurationWorkflow, Long> integrationInstanceConfigurationWorkflowId;

    @Column
    private MapWrapper inputs;

    @Column
    private boolean enabled;

    public IntegrationInstanceWorkflow(
        long integrationInstanceConfigurationWorkflowId, Map<String, ?> inputs, boolean enabled) {

        this.integrationInstanceConfigurationWorkflowId = AggregateReference.to(
            integrationInstanceConfigurationWorkflowId);
        this.inputs = new MapWrapper(inputs);
        this.enabled = enabled;
    }

    public Long getIntegrationInstanceConfigurationWorkflowId() {
        return integrationInstanceConfigurationWorkflowId.getId();
    }

    public Map<String, ?> getInputs() {
        return inputs.getMap();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setInputs(Map<String, ?> inputs) {
        this.inputs = new MapWrapper(inputs);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof IntegrationInstanceWorkflow that)) {
            return false;
        }

        return enabled == that.enabled &&
            Objects.equals(
                integrationInstanceConfigurationWorkflowId, that.integrationInstanceConfigurationWorkflowId)
            &&
            Objects.equals(inputs, that.inputs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(integrationInstanceConfigurationWorkflowId, inputs, enabled);
    }

    @Override
    public String toString() {
        return "IntegrationInstanceWorkflow{" +
            "integrationInstanceConfigurationWorkflowId=" + integrationInstanceConfigurationWorkflowId.getId() +
            ", inputs=" + inputs +
            ", enabled=" + enabled +
            '}';
    }
}
