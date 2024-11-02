/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.dto;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectInstance;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ApiCollectionDTO(
    int collectionVersion, String createdBy, LocalDateTime createdDate, String description, boolean enabled,
    List<ApiCollectionEndpointDTO> endpoints, Long id, String lastModifiedBy, LocalDateTime lastModifiedDate,
    String name, Project project, long projectId, ProjectInstance projectInstance, long projectInstanceId,
    int projectVersion, List<Tag> tags, int version) {

    public ApiCollectionDTO(
        ApiCollection apiCollection, List<ApiCollectionEndpointDTO> endpoints, Project project,
        ProjectInstance projectInstance, List<Tag> tags) {

        this(
            apiCollection.getCollectionVersion(), apiCollection.getCreatedBy(), apiCollection.getCreatedDate(),
            apiCollection.getDescription(), projectInstance.isEnabled(), endpoints, apiCollection.getId(),
            apiCollection.getLastModifiedBy(), apiCollection.getLastModifiedDate(), apiCollection.getName(), project,
            projectInstance.getProjectId(), projectInstance, apiCollection.getProjectInstanceId(),
            projectInstance.getProjectVersion(), tags, apiCollection.getVersion());
    }

    public ApiCollection toApiCollection() {
        ApiCollection apiCollection = new ApiCollection();

        apiCollection.setCollectionVersion(collectionVersion);
        apiCollection.setDescription(description);
        apiCollection.setId(id);
        apiCollection.setName(name);
        apiCollection.setProjectInstanceId(projectInstanceId);
        apiCollection.setTags(tags);
        apiCollection.setVersion(version);

        return apiCollection;
    }
}
