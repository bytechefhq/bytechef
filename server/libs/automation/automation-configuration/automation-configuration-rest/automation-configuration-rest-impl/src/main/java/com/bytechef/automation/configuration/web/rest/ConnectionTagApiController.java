/*
 * Copyright 2025 ByteChef
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

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.web.rest.model.TagModel;
import com.bytechef.automation.configuration.web.rest.model.UpdateTagsRequestModel;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.constant.PlatformType;
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
@RestController("com.bytechef.automation.configuration.web.rest.ConnectionTagApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/internal")
@ConditionalOnCoordinator
public class ConnectionTagApiController implements ConnectionTagApi {

    private final ConnectionFacade connectionFacade;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public ConnectionTagApiController(ConnectionFacade connectionFacade, ConversionService conversionService) {
        this.connectionFacade = connectionFacade;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<List<TagModel>> getConnectionTags() {
        return ResponseEntity.ok(
            connectionFacade.getConnectionTags(PlatformType.AUTOMATION)
                .stream()
                .map(tag -> conversionService.convert(tag, TagModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<Void> updateConnectionTags(Long id, UpdateTagsRequestModel updateTagsRequestModel) {
        List<Tag> tags = updateTagsRequestModel.getTags()
            .stream()
            .map(tagModel -> conversionService.convert(tagModel, Tag.class))
            .toList();

        connectionFacade.update(id, tags);

        return ResponseEntity.noContent()
            .build();
    }
}
