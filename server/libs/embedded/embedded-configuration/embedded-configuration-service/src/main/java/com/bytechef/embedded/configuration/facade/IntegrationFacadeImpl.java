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

package com.bytechef.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.domain.Workflow.SourceType;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.embedded.configuration.domain.Integration;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.embedded.configuration.domain.IntegrationVersion;
import com.bytechef.embedded.configuration.domain.IntegrationVersion.Status;
import com.bytechef.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.embedded.configuration.service.IntegrationService;
import com.bytechef.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class IntegrationFacadeImpl implements IntegrationFacade {

    private final CategoryService categoryService;
    private final ComponentDefinitionService componentDefinitionService;
    private final IntegrationService integrationService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final TagService tagService;
    private final WorkflowFacade workflowFacade;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;
    private final WorkflowNodeTestOutputService workflowNodeTestOutputService;

    @SuppressFBWarnings("EI2")
    public IntegrationFacadeImpl(
        CategoryService categoryService, ComponentDefinitionService componentDefinitionService,
        IntegrationService integrationService, IntegrationWorkflowService integrationWorkflowService,
        IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        TagService tagService, WorkflowFacade workflowFacade, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService,
        WorkflowNodeTestOutputService workflowNodeTestOutputService) {

        this.categoryService = categoryService;
        this.componentDefinitionService = componentDefinitionService;
        this.integrationService = integrationService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.integrationInstanceConfigurationFacade = integrationInstanceConfigurationFacade;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.tagService = tagService;
        this.workflowFacade = workflowFacade;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
        this.workflowNodeTestOutputService = workflowNodeTestOutputService;
    }

    @Override
    public long addWorkflow(long id, @NonNull String definition) {
        Integration integration = integrationService.getIntegration(id);

        Workflow workflow = workflowService.create(definition, Format.JSON, SourceType.JDBC);

        IntegrationWorkflow integrationWorkflow = integrationWorkflowService.addWorkflow(
            id, integration.getLastIntegrationVersion(), workflow.getId());

        return integrationWorkflow.getId();
    }

    @Override
    public long createIntegration(@NonNull IntegrationDTO integrationDTO) {
        Integration integration = integrationDTO.toIntegration();

        Category category = integrationDTO.category();

        if (integrationDTO.category() != null) {
            category = categoryService.save(category);

            integration.setCategory(category);
        }

        List<Tag> tags = integrationDTO.tags();

        if (!CollectionUtils.isEmpty(integrationDTO.tags())) {
            tags = tagService.save(integrationDTO.tags());

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

        for (IntegrationWorkflow integrationWorkflow : integrationWorkflows) {
            workflowService.delete(integrationWorkflow.getWorkflowId());
        }

        integrationWorkflowService.deleteIntegrationWorkflows(
            integrationWorkflows.stream()
                .map(IntegrationWorkflow::getId)
                .toList());

        integrationService.delete(id);

// TODO find a way to delete ll tags not referenced anymore
//        integration.getTagIds()
//            .forEach(tagService::delete);
    }

    @Override
    public void deleteWorkflow(@NonNull String workflowId) {
        Integration integration = integrationService.getWorkflowIntegration(workflowId);

        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(integration.getId());

        for (IntegrationInstanceConfiguration integrationInstanceConfiguration : integrationInstanceConfigurations) {
            List<IntegrationInstanceConfigurationWorkflow> projectInstanceWorkflows =
                integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                    Validate.notNull(integrationInstanceConfiguration.getId(), "id"));

            if (CollectionUtils.anyMatch(
                projectInstanceWorkflows,
                projectInstanceWorkflow -> Objects.equals(projectInstanceWorkflow.getWorkflowId(), workflowId))) {

                projectInstanceWorkflows.stream()
                    .filter(
                        projectInstanceWorkflow -> Objects.equals(projectInstanceWorkflow.getWorkflowId(), workflowId))
                    .findFirst()
                    .ifPresent(
                        projectInstanceWorkflow -> integrationInstanceConfigurationWorkflowService.delete(
                            projectInstanceWorkflow.getId()));
            }
        }

        integrationWorkflowService.removeWorkflow(
            integration.getId(), Validate.notNull(integration.getLastIntegrationVersion(), "lastVersion"), workflowId);

        workflowService.delete(workflowId);
    }

    @Override
    public List<IntegrationDTO> getEnabledIntegrationInstanceConfigurationIntegrations(Environment environment) {
        List<IntegrationDTO> integrationDTOs = List.of();

        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations =
            integrationInstanceConfigurationService.getEnabledIntegrationInstanceConfigurations(environment);

        if (!integrationInstanceConfigurations.isEmpty()) {
            List<Long> integrationIds = integrationInstanceConfigurations.stream()
                .map(IntegrationInstanceConfiguration::getIntegrationId)
                .toList();

            List<Integration> integrations = integrationService.getIntegrations(integrationIds);

            integrationDTOs = integrationInstanceConfigurations.stream()
                .map(integrationInstanceConfiguration -> {
                    Integration integration = integrations.stream()
                        .filter(curIntegration -> Objects.equals(
                            curIntegration.getId(), integrationInstanceConfiguration.getIntegrationId()))
                        .findFirst()
                        .orElseThrow();

                    IntegrationVersion lastIntegrationVersion = integration.getIntegrationVersions()
                        .stream()
                        .filter(integrationVersion -> integrationVersion
                            .getVersion() <= integrationInstanceConfiguration.getIntegrationVersion())
                        .max(Comparator.comparingInt(IntegrationVersion::getVersion))
                        .orElseThrow();

                    return new IntegrationDTO(
                        integration.getCategoryId() == null
                            ? null : categoryService.getCategory(integration.getCategoryId()),
                        componentDefinitionService.getComponentDefinition(integration.getComponentName(), null),
                        integration, getIntegrationWorkflowIds(integration), lastIntegrationVersion.getPublishedDate(),
                        lastIntegrationVersion.getStatus(), lastIntegrationVersion.getVersion());
                })
                .toList();
        }

        return integrationDTOs;
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrationDTO getIntegration(long id) {
        Integration integration = integrationService.getIntegration(id);

        return toIntegrationDTO(integration);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getIntegrationCategories() {
        List<Integration> integrations = integrationService.getIntegrations();

        List<Long> categoryIds = integrations.stream()
            .map(Integration::getCategoryId)
            .filter(Objects::nonNull)
            .toList();

        return categoryService.getCategories(categoryIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> getIntegrationTags() {
        List<Integration> integrations = integrationService.getIntegrations();

        List<Long> tagIds = integrations.stream()
            .map(Integration::getTagIds)
            .flatMap(Collection::stream)
            .toList();

        return tagService.getTags(tagIds);
    }

    @Override
    public List<IntegrationWorkflowDTO> getIntegrationVersionWorkflows(long id, int integrationVersion) {
        List<IntegrationWorkflow> integrationWorkflows = integrationWorkflowService.getIntegrationWorkflows(
            id, integrationVersion);

        return CollectionUtils.map(
            integrationWorkflows,
            integrationWorkflow -> new IntegrationWorkflowDTO(
                workflowFacade.getWorkflow(integrationWorkflow.getWorkflowId()), integrationWorkflow));
    }

    @Override
    public IntegrationWorkflowDTO getIntegrationWorkflow(String workflowId) {
        IntegrationWorkflow integrationWorkflow = integrationWorkflowService.getWorkflowIntegrationWorkflow(workflowId);

        return new IntegrationWorkflowDTO(workflowFacade.getWorkflow(workflowId), integrationWorkflow);
    }

    @Override
    public IntegrationWorkflowDTO getIntegrationWorkflow(long integrationWorkflowId) {
        IntegrationWorkflow integrationWorkflow = integrationWorkflowService.getIntegrationWorkflow(
            integrationWorkflowId);

        return new IntegrationWorkflowDTO(
            workflowFacade.getWorkflow(integrationWorkflow.getWorkflowId()), integrationWorkflow);
    }

    @Override
    public List<IntegrationWorkflowDTO> getIntegrationWorkflows() {
        List<IntegrationWorkflow> integrationWorkflows = integrationWorkflowService.getIntegrationWorkflows();

        return CollectionUtils.map(
            integrationWorkflows,
            integrationWorkflow -> new IntegrationWorkflowDTO(
                workflowFacade.getWorkflow(integrationWorkflow.getWorkflowId()), integrationWorkflow));
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationWorkflowDTO> getIntegrationWorkflows(long id) {
        List<IntegrationWorkflowDTO> workflowDTOs = List.of();

        Integration integration = integrationService.getIntegration(id);

        List<IntegrationWorkflow> integrationWorkflows = integrationWorkflowService.getIntegrationWorkflows(
            integration.getId(), integration.getLastIntegrationVersion());

        if (!integrationWorkflows.isEmpty()) {
            workflowDTOs = CollectionUtils.map(
                integrationWorkflows,
                integrationWorkflow -> new IntegrationWorkflowDTO(
                    workflowFacade.getWorkflow(integrationWorkflow.getWorkflowId()), integrationWorkflow));
        }

        return workflowDTOs;
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationDTO> getIntegrations(
        Long categoryId, boolean integrationInstanceConfigurations, Long tagId, Status status) {

        List<Long> integrationIds = List.of();

        if (integrationInstanceConfigurations) {
            integrationIds = integrationInstanceConfigurationService.getIntegrationIds();

            if (integrationIds.isEmpty()) {
                return List.of();
            }
        }

        List<Integration> integrations = integrationService.getIntegrations(categoryId, integrationIds, tagId, status);

        return CollectionUtils.map(
            integrations,
            integration -> new IntegrationDTO(
                CollectionUtils.findFirstFilterOrElse(
                    categoryService.getCategories(
                        integrations
                            .stream()
                            .map(Integration::getCategoryId)
                            .filter(Objects::nonNull)
                            .toList()),
                    category -> Objects.equals(integration.getCategoryId(), category.getId()),
                    null),
                integration,
                getIntegrationWorkflowIds(integration),
                CollectionUtils.filter(
                    tagService.getTags(
                        integrations.stream()
                            .flatMap(curIntegration -> CollectionUtils.stream(curIntegration.getTagIds()))
                            .filter(Objects::nonNull)
                            .toList()),
                    tag -> CollectionUtils.contains(integration.getTagIds(), tag.getId()))));
    }

    @Override
    public void publishIntegration(long id, String description) {
        Integration integration = integrationService.getIntegration(id);

        int oldIntegrationVersion = integration.getLastIntegrationVersion();

        List<IntegrationWorkflow> oldIntegrationWorkflows = integrationWorkflowService
            .getIntegrationWorkflows(integration.getId(), oldIntegrationVersion);

        int newIntegrationVersion = integrationService.publishIntegration(id, description);

        for (IntegrationWorkflow integrationWorkflow : oldIntegrationWorkflows) {
            String oldWorkflowId = integrationWorkflow.getWorkflowId();

            Workflow duplicatedWorkflow = workflowService.duplicateWorkflow(oldWorkflowId);

            integrationWorkflow.setIntegrationVersion(newIntegrationVersion);
            integrationWorkflow.setWorkflowId(duplicatedWorkflow.getId());

            integrationWorkflowService.update(integrationWorkflow);

            integrationWorkflowService.addWorkflow(
                integration.getId(), oldIntegrationVersion, oldWorkflowId,
                integrationWorkflow.getWorkflowReferenceCode());

            workflowTestConfigurationService.updateWorkflowId(oldWorkflowId, duplicatedWorkflow.getId());
            workflowNodeTestOutputService.updateWorkflowId(oldWorkflowId, duplicatedWorkflow.getId());
        }
    }

    @Override
    public void updateIntegration(@NonNull IntegrationDTO integrationDTO) {
        List<Tag> tags = CollectionUtils.isEmpty(integrationDTO.tags())
            ? Collections.emptyList()
            : tagService.save(integrationDTO.tags());

        Integration integration = integrationDTO.toIntegration();

        integration.setTags(tags);

        integrationService.update(integration);
    }

    @Override
    public void updateIntegrationTags(long id, @NonNull List<Tag> tags) {
        tags = CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);

        integrationService.update(id, CollectionUtils.map(tags, Tag::getId));
    }

    @Override
    public void updateWorkflow(String workflowId, String definition, int version) {
        workflowService.update(workflowId, definition, version);
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
            getCategory(integration), integration, getIntegrationWorkflowIds(integration),
            tagService.getTags(integration.getTagIds()));
    }
}
