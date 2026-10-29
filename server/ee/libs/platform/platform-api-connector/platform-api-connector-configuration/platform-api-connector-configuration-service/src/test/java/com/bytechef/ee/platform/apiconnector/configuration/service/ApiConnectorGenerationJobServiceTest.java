/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnectorGenerationJob;
import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnectorGenerationJob.Status;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ApiConnectorGenerationJobServiceTest {

    private ApiConnectorGenerationJobService firstCoordinator;
    private ApiConnectorGenerationJobService secondCoordinator;

    @BeforeEach
    void beforeEach() {
        CacheManager sharedCacheManager = new JsonRoundTripCacheManager();

        firstCoordinator = createCoordinator(sharedCacheManager);
        secondCoordinator = createCoordinator(sharedCacheManager);
    }

    @Test
    void testAnotherCoordinatorSeesTheJobAndItsStatus() {
        ApiConnectorGenerationJob job = firstCoordinator.create("https://example.com/docs");

        String jobId = job.getId();

        assertThat(secondCoordinator.get(jobId))
            .hasValueSatisfying(sharedJob -> {
                assertThat(sharedJob.getStatus()).isEqualTo(Status.PENDING);
                assertThat(sharedJob.getDocumentationUrl()).isEqualTo("https://example.com/docs");
                assertThat(sharedJob.getCreatedDate()).isEqualTo(job.getCreatedDate());
            });

        firstCoordinator.markAsProcessing(jobId);

        assertThat(secondCoordinator.get(jobId)).hasValueSatisfying(
            sharedJob -> assertThat(sharedJob.getStatus()).isEqualTo(Status.PROCESSING));

        firstCoordinator.markAsCompleted(jobId, "openapi: 3.0.1");

        assertThat(secondCoordinator.get(jobId))
            .hasValueSatisfying(sharedJob -> {
                assertThat(sharedJob.getStatus()).isEqualTo(Status.COMPLETED);
                assertThat(sharedJob.getSpecification()).isEqualTo("openapi: 3.0.1");
                assertThat(sharedJob.getCompletedDate()).isNotNull();
            });
    }

    @Test
    void testAnotherCoordinatorSeesAFailure() {
        String jobId = firstCoordinator.create("https://example.com/docs")
            .getId();

        firstCoordinator.markAsFailed(jobId, "Documentation not reachable");

        assertThat(secondCoordinator.get(jobId))
            .hasValueSatisfying(sharedJob -> {
                assertThat(sharedJob.getStatus()).isEqualTo(Status.FAILED);
                assertThat(sharedJob.getErrorMessage()).isEqualTo("Documentation not reachable");
            });
    }

    @Test
    void testCancellationOnAnotherCoordinatorReachesTheGeneratingCoordinator() {
        String jobId = firstCoordinator.create("https://example.com/docs")
            .getId();

        firstCoordinator.markAsProcessing(jobId);

        assertThat(secondCoordinator.requestCancellation(jobId)).isTrue();
        assertThat(firstCoordinator.isCancellationRequested(jobId)).isTrue();
        assertThat(firstCoordinator.get(jobId)).hasValueSatisfying(
            sharedJob -> assertThat(sharedJob.getStatus()).isEqualTo(Status.CANCELLED));
    }

    @Test
    void testACancelledJobStaysCancelled() {
        String cancelledBeforeProcessingJobId = firstCoordinator.create("https://example.com/docs")
            .getId();

        secondCoordinator.requestCancellation(cancelledBeforeProcessingJobId);

        firstCoordinator.markAsProcessing(cancelledBeforeProcessingJobId);

        String cancelledWhileProcessingJobId = firstCoordinator.create("https://example.com/docs")
            .getId();

        firstCoordinator.markAsProcessing(cancelledWhileProcessingJobId);

        secondCoordinator.requestCancellation(cancelledWhileProcessingJobId);

        firstCoordinator.markAsFailed(cancelledWhileProcessingJobId, "Documentation not reachable");

        String cancelledBeforeCompletionJobId = firstCoordinator.create("https://example.com/docs")
            .getId();

        secondCoordinator.requestCancellation(cancelledBeforeCompletionJobId);

        firstCoordinator.markAsCompleted(cancelledBeforeCompletionJobId, "openapi: 3.0.1");

        for (String jobId : List.of(
            cancelledBeforeProcessingJobId, cancelledWhileProcessingJobId, cancelledBeforeCompletionJobId)) {

            assertThat(secondCoordinator.get(jobId))
                .hasValueSatisfying(sharedJob -> {
                    assertThat(sharedJob.getStatus()).isEqualTo(Status.CANCELLED);
                    assertThat(sharedJob.getErrorMessage()).isNull();
                    assertThat(sharedJob.getSpecification()).isNull();
                });
        }
    }

    @Test
    void testUnknownJob() {
        assertThat(secondCoordinator.get("unknown")).isEmpty();
        assertThat(secondCoordinator.requestCancellation("unknown")).isFalse();
        assertThat(secondCoordinator.isCancellationRequested("unknown")).isFalse();
    }

    private static ApiConnectorGenerationJobService createCoordinator(CacheManager cacheManager) {
        return new ApiConnectorGenerationJobServiceImpl(cacheManager);
    }

    private static class JsonRoundTripCacheManager extends ConcurrentMapCacheManager {

        @Override
        protected org.springframework.cache.Cache createConcurrentMapCache(String name) {
            return new ConcurrentMapCache(name, new ConcurrentHashMap<>(256), false) {

                @Override
                protected Object toStoreValue(Object userValue) {
                    return JsonUtils.write(userValue);
                }

                @Override
                protected Object fromStoreValue(Object storeValue) {
                    return JsonUtils.read((String) storeValue);
                }
            };
        }
    }
}
