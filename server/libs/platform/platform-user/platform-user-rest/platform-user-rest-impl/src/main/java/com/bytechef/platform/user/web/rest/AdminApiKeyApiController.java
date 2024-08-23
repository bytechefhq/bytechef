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

package com.bytechef.platform.user.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.StringUtils;
import com.bytechef.platform.user.domain.ApiKey;
import com.bytechef.platform.user.facade.ApiKeyFacade;
import com.bytechef.platform.user.service.ApiKeyService;
import com.bytechef.platform.user.web.rest.model.AdminApiKeyModel;
import com.bytechef.platform.user.web.rest.model.CreateAdminApiKey200ResponseModel;
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
public class AdminApiKeyApiController implements AdminApiKeyApi {

    private final ApiKeyFacade apiKeyFacade;
    private final ApiKeyService apiKeyService;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public AdminApiKeyApiController(
        ApiKeyFacade apiKeyFacade, ApiKeyService apiKeyService, ConversionService conversionService) {

        this.apiKeyFacade = apiKeyFacade;
        this.apiKeyService = apiKeyService;
        this.conversionService = conversionService;
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<CreateAdminApiKey200ResponseModel> createAdminApiKey(AdminApiKeyModel adminApiKeyModel) {
        return ResponseEntity.ok(
            new CreateAdminApiKey200ResponseModel().secretKey(
                apiKeyFacade.create(conversionService.convert(adminApiKeyModel, ApiKey.class), null)));
    }

    @Override
    public ResponseEntity<Void> deleteAdminApiKey(Long id) {
        apiKeyService.delete(id);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<AdminApiKeyModel> getAdminApiKey(Long id) {
        AdminApiKeyModel adminApiKeyModel = conversionService.convert(
            apiKeyService.getApiKey(id), AdminApiKeyModel.class);

        return ResponseEntity.ok(adminApiKeyModel.secretKey(obfuscate(adminApiKeyModel.getSecretKey())));
    }

    @Override
    public ResponseEntity<List<AdminApiKeyModel>> getAdminApiKeys() {
        return ResponseEntity.ok(
            CollectionUtils.map(
                apiKeyService.getApiKeys(null),
                apiKey -> conversionService.convert(apiKey, AdminApiKeyModel.class)
                    .secretKey(obfuscate(apiKey.getSecretKey()))));
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<AdminApiKeyModel> updateAdminApiKey(Long id, AdminApiKeyModel appEventModel) {
        return ResponseEntity.ok(
            conversionService.convert(
                apiKeyService.update(conversionService.convert(appEventModel.id(id), ApiKey.class)),
                AdminApiKeyModel.class));
    }

    private static String obfuscate(String secretKey) {
        return StringUtils.obfuscate(secretKey, 26, 6);
    }
}
