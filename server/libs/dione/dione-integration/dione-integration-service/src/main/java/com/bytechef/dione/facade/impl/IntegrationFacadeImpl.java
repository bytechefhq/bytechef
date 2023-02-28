
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

package com.bytechef.dione.facade.impl;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.category.domain.Category;
import com.bytechef.dione.integration.domain.Integration;
import com.bytechef.dione.integration.facade.IntegrationFacade;
import com.bytechef.category.servicee.CategoryService;
import com.bytechef.dione.integration.service.IntegrationService;
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
    public Integration addWorkflow(long id, String name, String description, String definition) {
        if (definition == null) {
            definition = "{\"label\": \"%s\", \"description\": \"%s\", \"tasks\": []}"
                .formatted(name, description);
        }

        Workflow workflow = workflowService.create(definition, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        return integrationService.addWorkflow(id, workflow.getId());
    }

    @Override
    @SuppressFBWarnings("NP")
    public Integration create(Integration integration) {
        if (integration.getCategory() != null) {
            Category category = integration.getCategory();

            integration.setCategory(categoryService.save(category));
        }

        if (CollectionUtils.isEmpty(integration.getWorkflowIds())) {
            Workflow workflow = workflowService.create(null, Workflow.Format.JSON, Workflow.SourceType.JDBC);

            integration.setWorkflowIds(List.of(workflow.getId()));
        }

        if (!CollectionUtils.isEmpty(integration.getTags())) {
            integration.setTags(tagService.save(integration.getTags()));
        }

        return integrationService.create(integration);
    }

    @Override
    public void delete(Long id) {
//        Integration integration = integrationService.getIntegration(id);

        integrationService.delete(id);

// TODO find a way to delete ll tags not referenced anymore
//        integration.getTagIds()
//            .forEach(tagService::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public Integration getIntegration(Long id) {
        Integration integration = integrationService.getIntegration(id);

        if (integration.getCategoryId() != null) {
            categoryService.fetchCategory(integration.getCategoryId())
                .ifPresent(integration::setCategory);
        }

        integration.setTags(tagService.getTags(integration.getTagIds()));

        return integration;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integration> getIntegrations(List<Long> categoryIds, List<Long> tagIds) {
        List<Integration> integrations = integrationService.getIntegrations(categoryIds, tagIds);

        List<Category> categories = categoryService.getCategories(integrations.stream()
            .map(Integration::getCategoryId)
            .filter(Objects::nonNull)
            .toList());

        for (Category category : categories) {
            integrations.stream()
                .filter(integration -> Objects.equals(integration.getCategoryId(), category.getId()))
                .forEach(integration -> integration.setCategory(category));
        }

        List<Tag> tags = tagService.getTags(integrations.stream()
            .flatMap(integration -> integration.getTagIds()
                .stream())
            .filter(Objects::nonNull)
            .toList());

        for (Integration integration : integrations) {
            integration.setTags(
                tags.stream()
                    .filter(tag -> {
                        List<Long> curTagIds = integration.getTagIds();

                        return curTagIds.contains(tag.getId());
                    })
                    .toList());
        }

        return integrations;
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
    public List<Workflow> getIntegrationWorkflows(Long id) {
        Integration integration = integrationService.getIntegration(id);

        return workflowService.getWorkflows(integration.getWorkflowIds());
    }

    @Override
    public Integration update(Long id, List<Tag> tags) {
        tags = CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);

        return integrationService.update(id, tags);
    }

    @Override
    public Integration update(Integration integration) {
        integration
            .setCategory(integration.getCategory() == null ? null : categoryService.save(integration.getCategory()));
        integration
            .setTags(
                CollectionUtils.isEmpty(integration.getTags())
                    ? Collections.emptyList()
                    : tagService.save(integration.getTags()));

        return integrationService.update(integration);
    }
}
