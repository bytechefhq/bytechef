/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.dataset.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.ai.eval.dataset.config.AiEvalDatasetIntTestConfiguration;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDataset;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetVersion;
import com.bytechef.ee.platform.ai.eval.dataset.repository.AiEvalDatasetVersionRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test exercising the partial unique index {@code ux_ai_eval_dataset_version_unfrozen_per_dataset} from
 * {@code 00000000000010_ai_eval_dataset_unique_unfrozen.xml} against a real Postgres instance via Testcontainers,
 * catching JDBC-mapping regressions a Mockito-only test cannot. A future migration that drops the
 * {@code WHERE frozen = false} clause (or renames the index) fails this test instead of silently regressing the
 * at-most-one-unfrozen-version- per-dataset invariant.
 *
 * @author Ivica Cardic
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AiEvalDatasetIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
public class AiEvalDatasetVersionServiceIntTest {

    @Autowired
    private AiEvalDatasetService aiEvalDatasetService;

    @Autowired
    private AiEvalDatasetVersionService aiEvalDatasetVersionService;

    @Autowired
    private AiEvalDatasetVersionRepository aiEvalDatasetVersionRepository;

    @Test
    public void testConcurrentGetOrCreateUnfrozenVersionProducesExactlyOneRow() throws Exception {
        // Two threads both miss the read, both attempt the insert: the database's partial unique index must reject
        // the second insert and the service-layer DuplicateKeyException catch must re-fetch the winner. Without
        // this real-Postgres exercise, only the unit-level mocks cover the catch — a future migration that drops
        // `WHERE frozen = false` from the index (turning the partial unique into a full unique against multiple
        // frozen versions per dataset) slips past the unit tests but fails this one.
        AiEvalDataset dataset = aiEvalDatasetService.create(new AiEvalDataset("concurrent-fixture"));

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Callable<AiEvalDatasetVersion> firstTask = () -> {
                start.await();

                return aiEvalDatasetVersionService.getOrCreateUnfrozenVersion(dataset.getId());
            };
            Callable<AiEvalDatasetVersion> secondTask = () -> {
                start.await();

                return aiEvalDatasetVersionService.getOrCreateUnfrozenVersion(dataset.getId());
            };

            Future<AiEvalDatasetVersion> firstFuture = executor.submit(firstTask);
            Future<AiEvalDatasetVersion> secondFuture = executor.submit(secondTask);

            start.countDown();

            AiEvalDatasetVersion firstResult = firstFuture.get(10, TimeUnit.SECONDS);
            AiEvalDatasetVersion secondResult = secondFuture.get(10, TimeUnit.SECONDS);

            assertThat(firstResult.getId()).isNotNull();
            assertThat(secondResult.getId()).isNotNull();
            assertThat(firstResult.getId())
                .as("both racers must converge on the single winner row")
                .isEqualTo(secondResult.getId());

            List<AiEvalDatasetVersion> persistedVersions =
                aiEvalDatasetVersionRepository.findAllByDatasetId(dataset.getId());

            assertThat(persistedVersions)
                .as("partial unique index must keep exactly one row for the dataset")
                .hasSize(1);
            assertThat(persistedVersions.get(0)
                .isFrozen())
                    .isFalse();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testFrozenVersionDoesNotBlockNewUnfrozenInsert() {
        // Sanity check that the partial-unique clause `WHERE frozen = false` is honoured: a frozen version must
        // not block creation of a fresh unfrozen version on the same dataset (the operational pattern when a
        // caller freezes the current snapshot for an experiment and then continues editing).
        AiEvalDataset dataset = aiEvalDatasetService.create(new AiEvalDataset("freeze-then-edit-fixture"));

        AiEvalDatasetVersion frozenVersion = aiEvalDatasetVersionService.createVersion(dataset.getId(), "v1", true);

        assertThat(frozenVersion.isFrozen()).isTrue();

        AiEvalDatasetVersion fresh = aiEvalDatasetVersionService.getOrCreateUnfrozenVersion(dataset.getId());

        assertThat(fresh.getId()).isNotEqualTo(frozenVersion.getId());
        assertThat(fresh.isFrozen()).isFalse();
    }
}
