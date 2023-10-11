
/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.athena.configuration.facade;

import com.bytechef.athena.configuration.constant.IntegrationConstants;
import com.bytechef.athena.configuration.domain.Integration;
import com.bytechef.athena.configuration.dto.IntegrationDTO;
import com.bytechef.athena.configuration.service.IntegrationService;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.domain.Workflow.SourceType;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.category.domain.Category;
import com.bytechef.category.service.CategoryService;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bytechef.commons.util.CollectionUtils;

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
    public Workflow addWorkflow(long id, @NonNull String definition) {
        Workflow workflow = workflowService.create(
            definition, Format.JSON, SourceType.JDBC, IntegrationConstants.INTEGRATION_TYPE);

        integrationService.addWorkflow(id, workflow.getId());

        return workflow;
    }

    @Override
    public IntegrationDTO create(@NonNull IntegrationDTO integrationDTO) {
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
    public void deleteWorkflow(long id, @NonNull String workflowId) {
        integrationService.removeWorkflow(id, workflowId);

        workflowService.delete(workflowId);
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

        return CollectionUtils.map(
            integrations,
            integration -> new IntegrationDTO(
                integration,
                CollectionUtils.findFirstOrElse(
                    categoryService.getCategories(
                        integrations.stream()
                            .map(Integration::getCategoryId)
                            .filter(Objects::nonNull)
                            .toList()),
                    category -> Objects.equals(integration.getCategoryId(), category.getId()),
                    null),
                CollectionUtils.filter(
                    tagService.getTags(
                        integrations.stream()
                            .flatMap(curIntegration -> CollectionUtils.stream(curIntegration.getTagIds()))
                            .filter(Objects::nonNull)
                            .toList()),
                    tag -> CollectionUtils.contains(integration.getTagIds(), tag.getId()))));
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
    public IntegrationDTO update(long id, @NonNull List<Tag> tags) {
        tags = CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);

        Integration integration = integrationService.update(id, CollectionUtils.map(tags, Tag::getId));

        return new IntegrationDTO(
            integration,
            integration.getCategoryId() == null ? null : categoryService.getCategory(integration.getCategoryId()),
            tags);
    }

    @Override
    public IntegrationDTO update(@NonNull IntegrationDTO integrationDTO) {
        Category category = integrationDTO.category() == null ? null : categoryService.save(integrationDTO.category());
        List<Tag> tags = CollectionUtils.isEmpty(integrationDTO.tags())
            ? Collections.emptyList()
            : tagService.save(integrationDTO.tags());

        Integration integration = integrationDTO.toIntegration();

        integration.setTags(tags);

        return new IntegrationDTO(integrationService.update(integration), category, tags);
    }
}
