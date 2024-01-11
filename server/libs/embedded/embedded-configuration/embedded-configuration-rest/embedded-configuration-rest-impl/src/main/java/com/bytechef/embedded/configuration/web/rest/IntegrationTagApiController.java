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

package com.bytechef.embedded.configuration.web.rest;

import com.bytechef.embedded.configuration.facade.IntegrationFacade;
import com.bytechef.embedded.configuration.web.rest.model.TagModel;
import com.bytechef.embedded.configuration.web.rest.model.UpdateTagsRequestModel;
import com.bytechef.tag.domain.Tag;
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
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}")
public class IntegrationTagApiController implements IntegrationTagApi {

    private final IntegrationFacade integrationFacade;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI2")
    public IntegrationTagApiController(IntegrationFacade integrationFacade, ConversionService conversionService) {
        this.integrationFacade = integrationFacade;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<List<TagModel>> getIntegrationTags() {
        return ResponseEntity.ok(
            integrationFacade.getIntegrationTags()
                .stream()
                .map(tag -> conversionService.convert(tag, TagModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<Void> updateIntegrationTags(
        Long id, UpdateTagsRequestModel updateIntegrationTagsRequestModel) {

        List<TagModel> tagModels = updateIntegrationTagsRequestModel.getTags();

        integrationFacade.update(
            id,
            tagModels.stream()
                .map(tagModel -> conversionService.convert(tagModel, Tag.class))
                .toList());

        return ResponseEntity.noContent()
            .build();
    }
}
