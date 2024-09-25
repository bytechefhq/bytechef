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

package com.bytechef.platform.codeworkflow.configuration.automation.public_.web.rest;

import com.bytechef.platform.codeworkflow.configuration.automation.facade.ProjectCodeWorkflowFacade;
import com.bytechef.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/v1")
public class ProjectCodeWorkflowApiController implements ProjectCodeWorkflowApi {

    private final ProjectCodeWorkflowFacade projectCodeWorkflowFacade;

    @SuppressFBWarnings("EI")
    public ProjectCodeWorkflowApiController(ProjectCodeWorkflowFacade projectCodeWorkflowFacade) {

        this.projectCodeWorkflowFacade = projectCodeWorkflowFacade;
    }

    @Override
    public ResponseEntity<Void> deployProject(Long projectId, MultipartFile projectFile) {
        try {
            projectCodeWorkflowFacade.save(
                projectId, projectFile.getBytes(),
                Language.of(Objects.requireNonNull(projectFile.getOriginalFilename())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.noContent()
            .build();
    }
}
