/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.service;

import com.bytechef.ee.ai.copilot.domain.CopilotVectorStore;
import com.bytechef.ee.ai.copilot.repository.CopilotVectorStoreRepository;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Marko Kriskovic
 */
@Service
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class CopilotVectorStoreService {
    private final CopilotVectorStoreRepository repository;

    public CopilotVectorStoreService(CopilotVectorStoreRepository repository) {
        this.repository = repository;
    }

    public long count() {
        return repository.count();
    }

    public List<CopilotVectorStore> findAll() {
        return repository.findAll();
    }
}
