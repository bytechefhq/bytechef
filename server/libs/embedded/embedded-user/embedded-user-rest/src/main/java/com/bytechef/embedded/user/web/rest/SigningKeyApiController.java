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
import com.bytechef.embedded.user.web.rest.model.CreateSigningKey200ResponseModel;
import com.bytechef.platform.constant.AppType;
import com.bytechef.platform.user.domain.SigningKey;
import com.bytechef.platform.user.facade.SigningKeyFacade;
import com.bytechef.platform.user.service.SigningKeyService;
import com.bytechef.platform.user.web.rest.model.SigningKeyModel;
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
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/internal")
public class SigningKeyApiController implements SigningKeyApi {

    private final SigningKeyFacade signingKeyFacade;
    private final SigningKeyService signingKeyService;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public SigningKeyApiController(
        SigningKeyFacade signingKeyFacade, SigningKeyService signingKeyService, ConversionService conversionService) {

        this.signingKeyFacade = signingKeyFacade;
        this.signingKeyService = signingKeyService;
        this.conversionService = conversionService;
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<CreateSigningKey200ResponseModel> createSigningKey(SigningKeyModel signingKeyModel) {
        return ResponseEntity.ok(
            new CreateSigningKey200ResponseModel().privateKey(
                signingKeyFacade.create(
                    conversionService.convert(signingKeyModel, SigningKey.class), AppType.EMBEDDED)));
    }

    @Override
    public ResponseEntity<Void> deleteSigningKey(Long id) {
        signingKeyService.delete(id);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<SigningKeyModel> getSigningKey(Long id) {
        return ResponseEntity.ok(getSigningKeyModel(signingKeyService.getSigningKey(id)));
    }

    @Override
    public ResponseEntity<List<SigningKeyModel>> getSigningKeys() {
        return ResponseEntity.ok(
            CollectionUtils.map(signingKeyService.getSigningKeys(AppType.EMBEDDED), this::getSigningKeyModel));
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<SigningKeyModel> updateSigningKey(Long id, SigningKeyModel signingKeyModel) {
        return ResponseEntity.ok(
            getSigningKeyModel(
                signingKeyService.update(conversionService.convert(signingKeyModel.id(id), SigningKey.class))));
    }

    private SigningKeyModel getSigningKeyModel(SigningKey signingKey) {
        return conversionService.convert(signingKey, SigningKeyModel.class);
    }
}
