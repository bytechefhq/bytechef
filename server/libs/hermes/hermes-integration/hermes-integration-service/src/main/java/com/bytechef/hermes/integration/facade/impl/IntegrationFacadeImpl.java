/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.integration.facade.impl;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.atlas.workflow.WorkflowFormat;
import com.bytechef.hermes.integration.domain.Integration;
import com.bytechef.hermes.integration.facade.IntegrationFacade;
import com.bytechef.hermes.integration.service.IntegrationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class IntegrationFacadeImpl implements IntegrationFacade {

    private final IntegrationService integrationService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public IntegrationFacadeImpl(IntegrationService integrationService, WorkflowService workflowService) {
        this.integrationService = integrationService;
        this.workflowService = workflowService;
    }

    @Override
    public Integration add(Integration integration) {
        if (!integration.containsWorkflows()) {
            Workflow workflow = new Workflow();

            workflow.setDefinition(
                    """
                {
                    "tasks": []
                }
                """);
            workflow.setFormat(WorkflowFormat.JSON);

            workflow = workflowService.add(workflow);

            integration.addWorkflow(workflow.getId());
        }

        return integrationService.add(integration);
    }
}
