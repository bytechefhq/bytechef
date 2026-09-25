/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.facade.ConnectionAdminFacade;
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
@RestController("com.bytechef.ee.embedded.configuration.web.rest.ConnectionTagApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/internal")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class ConnectionTagApiController implements ConnectionTagApi {

    private final ConnectionAdminFacade connectionAdminFacade;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public ConnectionTagApiController(
        ConnectionAdminFacade connectionAdminFacade, ConversionService conversionService) {

        this.connectionAdminFacade = connectionAdminFacade;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<List<TagModel>> getConnectionTags() {
        return ResponseEntity.ok(
            connectionAdminFacade.getConnectionTags()
                .stream()
                .map(tag -> conversionService.convert(tag, TagModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<Void> updateConnectionTags(Long id, UpdateTagsRequestModel updateTagsRequestModel) {
        List<Tag> tags = updateTagsRequestModel.getTags()
            .stream()
            .map(tagModel -> conversionService.convert(tagModel, Tag.class))
            .toList();

        connectionAdminFacade.updateConnectionTags(id, tags);

        return ResponseEntity.noContent()
            .build();
    }
}
