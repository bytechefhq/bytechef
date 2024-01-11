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

package com.bytechef.embedded.configuration.dto;

import com.bytechef.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceWorkflowConnection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record IntegrationInstanceWorkflowDTO(
    List<IntegrationInstanceWorkflowConnection> connections, String createdBy, LocalDateTime createdDate,
    Map<String, ?> inputs, boolean enabled, Long id, LocalDateTime lastExecutionDate, String lastModifiedBy,
    LocalDateTime lastModifiedDate, Long integrationInstanceId, int version, String workflowId)
    implements Comparable<IntegrationInstanceWorkflowDTO> {

    public IntegrationInstanceWorkflowDTO(IntegrationInstanceWorkflow integrationInstanceWorkflow) {
        this(integrationInstanceWorkflow, null);
    }

    public IntegrationInstanceWorkflowDTO(
        IntegrationInstanceWorkflow integrationInstanceWorkflow, LocalDateTime lastExecutionDate) {

        this(
            integrationInstanceWorkflow.getConnections(), integrationInstanceWorkflow.getCreatedBy(),
            integrationInstanceWorkflow.getCreatedDate(), integrationInstanceWorkflow.getInputs(),
            integrationInstanceWorkflow.isEnabled(), integrationInstanceWorkflow.getIntegrationInstanceId(),
            lastExecutionDate, integrationInstanceWorkflow.getLastModifiedBy(),
            integrationInstanceWorkflow.getLastModifiedDate(), integrationInstanceWorkflow.getIntegrationInstanceId(),
            integrationInstanceWorkflow.getVersion(), integrationInstanceWorkflow.getWorkflowId());
    }

    @Override
    public int compareTo(IntegrationInstanceWorkflowDTO integrationInstanceWorkflowDTO) {
        return workflowId.compareTo(integrationInstanceWorkflowDTO.workflowId);
    }

    public IntegrationInstanceWorkflow toIntegrationInstanceWorkflow() {
        IntegrationInstanceWorkflow integrationInstanceWorkflow = new IntegrationInstanceWorkflow();

        integrationInstanceWorkflow.setConnections(connections);
        integrationInstanceWorkflow.setEnabled(enabled);
        integrationInstanceWorkflow.setId(id);
        integrationInstanceWorkflow.setInputs(inputs);
        integrationInstanceWorkflow.setVersion(version);
        integrationInstanceWorkflow.setWorkflowId(workflowId);

        return integrationInstanceWorkflow;
    }
}
