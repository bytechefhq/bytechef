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

package com.bytechef.embedded.user.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.embedded.user.web.rest.model.CreateApiKey200ResponseModel;
import com.bytechef.platform.constant.AppType;
import com.bytechef.platform.user.domain.ApiKey;
import com.bytechef.platform.user.facade.ApiKeyFacade;
import com.bytechef.platform.user.service.ApiKeyService;
import com.bytechef.platform.user.web.rest.AbstractApiKeyApiController;
import com.bytechef.platform.user.web.rest.model.ApiKeyModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController("com.bytechef.embedded.user.web.rest.ApiKeyApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/internal")
@ConditionalOnCoordinator
public class ApiKeyApiController extends AbstractApiKeyApiController implements ApiKeyApi {

    private final ApiKeyFacade apiKeyFacade;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public ApiKeyApiController(
        ApiKeyFacade apiKeyFacade, ApiKeyService apiKeyService, ConversionService conversionService) {

        super(apiKeyService, conversionService);

        this.apiKeyFacade = apiKeyFacade;
        this.conversionService = conversionService;
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<CreateApiKey200ResponseModel> createApiKey(ApiKeyModel appEventModel) {
        return ResponseEntity.ok(
            new CreateApiKey200ResponseModel().secretKey(
                apiKeyFacade.create(conversionService.convert(appEventModel, ApiKey.class), AppType.EMBEDDED)));
    }
}
