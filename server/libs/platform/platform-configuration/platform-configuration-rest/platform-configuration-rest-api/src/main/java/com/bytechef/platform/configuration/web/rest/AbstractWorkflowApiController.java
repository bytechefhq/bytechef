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

package com.bytechef.platform.configuration.web.rest;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractWorkflowApiController {

    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public AbstractWorkflowApiController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    protected ResponseEntity<Resource> doExportWorkflow(String id) {
        Workflow workflow = workflowService.getWorkflow(id);

        ResponseEntity.BodyBuilder bodyBuilder = ResponseEntity.ok();

        bodyBuilder.contentType(MediaType.APPLICATION_OCTET_STREAM);

        Workflow.Format format = workflow.getFormat();

        String fileName = String.format(
            "%s.%s", StringUtils.isEmpty(workflow.getLabel()) ? workflow.getId() : workflow.getLabel(),
            StringUtils.lowerCase(format.name()));

        bodyBuilder.header(HttpHeaders.CONTENT_DISPOSITION, "filename=\"" + fileName + "\"");

        String definition = workflow.getDefinition();

        return bodyBuilder.body(new ByteArrayResource(definition.getBytes(StandardCharsets.UTF_8)));
    }
}
