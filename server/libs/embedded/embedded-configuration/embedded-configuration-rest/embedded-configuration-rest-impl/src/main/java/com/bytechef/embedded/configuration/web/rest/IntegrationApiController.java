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

import com.bytechef.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.embedded.configuration.facade.IntegrationFacade;
import com.bytechef.embedded.configuration.web.rest.model.IntegrationModel;
import com.bytechef.platform.configuration.web.rest.model.WorkflowModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}")
public class IntegrationApiController implements IntegrationApi {

    private final ConversionService conversionService;
    private final IntegrationFacade integrationFacade;

    @SuppressFBWarnings("EI2")
    public IntegrationApiController(ConversionService conversionService, IntegrationFacade integrationFacade) {
        this.conversionService = conversionService;
        this.integrationFacade = integrationFacade;
    }

    @Override
    public ResponseEntity<IntegrationModel> createIntegration(IntegrationModel integrationModel) {
        return ResponseEntity.ok(
            conversionService.convert(
                integrationFacade.create(
                    Validate.notNull(
                        conversionService.convert(integrationModel, IntegrationDTO.class), "integrationDTO")),
                IntegrationModel.class));
    }

    @Override
    public ResponseEntity<WorkflowModel> createIntegrationWorkflow(Long id, WorkflowModel workflowModel) {
        return ResponseEntity.ok(
            conversionService.convert(
                integrationFacade.addWorkflow(id, workflowModel.getDefinition()), WorkflowModel.class));
    }

    @Override
    public ResponseEntity<Void> deleteIntegration(Long id) {
        integrationFacade.delete(id);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<IntegrationModel> getIntegration(Long id) {
        return ResponseEntity.ok(
            conversionService.convert(integrationFacade.getIntegration(id), IntegrationModel.class));
    }

    @Override
    public ResponseEntity<List<IntegrationModel>> getIntegrations(
        Long categoryId, Boolean integrationInstances, Long tagId, Boolean published) {

        return ResponseEntity.ok(
            integrationFacade.getIntegrations(categoryId, integrationInstances != null, tagId, published)
                .stream()
                .map(integration -> conversionService.convert(integration, IntegrationModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<IntegrationModel> updateIntegration(Long id, IntegrationModel integrationModel) {
        return ResponseEntity.ok(
            conversionService.convert(
                integrationFacade.update(
                    Validate.notNull(
                        conversionService.convert(integrationModel.id(id), IntegrationDTO.class), "integrationDTO")),
                IntegrationModel.class));
    }
}
