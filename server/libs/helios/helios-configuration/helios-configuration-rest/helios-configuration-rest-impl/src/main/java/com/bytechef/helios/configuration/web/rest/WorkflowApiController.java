
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

package com.bytechef.helios.configuration.web.rest;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;

import com.bytechef.atlas.configuration.workflow.mapper.WorkflowReader;
import com.bytechef.atlas.configuration.workflow.mapper.WorkflowResource;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.helios.configuration.constant.ProjectConstants;
import com.bytechef.helios.configuration.facade.ProjectFacade;
import com.bytechef.helios.configuration.web.rest.model.WorkflowFormatModel;
import com.bytechef.helios.configuration.web.rest.model.WorkflowModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@RestController("com.bytechef.helios.configuration.web.rest.workflowApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}")
@ConditionalOnProperty(prefix = "bytechef", name = "coordinator.enabled", matchIfMissing = true)
public class WorkflowApiController implements WorkflowApi {

    private final ConversionService conversionService;
    private final ObjectMapper objectMapper;
    private final ProjectFacade projectFacade;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public WorkflowApiController(
        ConversionService conversionService, ObjectMapper objectMapper, ProjectFacade projectFacade,
        WorkflowService workflowService) {

        this.conversionService = conversionService;
        this.objectMapper = objectMapper;
        this.projectFacade = projectFacade;
        this.workflowService = workflowService;
    }

    @Override
    public ResponseEntity<Void> deleteProjectWorkflow(Long id, String workflowId) {
        projectFacade.deleteWorkflow(id, workflowId);

        return ResponseEntity
            .noContent()
            .build();
    }

    @Override
    public ResponseEntity<WorkflowModel> duplicateWorkflow(Long id, String workflowId) {
        return ResponseEntity.ok(
            conversionService.convert(projectFacade.duplicateWorkflow(id, workflowId), WorkflowModel.class));
    }

    @Override
    public ResponseEntity<List<WorkflowModel>> getProjectWorkflows(Long id) {
        return ResponseEntity.ok(
            projectFacade.getProjectWorkflows(id)
                .stream()
                .map(workflow -> conversionService.convert(workflow, WorkflowModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<WorkflowModel> getWorkflow(String id) {
        return ResponseEntity.ok(conversionService.convert(workflowService.getWorkflow(id), WorkflowModel.class));
    }

    @Override
    public ResponseEntity<List<WorkflowModel>> getWorkflows() {
        List<WorkflowModel> workflowModels = new ArrayList<>();

        for (Workflow workflow : workflowService.getWorkflows(ProjectConstants.PROJECT_TYPE)) {
            workflowModels.add(conversionService.convert(workflow, WorkflowModel.class));
        }

        return ResponseEntity.ok(workflowModels);
    }

    @Override
    public ResponseEntity<WorkflowModel> updateWorkflow(String id, WorkflowModel workflowModel) {
        String definition = workflowModel.getDefinition();
        WorkflowFormatModel format = workflowModel.getFormat();

        Map<String, Object> sourceMap;

        try {
            sourceMap = WorkflowReader.readWorkflowMap(
                new WorkflowResource(
                    id, Map.of(), new ByteArrayResource(definition.getBytes(StandardCharsets.UTF_8)),
                    Workflow.Format.valueOf(format.name()))
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        sourceMap.put(WorkflowConstants.DESCRIPTION, workflowModel.getDescription());

        return ResponseEntity.ok(
            conversionService.convert(workflowService.update(
                id, JsonUtils.write(sourceMap, objectMapper), ), WorkflowModel.class));
    }
}
