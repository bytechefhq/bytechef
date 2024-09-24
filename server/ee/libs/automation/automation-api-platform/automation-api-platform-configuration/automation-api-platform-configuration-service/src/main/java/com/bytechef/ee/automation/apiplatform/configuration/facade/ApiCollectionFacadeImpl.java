/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.facade;

import com.bytechef.automation.configuration.domain.ProjectInstance;
import com.bytechef.automation.configuration.domain.ProjectInstanceWorkflow;
import com.bytechef.automation.configuration.service.ProjectInstanceService;
import com.bytechef.automation.configuration.service.ProjectInstanceWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
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
    private final ProjectInstanceService projectInstanceService;
    private final ProjectInstanceWorkflowService projectInstanceWorkflowService;
    private final ProjectWorkflowService projectWorkflowService;
    private final TagService tagService;

    @SuppressFBWarnings("EI")
    public ApiCollectionFacadeImpl(
        ApiCollectionService apiCollectionService, ApiCollectionEndpointService apiCollectionEndpointService,
        ProjectInstanceService projectInstanceService, ProjectInstanceWorkflowService projectInstanceWorkflowService,
        ProjectWorkflowService projectWorkflowService, TagService tagService) {

        this.apiCollectionService = apiCollectionService;
        this.apiCollectionEndpointService = apiCollectionEndpointService;
        this.projectInstanceService = projectInstanceService;
        this.projectInstanceWorkflowService = projectInstanceWorkflowService;
        this.projectWorkflowService = projectWorkflowService;
        this.tagService = tagService;
    }

    @Override
    public ApiCollectionDTO createApiCollection(@NonNull ApiCollectionDTO apiCollectionDTO) {
        ApiCollection apiCollection = apiCollectionDTO.toApiCollection();

        ProjectInstance projectInstance = new ProjectInstance();

        projectInstance.setDescription(apiCollection.getDescription());
        projectInstance.setEnvironment(Environment.TEST);
        projectInstance.setName(apiCollection.getName());
        projectInstance.setProjectId(apiCollectionDTO.projectId());
        projectInstance.setProjectVersion(apiCollectionDTO.projectVersion());

        projectInstance = projectInstanceService.create(projectInstance);

        apiCollection.setProjectInstanceId(projectInstance.getId());

        return toApiCollectionDTO(apiCollectionService.create(apiCollection));
    }

    @Override
    public ApiCollectionEndpointDTO createApiCollectionEndpoint(
        @NonNull ApiCollectionEndpointDTO apiCollectionEndpointDTO) {

        ApiCollectionEndpoint apiCollectionEndpoint = apiCollectionEndpointDTO.toApiCollectionEndpoint();

        ApiCollection apiCollection = apiCollectionService.getApiCollection(
            apiCollectionEndpoint.getApiCollectionId());

        ProjectInstanceWorkflow projectInstanceWorkflow = new ProjectInstanceWorkflow();

        projectInstanceWorkflow.setProjectInstanceId(apiCollection.getProjectInstanceId());
        projectInstanceWorkflow.setWorkflowId(
            projectWorkflowService.getProjectWorkflowId(
                apiCollection.getProjectInstanceId(), apiCollectionEndpoint.getWorkflowReferenceCode()));

        projectInstanceWorkflow = projectInstanceWorkflowService.create(projectInstanceWorkflow);

        apiCollectionEndpoint.setProjectInstanceWorkflowId(projectInstanceWorkflow.getId());

        return new ApiCollectionEndpointDTO(
            apiCollectionEndpointService.create(apiCollectionEndpoint), projectInstanceWorkflow);
    }

    @Override
    public void deleteApiCollection(long id) {
        for (ApiCollectionEndpoint apiCollectionEndpoint : apiCollectionEndpointService.getApiEndpoints(id)) {
            apiCollectionEndpointService.delete(apiCollectionEndpoint.getId());
        }

        apiCollectionService.delete(id);
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
        return List.of();
    }

    @Override
    public ApiCollectionDTO updateApiCollection(@NonNull ApiCollectionDTO apiCollectionDTO) {
        return toApiCollectionDTO(apiCollectionService.update(apiCollectionDTO.toApiCollection()));
    }

    @Override
    public void updateApiCollectionTags(long id, List<Tag> tags) {

    }

    @Override
    public ApiCollectionEndpointDTO updateApiCollectionEndpoint(ApiCollectionEndpointDTO apiCollectionEndpointDTO) {
        ApiCollectionEndpoint apiCollectionEndpoint = apiCollectionEndpointDTO.toApiCollectionEndpoint();

        apiCollectionEndpoint = apiCollectionEndpointService.update(apiCollectionEndpoint);

        return new ApiCollectionEndpointDTO(
            apiCollectionEndpoint, projectInstanceWorkflowService.getProjectInstanceWorkflow(
                apiCollectionEndpoint.getProjectInstanceWorkflowId()));
    }

    private ApiCollectionDTO toApiCollectionDTO(ApiCollection apiCollection) {
        ProjectInstance projectInstance = projectInstanceService.getProjectInstance(
            apiCollection.getProjectInstanceId());

        List<ApiCollectionEndpointDTO> apiCollectionEndpointDTOs = apiCollectionEndpointService.getApiEndpoints(
            apiCollection.getId())
            .stream()
            .map(apiCollectionEndpoint -> new ApiCollectionEndpointDTO(
                apiCollectionEndpoint, projectInstanceWorkflowService.getProjectInstanceWorkflow(
                    apiCollectionEndpoint.getProjectInstanceWorkflowId())))
            .toList();

        return new ApiCollectionDTO(
            apiCollection, apiCollectionEndpointDTOs, projectInstance, tagService.getTags(apiCollection.getTagIds()));
    }
}
