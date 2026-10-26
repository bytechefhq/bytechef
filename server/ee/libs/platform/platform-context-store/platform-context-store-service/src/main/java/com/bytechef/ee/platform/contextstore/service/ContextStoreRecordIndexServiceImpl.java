/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.service;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreRecordIndex;
import com.bytechef.ee.platform.contextstore.repository.ContextStoreRecordIndexRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
@SuppressFBWarnings("EI2")
public class ContextStoreRecordIndexServiceImpl implements ContextStoreRecordIndexService {

    private final ContextStoreRecordIndexRepository contextStoreRecordIndexRepository;

    public ContextStoreRecordIndexServiceImpl(ContextStoreRecordIndexRepository contextStoreRecordIndexRepository) {
        this.contextStoreRecordIndexRepository = contextStoreRecordIndexRepository;
    }

    @Override
    public ContextStoreRecordIndex save(ContextStoreRecordIndex index) {
        return contextStoreRecordIndexRepository.save(index);
    }

    @Override
    public void deleteAllByRecordId(Long recordId) {
        contextStoreRecordIndexRepository.deleteAllByRecordId(recordId);
    }

    @Override
    public int deleteAllForTombstonedRecords(Long sourceId) {
        return contextStoreRecordIndexRepository.deleteAllForTombstonedRecords(sourceId);
    }
}
