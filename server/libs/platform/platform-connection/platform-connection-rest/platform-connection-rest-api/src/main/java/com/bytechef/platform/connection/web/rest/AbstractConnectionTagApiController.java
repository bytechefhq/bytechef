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

package com.bytechef.platform.connection.web.rest;

import com.bytechef.hermes.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.web.rest.model.TagModel;
import com.bytechef.platform.connection.web.rest.model.UpdateTagsRequestModel;
import com.bytechef.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;

/**
 * @author Ivica Cardic
 */
public class AbstractConnectionTagApiController {

    private final ConnectionFacade connectionFacade;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public AbstractConnectionTagApiController(ConnectionFacade connectionFacade, ConversionService conversionService) {
        this.connectionFacade = connectionFacade;
        this.conversionService = conversionService;
    }

    protected ResponseEntity<Void> updateConnectionTags(Long id, UpdateTagsRequestModel updateTagsRequestModel) {
        List<TagModel> tagModels = updateTagsRequestModel.getTags();

        connectionFacade.update(
            id,
            tagModels.stream()
                .map(tagModel -> conversionService.convert(tagModel, Tag.class))
                .toList());

        return ResponseEntity.noContent()
            .build();
    }
}
