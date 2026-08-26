/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.repository;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JDBC repository for {@link ContextStoreSource}. Workspace membership is the nullable
 * {@code context_store_source.workspace_id} column; a source with a null workspace matches no workspace-scoped query.
 * The repository also owns the workspace-agnostic lifecycle queries (e.g. all enabled sources across workspaces, used
 * by the background sync scheduler).
 *
 * @author Ivica Cardic
 * @version ee
 */
@Repository
public interface ContextStoreSourceRepository
    extends PagingAndSortingRepository<ContextStoreSource, Long>, ListCrudRepository<ContextStoreSource, Long> {

    List<ContextStoreSource> findAllByEnabled(boolean enabled);

    List<ContextStoreSource> findAllByIdIn(List<Long> ids);

    List<ContextStoreSource> findAllByIdInAndEnabled(List<Long> ids, boolean enabled);

    List<ContextStoreSource> findAllByContextStoreId(Long contextStoreId);

    List<ContextStoreSource> findAllByWorkflowId(String workflowId);

    List<ContextStoreSource> findAllByWorkspaceId(long workspaceId);

    List<ContextStoreSource> findAllByWorkspaceIdAndEnabled(long workspaceId, boolean enabled);
}
