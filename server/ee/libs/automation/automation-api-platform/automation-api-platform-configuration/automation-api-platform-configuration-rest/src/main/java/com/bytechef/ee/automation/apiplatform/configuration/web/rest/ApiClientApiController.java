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

package com.bytechef.ee.automation.apiplatform.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.ObfuscateUtils;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiClient;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiClientService;
import com.bytechef.ee.automation.apiplatform.configuration.web.rest.model.ApiClientModel;
import com.bytechef.ee.automation.apiplatform.configuration.web.rest.model.CreateApiClient200ResponseModel;
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
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/api-platform/internal")
@ConditionalOnCoordinator
public class ApiClientApiController implements ApiClientApi {

    private final ApiClientService apiClientService;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public ApiClientApiController(
        ApiClientService apiClientService, ConversionService conversionService) {

        this.apiClientService = apiClientService;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<CreateApiClient200ResponseModel> createApiClient(ApiClientModel apiClientModel) {
        return ResponseEntity.ok(
            new CreateApiClient200ResponseModel().secretKey(
                apiClientService.create(conversionService.convert(apiClientModel, ApiClient.class))));
    }

    @Override
    public ResponseEntity<Void> deleteApiClient(Long id) {
        apiClientService.delete(id);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<ApiClientModel> getApiClient(Long id) {
        ApiClientModel apiClientModel = conversionService.convert(
            apiClientService.getApiClient(id), ApiClientModel.class);

        return ResponseEntity.ok(apiClientModel.secretKey(obfuscate(apiClientModel.getSecretKey())));
    }

    @Override
    public ResponseEntity<List<ApiClientModel>> getApiClients() {
        return ResponseEntity.ok(
            CollectionUtils.map(
                apiClientService.getApiClients(),
                apiKey -> conversionService.convert(apiKey, ApiClientModel.class)
                    .secretKey(obfuscate(apiKey.getSecretKey()))));
    }

    @Override
    public ResponseEntity<Void> updateApiClient(Long id, ApiClientModel apiClientModel) {
        apiClientService.update(conversionService.convert(apiClientModel.id(id), ApiClient.class));

        return ResponseEntity.noContent()
            .build();
    }

    private static String obfuscate(String secretKey) {
        return ObfuscateUtils.obfuscate(secretKey, 26, 6);
    }
}
