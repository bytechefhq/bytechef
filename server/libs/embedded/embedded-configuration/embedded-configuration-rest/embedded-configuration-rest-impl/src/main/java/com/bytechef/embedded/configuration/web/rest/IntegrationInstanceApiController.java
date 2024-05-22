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

import com.bytechef.embedded.configuration.facade.IntegrationInstanceFacade;
import com.bytechef.embedded.configuration.web.rest.model.IntegrationInstanceModel;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}")
public class IntegrationInstanceApiController implements IntegrationInstanceApi {

    private final ConversionService conversionService;
    private final IntegrationInstanceFacade integrationInstanceFacade;

    public IntegrationInstanceApiController(
        ConversionService conversionService, IntegrationInstanceFacade integrationInstanceFacade) {

        this.conversionService = conversionService;
        this.integrationInstanceFacade = integrationInstanceFacade;
    }

    @Override
    public ResponseEntity<Void> enableIntegrationInstance(Long id, Boolean enable) {
        integrationInstanceFacade.enableIntegrationInstance(id, enable);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> enableIntegrationInstanceWorkflow(Long id, String workflowId, Boolean enable) {
        integrationInstanceFacade.enableIntegrationInstanceWorkflow(id, workflowId, enable);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<IntegrationInstanceModel> getIntegrationInstance(Long id) {
        return ResponseEntity.ok(
            conversionService.convert(
                integrationInstanceFacade.getIntegrationInstance(id), IntegrationInstanceModel.class));
    }
}
