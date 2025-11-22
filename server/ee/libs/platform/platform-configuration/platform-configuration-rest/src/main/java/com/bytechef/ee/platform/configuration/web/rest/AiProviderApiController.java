/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.ObfuscateUtils;
import com.bytechef.ee.platform.configuration.facade.AiProviderFacade;
import com.bytechef.ee.platform.configuration.web.rest.model.AiProviderModel;
import com.bytechef.ee.platform.configuration.web.rest.model.UpdateAiProviderRequestModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class AiProviderApiController implements AiProviderApi {

    private final AiProviderFacade aiProviderFacade;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public AiProviderApiController(AiProviderFacade aiProviderFacade, ConversionService conversionService) {
        this.aiProviderFacade = aiProviderFacade;
        this.conversionService = conversionService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ResponseEntity<Void> deleteAiProvider(Integer id) {
        aiProviderFacade.deleteAiProvider(id);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ResponseEntity<List<AiProviderModel>> getAiProviders() {
        return ResponseEntity.ok(
            aiProviderFacade.getAiProviders()
                .stream()
                .map(aiProviderDTO -> conversionService.convert(aiProviderDTO, AiProviderModel.class))
                .peek(aiProviderModel -> {
                    if (aiProviderModel != null) {
                        aiProviderModel.setApiKey(ObfuscateUtils.obfuscate(aiProviderModel.getApiKey(), 26, 6));
                    }
                })
                .toList());
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ResponseEntity<Void> enableAiProvider(Integer id, Boolean enable) {
        aiProviderFacade.updateAiProvider(id, enable);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ResponseEntity<Void> updateAiProvider(
        Integer id, UpdateAiProviderRequestModel updateAiProviderRequestModel) {

        aiProviderFacade.updateAiProvider(id, updateAiProviderRequestModel.getApiKey());

        return ResponseEntity.noContent()
            .build();
    }
}
