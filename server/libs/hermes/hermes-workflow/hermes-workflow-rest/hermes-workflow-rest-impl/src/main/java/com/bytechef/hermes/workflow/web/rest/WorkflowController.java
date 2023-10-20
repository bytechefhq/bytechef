
/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.hermes.workflow.web.rest;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.hermes.workflow.dto.WorkflowDTO;
import com.bytechef.hermes.workflow.executor.WorkflowExecutor;
import com.bytechef.hermes.workflow.facade.WorkflowFacade;
import com.bytechef.hermes.workflow.web.rest.model.WorkflowFormatModel;
import com.bytechef.hermes.workflow.web.rest.model.WorkflowModel;
import com.bytechef.hermes.workflow.web.rest.model.WorkflowResponseModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@RestController

@RequestMapping("${openapi.openAPIDefinition.base-path:}/core")
public class WorkflowController implements WorkflowsApi {

    private final ConversionService conversionService;
    private final WorkflowFacade workflowFacade;
    private final WorkflowService workflowService;
    private final WorkflowExecutor testWorkflowExecutor;

    @SuppressFBWarnings("EI2")
    public WorkflowController(
        ConversionService conversionService, WorkflowFacade workflowFacade, WorkflowService workflowService,
        WorkflowExecutor testWorkflowExecutor) {

        this.conversionService = conversionService;
        this.workflowFacade = workflowFacade;
        this.workflowService = workflowService;
        this.testWorkflowExecutor = testWorkflowExecutor;
    }

    @Override
    public ResponseEntity<Void> deleteWorkflow(String id) {
        workflowService.delete(id);

        return ResponseEntity
            .noContent()
            .build();
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<WorkflowModel> getWorkflow(String id) {
        return ResponseEntity.ok(conversionService.convert(workflowFacade.getWorkflow(id), WorkflowModel.class)
            .definition(null));
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<List<WorkflowModel>> getWorkflows() {
        List<WorkflowModel> workflowModels = new ArrayList<>();

        for (WorkflowDTO workflowDTO : workflowFacade.getWorkflows()) {
            workflowModels.add(
                conversionService.convert(workflowDTO, WorkflowModel.class)
                    .definition(null));
        }

        return ResponseEntity.ok(workflowModels);
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<WorkflowModel> createWorkflow(WorkflowModel workflowModel) {
        WorkflowFormatModel workflowFormatModel = workflowModel.getFormat();
        WorkflowModel.SourceTypeEnum sourceTypeEnum = workflowModel.getSourceType();

        return ResponseEntity.ok(
            conversionService.convert(
                workflowFacade.create(
                    workflowModel.getDefinition(),
                    Workflow.Format.valueOf(workflowFormatModel.name()),
                    Workflow.SourceType.valueOf(sourceTypeEnum.name())),
                WorkflowModel.class));
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<WorkflowResponseModel> testWorkflow(String id, Map<String, Object> inputs) {
        return ResponseEntity.ok(
            conversionService.convert(testWorkflowExecutor.execute(id, inputs), WorkflowResponseModel.class));
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<WorkflowModel> updateWorkflow(String id, WorkflowModel workflowModel) {
        return ResponseEntity.ok(
            conversionService.convert(workflowFacade.update(id, workflowModel.getDefinition()), WorkflowModel.class));
    }
}
