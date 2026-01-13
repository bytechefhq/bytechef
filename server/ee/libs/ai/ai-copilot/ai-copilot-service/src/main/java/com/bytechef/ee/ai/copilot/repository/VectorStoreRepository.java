/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.repository;

import com.bytechef.ee.ai.copilot.domain.VectorStore;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Marko Kriskovic
 */
@Repository
public interface VectorStoreRepository extends ListCrudRepository<VectorStore, Long> {
}
