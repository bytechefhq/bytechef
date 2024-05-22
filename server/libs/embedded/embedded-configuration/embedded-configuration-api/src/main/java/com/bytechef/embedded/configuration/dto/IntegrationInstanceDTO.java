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

import com.bytechef.embedded.configuration.domain.IntegrationInstance;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record IntegrationInstanceDTO(
    long connectedUserId, long connectionId, String createdBy, LocalDateTime createdDate, boolean enabled, Long id,
    String lastModifiedBy, LocalDateTime lastModifiedDate, long integrationInstanceConfigurationId,
    Set<IntegrationInstanceWorkflowDTO> integrationInstanceWorkflows, int version) {

    public IntegrationInstanceDTO(
        IntegrationInstance integrationInstance, Set<IntegrationInstanceWorkflowDTO> integrationInstanceWorkflows) {

        this(
            integrationInstance.getConnectedUserId(), integrationInstance.getConnectionId(),
            integrationInstance.getCreatedBy(), integrationInstance.getCreatedDate(), integrationInstance.isEnabled(),
            integrationInstance.getId(), integrationInstance.getLastModifiedBy(),
            integrationInstance.getLastModifiedDate(), integrationInstance.getIntegrationInstanceConfigurationId(),
            integrationInstanceWorkflows, integrationInstance.getVersion());
    }
}
