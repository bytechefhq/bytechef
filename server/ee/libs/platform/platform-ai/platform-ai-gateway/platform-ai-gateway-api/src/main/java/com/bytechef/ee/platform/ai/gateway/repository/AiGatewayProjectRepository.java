/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.repository;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProject;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiGatewayProjectRepository extends ListCrudRepository<AiGatewayProject, Long> {

    /**
     * Returns the projects owned by the given workspace. A project with a null {@code workspace_id} belongs to no
     * workspace and is therefore never returned here — SQL equality never matches NULL.
     */
    List<AiGatewayProject> findAllByWorkspaceId(long workspaceId);

    Optional<AiGatewayProject> findByWorkspaceIdAndSlug(long workspaceId, String slug);
}
