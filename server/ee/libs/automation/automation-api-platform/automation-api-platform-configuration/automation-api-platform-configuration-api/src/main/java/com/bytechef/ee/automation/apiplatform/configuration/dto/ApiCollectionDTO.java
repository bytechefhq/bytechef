/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.dto;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ApiCollectionDTO(
    int collectionVersion, String contextPath, String createdBy, Instant createdDate, String description,
    boolean enabled, List<ApiCollectionEndpointDTO> endpoints, Long id, String lastModifiedBy, Instant lastModifiedDate,
    String name, Project project, long projectId, ProjectDeployment projectDeployment, long projectDeploymentId,
    int projectVersion, List<Tag> tags, int version) {

    public ApiCollectionDTO(
        ApiCollection apiCollection, List<ApiCollectionEndpointDTO> endpoints, Project project,
        ProjectDeployment projectDeployment, List<Tag> tags) {

        this(
            apiCollection.getCollectionVersion(), apiCollection.getContextPath(), apiCollection.getCreatedBy(),
            apiCollection.getCreatedDate(), apiCollection.getDescription(), projectDeployment.isEnabled(), endpoints,
            apiCollection.getId(), apiCollection.getLastModifiedBy(), apiCollection.getLastModifiedDate(),
            apiCollection.getName(), project, projectDeployment.getProjectId(), projectDeployment,
            apiCollection.getProjectDeploymentId(), projectDeployment.getProjectVersion(), tags,
            apiCollection.getVersion());
    }

    public ApiCollection toApiCollection() {
        ApiCollection apiCollection = new ApiCollection();

        apiCollection.setCollectionVersion(collectionVersion);
        apiCollection.setContextPath(contextPath);
        apiCollection.setDescription(description);
        apiCollection.setId(id);
        apiCollection.setName(name);
        apiCollection.setProjectDeploymentId(projectDeploymentId);
        apiCollection.setTags(tags);
        apiCollection.setVersion(version);

        return apiCollection;
    }
}
