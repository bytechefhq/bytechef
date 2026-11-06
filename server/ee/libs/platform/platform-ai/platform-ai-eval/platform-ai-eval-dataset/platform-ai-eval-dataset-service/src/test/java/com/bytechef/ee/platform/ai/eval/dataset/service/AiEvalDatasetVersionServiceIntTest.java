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
        AiEvalDataset dataset = aiEvalDatasetService.create(new AiEvalDataset("freeze-then-edit-fixture"));

        AiEvalDatasetVersion frozenVersion = aiEvalDatasetVersionService.createVersion(dataset.getId(), "v1", true);

        assertThat(frozenVersion.isFrozen()).isTrue();

        AiEvalDatasetVersion fresh = aiEvalDatasetVersionService.getOrCreateUnfrozenVersion(dataset.getId());

        assertThat(fresh.getId()).isNotEqualTo(frozenVersion.getId());
        assertThat(fresh.isFrozen()).isFalse();
    }
}
