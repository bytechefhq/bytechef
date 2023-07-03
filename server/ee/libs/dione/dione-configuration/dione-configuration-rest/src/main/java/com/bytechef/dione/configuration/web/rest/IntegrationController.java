
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.dione.configuration.web.rest;

import com.bytechef.dione.configuration.web.rest.model.CategoryModel;
import com.bytechef.dione.configuration.web.rest.model.TagModel;
import com.bytechef.hermes.configuration.web.rest.model.WorkflowModel;
import com.bytechef.dione.configuration.dto.IntegrationDTO;
import com.bytechef.dione.configuration.facade.IntegrationFacade;
import com.bytechef.dione.configuration.web.rest.model.CreateIntegrationWorkflowRequestModel;
import com.bytechef.dione.configuration.web.rest.model.IntegrationModel;
import com.bytechef.dione.configuration.web.rest.model.UpdateTagsRequestModel;
import com.bytechef.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/embedded")
public class IntegrationController implements IntegrationsApi {

    private final ConversionService conversionService;
    private final IntegrationFacade integrationFacade;

    @SuppressFBWarnings("EI2")
    public IntegrationController(ConversionService conversionService, IntegrationFacade integrationFacade) {
        this.conversionService = conversionService;
        this.integrationFacade = integrationFacade;
    }

    @Override
    public ResponseEntity<Void> deleteIntegration(Long id) {
        integrationFacade.delete(id);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<IntegrationModel> getIntegration(Long id) {
        return ResponseEntity.ok(
            conversionService.convert(integrationFacade.getIntegration(id), IntegrationModel.class));
    }

    @Override
    public ResponseEntity<List<IntegrationModel>> getIntegrations(
        List<Long> categoryIds, List<Long> tagIds) {
        return ResponseEntity.ok(
            integrationFacade.getIntegrations(categoryIds, tagIds)
                .stream()
                .map(integration -> conversionService.convert(integration, IntegrationModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<List<CategoryModel>> getIntegrationCategories() {
        return ResponseEntity.ok(integrationFacade.getIntegrationCategories()
            .stream()
            .map(category -> conversionService.convert(category, CategoryModel.class))
            .toList());
    }

    @Override
    public ResponseEntity<List<TagModel>> getIntegrationTags() {
        return ResponseEntity.ok(
            integrationFacade.getIntegrationTags()
                .stream()
                .map(tag -> conversionService.convert(tag, TagModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<List<WorkflowModel>> getIntegrationWorkflows(Long id) {
        return ResponseEntity.ok(
            integrationFacade.getIntegrationWorkflows(id)
                .stream()
                .map(workflow -> conversionService.convert(workflow, WorkflowModel.class))
                .toList());
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<IntegrationModel> createIntegration(IntegrationModel integrationModel) {
        return ResponseEntity.ok(
            conversionService.convert(
                integrationFacade.create(
                    conversionService.convert(integrationModel, IntegrationDTO.class)),
                IntegrationModel.class));
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<WorkflowModel> createIntegrationWorkflow(
        Long id, CreateIntegrationWorkflowRequestModel createIntegrationWorkflowRequestModel) {

        return ResponseEntity.ok(
            conversionService.convert(
                integrationFacade.addWorkflow(
                    id, createIntegrationWorkflowRequestModel.getLabel(),
                    createIntegrationWorkflowRequestModel.getDescription(),
                    createIntegrationWorkflowRequestModel.getDefinition()),
                WorkflowModel.class));
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<IntegrationModel> updateIntegration(Long id, IntegrationModel integrationModel) {
        return ResponseEntity.ok(
            conversionService.convert(
                integrationFacade.update(conversionService.convert(integrationModel.id(id), IntegrationDTO.class)),
                IntegrationModel.class));
    }

    @Override
    public ResponseEntity<Void> updateIntegrationTags(
        Long id, UpdateTagsRequestModel updateIntegrationTagsRequestModel) {

        List<TagModel> tagModels = updateIntegrationTagsRequestModel.getTags();

        integrationFacade.update(
            id,
            tagModels.stream()
                .map(tagModel -> conversionService.convert(tagModel, Tag.class))
                .toList());

        return ResponseEntity
            .noContent()
            .build();
    }
}
