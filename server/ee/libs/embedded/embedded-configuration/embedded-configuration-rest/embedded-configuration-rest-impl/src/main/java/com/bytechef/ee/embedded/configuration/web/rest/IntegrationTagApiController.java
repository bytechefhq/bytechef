/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.facade.IntegrationTagFacade;
import com.bytechef.ee.embedded.configuration.web.rest.model.TagModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.UpdateTagsRequestModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.tag.domain.Tag;
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
public class IntegrationTagApiController implements IntegrationTagApi {

    private final ConversionService conversionService;
    private final IntegrationTagFacade integrationTagFacade;

    @SuppressFBWarnings("EI2")
    public IntegrationTagApiController(ConversionService conversionService, IntegrationTagFacade integrationTagFacade) {
        this.conversionService = conversionService;
        this.integrationTagFacade = integrationTagFacade;
    }

    @Override
    public ResponseEntity<List<TagModel>> getIntegrationTags() {
        return ResponseEntity.ok(
            integrationTagFacade.getIntegrationTags()
                .stream()
                .map(tag -> conversionService.convert(tag, TagModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<Void> updateIntegrationTags(
        Long id, UpdateTagsRequestModel updateIntegrationTagsRequestModel) {

        List<TagModel> tagModels = updateIntegrationTagsRequestModel.getTags();

        integrationTagFacade.updateIntegrationTags(
            id,
            tagModels.stream()
                .map(tagModel -> conversionService.convert(tagModel, Tag.class))
                .toList());

        return ResponseEntity.noContent()
            .build();
    }
}
