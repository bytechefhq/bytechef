
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

package com.bytechef.dione.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.category.domain.Category;
import com.bytechef.dione.configuration.domain.Integration;
import com.bytechef.category.service.CategoryService;
import com.bytechef.dione.configuration.dto.IntegrationDTO;
import com.bytechef.dione.configuration.service.IntegrationService;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class IntegrationFacadeImpl implements IntegrationFacade {

    private final CategoryService categoryService;
    private final IntegrationService integrationService;
    private final TagService tagService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public IntegrationFacadeImpl(
        CategoryService categoryService, IntegrationService integrationService, TagService tagService,
        WorkflowService workflowService) {

        this.categoryService = categoryService;
        this.integrationService = integrationService;
        this.tagService = tagService;
        this.workflowService = workflowService;
    }

    @Override
    public Workflow addWorkflow(long id, String label, String description, String definition) {
        if (definition == null) {
            definition = "{\"label\": \"%s\", \"description\": \"%s\", \"tasks\": []}"
                .formatted(label, description);
        }

        Workflow workflow = workflowService.create(definition, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        integrationService.addWorkflow(id, workflow.getId());

        return workflow;
    }

    @Override
    @SuppressFBWarnings("NP")
    public IntegrationDTO create(IntegrationDTO integrationDTO) {
        Integration integration = integrationDTO.toIntegration();

        Category category = integrationDTO.category();

        if (integrationDTO.category() != null) {
            category = categoryService.save(category);

            integration.setCategory(category);
        }

        List<Tag> tags = integrationDTO.tags();

        if (!CollectionUtils.isEmpty(tags)) {
            tags = tagService.save(tags);

            integration.setTags(tags);
        }

        return new IntegrationDTO(integrationService.create(integration), category, tags);
    }

    @Override
    public void delete(long id) {
        Integration integration = integrationService.getIntegration(id);

        for (String workflowId : integration.getWorkflowIds()) {
            workflowService.delete(workflowId);
        }

        integrationService.delete(id);

// TODO find a way to delete ll tags not referenced anymore
//        integration.getTagIds()
//            .forEach(tagService::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrationDTO getIntegration(long id) {
        Integration integration = integrationService.getIntegration(id);

        return new IntegrationDTO(
            integration,
            integration.getCategoryId() == null
                ? null
                : categoryService.getCategory(integration.getCategoryId()),
            tagService.getTags(integration.getTagIds()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getIntegrationCategories() {
        List<Integration> integrations = integrationService.getIntegrations(null, null);

        List<Long> categoryIds = integrations.stream()
            .map(Integration::getCategoryId)
            .filter(Objects::nonNull)
            .toList();

        return categoryService.getCategories(categoryIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationDTO> getIntegrations(Long categoryId, Long tagId) {
        List<Integration> integrations = integrationService.getIntegrations(categoryId, tagId);

        return com.bytechef.commons.util.CollectionUtils.map(
            integrations,
            integration -> new IntegrationDTO(
                integration,
                com.bytechef.commons.util.CollectionUtils.findFirstOrElse(
                    categoryService.getCategories(
                        integrations.stream()
                            .map(Integration::getCategoryId)
                            .filter(Objects::nonNull)
                            .toList()),
                    category -> Objects.equals(integration.getCategoryId(), category.getId()),
                    null),
                com.bytechef.commons.util.CollectionUtils.filter(
                    tagService.getTags(
                        integrations.stream()
                            .flatMap(curIntegration -> com.bytechef.commons.util.CollectionUtils.stream(
                                curIntegration.getTagIds()))
                            .filter(Objects::nonNull)
                            .toList()),
                    tag -> com.bytechef.commons.util.CollectionUtils.contains(integration.getTagIds(), tag.getId()))));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> getIntegrationTags() {
        List<Integration> integrations = integrationService.getIntegrations(null, null);

        List<Long> tagIds = integrations.stream()
            .map(Integration::getTagIds)
            .flatMap(Collection::stream)
            .toList();

        return tagService.getTags(tagIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Workflow> getIntegrationWorkflows(long id) {
        Integration integration = integrationService.getIntegration(id);

        return workflowService.getWorkflows(integration.getWorkflowIds());
    }

    @Override
    public IntegrationDTO update(long id, List<Tag> tags) {
        tags = CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);

        Integration integration = integrationService.update(
            id, com.bytechef.commons.util.CollectionUtils.map(tags, Tag::getId));

        return new IntegrationDTO(
            integration,
            integration.getCategoryId() == null ? null : categoryService.getCategory(integration.getCategoryId()),
            tags);
    }

    @Override
    public IntegrationDTO update(IntegrationDTO integrationDTO) {
        Category category = integrationDTO.category() == null ? null : categoryService.save(integrationDTO.category());
        List<Tag> tags = CollectionUtils.isEmpty(integrationDTO.tags())
            ? Collections.emptyList()
            : tagService.save(integrationDTO.tags());

        Integration integration = integrationDTO.toIntegration();

        integration.setTags(tags);

        return new IntegrationDTO(integrationService.update(integration), category, tags);
    }
}
