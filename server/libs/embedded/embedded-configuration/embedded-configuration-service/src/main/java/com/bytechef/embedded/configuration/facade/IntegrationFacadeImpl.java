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
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.embedded.configuration.constant.IntegrationErrorType;
import com.bytechef.embedded.configuration.domain.Integration;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.embedded.configuration.domain.IntegrationVersion.Status;
import com.bytechef.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.embedded.configuration.service.IntegrationService;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.configuration.exception.ApplicationException;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class IntegrationFacadeImpl implements IntegrationFacade {

    private final CategoryService categoryService;
    private final IntegrationService integrationService;
    private final JobService jobService;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final TagService tagService;
    private final WorkflowFacade workflowFacade;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public IntegrationFacadeImpl(
        CategoryService categoryService, IntegrationService integrationService, JobService jobService,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        TagService tagService, WorkflowFacade workflowFacade, WorkflowService workflowService) {

        this.categoryService = categoryService;
        this.integrationService = integrationService;
        this.jobService = jobService;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.tagService = tagService;
        this.workflowFacade = workflowFacade;
        this.workflowService = workflowService;
    }

    @Override
    public Workflow addWorkflow(long id, @NonNull String definition) {
        checkIntegrationStatus(id, null);

        Workflow workflow = workflowService.create(definition, Format.JSON, SourceType.JDBC);

        integrationService.addWorkflow(id, workflow.getId());

        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(id);

        for (IntegrationInstanceConfiguration integrationInstanceConfiguration : integrationInstanceConfigurations) {
            if (integrationInstanceConfiguration.getIntegrationVersion() == null) {
                continue;
            }

            IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                new IntegrationInstanceConfigurationWorkflow();

            integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
                integrationInstanceConfiguration.getId());
            integrationInstanceConfigurationWorkflow.setWorkflowId(workflow.getId());

            integrationInstanceConfigurationWorkflowService.create(integrationInstanceConfigurationWorkflow);
        }

        return workflow;
    }

    @Override
    public void checkIntegrationStatus(long id, @Nullable String workflowId) {
        Integration integration = integrationService.getIntegration(id);

        if (integration.getLastVersion() != null) {
            List<String> latestWorkflowIds = integration.getWorkflowIds(integration.getLastVersion());

            if (workflowId != null && !latestWorkflowIds.contains(workflowId)) {
                throw new ApplicationException(
                    "Older version of the workflow cannot be updated.", IntegrationErrorType.UPDATE_OLD_WORKFLOW);
            }
        }

        integration.fetchLastStatus()
            .ifPresent(lastStatus -> {
                if (lastStatus == Status.PUBLISHED) {
                    List<String> duplicatedVersionWorkflowIds = new ArrayList<>();

                    for (String curWorkflowId : integration.getWorkflowIds(
                        OptionalUtils.get(integration.fetchLastVersion()))) {

                        Workflow duplicatedWorkflow = workflowService.duplicateWorkflow(curWorkflowId);

                        jobService.updateWorkflowId(curWorkflowId, duplicatedWorkflow.getId());

                        duplicatedVersionWorkflowIds.add(duplicatedWorkflow.getId());

                        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations =
                            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(
                                integration.getId());

                        for (IntegrationInstanceConfiguration integrationInstanceConfiguration : integrationInstanceConfigurations) {

                            List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows =
                                integrationInstanceConfigurationWorkflowService
                                    .getIntegrationInstanceConfigurationWorkflows(
                                        integrationInstanceConfiguration.getId());

                            for (IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow : integrationInstanceConfigurationWorkflows) {

                                if (Objects.equals(
                                    integrationInstanceConfigurationWorkflow.getWorkflowId(), curWorkflowId)) {

                                    integrationInstanceConfigurationWorkflow.setWorkflowId(duplicatedWorkflow.getId());

                                    integrationInstanceConfigurationWorkflowService.update(
                                        integrationInstanceConfigurationWorkflow);
                                }
                            }
                        }
                    }

                    integrationService.addVersion(id, duplicatedVersionWorkflowIds);
                }
            });
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

        if (!CollectionUtils.isEmpty(integrationDTO.tags())) {
            tags = tagService.save(integrationDTO.tags());

            integration.setTags(tags);
        }

        return new IntegrationDTO(category, integrationService.create(integration), tags);
    }

    @Override
    public void deleteIntegration(long id) {
        if (!CollectionUtils.isEmpty(
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(id))) {

            throw new ApplicationException(
                "Integration id=%s cannot be deleted".formatted(id), IntegrationErrorType.DELETE_INTEGRATION);
        }

        Integration integration = integrationService.getIntegration(id);

        List<String> workflowIds = integration.getAllWorkflowIds();

        for (String workflowId : workflowIds) {
            deleteWorkflow(workflowId);
        }

        integrationService.delete(id);

// TODO find a way to delete ll tags not referenced anymore
//        integration.getTagIds()
//            .forEach(tagService::delete);
    }

    @Override
    public void deleteWorkflow(@NonNull String workflowId) {
        Integration integration = integrationService.getWorkflowIntegration(workflowId);

        checkIntegrationStatus(integration.getId(), workflowId);

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

        integrationService.removeWorkflow(integration.getId(), workflowId);

        workflowService.delete(workflowId);
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrationDTO getIntegration(long id) {
        Integration integration = integrationService.getIntegration(id);

        return getIntegrationDTO(integration);
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
    public List<IntegrationDTO> getIntegrations(
        Long categoryId, boolean integrationInstances, Long tagId, Status status) {

        List<Long> integrationIds = List.of();

        if (integrationInstances) {
            integrationIds = integrationInstanceConfigurationService.getIntegrationIds();
        }

        List<Integration> integrations = integrationService.getIntegrations(categoryId, integrationIds, tagId, status);

        return CollectionUtils.map(
            integrations,
            integration -> new IntegrationDTO(
                CollectionUtils.findFirstFilterOrElse(
                    categoryService.getCategories(
                        integrations.stream()
                            .map(Integration::getCategoryId)
                            .filter(Objects::nonNull)
                            .toList()),
                    category -> Objects.equals(integration.getCategoryId(), category.getId()),
                    null),
                integration,
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
        List<Integration> integrations = integrationService.getIntegrations();

        List<Long> tagIds = integrations.stream()
            .map(Integration::getTagIds)
            .flatMap(Collection::stream)
            .toList();

        return tagService.getTags(tagIds);
    }

    @Override
    public List<Workflow> getIntegrationWorkflows() {
        return workflowService.getWorkflows(
            integrationService.getIntegrations()
                .stream()
                .flatMap(integration -> CollectionUtils.stream(integration.getAllWorkflowIds()))
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Workflow> getIntegrationWorkflows(long id) {
        Integration integration = integrationService.getIntegration(id);

        return integration.fetchLastVersion()
            .map(lastVersion -> workflowService.getWorkflows(integration.getWorkflowIds(lastVersion)))
            .orElse(List.of());
    }

    @Override
    public List<Workflow> getIntegrationVersionWorkflows(long id, int integrationVersion) {
        Integration integration = integrationService.getIntegration(id);

        return workflowService.getWorkflows(
            integration.fetchLastVersion()
                .map(integration::getWorkflowIds)
                .orElse(List.of()));
    }

    @Override
    public IntegrationDTO updateIntegration(@NonNull IntegrationDTO integrationDTO) {
        Category category = integrationDTO.category() == null ? null : categoryService.save(integrationDTO.category());
        List<Tag> tags = CollectionUtils.isEmpty(integrationDTO.tags())
            ? Collections.emptyList()
            : tagService.save(integrationDTO.tags());

        Integration integration = integrationDTO.toIntegration();

        integration.setTags(tags);

        return new IntegrationDTO(category, integrationService.update(integration), tags);
    }

    @Override
    public void updateIntegrationTags(long id, @NonNull List<Tag> tags) {
        tags = CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);

        integrationService.update(id, CollectionUtils.map(tags, Tag::getId));
    }

    @Override
    public WorkflowDTO updateWorkflow(String workflowId, String definition, int version) {
        Integration project = integrationService.getWorkflowIntegration(workflowId);

        checkIntegrationStatus(project.getId(), workflowId);

        return workflowFacade.update(workflowId, definition, version);
    }

    private Category getCategory(Integration integration) {
        return integration.getCategoryId() == null ? null : categoryService.getCategory(integration.getCategoryId());
    }

    private IntegrationDTO getIntegrationDTO(Integration integration) {
        return new IntegrationDTO(getCategory(integration), integration, tagService.getTags(integration.getTagIds()));
    }
}
