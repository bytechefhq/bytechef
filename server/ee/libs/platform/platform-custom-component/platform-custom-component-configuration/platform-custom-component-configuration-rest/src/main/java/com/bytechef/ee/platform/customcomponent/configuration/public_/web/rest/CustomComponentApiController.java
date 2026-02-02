/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent.Language;
import com.bytechef.ee.platform.customcomponent.configuration.facade.CustomComponentFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.platform.custom.component.configuration.public_.web.rest.CustomComponentApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class CustomComponentApiController implements CustomComponentApi {

    private final CustomComponentFacade customComponentFacade;

    @SuppressFBWarnings("EI")
    public CustomComponentApiController(CustomComponentFacade customComponentFacade) {
        this.customComponentFacade = customComponentFacade;
    }

    @Override
//    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ResponseEntity<Void> deployCustomComponent(MultipartFile componentFile) {
        try {
            customComponentFacade.save(
                componentFile.getBytes(), Language.of(Objects.requireNonNull(componentFile.getOriginalFilename())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.noContent()
            .build();
    }
}
