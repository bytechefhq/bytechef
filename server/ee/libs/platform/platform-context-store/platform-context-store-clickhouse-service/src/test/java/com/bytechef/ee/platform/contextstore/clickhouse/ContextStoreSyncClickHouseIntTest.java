/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.clickhouse;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Phase 16 commit 6 — disabled IntTest skeleton mirroring
 * {@code com.bytechef.ee.platform.contextstore.ContextStoreSyncE2EIntTest} but routed through the ClickHouse-backed
 * records repository. Lights up when both halves are wired: (a) the full Atlas + DataStream coordinator/worker stack
 * the Postgres E2E test is also waiting on, and (b) a {@code ClickHouseContainer} (Testcontainers) connected to the
 * configuration-time {@code bytechef.context-store.clickhouse.url}.
 *
 * <p>
 * What to pin once enabled (parallel to the Postgres E2E, but with ClickHouse-specific assertions):
 * </p>
 *
 * <ol>
 * <li><b>Initial sync onto ClickHouse:</b> create a Context Source via {@code WorkspaceContextStoreSourceFacade#create}
 * with {@code backend=CLICKHOUSE}. The facade hook (deferred from commit 3 to a commit-5-follow-up) issues
 * {@code CREATE TABLE} via {@code ClickHouseTableDdlGenerator}; assert the row's {@code clickhouse_table_name} matches
 * {@code ClickHouseTableNameSanitizer.tableNameFor()} and the table actually exists ({@code system.tables} query).
 * After {@code refreshNow}, assert both records appear in the per-entity ClickHouse table. The router dispatches
 * inserts through {@code ContextStoreRecordClickHouseRepository.save()}.</li>
 *
 * <li><b>Change-detection on re-sync:</b> rewrite a record's payload, re-trigger. ClickHouse's {@code
 * ReplacingMergeTree(_last_seen_at)} engine retains both versions until background merge; the test must run
 * {@code OPTIMIZE TABLE <name> FINAL} before assertions, or address rows via {@code SELECT ... FINAL} so the latest
 * version surfaces. Assert: the updated payload_hash wins; the older version is collapsed away after the OPTIMIZE.</li>
 *
 * <li><b>Tombstone-on-disappear (async ALTER UPDATE):</b> drop a record from the source, re-trigger. The router's
 * tombstoneUnseen call queues {@code ALTER TABLE ... UPDATE _deleted_at = ?} which is async — visible only after the
 * mutation completes. Poll {@code SELECT count() FROM system.mutations WHERE is_done = 0 AND database = ? AND table
 * = ?} until zero, or run {@code OPTIMIZE TABLE ... FINAL} (which short-circuits the mutation). Then assert the
 * tombstoned record's {@code _deleted_at} is non-null while the remaining record's stays null.</li>
 *
 * <li><b>findTombstonedRecordIds returns cityHash64 ids:</b> the Postgres-backed sibling asserts on the original
 * {@code Long id} values. ClickHouse's repository computes ids on the fly via {@code toInt64(cityHash64(_id))}, so the
 * assertion shape is different — just check that the returned list has the right cardinality and the values are stable
 * across calls (deterministic hash).</li>
 *
 * <li><b>UnsupportedOperationException on findAllById / deleteById:</b> direct calls through the router to a
 * CLICKHOUSE-backed source must surface the same {@code UnsupportedOperationException} the ClickHouse repo throws (the
 * router's commit 5 code dispatches these to Postgres unconditionally, so this assertion verifies the routing decision
 * — call directly via the qualified Spring bean if needed).</li>
 *
 * <li><b>Mixed-backend coexistence:</b> create two sources side by side, one POSTGRES and one CLICKHOUSE. Assert each
 * writes records into its own backend; the router doesn't bleed (Postgres source records never appear in ClickHouse and
 * vice versa). This is the "paired-backend coverage" the plan called out — the most valuable regression signal for the
 * router's per-call dispatch logic.</li>
 * </ol>
 *
 * <p>
 * <b>Container setup notes:</b> use {@code org.testcontainers.clickhouse.ClickHouseContainer} (Testcontainers JDBC
 * support). Inject the connection details into the Spring context via {@code @DynamicPropertySource}:
 * </p>
 *
 * <pre>{@code
 * @DynamicPropertySource
 * static void clickHouseProps(DynamicPropertyRegistry registry) {
 *     registry.add("bytechef.context-store.clickhouse.url", clickHouseContainer::getJdbcUrl);
 *     registry.add("bytechef.context-store.clickhouse.username", clickHouseContainer::getUsername);
 *     registry.add("bytechef.context-store.clickhouse.password", clickHouseContainer::getPassword);
 * }
 * }</pre>
 *
 * <p>
 * <b>Why disabled:</b> same reason as the Postgres E2E — the Atlas coordinator + worker + DataStream Spring Batch stack
 * isn't wired into a single {@code @SpringBootTest} config that this module can pull in cheaply. The unit-test layer
 * (the repository test in this module + the router test in {@code platform-context-store-service}) covers each link in
 * the dispatch chain with focused fixtures. This skeleton lights up when the parent E2E does.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
class ContextStoreSyncClickHouseIntTest {

    @Test
    @Disabled
    void testInitialSyncWritesToClickhouseTableViaRouter() {
        // See class Javadoc item 1.
        throw new UnsupportedOperationException("not implemented");
    }

    @Test
    @Disabled
    void testReSyncReplacingMergeTreeRetainsLatestVersionAfterOptimize() {
        // See class Javadoc item 2. Must add OPTIMIZE TABLE ... FINAL between mutate-via-save and the assertion.
        throw new UnsupportedOperationException("not implemented");
    }

    @Test
    @Disabled
    void testTombstoneUnseenAsyncMutationVisibleAfterOptimize() {
        // See class Javadoc item 3. ALTER UPDATE is async — pump OPTIMIZE TABLE ... FINAL before asserting _deleted_at.
        throw new UnsupportedOperationException("not implemented");
    }

    @Test
    @Disabled
    void testFindTombstonedRecordIdsReturnsDeterministicCityHash64Values() {
        // See class Javadoc item 4. Cardinality match + identity across calls is enough — the actual hash values
        // depend on the _id literals and ClickHouse's cityHash64 impl.
        throw new UnsupportedOperationException("not implemented");
    }

    @Test
    @Disabled
    void testFindAllByIdAndDeleteByIdThrowOnClickhouseBackedSource() {
        // See class Javadoc item 5. Call the qualified bean directly — the router masks the
        // UnsupportedOperationException by dispatching to Postgres unconditionally for these two methods.
        throw new UnsupportedOperationException("not implemented");
    }

    @Test
    @Disabled
    void testMixedBackendSourcesCoexistInOneWorkspace() {
        // See class Javadoc item 6. The primary regression signal for commit 5's router dispatch logic.
        throw new UnsupportedOperationException("not implemented");
    }
}
