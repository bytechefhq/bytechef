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

package com.bytechef.component.datastream.item;

import static com.bytechef.component.definition.datastream.ItemReader.SINCE_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.datastream.ItemReader;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.lang.reflect.Field;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;

/**
 * @author Ivica Cardic
 */
class ItemStreamReaderDelegateTest {

    private static final String TEST_TENANT_ID = "test-tenant";

    @Test
    void testCopiesSinceFromJobParameterToExecutionContext() throws Exception {
        ItemReader itemReader = mock(ItemReader.class);

        ItemStreamReaderDelegate delegate = newDelegate(itemReader);

        StepExecution stepExecution = newStepExecution(
            new JobParameters(Set.of(new JobParameter<>(SINCE_KEY, 12345L, Long.class, true))));

        delegate.doBeforeStep(stepExecution);

        ExecutionContext executionContext = new ExecutionContext();

        delegate.open(executionContext);

        assertThat(executionContext.containsKey(SINCE_KEY)).isTrue();
        assertThat(executionContext.getLong(SINCE_KEY)).isEqualTo(12345L);
    }

    @Test
    void testNoCopyWhenJobParameterAbsent() throws Exception {
        ItemReader itemReader = mock(ItemReader.class);

        ItemStreamReaderDelegate delegate = newDelegate(itemReader);

        StepExecution stepExecution = newStepExecution(new JobParameters());

        delegate.doBeforeStep(stepExecution);

        ExecutionContext executionContext = new ExecutionContext();

        delegate.open(executionContext);

        assertThat(executionContext.containsKey(SINCE_KEY)).isFalse();
    }

    private ItemStreamReaderDelegate newDelegate(ItemReader itemReader) throws Exception {
        ClusterElementDefinitionService clusterElementDefinitionService = mock(
            ClusterElementDefinitionService.class);
        ContextFactory contextFactory = mock(ContextFactory.class);

        when(clusterElementDefinitionService.<ItemReader>getClusterElement(any(), any(Integer.class), any()))
            .thenReturn(itemReader);

        ItemStreamReaderDelegate delegate = new ItemStreamReaderDelegate(
            clusterElementDefinitionService, contextFactory);

        // tenantId is normally set by AbstractItemStreamDelegate.beforeStep, which requires the full
        // cluster-element JobParameter. We populate it directly to keep this test focused on SINCE_KEY plumbing.
        Field tenantIdField = AbstractItemStreamDelegate.class.getDeclaredField("tenantId");

        tenantIdField.setAccessible(true);
        tenantIdField.set(delegate, TEST_TENANT_ID);

        return delegate;
    }

    private StepExecution newStepExecution(JobParameters jobParameters) {
        JobExecution jobExecution = mock(JobExecution.class);
        StepExecution stepExecution = mock(StepExecution.class);

        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(stepExecution.getJobParameters()).thenReturn(jobParameters);
        when(jobExecution.getJobParameters()).thenReturn(jobParameters);

        return stepExecution;
    }
}
