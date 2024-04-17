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
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}")
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
    public ResponseEntity<IntegrationInstanceConfigurationModel> createIntegrationInstanceConfiguration(
        IntegrationInstanceConfigurationModel integrationInstanceConfigurationModel) {

        return ResponseEntity.ok(
            conversionService.convert(
                integrationInstanceConfigurationFacade.createIntegrationInstanceConfiguration(
                    conversionService.convert(
                        integrationInstanceConfigurationModel, IntegrationInstanceConfigurationDTO.class)),
                IntegrationInstanceConfigurationModel.class));
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
            conversionService.convert(
                integrationInstanceConfigurationFacade.getIntegrationInstanceConfiguration(id),
                IntegrationInstanceConfigurationModel.class));
    }

    @Override
    public ResponseEntity<List<IntegrationInstanceConfigurationModel>> getIntegrationInstanceConfigurations(
        EnvironmentModel environment, Long integrationId, Long tagId) {

        return ResponseEntity.ok(
            integrationInstanceConfigurationFacade
                .getIntegrationInstanceConfigurations(
                    environment == null ? null : Environment.valueOf(environment.getValue()), integrationId, tagId)
                .stream()
                .map(projectInstance -> conversionService.convert(
                    projectInstance, IntegrationInstanceConfigurationModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<IntegrationInstanceConfigurationModel> updateIntegrationInstanceConfiguration(
        Long id, IntegrationInstanceConfigurationModel integrationInstanceConfigurationModel) {

        return ResponseEntity.ok(conversionService.convert(
            integrationInstanceConfigurationFacade.updateIntegrationInstanceConfiguration(
                conversionService.convert(
                    integrationInstanceConfigurationModel.id(id), IntegrationInstanceConfigurationDTO.class)),
            IntegrationInstanceConfigurationModel.class));
    }

    @Override
    public ResponseEntity<IntegrationInstanceConfigurationWorkflowModel> updateIntegrationInstanceConfigurationWorkflow(
        Long id, Long integrationInstanceConfigurationWorkflowId,
        IntegrationInstanceConfigurationWorkflowModel integrationInstanceConfigurationWorkflowModel) {

        return ResponseEntity.ok(conversionService.convert(
            integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationWorkflow(
                conversionService.convert(
                    integrationInstanceConfigurationWorkflowModel.id(integrationInstanceConfigurationWorkflowId)
                        .integrationInstanceConfigurationId(id),
                    IntegrationInstanceConfigurationWorkflow.class)),
            IntegrationInstanceConfigurationWorkflowModel.class));
    }
}
