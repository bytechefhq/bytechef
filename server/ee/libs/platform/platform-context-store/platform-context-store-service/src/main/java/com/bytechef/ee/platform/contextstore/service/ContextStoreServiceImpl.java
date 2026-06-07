/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.service;

import com.bytechef.ee.platform.contextstore.domain.ContextStore;
import com.bytechef.ee.platform.contextstore.repository.ContextStoreRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
public class ContextStoreServiceImpl implements ContextStoreService {

    private final ContextStoreRepository contextStoreRepository;

    public ContextStoreServiceImpl(ContextStoreRepository contextStoreRepository) {
        this.contextStoreRepository = contextStoreRepository;
    }

    @Override
    public ContextStore create(ContextStore contextStore) {
        return contextStoreRepository.save(contextStore);
    }

    @Override
    public ContextStore update(ContextStore contextStore) {
        return contextStoreRepository.save(contextStore);
    }

    @Override
    public void delete(Long id) {
        contextStoreRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ContextStore get(Long id) {
        return contextStoreRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("ContextStore " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ContextStore> fetch(Long id) {
        return contextStoreRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextStore> getAllByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        return contextStoreRepository.findAllByIdIn(ids);
    }
}
