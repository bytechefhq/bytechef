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

package com.bytechef.platform.security.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.StringUtils;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.rest.model.ApiKeyModel;
import com.bytechef.platform.security.web.rest.model.CreateApiKey200ResponseModel;
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
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
@ConditionalOnCoordinator
public class ApiKeyApiController implements ApiKeyApi {

    private final ApiKeyFacade apiKeyFacade;
    private final ApiKeyService apiKeyService;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public ApiKeyApiController(
        ApiKeyFacade apiKeyFacade, ApiKeyService apiKeyService, ConversionService conversionService) {

        this.apiKeyFacade = apiKeyFacade;
        this.apiKeyService = apiKeyService;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<CreateApiKey200ResponseModel> createApiKey(ApiKeyModel apiKeyModel) {
        return ResponseEntity.ok(
            new CreateApiKey200ResponseModel().secretKey(
                apiKeyFacade.create(conversionService.convert(apiKeyModel, ApiKey.class), null)));
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
        ApiKeyModel adminApiKeyModel = conversionService.convert(
            apiKeyService.getApiKey(id), ApiKeyModel.class);

        return ResponseEntity.ok(adminApiKeyModel.secretKey(obfuscate(adminApiKeyModel.getSecretKey())));
    }

    @Override
    public ResponseEntity<List<ApiKeyModel>> getApiKeys(Long environmentId) {
        return ResponseEntity.ok(
            CollectionUtils.map(
                apiKeyService.getApiKeys(environmentId),
                apiKey -> conversionService.convert(apiKey, ApiKeyModel.class)
                    .secretKey(obfuscate(apiKey.getSecretKey()))));
    }

    @Override
    public ResponseEntity<Void> updateApiKey(Long id, ApiKeyModel appEventModel) {
        apiKeyService.update(conversionService.convert(appEventModel.id(id), ApiKey.class));

        return ResponseEntity.noContent()
            .build();
    }

    private static String obfuscate(String secretKey) {
        return StringUtils.obfuscate(secretKey, 26, 6);
    }
}
