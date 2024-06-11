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

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.embedded.user.web.rest.model.ApiKeyModel;
import com.bytechef.embedded.user.web.rest.model.CreateApiKey200ResponseModel;
import com.bytechef.platform.annotation.ConditionalOnEndpoint;
import com.bytechef.platform.constant.AppType;
import com.bytechef.platform.user.domain.ApiKey;
import com.bytechef.platform.user.service.ApiKeyService;
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
@ConditionalOnEndpoint
public class IntegrationApiKeyApiController implements ApiKeyApi {

    private final ApiKeyService apiKeyService;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public IntegrationApiKeyApiController(ApiKeyService apiKeyService, ConversionService conversionService) {
        this.apiKeyService = apiKeyService;
        this.conversionService = conversionService;
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<CreateApiKey200ResponseModel> createApiKey(ApiKeyModel appEventModel) {
        ApiKey apiKey = conversionService.convert(appEventModel, ApiKey.class);

        apiKey.setType(AppType.EMBEDDED);

        return ResponseEntity.ok(new CreateApiKey200ResponseModel().secretKey(apiKeyService.create(apiKey)));
    }

    @Override
    public ResponseEntity<Void> deleteApiKey(Long id) {
        apiKeyService.delete(id);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<ApiKeyModel> getApiKey(Long id) {
        ApiKeyModel apiKeyModel = conversionService.convert(apiKeyService.getApiKey(id), ApiKeyModel.class);

        return ResponseEntity.ok(apiKeyModel.secretKey(abbreviateSecretKey(apiKeyModel.getSecretKey())));
    }

    @Override
    public ResponseEntity<List<ApiKeyModel>> getApiKeys() {
        return ResponseEntity.ok(
            CollectionUtils.map(
                apiKeyService.getApiKeys(AppType.EMBEDDED),
                apiKey -> {
                    ApiKeyModel apiKeyModel = conversionService.convert(apiKey, ApiKeyModel.class);

                    return apiKeyModel.secretKey(abbreviateSecretKey(apiKeyModel.getSecretKey()));
                }));
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<ApiKeyModel> updateApiKey(Long id, ApiKeyModel appEventModel) {
        return ResponseEntity.ok(
            conversionService.convert(
                apiKeyService.update(conversionService.convert(appEventModel.id(id), ApiKey.class)),
                ApiKeyModel.class));
    }

    private String abbreviateSecretKey(String secretKey) {
        return ".".repeat(28) + secretKey.substring(secretKey.length() - 4);
    }
}
