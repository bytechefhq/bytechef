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

package com.bytechef.ee.automation.apiplatform.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.web.rest.model.TagModel;
import com.bytechef.platform.tag.web.rest.model.UpdateTagsRequestModel;
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
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/internal")
@ConditionalOnCoordinator
public class ApiCollectionTagApiController implements ApiCollectionTagApi {

    private final ApiCollectionFacade apiCollectionFacade;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public ApiCollectionTagApiController(ApiCollectionFacade apiCollectionFacade, ConversionService conversionService) {
        this.conversionService = conversionService;
        this.apiCollectionFacade = apiCollectionFacade;
    }

    @Override
    public ResponseEntity<List<TagModel>> getApiCollectionTags() {
        return ResponseEntity.ok(apiCollectionFacade.getApiCollectionTags()
            .stream()
            .map(tag -> conversionService.convert(tag, TagModel.class))
            .toList());
    }

    @Override
    public ResponseEntity<Void> updateApiCollectionTags(Long id, UpdateTagsRequestModel updateTagsRequestModel) {
        List<TagModel> tagModels = updateTagsRequestModel.getTags();

        apiCollectionFacade.updateApiCollectionTags(
            id,
            tagModels.stream()
                .map(tagModel -> conversionService.convert(tagModel, Tag.class))
                .toList());

        return ResponseEntity.noContent()
            .build();
    }
}
