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

package com.bytechef.automation.configuration.web.rest;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.web.rest.model.WorkflowModel;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.annotation.ConditionalOnEndpoint;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController("com.bytechef.automation.configuration.web.rest.WorkflowApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}")
@ConditionalOnEndpoint
public class WorkflowApiController implements WorkflowApi {

    private final ConversionService conversionService;
    private final ProjectFacade projectFacade;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public WorkflowApiController(
        ConversionService conversionService, ProjectFacade projectFacade, WorkflowService workflowService) {

        this.conversionService = conversionService;
        this.projectFacade = projectFacade;
        this.workflowService = workflowService;
    }

    @Override
    public ResponseEntity<WorkflowModel> createProjectWorkflow(Long id, WorkflowModel workflowModel) {
        return ResponseEntity.ok(
            conversionService.convert(
                projectFacade.addWorkflow(id, workflowModel.getDefinition()), WorkflowModel.class));
    }

    @Override
    public ResponseEntity<Void> deleteWorkflow(String workflowId) {
        projectFacade.deleteWorkflow(workflowId);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<String> duplicateWorkflow(Long id, String workflowId) {
        return ResponseEntity.ok(projectFacade.duplicateWorkflow(id, workflowId));
    }

    @GetMapping("/workflows/{id}/export")
    @ResponseBody
    public ResponseEntity<Resource> exportWorkflow(@PathVariable("id") String id) {
        Workflow workflow = workflowService.getWorkflow(id);

        ResponseEntity.BodyBuilder bodyBuilder = ResponseEntity.ok();

        bodyBuilder.contentType(MediaType.APPLICATION_OCTET_STREAM);

        Workflow.Format format = workflow.getFormat();

        String fileName = String.format(
            "%s.%s", StringUtils.isEmpty(workflow.getLabel()) ? workflow.getId() : workflow.getLabel(),
            StringUtils.lowerCase(format.name()));

        bodyBuilder.header(
            HttpHeaders.CONTENT_DISPOSITION, "filename=\"" + fileName + "\"");

        String definition = workflow.getDefinition();

        return bodyBuilder.body(new ByteArrayResource(definition.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public ResponseEntity<WorkflowModel> getProjectWorkflow(Long projectWorkflowId) {
        return ResponseEntity.ok(
            conversionService.convert(projectFacade.getProjectWorkflow(projectWorkflowId), WorkflowModel.class));
    }

    @Override
    public ResponseEntity<List<WorkflowModel>> getProjectWorkflows(Long id) {
        return ResponseEntity.ok(
            CollectionUtils.map(
                projectFacade.getProjectWorkflows(id),
                workflow -> conversionService.convert(workflow, WorkflowModel.class)));
    }

    @Override
    public ResponseEntity<List<WorkflowModel>> getProjectVersionWorkflows(Long id, Integer projectVersion) {
        return ResponseEntity.ok(
            CollectionUtils.map(
                projectFacade.getProjectVersionWorkflows(id, projectVersion),
                workflow -> conversionService.convert(workflow, WorkflowModel.class)));
    }

    @Override
    public ResponseEntity<WorkflowModel> getWorkflow(String id) {
        // TODO Add check regarding platform type

        return ResponseEntity.ok(conversionService.convert(projectFacade.getProjectWorkflow(id), WorkflowModel.class));
    }

    @Override
    public ResponseEntity<WorkflowModel> updateWorkflow(String id, WorkflowModel workflowModel) {
        // TODO Add check regarding platform type

        return ResponseEntity.ok(
            conversionService.convert(
                projectFacade.updateWorkflow(id, workflowModel.getDefinition(), workflowModel.getVersion()),
                WorkflowModel.class));
    }
}
