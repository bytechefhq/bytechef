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
import com.bytechef.ee.embedded.configuration.domain.IntegrationCodeWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationVersion;
import com.bytechef.ee.embedded.configuration.domain.IntegrationVersion.Status;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.service.IntegrationCodeWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.configuration.workflow.WorkflowPreDeleteListener;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final CodeWorkflowContainerService codeWorkflowContainerService;
    private final ComponentDefinitionService componentDefinitionService;
    private final IntegrationCodeWorkflowService integrationCodeWorkflowService;
    private final IntegrationService integrationService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final TagService tagService;
    private final List<WorkflowPreDeleteListener> workflowPreDeleteListeners;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;
    private final WorkflowNodeTestOutputService workflowNodeTestOutputService;

    @SuppressFBWarnings("EI2")
    public IntegrationFacadeImpl(
        CategoryService categoryService, CodeWorkflowContainerService codeWorkflowContainerService,
        ComponentDefinitionService componentDefinitionService,
        IntegrationCodeWorkflowService integrationCodeWorkflowService, IntegrationService integrationService,
        IntegrationWorkflowService integrationWorkflowService,
        IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        TagService tagService, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService,
        WorkflowNodeTestOutputService workflowNodeTestOutputService,
        List<WorkflowPreDeleteListener> workflowPreDeleteListeners) {

        this.categoryService = categoryService;
        this.codeWorkflowContainerService = codeWorkflowContainerService;
        this.componentDefinitionService = componentDefinitionService;
        this.integrationCodeWorkflowService = integrationCodeWorkflowService;
        this.integrationService = integrationService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.integrationInstanceConfigurationFacade = integrationInstanceConfigurationFacade;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.tagService = tagService;
        this.workflowService = workflowService;
        this.workflowPreDeleteListeners = workflowPreDeleteListeners;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
        this.workflowNodeTestOutputService = workflowNodeTestOutputService;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
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
    @PreAuthorize("isTenantAdmin()")
    public void deleteIntegration(long id) {
        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(id);

        for (IntegrationInstanceConfiguration integrationInstanceConfiguration : integrationInstanceConfigurations) {
            integrationInstanceConfigurationFacade.deleteIntegrationInstanceConfiguration(
                integrationInstanceConfiguration.getId());
        }

        List<IntegrationWorkflow> integrationWorkflows = integrationWorkflowService.getIntegrationWorkflows(id);

        for (IntegrationWorkflow integrationWorkflow : integrationWorkflows) {
            for (WorkflowPreDeleteListener workflowPreDeleteListener : workflowPreDeleteListeners) {
                workflowPreDeleteListener.onWorkflowPreDelete(integrationWorkflow.getWorkflowId());
            }
        }

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
    @PreAuthorize("isTenantAdmin()")
    public IntegrationDTO getIntegration(long id) {
        Integration integration = integrationService.getIntegration(id);

        return toIntegrationDTO(integration);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isTenantAdmin()")
    public List<IntegrationVersion> getIntegrationVersions(long id) {
        return integrationService.getIntegrationVersions(id);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isTenantAdmin()")
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

            // An integration whose component this instance does not have (uninstalled, or a name that never
            // resolved) maps with a null definition rather than failing the whole list.
            Map<String, ComponentDefinition> componentDefinitionMap = new HashMap<>();

            for (String componentName : integrations.stream()
                .map(Integration::getComponentName)
                .distinct()
                .toList()) {

                componentDefinitionService.fetchComponentDefinition(componentName, null)
                    .ifPresent(componentDefinition -> componentDefinitionMap.put(componentName, componentDefinition));
            }

            Set<Long> codeWorkflowIntegrationIds =
                Set.copyOf(integrationCodeWorkflowService.getCodeWorkflowIntegrationIds());

            // Resolved for the listed code integrations only. Without it the list reports every
            // code integration with a null language, and the client's badge falls back to a bare
            // "Code" instead of naming the language.
            Map<Long, String> codeWorkflowLanguages = new HashMap<>();

            for (Integration integration : integrations) {
                Long integrationId = integration.getId();

                if (!codeWorkflowIntegrationIds.contains(integrationId)) {
                    continue;
                }

                integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(integrationId)
                    .flatMap(this::fetchCodeWorkflowContainer)
                    .ifPresent(codeWorkflowContainer -> codeWorkflowLanguages.put(
                        integrationId, codeWorkflowContainer.getLanguage()
                            .name()));
            }

            return CollectionUtils.map(
                integrations,
                integration -> new IntegrationDTO(
                    CollectionUtils.findFirstFilterOrElse(
                        categories, category -> Objects.equals(integration.getCategoryId(), category.getId()), null),
                    componentDefinitionMap.get(integration.getComponentName()),
                    integration,
                    getIntegrationWorkflowIds(integration),
                    CollectionUtils.filter(
                        tags, tag -> CollectionUtils.contains(integration.getTagIds(), tag.getId())),
                    codeWorkflowIntegrationIds.contains(integration.getId()),
                    codeWorkflowLanguages.get(integration.getId())));
        } else {
            return CollectionUtils.map(integrations, IntegrationDTO::new);
        }
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
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
    @PreAuthorize("isTenantAdmin()")
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
        Optional<CodeWorkflowContainer> codeWorkflowContainer = integrationCodeWorkflowService
            .fetchIntegrationCodeWorkflow(integration.getId())
            .flatMap(this::fetchCodeWorkflowContainer);

        return new IntegrationDTO(
            getCategory(integration),
            componentDefinitionService.fetchComponentDefinition(integration.getComponentName(), null)
                .orElse(null),
            integration, getIntegrationWorkflowIds(integration),
            tagService.getTags(integration.getTagIds()), codeWorkflowContainer.isPresent(),
            codeWorkflowContainer.map(container -> container.getLanguage()
                .name())
                .orElse(null));
    }

    private Optional<CodeWorkflowContainer> fetchCodeWorkflowContainer(
        IntegrationCodeWorkflow integrationCodeWorkflow) {

        try {
            return Optional.of(
                codeWorkflowContainerService.getCodeWorkflowContainer(
                    integrationCodeWorkflow.getCodeWorkflowContainerId()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
