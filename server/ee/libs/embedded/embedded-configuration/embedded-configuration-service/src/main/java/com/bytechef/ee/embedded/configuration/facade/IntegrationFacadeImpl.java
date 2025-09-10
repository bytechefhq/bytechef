/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationVersion.Status;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class IntegrationFacadeImpl implements IntegrationFacade {

    private final CategoryService categoryService;
    private final ComponentDefinitionService componentDefinitionService;
    private final IntegrationService integrationService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final TagService tagService;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;
    private final WorkflowNodeTestOutputService workflowNodeTestOutputService;

    @SuppressFBWarnings("EI2")
    public IntegrationFacadeImpl(
        CategoryService categoryService, ComponentDefinitionService componentDefinitionService,
        IntegrationService integrationService, IntegrationWorkflowService integrationWorkflowService,
        IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        TagService tagService, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService,
        WorkflowNodeTestOutputService workflowNodeTestOutputService) {

        this.categoryService = categoryService;
        this.componentDefinitionService = componentDefinitionService;
        this.integrationService = integrationService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.integrationInstanceConfigurationFacade = integrationInstanceConfigurationFacade;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.tagService = tagService;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
        this.workflowNodeTestOutputService = workflowNodeTestOutputService;
    }

    @Override
    public long createIntegration(IntegrationDTO integrationDTO) {
        Integration integration = integrationDTO.toIntegration();
        Category category = integrationDTO.category();

        if (category != null) {
            category = categoryService.save(category);

            integration.setCategory(category);
        }

        if (!CollectionUtils.isEmpty(integrationDTO.tags())) {
            List<Tag> tags = tagService.save(integrationDTO.tags());

            integration.setTags(tags);
        }

        integration = integrationService.create(integration);

        return integration.getId();
    }

    @Override
    public void deleteIntegration(long id) {
        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(id);

        for (IntegrationInstanceConfiguration integrationInstanceConfiguration : integrationInstanceConfigurations) {
            integrationInstanceConfigurationFacade.deleteIntegrationInstanceConfiguration(
                integrationInstanceConfiguration.getId());
        }

        List<IntegrationWorkflow> integrationWorkflows = integrationWorkflowService.getIntegrationWorkflows(id);

        workflowService.delete(
            integrationWorkflows.stream()
                .map(IntegrationWorkflow::getWorkflowId)
                .toList());

        workflowTestConfigurationService.delete(
            integrationWorkflows.stream()
                .map(IntegrationWorkflow::getWorkflowId)
                .toList());

        integrationWorkflowService.delete(
            integrationWorkflows.stream()
                .map(IntegrationWorkflow::getId)
                .toList());

        integrationService.delete(id);

// TODO find a way to delete ll tags not referenced anymore
//        integration.getTagIds()
//            .forEach(tagService::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrationDTO getIntegration(long id) {
        Integration integration = integrationService.getIntegration(id);

        return toIntegrationDTO(integration);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationDTO> getIntegrations(
        Long categoryId, boolean integrationInstanceConfigurations, Long tagId, Status status,
        boolean includeAllFields) {

        List<Long> integrationIds = List.of();

        if (integrationInstanceConfigurations) {
            integrationIds = integrationInstanceConfigurationService.getIntegrationIds();

            if (integrationIds.isEmpty()) {
                return List.of();
            }
        }

        List<Integration> integrations = integrationService.getIntegrations(categoryId, integrationIds, tagId, status);

        if (includeAllFields) {
            List<Category> categories = categoryService.getCategories(
                integrations.stream()
                    .map(Integration::getCategoryId)
                    .filter(Objects::nonNull)
                    .toList());
            List<Tag> tags = tagService.getTags(
                integrations.stream()
                    .flatMap(curIntegration -> CollectionUtils.stream(curIntegration.getTagIds()))
                    .filter(Objects::nonNull)
                    .toList());

            return CollectionUtils.map(
                integrations,
                integration -> new IntegrationDTO(
                    CollectionUtils.findFirstFilterOrElse(
                        categories, category -> Objects.equals(integration.getCategoryId(), category.getId()), null),
                    componentDefinitionService.getComponentDefinition(integration.getComponentName(), null),
                    integration,
                    getIntegrationWorkflowIds(integration),
                    CollectionUtils.filter(
                        tags, tag -> CollectionUtils.contains(integration.getTagIds(), tag.getId()))));
        } else {
            return CollectionUtils.map(integrations, IntegrationDTO::new);
        }
    }

    @Override
    public void publishIntegration(long id, String description) {
        Integration integration = integrationService.getIntegration(id);

        int oldIntegrationVersion = integration.getLastIntegrationVersion();

        List<IntegrationWorkflow> oldIntegrationWorkflows = integrationWorkflowService
            .getIntegrationWorkflows(integration.getId(), oldIntegrationVersion);

        int newIntegrationVersion = integrationService.publishIntegration(id, description);

        for (IntegrationWorkflow oldIntegrationWorkflow : oldIntegrationWorkflows) {
            String oldWorkflowId = oldIntegrationWorkflow.getWorkflowId();

            Workflow duplicatedWorkflow = workflowService.duplicateWorkflow(oldWorkflowId);

            oldIntegrationWorkflow.setIntegrationVersion(newIntegrationVersion);
            oldIntegrationWorkflow.setWorkflowId(duplicatedWorkflow.getId());

            integrationWorkflowService.publishWorkflow(
                integration.getId(), integration.getLastIntegrationVersion(), oldWorkflowId, oldIntegrationWorkflow);

            workflowTestConfigurationService.updateWorkflowId(oldWorkflowId, duplicatedWorkflow.getId());
            workflowNodeTestOutputService.updateWorkflowId(oldWorkflowId, duplicatedWorkflow.getId());
        }
    }

    @Override
    public void updateIntegration(IntegrationDTO integrationDTO) {
        List<Tag> tags = CollectionUtils.isEmpty(integrationDTO.tags())
            ? Collections.emptyList()
            : tagService.save(integrationDTO.tags());

        Integration integration = integrationDTO.toIntegration();
        Category category = integrationDTO.category();

        if (category != null) {
            category = categoryService.save(category);

            integration.setCategory(category);
        }

        integration.setTags(tags);

        integrationService.update(integration);
    }

    private Category getCategory(Integration integration) {
        return integration.getCategoryId() == null ? null : categoryService.getCategory(integration.getCategoryId());
    }

    private List<Long> getIntegrationWorkflowIds(Integration integration) {
        return integrationWorkflowService.getIntegrationWorkflowIds(
            integration.getId(), integration.getLastIntegrationVersion());
    }

    private IntegrationDTO toIntegrationDTO(Integration integration) {
        return new IntegrationDTO(
            getCategory(integration),
            componentDefinitionService.getComponentDefinition(integration.getComponentName(), null),
            integration, getIntegrationWorkflowIds(integration),
            tagService.getTags(integration.getTagIds()));
    }
}
