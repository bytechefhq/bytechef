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
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.embedded.configuration.constant.IntegrationErrorType;
import com.bytechef.embedded.configuration.domain.Integration;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.embedded.configuration.domain.IntegrationVersion.Status;
import com.bytechef.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.embedded.configuration.dto.WorkflowDTO;
import com.bytechef.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.embedded.configuration.service.IntegrationService;
import com.bytechef.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.component.registry.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.dto.UpdateParameterResultDTO;
import com.bytechef.platform.configuration.exception.ConfigurationException;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    private final ComponentDefinitionService componentDefinitionService;
    private final IntegrationService integrationService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final TagService tagService;
    private final WorkflowFacade workflowFacade;
    private final WorkflowNodeParameterFacade workflowNodeParameterFacade;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI2")
    public IntegrationFacadeImpl(
        CategoryService categoryService, ComponentDefinitionService componentDefinitionService,
        IntegrationService integrationService, IntegrationWorkflowService integrationWorkflowService,
        IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        TagService tagService, WorkflowFacade workflowFacade, WorkflowNodeParameterFacade workflowNodeParameterFacade,
        WorkflowService workflowService, WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.categoryService = categoryService;
        this.componentDefinitionService = componentDefinitionService;
        this.integrationService = integrationService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.integrationInstanceConfigurationFacade = integrationInstanceConfigurationFacade;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.tagService = tagService;
        this.workflowFacade = workflowFacade;
        this.workflowNodeParameterFacade = workflowNodeParameterFacade;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    public Workflow addWorkflow(long id, @NonNull String definition) {
        checkIntegrationStatus(id, null);

        Integration integration = integrationService.getIntegration(id);

        Workflow workflow = workflowService.create(definition, Format.JSON, SourceType.JDBC);

        int lastVersion = integration.fetchLastVersion()
            .orElseGet(() -> integrationService.addVersion(id));

        integrationWorkflowService.addWorkflow(id, lastVersion, workflow.getId());

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
    public IntegrationDTO createIntegration(@NonNull IntegrationDTO integrationDTO) {
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

        return new IntegrationDTO(category, integrationService.create(integration), List.of(), tags);
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

        integrationWorkflowService.removeWorkflow(
            integration.getId(), Validate.notNull(integration.getLastVersion(), "lastVersion"), workflowId);

        workflowService.delete(workflowId);
    }

    @Override
    public Map<String, ?> deleteWorkflowParameter(
        String workflowId, String workflowNodeName, String path, String name, Integer arrayIndex) {

        Integration project = integrationService.getWorkflowIntegration(workflowId);

        IntegrationWorkflowStatusResult integrationWorkflowStatusResult = checkIntegrationStatus(
            project.getId(), workflowId);

        if (integrationWorkflowStatusResult != null) {
            workflowId = integrationWorkflowStatusResult.workflowId;
        }

        return workflowNodeParameterFacade.deleteParameter(workflowId, workflowNodeName, path, name, arrayIndex);
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
    public WorkflowDTO getIntegrationWorkflow(String workflowId) {
        IntegrationWorkflow integrationWorkflow = integrationWorkflowService.getWorkflowIntegrationWorkflow(workflowId);

        return new WorkflowDTO(workflowFacade.getWorkflow(workflowId), integrationWorkflow.getId());
    }

    @Override
    public WorkflowDTO getIntegrationWorkflow(long integrationWorkflowId) {
        IntegrationWorkflow integrationWorkflow = integrationWorkflowService.getIntegrationWorkflow(
            integrationWorkflowId);

        return new WorkflowDTO(
            workflowFacade.getWorkflow(integrationWorkflow.getWorkflowId()), integrationWorkflow.getId());
    }

    @Override
    public List<WorkflowDTO> getIntegrationWorkflows() {
        List<IntegrationWorkflow> integrationWorkflows = integrationWorkflowService.getIntegrationWorkflows();

        return workflowService.getWorkflows(
            integrationWorkflows.stream()
                .map(IntegrationWorkflow::getWorkflowId)
                .toList())
            .stream()
            .map(workflow -> new WorkflowDTO(workflow, getIntegrationWorkflowId(workflow, integrationWorkflows)))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowDTO> getIntegrationWorkflows(long id) {
        List<WorkflowDTO> workflowDTOs = List.of();

        Integration integration = integrationService.getIntegration(id);

        List<IntegrationWorkflow> integrationWorkflows = integration.fetchLastVersion()
            .map(
                lastVersion -> integrationWorkflowService.getIntegrationWorkflows(
                    integration.getId(), lastVersion))
            .orElse(List.of());

        if (!integrationWorkflows.isEmpty()) {
            workflowDTOs = workflowService.getWorkflows(
                integrationWorkflows.stream()
                    .map(IntegrationWorkflow::getWorkflowId)
                    .toList())
                .stream()
                .map(workflow -> new WorkflowDTO(workflow, getIntegrationWorkflowId(workflow, integrationWorkflows)))
                .toList();
        }

        return workflowDTOs;
    }

    @Override
    public List<WorkflowDTO> getIntegrationVersionWorkflows(long id, int integrationVersion) {
        List<IntegrationWorkflow> integrationWorkflows = integrationWorkflowService.getIntegrationWorkflows(
            id, integrationVersion);

        return workflowService.getWorkflows(
            integrationWorkflows.stream()
                .map(IntegrationWorkflow::getWorkflowId)
                .toList())
            .stream()
            .map(workflow -> new WorkflowDTO(workflow, getIntegrationWorkflowId(workflow, integrationWorkflows)))
            .toList();
    }

    @Override
    public List<IntegrationDTO> getIntegrations(Environment environment) {
        List<IntegrationDTO> integrations = List.of();

        List<Long> integrationIds = integrationInstanceConfigurationService.getIntegrationIds(environment);

        if (!integrationIds.isEmpty()) {
            integrations = integrationService.getIntegrations(null, integrationIds, null, null)
                .stream()
                .map(integration -> new IntegrationDTO(
                    integration.getCategoryId() == null
                        ? null : categoryService.getCategory(integration.getCategoryId()),
                    componentDefinitionService.getComponentDefinition(
                        integration.getComponentName(), integration.getComponentVersion()),
                    integration))
                .toList();
        }

        return integrations;
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
                getIntegrationWorkflowIds(integration), CollectionUtils.filter(
                    tagService.getTags(
                        integrations.stream()
                            .flatMap(curIntegration -> CollectionUtils.stream(curIntegration.getTagIds()))
                            .filter(Objects::nonNull)
                            .toList()),
                    tag -> CollectionUtils.contains(integration.getTagIds(), tag.getId()))));
    }

    @Override
    public IntegrationDTO updateIntegration(@NonNull IntegrationDTO integrationDTO) {
        Category category = integrationDTO.category() == null ? null : categoryService.save(integrationDTO.category());
        List<Tag> tags = CollectionUtils.isEmpty(integrationDTO.tags())
            ? Collections.emptyList()
            : tagService.save(integrationDTO.tags());

        Integration integration = integrationDTO.toIntegration();

        integration.setTags(tags);

        return new IntegrationDTO(
            category, integrationService.update(integration), getIntegrationWorkflowIds(integration), tags);
    }

    @Override
    public void updateIntegrationTags(long id, @NonNull List<Tag> tags) {
        tags = CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);

        integrationService.update(id, CollectionUtils.map(tags, Tag::getId));
    }

    @Override
    public WorkflowDTO updateWorkflow(String workflowId, String definition, int version) {
        IntegrationWorkflow integrationWorkflow = integrationWorkflowService.getWorkflowIntegrationWorkflow(workflowId);

        IntegrationWorkflowStatusResult integrationWorkflowStatusResult = checkIntegrationStatus(
            integrationWorkflow.getIntegrationId(), workflowId);

        if (integrationWorkflowStatusResult != null) {
            workflowId = integrationWorkflowStatusResult.workflowId;
            version = integrationWorkflowStatusResult.version;
        }

        return new WorkflowDTO(workflowFacade.update(workflowId, definition, version), integrationWorkflow.getId());
    }

    @Override
    public UpdateParameterResultDTO updateWorkflowParameter(
        String workflowId, String workflowNodeName, String path, String name, Integer arrayIndex, Object value) {

        Integration integration = integrationService.getWorkflowIntegration(workflowId);

        IntegrationWorkflowStatusResult projectWorkflowStatusResult = checkIntegrationStatus(
            integration.getId(), workflowId);

        if (projectWorkflowStatusResult != null) {
            workflowId = projectWorkflowStatusResult.workflowId;
        }

        return workflowNodeParameterFacade.updateParameter(workflowId, workflowNodeName, path, name, arrayIndex, value);
    }

    private IntegrationWorkflowStatusResult checkIntegrationStatus(long id, @Nullable String workflowId) {
        Integration integration = integrationService.getIntegration(id);

        final List<IntegrationWorkflow> latestIntegrationWorkflows = new ArrayList<>();

        if (integration.getLastVersion() != null) {
            latestIntegrationWorkflows.addAll(
                integrationWorkflowService.getIntegrationWorkflows(
                    integration.getId(), integration.getLastVersion()));

            List<String> latestWorkflowIds = latestIntegrationWorkflows.stream()
                .map(IntegrationWorkflow::getWorkflowId)
                .toList();

            if (workflowId != null && !latestWorkflowIds.contains(workflowId)) {
                throw new ConfigurationException(
                    "Older version of the workflow cannot be updated.", IntegrationErrorType.UPDATE_OLD_WORKFLOW);
            }
        }

        return integration.fetchLastStatus()
            .map(lastStatus -> {
                IntegrationWorkflowStatusResult integrationWorkflowStatusResult = null;

                if (lastStatus == Status.PUBLISHED) {
                    int lastVersion = integration.getLastVersion();
                    int newVersion = integrationService.addVersion(id);

                    for (IntegrationWorkflow integrationWorkflow : latestIntegrationWorkflows) {
                        String oldWorkflowId = integrationWorkflow.getWorkflowId();

                        Workflow duplicatedWorkflow = workflowService.duplicateWorkflow(oldWorkflowId);

                        if (Objects.equals(workflowId, oldWorkflowId)) {
                            integrationWorkflowStatusResult = new IntegrationWorkflowStatusResult(
                                duplicatedWorkflow.getId(), duplicatedWorkflow.getVersion());
                        }

                        integrationWorkflow.setIntegrationVersion(newVersion);
                        integrationWorkflow.setWorkflowId(duplicatedWorkflow.getId());

                        integrationWorkflowService.update(integrationWorkflow);
                        integrationWorkflowService.addWorkflow(integration.getId(), lastVersion, oldWorkflowId);
                        workflowTestConfigurationService.updateWorkflowId(oldWorkflowId, duplicatedWorkflow.getId());
                    }
                }

                return integrationWorkflowStatusResult;
            })
            .orElse(null);
    }

    private Category getCategory(Integration integration) {
        return integration.getCategoryId() == null ? null : categoryService.getCategory(integration.getCategoryId());
    }

    private static Long getIntegrationWorkflowId(Workflow workflow, List<IntegrationWorkflow> integrationWorkflows) {
        return CollectionUtils.getFirst(
            integrationWorkflows,
            integrationWorkflow -> Objects.equals(integrationWorkflow.getWorkflowId(), workflow.getId()))
            .getId();
    }

    private List<Long> getIntegrationWorkflowIds(Integration integration) {
        return OptionalUtils.mapOrElse(
            integration.fetchLastVersion(),
            lastVersion -> integrationWorkflowService.getIntegrationWorkflowIds(integration.getId(), lastVersion),
            List.of());
    }

    private IntegrationDTO toIntegrationDTO(Integration integration) {
        return new IntegrationDTO(
            getCategory(integration), integration, getIntegrationWorkflowIds(integration),
            tagService.getTags(integration.getTagIds()));
    }

    private record IntegrationWorkflowStatusResult(String workflowId, int version) {
    }
}
