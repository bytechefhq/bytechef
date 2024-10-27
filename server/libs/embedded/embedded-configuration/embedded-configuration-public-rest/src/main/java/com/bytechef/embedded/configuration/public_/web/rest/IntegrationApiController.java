/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.embedded.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.embedded.configuration.facade.IntegrationFacade;
import com.bytechef.embedded.configuration.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.embedded.configuration.public_.web.rest.model.IntegrationModel;
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
 * @author Ivica Cardic
 */
@CrossOrigin
@RestController("com.bytechef.embedded.configuration.public_.web.rest.IntegrationApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
public class IntegrationApiController implements IntegrationApi {

    private final ConversionService conversionService;
    private final IntegrationFacade integrationFacade;

    @SuppressFBWarnings("EI")
    public IntegrationApiController(ConversionService conversionService, IntegrationFacade integrationFacade) {
        this.conversionService = conversionService;
        this.integrationFacade = integrationFacade;
    }

    @Override
    public ResponseEntity<List<IntegrationModel>> getIntegrations(EnvironmentModel xEnvironment) {
        Environment environment = xEnvironment == null
            ? Environment.PRODUCTION : Environment.valueOf(StringUtils.upperCase(xEnvironment.name()));

        return ResponseEntity.ok(
            integrationFacade
                .getEnabledIntegrationInstanceConfigurationIntegrations(environment)
                .stream()
                .map(integrationDTO -> conversionService.convert(integrationDTO, IntegrationModel.class))
                .toList());
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }
}
