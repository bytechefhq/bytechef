/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ai.copilot.service;

import com.bytechef.ai.copilot.domain.VectorStore;
import com.bytechef.ai.copilot.repository.VectorStoreRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Marko Kriskovic
 */
@Service
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
