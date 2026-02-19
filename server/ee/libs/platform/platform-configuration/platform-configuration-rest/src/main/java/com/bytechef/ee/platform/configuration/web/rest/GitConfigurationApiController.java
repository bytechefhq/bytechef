/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.platform.configuration.dto.GitConfigurationDTO;
import com.bytechef.ee.platform.configuration.facade.GitConfigurationFacade;
import com.bytechef.ee.platform.configuration.web.rest.model.GitConfigurationModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class GitConfigurationApiController implements GitConfigurationApi {

    protected static final String PASSWORD = "********";
    private final GitConfigurationFacade gitConfigurationFacade;
    private final ConversionService conversionService;

    public GitConfigurationApiController(
        GitConfigurationFacade gitConfigurationFacade, ConversionService conversionService) {

        this.gitConfigurationFacade = gitConfigurationFacade;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<GitConfigurationModel> getGitConfiguration(Long id) {
        return gitConfigurationFacade.fetchGitConfiguration(id)
            .map(gitConfigurationDTO -> conversionService.convert(gitConfigurationDTO, GitConfigurationModel.class))
            .map(gitConfigurationModel -> gitConfigurationModel.password(PASSWORD))
            .map(ResponseEntity::ok)
            .orElse(noContent());
    }

    @Override
    public ResponseEntity<Void> updateGitConfiguration(Long id, GitConfigurationModel gitConfigurationModel) {
        if (PASSWORD.equals(gitConfigurationModel.getPassword())) {
            gitConfigurationModel.password(null);
        }

        GitConfigurationDTO gitConfigurationDTO = conversionService.convert(
            gitConfigurationModel, GitConfigurationDTO.class);

        gitConfigurationFacade.save(gitConfigurationDTO, id);

        return noContent();
    }

    private static <T> ResponseEntity<T> noContent() {
        return ResponseEntity.noContent()
            .build();
    }
}
