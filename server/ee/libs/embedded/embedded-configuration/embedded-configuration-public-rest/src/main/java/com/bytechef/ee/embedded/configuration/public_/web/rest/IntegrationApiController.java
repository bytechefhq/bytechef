/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceConfigurationFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.IntegrationModel;
import com.bytechef.platform.constant.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.embedded.configuration.public_.web.rest.IntegrationApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
public class IntegrationApiController implements IntegrationApi {

    private final ConversionService conversionService;
    private final IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade;

    @SuppressFBWarnings("EI")
    public IntegrationApiController(
        ConversionService conversionService,
        IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade) {

        this.conversionService = conversionService;
        this.integrationInstanceConfigurationFacade = integrationInstanceConfigurationFacade;
    }

    @CrossOrigin
    @Override
    public ResponseEntity<IntegrationModel> getFrontendIntegration(Long id, EnvironmentModel xEnvironment) {
        Environment environment = xEnvironment == null
            ? Environment.PRODUCTION : Environment.valueOf(StringUtils.upperCase(xEnvironment.name()));

        return ResponseEntity.ok(
            conversionService.convert(
                integrationInstanceConfigurationFacade.getIntegrationInstanceConfigurationIntegration(
                    id, environment, true),
                IntegrationModel.class));
    }

    @CrossOrigin
    @Override
    public ResponseEntity<List<IntegrationModel>> getFrontendIntegrations(EnvironmentModel xEnvironment) {
        Environment environment = xEnvironment == null
            ? Environment.PRODUCTION : Environment.valueOf(StringUtils.upperCase(xEnvironment.name()));

        return ResponseEntity.ok(
            integrationInstanceConfigurationFacade
                .getIntegrationInstanceConfigurationIntegrations(environment, true)
                .stream()
                .map(integrationDTO -> conversionService.convert(integrationDTO, IntegrationModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<List<IntegrationModel>> getIntegrations(
        String externalUserId, EnvironmentModel xEnvironment) {

        Environment environment = xEnvironment == null
            ? Environment.PRODUCTION : Environment.valueOf(StringUtils.upperCase(xEnvironment.name()));

        return ResponseEntity.ok(
            integrationInstanceConfigurationFacade
                .getIntegrationInstanceConfigurationIntegrations(environment, true)
                .stream()
                .map(integrationDTO -> conversionService.convert(integrationDTO, IntegrationModel.class))
                .toList());
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }
}
