/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.repository;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreRecord;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Bridges the Spring Data JDBC {@link ContextStoreRecordPostgresRepository} into the
 * {@link ContextStoreRecordRepository} SPI. The service layer depends on the SPI; this adapter is the only code that
 * knows about both sides of the wire.
 *
 * <p>
 * The adapter is unconditional — every deployment that ships the Postgres backend (i.e. every deployment) gets it. The
 * ClickHouse-side SPI implementation carries {@code @Primary} + {@code @ConditionalOnBean(clickHouseDataSource)}, so
 * when the operator opts in to ClickHouse it overrides this adapter for autowire targets deployment-wide.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
public class ContextStoreRecordPostgresRepositoryAdapter implements ContextStoreRecordRepository {

    private final ContextStoreRecordPostgresRepository delegate;

    @SuppressFBWarnings("EI2")
    public ContextStoreRecordPostgresRepositoryAdapter(ContextStoreRecordPostgresRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public ContextStoreRecord save(ContextStoreRecord contextStoreRecord) {
        return delegate.save(contextStoreRecord);
    }

    @Override
    public Optional<ContextStoreRecord> findBySourceIdAndSourceRecordId(Long sourceId, String sourceRecordId) {
        return delegate.findBySourceIdAndSourceRecordId(sourceId, sourceRecordId);
    }

    @Override
    public int tombstoneUnseen(Long sourceId, Collection<String> seenIds, Instant deletedAt) {
        return delegate.tombstoneUnseen(sourceId, seenIds, deletedAt);
    }

    @Override
    public List<ContextStoreRecord> findAllById(Iterable<Long> ids) {
        return delegate.findAllById(ids);
    }

    @Override
    public List<Long> findTombstonedRecordIdsBySourceId(Long sourceId) {
        return delegate.findTombstonedRecordIdsBySourceId(sourceId);
    }

    @Override
    public void deleteById(Long id) {
        delegate.deleteById(id);
    }
}
