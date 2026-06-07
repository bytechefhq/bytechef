/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.service;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreRecord;
import com.bytechef.ee.platform.contextstore.repository.ContextStoreRecordRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
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
public class ContextStoreRecordServiceImpl implements ContextStoreRecordService {

    private final ContextStoreRecordRepository contextStoreRecordRepository;

    @SuppressFBWarnings("EI2")
    public ContextStoreRecordServiceImpl(ContextStoreRecordRepository contextStoreRecordRepository) {
        this.contextStoreRecordRepository = contextStoreRecordRepository;
    }

    @Override
    public ContextStoreRecord save(ContextStoreRecord record) {
        return contextStoreRecordRepository.save(record);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ContextStoreRecord> fetchByKey(Long sourceId, String sourceRecordId) {
        return contextStoreRecordRepository.findBySourceIdAndSourceRecordId(sourceId, sourceRecordId);
    }

    @Override
    public int tombstoneUnseen(Long sourceId, Collection<String> seenIds, Instant deletedAt) {
        if (seenIds == null || seenIds.isEmpty()) {
            // Postgres rejects NOT IN () so we short-circuit rather than risk an invalid statement or a
            // surprise full-source wipe. A sync run that yields zero records is treated as a no-op until a
            // dedicated tombstone-everything path is added.
            return 0;
        }

        return contextStoreRecordRepository.tombstoneUnseen(sourceId, seenIds, deletedAt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextStoreRecord> findAllById(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<ContextStoreRecord> records = new ArrayList<>();

        contextStoreRecordRepository.findAllById(ids)
            .forEach(records::add);

        return records;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findTombstonedRecordIdsBySourceId(Long sourceId) {
        return contextStoreRecordRepository.findTombstonedRecordIdsBySourceId(sourceId);
    }

    @Override
    public void delete(Long id) {
        contextStoreRecordRepository.deleteById(id);
    }
}
