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

package com.bytechef.embedded.configuration.facade;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.embedded.configuration.dto.IntegrationInstanceDTO;
import com.bytechef.embedded.configuration.dto.IntegrationInstanceWorkflowDTO;
import com.bytechef.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.embedded.configuration.service.IntegrationInstanceService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class IntegrationInstanceFacadeImpl implements IntegrationInstanceFacade {

    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationInstanceService integrationInstanceService;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceFacadeImpl(
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceService integrationInstanceService) {

        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationInstanceService = integrationInstanceService;
    }

    @Override
    public void enableIntegrationInstance(long integrationInstanceId, boolean enable) {
        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            integrationInstanceId);

        Set<IntegrationInstanceWorkflow> integrationInstanceWorkflows = integrationInstance
            .getIntegrationInstanceWorkflows();

        for (IntegrationInstanceWorkflow integrationInstanceWorkflow : integrationInstanceWorkflows) {
            if (!integrationInstanceWorkflow.isEnabled()) {
                continue;
            }

            if (enable) {
                enableWorkflowTriggers(integrationInstanceWorkflow);
            } else {
                disableWorkflowTriggers(integrationInstanceWorkflow);
            }
        }

        integrationInstanceService.updateEnabled(integrationInstanceId, enable);
    }

    @Override
    public void enableIntegrationInstanceWorkflow(long integrationInstanceId, String workflowId, boolean enable) {
        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            integrationInstanceId);

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                integrationInstance.getIntegrationInstanceConfigurationId(), workflowId);

        IntegrationInstanceWorkflow integrationInstanceWorkflow = integrationInstanceService.updateWorkflowEnabled(
            integrationInstanceId, integrationInstanceConfigurationWorkflow.getId(), enable);

        if (integrationInstance.isEnabled()) {
            if (enable) {
                enableWorkflowTriggers(integrationInstanceWorkflow);
            } else {
                disableWorkflowTriggers(integrationInstanceWorkflow);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrationInstanceDTO getIntegrationInstance(long id) {
        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(id);

        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                integrationInstance.getIntegrationInstanceConfigurationId());

        return new IntegrationInstanceDTO(
            integrationInstance,
            integrationInstance.getIntegrationInstanceWorkflows()
                .stream()
                .map(integrationInstanceWorkflow -> new IntegrationInstanceWorkflowDTO(
                    integrationInstanceWorkflow,
                    CollectionUtils.getFirstFilter(
                        integrationInstanceConfigurationWorkflows,
                        integrationInstanceConfigurationWorkflow -> Objects.equals(
                            integrationInstanceConfigurationWorkflow.getId(),
                            integrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId()),
                        IntegrationInstanceConfigurationWorkflow::getWorkflowId)))
                .collect(Collectors.toSet()));
    }

    private void disableWorkflowTriggers(IntegrationInstanceWorkflow integrationInstanceWorkflow) {
        // TODO
    }

    private void enableWorkflowTriggers(IntegrationInstanceWorkflow integrationInstanceWorkflow) {
        // TODO
    }
}
