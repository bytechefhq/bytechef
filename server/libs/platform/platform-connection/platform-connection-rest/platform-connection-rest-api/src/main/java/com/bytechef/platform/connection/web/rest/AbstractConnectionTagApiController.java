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

import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.constant.AppType;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.web.rest.model.TagModel;
import com.bytechef.platform.tag.web.rest.model.UpdateTagsRequestModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractConnectionTagApiController {

    private final ConnectionFacade connectionFacade;
    private final ConversionService conversionService;
    private final AppType type;

    @SuppressFBWarnings("EI")
    public AbstractConnectionTagApiController(
        ConnectionFacade connectionFacade, ConversionService conversionService, AppType type) {

        this.connectionFacade = connectionFacade;
        this.conversionService = conversionService;
        this.type = type;
    }

    protected ResponseEntity<List<TagModel>> doGetConnectionTags() {
        return ResponseEntity.ok(
            connectionFacade.getConnectionTags(type)
                .stream()
                .map(tag -> conversionService.convert(tag, TagModel.class))
                .toList());
    }

    protected ResponseEntity<Void> doUpdateConnectionTags(Long id, UpdateTagsRequestModel updateTagsRequestModel) {
        connectionFacade.update(
            id,
            updateTagsRequestModel.getTags()
                .stream()
                .map(tagModel -> conversionService.convert(tagModel, Tag.class))
                .toList());

        return ResponseEntity.noContent()
            .build();
    }
}
