/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.platform.customcomponent.configuration.facade.CustomComponentFacade;
import com.bytechef.ee.platform.customcomponent.configuration.service.CustomComponentService;
import com.bytechef.ee.platform.customcomponent.configuration.web.rest.model.CustomComponentModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
@RestController("com.bytechef.platform.custom.component.configuration.web.rest.CustomComponentApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class CustomComponentApiController implements CustomComponentApi {

    private final CustomComponentFacade customComponentFacade;
    private final CustomComponentService customComponentService;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public CustomComponentApiController(
        CustomComponentFacade customComponentFacade, CustomComponentService customComponentService,
        ConversionService conversionService) {

        this.customComponentFacade = customComponentFacade;
        this.customComponentService = customComponentService;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<Void> deleteCustomComponent(Long id) {
        customComponentFacade.delete(id);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<Void> enableCustomComponent(Long id, Boolean enable) {
        customComponentService.enableCustomComponent(id, enable);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<CustomComponentModel> getCustomComponent(Long id) {
        return ResponseEntity.ok(
            conversionService.convert(customComponentService.getCustomComponent(id), CustomComponentModel.class));
    }

    @Override
    public ResponseEntity<List<CustomComponentModel>> getCustomComponents() {
        return ResponseEntity.ok(
            CollectionUtils.map(
                customComponentFacade.getCustomComponents(),
                openSdkComponent -> conversionService.convert(openSdkComponent, CustomComponentModel.class)));
    }
}
