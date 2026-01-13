/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.service;

import com.bytechef.ee.ai.copilot.domain.VectorStore;
import com.bytechef.ee.ai.copilot.repository.VectorStoreRepository;
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
public class VectorStoreService {
    private final VectorStoreRepository repository;

    public VectorStoreService(VectorStoreRepository repository) {
        this.repository = repository;
    }

    public long count() {
        return repository.count();
    }

    public List<VectorStore> findAll() {
        return repository.findAll();
    }
}
