/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.atlas.execution.repository.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.tenant.TenantContext;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class InMemoryJobRepositoryTest {

    private InMemoryJobRepository inMemoryJobRepository;

    @BeforeEach
    void beforeEach() {
        TenantContext.resetCurrentTenantId();

        inMemoryJobRepository = new InMemoryJobRepository(
            new InMemoryTaskExecutionRepository(), JsonMapper.builder()
                .build());

        TenantContext.setCurrentTenantId("T");
    }

    @AfterEach
    void tearDown() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testFindAllByParentTaskExecutionIdIn() {
        long firstChildJobId = saveJob(101L);

        saveJob(102L);
        saveJob(null);

        List<Long> childJobIds = inMemoryJobRepository.findAllByParentTaskExecutionIdIn(List.of(101L, 103L))
            .stream()
            .map(Job::getId)
            .toList();

        assertEquals(List.of(firstChildJobId), childJobIds);
    }

    @Test
    void testFindAllByParentTaskExecutionIdInReturnsEmptyListForEmptyInput() {
        saveJob(101L);

        assertTrue(inMemoryJobRepository.findAllByParentTaskExecutionIdIn(List.of())
            .isEmpty());
    }

    private long saveJob(Long parentTaskExecutionId) {
        Job job = new Job();

        job.setParentTaskExecutionId(parentTaskExecutionId);
        job.setStatus(Job.Status.STARTED);
        job.setWorkflowId("demo:1234");

        Job savedJob = inMemoryJobRepository.save(job);

        return Objects.requireNonNull(savedJob.getId());
    }
}
