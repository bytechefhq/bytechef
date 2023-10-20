
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

package com.bytechef.dione.configuration.web.rest;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.dione.configuration.facade.IntegrationFacade;
import com.bytechef.dione.configuration.web.rest.model.WorkflowModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@RestController("com.bytechef.dione.configuration.web.rest.workflowApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}")
public class WorkflowApiController implements WorkflowApi {

    private final IntegrationFacade integrationFacade;
    private final ConversionService conversionService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public WorkflowApiController(
        IntegrationFacade integrationFacade, ConversionService conversionService, WorkflowService workflowService) {
        this.integrationFacade = integrationFacade;

        this.conversionService = conversionService;
        this.workflowService = workflowService;
    }

    @Override
    public ResponseEntity<Void> deleteWorkflow(String id) {
        workflowService.delete(id);

        return ResponseEntity
            .noContent()
            .build();
    }

    @Override
    public ResponseEntity<List<WorkflowModel>> getIntegrationWorkflows(Long id) {
        return ResponseEntity.ok(
            integrationFacade.getIntegrationWorkflows(id)
                .stream()
                .map(workflow -> conversionService.convert(workflow, WorkflowModel.class))
                .toList());
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<WorkflowModel> getWorkflow(String id) {
        return ResponseEntity.ok(conversionService.convert(workflowService.getWorkflow(id), WorkflowModel.class)
            .definition(null));
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<List<WorkflowModel>> getWorkflows() {
        List<WorkflowModel> workflowModels = new ArrayList<>();

        for (Workflow workflow : workflowService.getWorkflows()) {
            workflowModels.add(
                conversionService.convert(workflow, WorkflowModel.class)
                    .definition(null));
        }

        return ResponseEntity.ok(workflowModels);
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<WorkflowModel> updateWorkflow(String id, WorkflowModel workflowModel) {
        return ResponseEntity.ok(
            conversionService.convert(workflowService.update(id, workflowModel.getDefinition()), WorkflowModel.class));
    }
}
