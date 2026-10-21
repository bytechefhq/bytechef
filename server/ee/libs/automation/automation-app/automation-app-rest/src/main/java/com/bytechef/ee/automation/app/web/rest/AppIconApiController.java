/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.automation.app.web.rest.AppIconApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/internal/apps")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class AppIconApiController {

    private static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";

    private final AppFacade appFacade;

    @SuppressFBWarnings("EI")
    public AppIconApiController(AppFacade appFacade) {
        this.appFacade = appFacade;
    }

    @DeleteMapping("/{id}/icon")
    public ResponseEntity<Void> deleteAppIcon(@PathVariable long id) {
        appFacade.deleteAppIcon(id);

        return ResponseEntity.noContent()
            .build();
    }

    @GetMapping("/{id}/icon")
    public ResponseEntity<byte[]> getAppIcon(@PathVariable long id) {
        return appFacade.getAppIcon(id)
            .map(appIcon -> ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(appIcon.contentType()))
                .header(X_CONTENT_TYPE_OPTIONS, "nosniff")
                .body(appIcon.content()))
            .orElseGet(() -> ResponseEntity.notFound()
                .build());
    }

    @PostMapping("/{id}/icon")
    public ResponseEntity<Void> uploadAppIcon(@PathVariable long id, @RequestParam("file") MultipartFile file)
        throws IOException {

        try (InputStream inputStream = file.getInputStream()) {
            appFacade.updateAppIcon(id, inputStream);
        }

        return ResponseEntity.noContent()
            .build();
    }
}
