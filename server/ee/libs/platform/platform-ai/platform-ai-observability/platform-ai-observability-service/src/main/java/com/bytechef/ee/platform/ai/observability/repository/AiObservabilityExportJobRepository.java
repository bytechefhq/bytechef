/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.repository;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityExportJob;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiObservabilityExportJobRepository extends ListCrudRepository<AiObservabilityExportJob, Long> {

    /**
     * Export jobs owned by one workspace, newest first. A job whose {@code workspace_id} is null belongs to no
     * workspace and is invisible here, which is the intended behavior for a workspace-scoped listing.
     */
    List<AiObservabilityExportJob> findAllByWorkspaceIdOrderByCreatedDateDesc(Long workspaceId);
}
