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

import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.web.rest.model.TagModel;
import com.bytechef.automation.configuration.web.rest.model.UpdateTagsRequestModel;
import com.bytechef.platform.annotation.ConditionalOnEndpoint;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}")
@ConditionalOnEndpoint
public class ProjectTagApiController implements ProjectTagApi {

    private final ConversionService conversionService;
    private final ProjectFacade projectFacade;

    @SuppressFBWarnings("EI")
    public ProjectTagApiController(ConversionService conversionService, ProjectFacade projectFacade) {
        this.conversionService = conversionService;
        this.projectFacade = projectFacade;
    }

    @Override
    public ResponseEntity<List<TagModel>> getProjectTags() {
        return ResponseEntity.ok(projectFacade.getProjectTags()
            .stream()
            .map(tag -> conversionService.convert(tag, TagModel.class))
            .toList());
    }

    @Override
    public ResponseEntity<Void> updateProjectTags(Long id, UpdateTagsRequestModel updateTagsRequestModel) {
        List<TagModel> tagModels = updateTagsRequestModel.getTags();

        projectFacade.updateProjectTags(
            id,
            tagModels.stream()
                .map(tagModel -> conversionService.convert(tagModel, Tag.class))
                .toList());

        return ResponseEntity.noContent()
            .build();
    }
}
