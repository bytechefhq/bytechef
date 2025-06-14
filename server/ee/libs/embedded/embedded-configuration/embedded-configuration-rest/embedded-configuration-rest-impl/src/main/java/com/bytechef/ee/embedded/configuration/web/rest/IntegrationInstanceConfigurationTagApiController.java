/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceConfigurationFacade;
import com.bytechef.ee.embedded.configuration.web.rest.model.TagModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.UpdateTagsRequestModel;
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
public class IntegrationInstanceConfigurationTagApiController implements IntegrationInstanceConfigurationTagApi {

    private final ConversionService conversionService;
    private final IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceConfigurationTagApiController(
        ConversionService conversionService,
        IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade) {

        this.conversionService = conversionService;
        this.integrationInstanceConfigurationFacade = integrationInstanceConfigurationFacade;
    }

    @Override
    public ResponseEntity<List<TagModel>> getIntegrationInstanceConfigurationTags() {
        return ResponseEntity.ok(
            integrationInstanceConfigurationFacade.getIntegrationInstanceConfigurationTags()
                .stream()
                .map(tag -> conversionService.convert(tag, TagModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<Void> updateIntegrationInstanceConfigurationTags(
        Long id, UpdateTagsRequestModel updateTagsRequestModel) {

        List<TagModel> tagModels = updateTagsRequestModel.getTags();

        integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationTags(
            id,
            tagModels.stream()
                .map(tagModel -> conversionService.convert(tagModel, Tag.class))
                .toList());

        return ResponseEntity.noContent()
            .build();
    }
}
