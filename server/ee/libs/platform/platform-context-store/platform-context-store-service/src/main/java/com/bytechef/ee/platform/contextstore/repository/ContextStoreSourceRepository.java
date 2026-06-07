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
 * Spring Data JDBC repository for {@link ContextStoreSource}. Workspace scoping has been moved out of the source row
 * itself — workspace membership is enforced by the {@code workspace_context_store_source} relation table managed by the
 * automation-side {@code WorkspaceContextStoreSourceRepository}. This repository owns only the source-level lifecycle
 * queries (e.g. all enabled sources across workspaces, used by the background sync scheduler).
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
}
