/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.repository;

import com.bytechef.ee.platform.contextstore.domain.ContextStore;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JDBC repository for {@link ContextStore}. Workspace membership is held by the
 * {@code workspace_context_store} relation table; this repository owns store-level lifecycle only.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Repository
public interface ContextStoreRepository extends ListCrudRepository<ContextStore, Long> {

    List<ContextStore> findAllByIdIn(List<Long> ids);
}
