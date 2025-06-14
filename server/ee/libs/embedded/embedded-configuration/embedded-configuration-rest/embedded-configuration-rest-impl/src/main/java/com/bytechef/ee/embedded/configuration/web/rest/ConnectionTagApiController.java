/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.web.rest.AbstractConnectionTagApiController;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.tag.web.rest.model.TagModel;
import com.bytechef.platform.tag.web.rest.model.UpdateTagsRequestModel;
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
public class ConnectionTagApiController extends AbstractConnectionTagApiController implements ConnectionTagApi {

    @SuppressFBWarnings("EI")
    public ConnectionTagApiController(ConnectionFacade connectionFacade, ConversionService conversionService) {
        super(connectionFacade, conversionService, ModeType.EMBEDDED);
    }

    @Override
    public ResponseEntity<List<TagModel>> getConnectionTags() {
        return doGetConnectionTags();
    }

    @Override
    public ResponseEntity<Void> updateConnectionTags(Long id, UpdateTagsRequestModel updateTagsRequestModel) {
        return doUpdateConnectionTags(id, updateTagsRequestModel);
    }
}
