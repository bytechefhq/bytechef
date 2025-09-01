/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.user.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.embedded.user.web.rest.model.CreateSigningKey200ResponseModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.ModeType;
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
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/internal")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
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
                    conversionService.convert(signingKeyModel, SigningKey.class), ModeType.EMBEDDED)));
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
            CollectionUtils.map(signingKeyService.getSigningKeys(ModeType.EMBEDDED), this::getSigningKeyModel));
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<Void> updateSigningKey(Long id, SigningKeyModel signingKeyModel) {
        signingKeyService.update(conversionService.convert(signingKeyModel.id(id), SigningKey.class));

        return ResponseEntity.noContent()
            .build();
    }

    private SigningKeyModel getSigningKeyModel(SigningKey signingKey) {
        return conversionService.convert(signingKey, SigningKeyModel.class);
    }
}
