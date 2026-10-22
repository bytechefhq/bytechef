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

package com.bytechef.automation.workflow.execution.facade;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.memory.InMemoryTaskExecutionRepository;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.lang.reflect.Field;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class LazyTaskExecutionTreeBuilderRepositoryTest extends AbstractLazyTaskExecutionTreeBuilderRepositoryTest {

    private static final long JOB_ID = 1L;

    private InMemoryTaskExecutionRepository taskExecutionRepository;

    @BeforeEach
    void beforeEach() {
        TenantContext.resetCurrentTenantId();

        taskExecutionRepository = new InMemoryTaskExecutionRepository();

        setUp(new TaskExecutionServiceImpl(taskExecutionRepository));

        TenantContext.setCurrentTenantId("T");
    }

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Override
    protected long getJobId() {
        return JOB_ID;
    }

    @Override
    protected TaskExecution save(TaskExecution taskExecution, Instant createdDate) {
        setCreatedDate(taskExecution, createdDate);

        return taskExecutionRepository.save(taskExecution);
    }

    private static void setCreatedDate(TaskExecution taskExecution, Instant createdDate) {
        try {
            Field createdDateField = TaskExecution.class.getDeclaredField("createdDate");

            createdDateField.setAccessible(true);
            createdDateField.set(taskExecution, createdDate);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IllegalStateException(reflectiveOperationException);
        }
    }
}
