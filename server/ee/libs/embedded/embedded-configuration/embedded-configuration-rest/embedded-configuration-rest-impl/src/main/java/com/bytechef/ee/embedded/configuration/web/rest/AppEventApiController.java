/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.embedded.configuration.domain.AppEvent;
import com.bytechef.ee.embedded.configuration.service.AppEventService;
import com.bytechef.ee.embedded.configuration.web.rest.model.AppEventModel;
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
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/internal")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class AppEventApiController implements AppEventApi {

    private final AppEventService appEventService;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public AppEventApiController(AppEventService appEventService, ConversionService conversionService) {
        this.appEventService = appEventService;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<Long> createAppEvent(AppEventModel appEventModel) {
        AppEvent appEvent = appEventService.create(conversionService.convert(appEventModel, AppEvent.class));

        return ResponseEntity.ok(appEvent.getId());
    }

    @Override
    public ResponseEntity<Void> deleteAppEvent(Long id) {
        appEventService.delete(id);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<AppEventModel> getAppEvent(Long id) {
        return ResponseEntity.ok(conversionService.convert(appEventService.getAppEvent(id), AppEventModel.class));
    }

    @Override
    public ResponseEntity<List<AppEventModel>> getAppEvents() {
        return ResponseEntity.ok(
            CollectionUtils.map(
                appEventService.getAppEvents(), appEvent -> conversionService.convert(appEvent, AppEventModel.class)));
    }

    @Override
    public ResponseEntity<Void> updateAppEvent(Long id, AppEventModel appEventModel) {
        appEventService.update(conversionService.convert(appEventModel, AppEvent.class));

        return ResponseEntity.noContent()
            .build();
    }
}
