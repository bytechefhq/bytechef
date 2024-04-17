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
import com.bytechef.embedded.user.domain.SigningKey;
import com.bytechef.embedded.user.service.SigningKeyService;
import com.bytechef.embedded.user.web.rest.model.CreateSigningKey200ResponseModel;
import com.bytechef.embedded.user.web.rest.model.SigningKeyModel;
import com.bytechef.platform.annotation.ConditionalOnEndpoint;
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
public class SigningKeyApiController implements SigningKeyApi {

    private final SigningKeyService signingKeyService;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public SigningKeyApiController(SigningKeyService signingKeyService, ConversionService conversionService) {
        this.signingKeyService = signingKeyService;
        this.conversionService = conversionService;
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<CreateSigningKey200ResponseModel> createSigningKey(SigningKeyModel signingKeyModel) {
        return ResponseEntity.ok(
            new CreateSigningKey200ResponseModel().privateKey(
                signingKeyService.create(conversionService.convert(signingKeyModel, SigningKey.class))));
    }

    @Override
    public ResponseEntity<Void> deleteSigningKey(Long id) {
        signingKeyService.delete(id);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<SigningKeyModel> getSigningKey(Long id) {
        return ResponseEntity.ok(conversionService.convert(signingKeyService.getSigningKey(id), SigningKeyModel.class));
    }

    @Override
    public ResponseEntity<List<SigningKeyModel>> getSigningKeys() {
        return ResponseEntity.ok(
            CollectionUtils.map(
                signingKeyService.getSigningKeys(), signingKey -> conversionService.convert(
                    signingKey, SigningKeyModel.class)));
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<SigningKeyModel> updateSigningKey(Long id, SigningKeyModel appEventModel) {
        return ResponseEntity.ok(
            conversionService.convert(
                signingKeyService.update(conversionService.convert(appEventModel, SigningKey.class)),
                SigningKeyModel.class));
    }
}
