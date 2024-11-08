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

package com.bytechef.embedded.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.StringUtils;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.embedded.configuration.facade.IntegrationInstanceConfigurationFacade;
import com.bytechef.embedded.configuration.web.rest.model.CreateIntegrationInstanceConfigurationWorkflowJob200ResponseModel;
import com.bytechef.embedded.configuration.web.rest.model.EnvironmentModel;
import com.bytechef.embedded.configuration.web.rest.model.IntegrationInstanceConfigurationModel;
import com.bytechef.embedded.configuration.web.rest.model.IntegrationInstanceConfigurationWorkflowModel;
import com.bytechef.platform.constant.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/internal")
@ConditionalOnCoordinator
public class IntegrationInstanceConfigurationApiController implements IntegrationInstanceConfigurationApi {

    private final ConversionService conversionService;
    private final IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceConfigurationApiController(
        ConversionService conversionService,
        IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade) {

        this.conversionService = conversionService;
        this.integrationInstanceConfigurationFacade = integrationInstanceConfigurationFacade;
    }

    @Override
    public ResponseEntity<Long> createIntegrationInstanceConfiguration(
        IntegrationInstanceConfigurationModel integrationInstanceConfigurationModel) {

        return ResponseEntity.ok(
            integrationInstanceConfigurationFacade.createIntegrationInstanceConfiguration(
                conversionService.convert(
                    integrationInstanceConfigurationModel, IntegrationInstanceConfigurationDTO.class)));
    }

    @Override
    public ResponseEntity<CreateIntegrationInstanceConfigurationWorkflowJob200ResponseModel>
        createIntegrationInstanceConfigurationWorkflowJob(Long id, String workflowId) {

        return ResponseEntity.ok(
            new CreateIntegrationInstanceConfigurationWorkflowJob200ResponseModel().jobId(
                integrationInstanceConfigurationFacade.createIntegrationInstanceConfigurationWorkflowJob(
                    id, workflowId)));
    }

    @Override
    public ResponseEntity<Void> deleteIntegrationInstanceConfiguration(Long id) {
        integrationInstanceConfigurationFacade.deleteIntegrationInstanceConfiguration(id);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<Void> enableIntegrationInstanceConfiguration(Long id, Boolean enable) {
        integrationInstanceConfigurationFacade.enableIntegrationInstanceConfiguration(id, enable);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<Void> enableIntegrationInstanceConfigurationWorkflow(
        Long id, String workflowId, Boolean enable) {

        integrationInstanceConfigurationFacade.enableIntegrationInstanceConfigurationWorkflow(id, workflowId, enable);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<IntegrationInstanceConfigurationModel> getIntegrationInstanceConfiguration(Long id) {
        return ResponseEntity.ok(
            toIntegrationInstanceConfigurationModel(
                integrationInstanceConfigurationFacade.getIntegrationInstanceConfiguration(id)));
    }

    @Override
    public ResponseEntity<List<IntegrationInstanceConfigurationModel>> getIntegrationInstanceConfigurations(
        EnvironmentModel environment, Long integrationId, Long tagId) {

        return ResponseEntity.ok(
            integrationInstanceConfigurationFacade
                .getIntegrationInstanceConfigurations(
                    environment == null ? null : Environment.valueOf(environment.getValue()), integrationId, tagId)
                .stream()
                .map(this::toIntegrationInstanceConfigurationModel)
                .toList());
    }

    @Override
    public ResponseEntity<Void> updateIntegrationInstanceConfiguration(
        Long id, IntegrationInstanceConfigurationModel integrationInstanceConfigurationModel) {

        integrationInstanceConfigurationFacade.updateIntegrationInstanceConfiguration(
            conversionService.convert(
                integrationInstanceConfigurationModel.id(id), IntegrationInstanceConfigurationDTO.class));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> updateIntegrationInstanceConfigurationWorkflow(
        Long id, Long integrationInstanceConfigurationWorkflowId,
        IntegrationInstanceConfigurationWorkflowModel integrationInstanceConfigurationWorkflowModel) {

        integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationWorkflow(
            conversionService.convert(
                integrationInstanceConfigurationWorkflowModel.id(integrationInstanceConfigurationWorkflowId)
                    .integrationInstanceConfigurationId(id),
                IntegrationInstanceConfigurationWorkflow.class));

        return ResponseEntity.noContent()
            .build();
    }

    @SuppressFBWarnings("NP")
    private IntegrationInstanceConfigurationModel toIntegrationInstanceConfigurationModel(
        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO) {

        IntegrationInstanceConfigurationModel integrationInstanceConfigurationModel = conversionService.convert(
            integrationInstanceConfigurationDTO, IntegrationInstanceConfigurationModel.class);

        integrationInstanceConfigurationModel.connectionAuthorizationParameters(
            MapUtils.toMap(
                integrationInstanceConfigurationModel.getConnectionAuthorizationParameters(),
                Map.Entry::getKey,
                entry -> StringUtils.obfuscate(String.valueOf(entry.getValue()), 28, 8)));

        return Validate.notNull(integrationInstanceConfigurationModel, "integrationInstanceConfigurationModel")
            .connectionParameters(null);
    }
}
