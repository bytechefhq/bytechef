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

package com.bytechef.hermes.test.web.rest;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.hermes.test.domain.WorkflowTestConfiguration;
import com.bytechef.hermes.test.service.WorkflowTestConfigurationService;
import com.bytechef.hermes.test.web.rest.model.WorkflowTestConfigurationModel;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.core:}")
@ConditionalOnProperty(prefix = "bytechef", name = "coordinator.enabled", matchIfMissing = true)
public class WorkflowTestConfigurationApiController implements WorkflowTestConfigurationApi {

    private final WorkflowTestConfigurationService workflowTestConfigurationService;
    private final ConversionService conversionService;

    public WorkflowTestConfigurationApiController(
        WorkflowTestConfigurationService workflowTestConfigurationService, ConversionService conversionService) {

        this.workflowTestConfigurationService = workflowTestConfigurationService;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<WorkflowTestConfigurationModel> createWorkflowTestConfiguration(
        WorkflowTestConfigurationModel workflowTestConfigurationModel) {

        return ResponseEntity.ok(
            conversionService.convert(
                workflowTestConfigurationService.create(
                    conversionService.convert(workflowTestConfigurationModel, WorkflowTestConfiguration.class)),
                WorkflowTestConfigurationModel.class));
    }

    @Override
    public ResponseEntity<WorkflowTestConfigurationModel> getWorkflowTestConfiguration(Long id) {
        return ResponseEntity.ok(
            conversionService.convert(
                workflowTestConfigurationService.getWorkflowTestConfiguration(id),
                WorkflowTestConfigurationModel.class));
    }

    @Override
    public ResponseEntity<List<WorkflowTestConfigurationModel>> getWorkflowTestConfigurations() {
        return ResponseEntity.ok(
            CollectionUtils.map(
                workflowTestConfigurationService.getWorkflowTestConfigurations(),
                workflowTestConfiguration -> conversionService.convert(
                    workflowTestConfiguration, WorkflowTestConfigurationModel.class)));
    }

    @Override
    public ResponseEntity<WorkflowTestConfigurationModel> updateWorkflowTestConfiguration(
        Long id, WorkflowTestConfigurationModel workflowTestConfigurationModel) {

        return ResponseEntity.ok(
            conversionService.convert(
                workflowTestConfigurationService.updateWorkflowTestConfiguration(
                    Validate.notNull(
                        conversionService.convert(
                            workflowTestConfigurationModel.id(id), WorkflowTestConfiguration.class),
                        "WorkflowTestConfiguration")),
                WorkflowTestConfigurationModel.class));
    }
}
