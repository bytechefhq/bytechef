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

package com.bytechef.platform.configuration.web.rest;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.annotation.ConditionalOnEndpoint;
import com.bytechef.platform.configuration.domain.ApiKey;
import com.bytechef.platform.configuration.service.ApiKeyService;
import com.bytechef.platform.configuration.web.rest.model.ApiKeyModel;
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
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}")
@ConditionalOnEndpoint
public class ApiKeyApiController implements ApiKeyApi {

    private final ApiKeyService apiKeyService;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public ApiKeyApiController(ApiKeyService apiKeyService, ConversionService conversionService) {
        this.apiKeyService = apiKeyService;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<ApiKeyModel> createApiKey(ApiKeyModel appEventModel) {
        return ResponseEntity.ok(
            conversionService.convert(
                apiKeyService.create(
                    conversionService.convert(appEventModel, ApiKey.class)),
                ApiKeyModel.class));
    }

    @Override
    public ResponseEntity<Void> deleteApiKey(Long id) {
        apiKeyService.delete(id);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<ApiKeyModel> getApiKey(Long id) {
        return ResponseEntity.ok(conversionService.convert(apiKeyService.getApiKey(id), ApiKeyModel.class));
    }

    @Override
    public ResponseEntity<List<ApiKeyModel>> getApiKeys() {
        return ResponseEntity.ok(
            CollectionUtils.map(
                apiKeyService.getApiKeys(), signingKey -> conversionService.convert(
                    signingKey, ApiKeyModel.class)));
    }

    @Override
    public ResponseEntity<ApiKeyModel> updateApiKey(Long id, ApiKeyModel appEventModel) {
        return ResponseEntity.ok(
            conversionService.convert(
                apiKeyService.update(conversionService.convert(appEventModel, ApiKey.class)), ApiKeyModel.class));
    }
}
