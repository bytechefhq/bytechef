/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.workflow.web;

import com.integri.atlas.engine.coordinator.annotation.ConditionalOnCoordinator;
import com.integri.atlas.engine.coordinator.workflow.Workflow;
import com.integri.atlas.engine.coordinator.workflow.repository.WorkflowRepository;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

/**
 * @author Arik Cohen
 */
@RestController
@ConditionalOnCoordinator
public class WorkflowsController {

    @Autowired
    private WorkflowRepository workflowRepository;

    @PostMapping(path = "/workflows", consumes = { "application/json", "application/yaml" })
    public ResponseEntity create(@RequestBody String content, @RequestHeader("Content-Type") String contentType) {
        Workflow workflow = workflowRepository.create(content, contentType.substring(contentType.lastIndexOf('/') + 1));

        return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(workflow);
    }

    @PutMapping(path = "/workflows/{id}", consumes = { "application/json", "application/yaml" })
    public ResponseEntity update(
        @PathVariable String id,
        @RequestBody String content,
        @RequestHeader("Content-Type") String contentType
    ) {
        Workflow workflow = workflowRepository.update(
            id,
            content,
            contentType.substring(contentType.lastIndexOf('/') + 1)
        );

        return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(workflow);
    }

    @GetMapping("/workflows")
    public List<Workflow> list() {
        return workflowRepository.findAll();
    }

    @GetMapping("/workflows/**")
    public Workflow get(HttpServletRequest aRequest) {
        String path = (String) aRequest.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String workflowId = path.replaceFirst("/workflows/", "");
        return workflowRepository.findOne(workflowId);
    }
}
