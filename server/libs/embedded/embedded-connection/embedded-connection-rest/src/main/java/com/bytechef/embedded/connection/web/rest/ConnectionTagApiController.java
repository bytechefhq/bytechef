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

package com.bytechef.embedded.connection.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.web.rest.AbstractConnectionTagApiController;
import com.bytechef.platform.constant.AppType;
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
@RestController("com.bytechef.embedded.connection.web.rest.ConnectionTagApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/internal")
@ConditionalOnCoordinator
public class ConnectionTagApiController extends AbstractConnectionTagApiController implements ConnectionTagApi {

    @SuppressFBWarnings("EI")
    public ConnectionTagApiController(ConnectionFacade connectionFacade, ConversionService conversionService) {
        super(connectionFacade, conversionService, AppType.EMBEDDED);
    }

    @Override
    public ResponseEntity<List<TagModel>> getConnectionTags() {
        return doGetConnectionTags();
    }

    @Override
    public ResponseEntity<Void> updateConnectionTags(
        Long id, UpdateTagsRequestModel comBytechefPlatformTagWebRestModelUpdateTagsRequestModel) {

        return doUpdateConnectionTags(id, comBytechefPlatformTagWebRestModelUpdateTagsRequestModel);
    }
}
