/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.repository;

import com.bytechef.ee.ai.copilot.domain.CopilotVectorStore;
import com.bytechef.platform.jdbc.ConditionalJdbcRepository;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 *
 * @author Marko Kriskovic
 */
@ConditionalJdbcRepository
public interface CopilotVectorStoreRepository extends ListCrudRepository<CopilotVectorStore, Long> {
}
