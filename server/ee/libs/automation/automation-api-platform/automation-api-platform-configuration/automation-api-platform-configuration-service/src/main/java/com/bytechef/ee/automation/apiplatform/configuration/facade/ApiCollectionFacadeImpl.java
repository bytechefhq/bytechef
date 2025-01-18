/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.facade;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionEndpointDTO;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionEndpointService;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ApiCollectionFacadeImpl implements ApiCollectionFacade {

    private final ApiCollectionService apiCollectionService;
    private final ApiCollectionEndpointService apiCollectionEndpointService;
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final TagService tagService;

    @SuppressFBWarnings("EI")
    public ApiCollectionFacadeImpl(
        ApiCollectionService apiCollectionService, ApiCollectionEndpointService apiCollectionEndpointService,
        ProjectDeploymentFacade projectDeploymentFacade, ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, TagService tagService) {

        this.apiCollectionService = apiCollectionService;
        this.apiCollectionEndpointService = apiCollectionEndpointService;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.tagService = tagService;
    }

    @Override
    public ApiCollectionDTO createApiCollection(@NonNull ApiCollectionDTO apiCollectionDTO) {
        ApiCollection apiCollection = apiCollectionDTO.toApiCollection();

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setDescription(apiCollection.getDescription());
        projectDeployment.setEnvironment(Environment.TEST);
        projectDeployment.setName(apiCollection.getName());
        projectDeployment.setProjectId(apiCollectionDTO.projectId());
        projectDeployment.setProjectVersion(apiCollectionDTO.projectVersion());

        projectDeployment = projectDeploymentService.create(projectDeployment);

        apiCollection.setProjectDeploymentId(projectDeployment.getId());

        return toApiCollectionDTO(apiCollectionService.create(apiCollection));
    }

    @Override
    public ApiCollectionEndpointDTO createApiCollectionEndpoint(
        @NonNull ApiCollectionEndpointDTO apiCollectionEndpointDTO) {

        ApiCollectionEndpoint apiCollectionEndpoint = apiCollectionEndpointDTO.toApiCollectionEndpoint();

        ApiCollection apiCollection = apiCollectionService.getApiCollection(
            apiCollectionEndpoint.getApiCollectionId());

        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowService
            .fetchProjectDeploymentWorkflow(
                apiCollection.getProjectDeploymentId(),
                projectWorkflowService.getProjectDeploymentProjectWorkflowWorkflowId(
                    apiCollection.getProjectDeploymentId(), apiCollectionEndpoint.getWorkflowReferenceCode()))
            .orElseGet(() -> {
                ProjectDeploymentWorkflow newProjectDeploymentWorkflow = new ProjectDeploymentWorkflow();

                newProjectDeploymentWorkflow.setProjectDeploymentId(apiCollection.getProjectDeploymentId());
                newProjectDeploymentWorkflow.setWorkflowId(
                    projectWorkflowService.getProjectDeploymentProjectWorkflowWorkflowId(
                        apiCollection.getProjectDeploymentId(), apiCollectionEndpoint.getWorkflowReferenceCode()));

                return projectDeploymentWorkflowService.create(newProjectDeploymentWorkflow);
            });

        apiCollectionEndpoint.setProjectDeploymentWorkflowId(projectDeploymentWorkflow.getId());

        return new ApiCollectionEndpointDTO(
            apiCollectionEndpointService.create(apiCollectionEndpoint), projectDeploymentWorkflow);
    }

    @Override
    public void deleteApiCollection(long id) {
        ApiCollection apiCollection = apiCollectionService.getApiCollection(id);

        for (ApiCollectionEndpoint apiCollectionEndpoint : apiCollectionEndpointService.getApiEndpoints(id)) {
            apiCollectionEndpointService.delete(apiCollectionEndpoint.getId());
        }

        apiCollectionService.delete(id);
        projectDeploymentFacade.deleteProjectDeployment(apiCollection.getProjectDeploymentId());
    }

    @Override
    public ApiCollectionDTO getApiCollection(long id) {
        return toApiCollectionDTO(apiCollectionService.getApiCollection(id));
    }

    @Override
    public List<ApiCollectionDTO> getApiCollections(
        long workspaceId, Environment environment, Long projectId, Long tagId) {

        return apiCollectionService.getApiCollections(workspaceId, environment, projectId, tagId)
            .stream()
            .map(this::toApiCollectionDTO)
            .toList();
    }

    @Override
    public List<Tag> getApiCollectionTags() {
        List<ApiCollection> apiCollections = apiCollectionService.getApiCollections(null, null, null, null);

        return tagService.getTags(
            apiCollections
                .stream()
                .map(ApiCollection::getTagIds)
                .flatMap(Collection::stream)
                .toList());
    }

    @Override
    public ApiCollectionDTO updateApiCollection(@NonNull ApiCollectionDTO apiCollectionDTO) {
        ApiCollection apiCollection = apiCollectionDTO.toApiCollection();

        List<Tag> tags = checkTags(apiCollectionDTO.tags());

        if (!tags.isEmpty()) {
            apiCollection.setTags(tags);
        }

        return toApiCollectionDTO(apiCollectionService.update(apiCollection));
    }

    @Override
    public ApiCollectionEndpointDTO updateApiCollectionEndpoint(ApiCollectionEndpointDTO apiCollectionEndpointDTO) {
        ApiCollectionEndpoint apiCollectionEndpoint = apiCollectionEndpointDTO.toApiCollectionEndpoint();

        apiCollectionEndpoint = apiCollectionEndpointService.update(apiCollectionEndpoint);

        return new ApiCollectionEndpointDTO(
            apiCollectionEndpoint, projectDeploymentWorkflowService.getProjectDeploymentWorkflow(
                apiCollectionEndpoint.getProjectDeploymentWorkflowId()));
    }

    @Override
    public void updateApiCollectionTags(long id, List<Tag> tags) {
        tags = checkTags(tags);

        apiCollectionService.update(id, CollectionUtils.map(tags, Tag::getId));
    }

    private List<Tag> checkTags(List<Tag> tags) {
        return CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);
    }

    private ApiCollectionDTO toApiCollectionDTO(ApiCollection apiCollection) {
        Project project = projectService.getProjectDeploymentProject(apiCollection.getProjectDeploymentId());

        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(
            apiCollection.getProjectDeploymentId());

        List<ApiCollectionEndpointDTO> apiCollectionEndpointDTOs = apiCollectionEndpointService.getApiEndpoints(
            apiCollection.getId())
            .stream()
            .map(apiCollectionEndpoint -> new ApiCollectionEndpointDTO(
                apiCollectionEndpoint, projectDeploymentWorkflowService.getProjectDeploymentWorkflow(
                    apiCollectionEndpoint.getProjectDeploymentWorkflowId())))
            .toList();

        return new ApiCollectionDTO(
            apiCollection, apiCollectionEndpointDTOs, project, projectDeployment,
            tagService.getTags(apiCollection.getTagIds()));
    }
}
