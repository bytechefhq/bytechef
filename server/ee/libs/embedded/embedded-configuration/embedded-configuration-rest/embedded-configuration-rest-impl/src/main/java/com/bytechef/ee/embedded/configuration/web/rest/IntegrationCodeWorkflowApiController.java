/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.facade.IntegrationCodeWorkflowFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
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
@RestController("com.bytechef.ee.embedded.configuration.web.rest.IntegrationCodeWorkflowApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/internal")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class IntegrationCodeWorkflowApiController implements IntegrationCodeWorkflowApi {

    private final IntegrationCodeWorkflowFacade integrationCodeWorkflowFacade;

    @SuppressFBWarnings("EI")
    public IntegrationCodeWorkflowApiController(IntegrationCodeWorkflowFacade integrationCodeWorkflowFacade) {
        this.integrationCodeWorkflowFacade = integrationCodeWorkflowFacade;
    }

    /**
     * Authorization note: the {@code ROLE_ADMIN} guard lives on {@code IntegrationCodeWorkflowFacadeImpl#save} so it
     * protects every caller of the facade (the deploy loads and executes the uploaded artifact), not only this REST
     * entry point.
     */
    @Override
    public ResponseEntity<Void> deployIntegration(MultipartFile integrationFile) {
        try {
            integrationCodeWorkflowFacade.save(
                integrationFile.getBytes(),
                Language.of(Objects.requireNonNull(integrationFile.getOriginalFilename())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.noContent()
            .build();
    }
}
