/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.app.facade.AppPortalFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.automation.app.web.rest.AppPortalApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/internal/portal/apps")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class AppPortalApiController {

    private static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";

    private final AppPortalFacade appPortalFacade;

    @SuppressFBWarnings("EI")
    public AppPortalApiController(AppPortalFacade appPortalFacade) {
        this.appPortalFacade = appPortalFacade;
    }

    @GetMapping("/{id}/icon")
    public ResponseEntity<byte[]> getPortalAppIcon(@PathVariable long id) {
        return appPortalFacade.getPortalAppIcon(id)
            .map(appIcon -> ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(appIcon.contentType()))
                .header(X_CONTENT_TYPE_OPTIONS, "nosniff")
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(300))
                    .cachePrivate())
                .body(appIcon.content()))
            .orElseGet(() -> ResponseEntity.notFound()
                .build());
    }
}
