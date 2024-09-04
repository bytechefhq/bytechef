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

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.StringUtils;
import com.bytechef.platform.constant.AppType;
import com.bytechef.platform.user.domain.ApiKey;
import com.bytechef.platform.user.service.ApiKeyService;
import com.bytechef.platform.user.web.rest.model.ApiKeyModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractApiKeyApiController {

    private final ApiKeyService apiKeyService;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public AbstractApiKeyApiController(ApiKeyService apiKeyService, ConversionService conversionService) {
        this.apiKeyService = apiKeyService;
        this.conversionService = conversionService;
    }

    protected ResponseEntity<Void> doDeleteApiKey(Long id) {
        apiKeyService.delete(id);

        return ResponseEntity.ok()
            .build();
    }

    @SuppressFBWarnings("NP")
    protected ResponseEntity<ApiKeyModel> doGetApiKey(Long id) {
        ApiKeyModel apiKeyModel = conversionService.convert(apiKeyService.getApiKey(id), ApiKeyModel.class);

        return ResponseEntity.ok(apiKeyModel.secretKey(obfuscate(apiKeyModel.getSecretKey())));
    }

    protected ResponseEntity<List<ApiKeyModel>> doGetApiKeys() {
        return ResponseEntity.ok(
            CollectionUtils.map(
                apiKeyService.getApiKeys(AppType.EMBEDDED),
                apiKey -> conversionService.convert(apiKey, ApiKeyModel.class)
                    .secretKey(obfuscate(apiKey.getSecretKey()))));
    }

    @SuppressFBWarnings("NP")
    protected ResponseEntity<ApiKeyModel> doUpdateApiKey(Long id, ApiKeyModel appEventModel) {
        return ResponseEntity.ok(
            conversionService.convert(
                apiKeyService.update(conversionService.convert(appEventModel.id(id), ApiKey.class)),
                ApiKeyModel.class));
    }

    private static String obfuscate(String secretKey) {
        return StringUtils.obfuscate(secretKey, 26, 6);
    }
}
