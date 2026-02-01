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

package com.bytechef.component.datastream.batch;

import com.bytechef.component.datastream.item.ItemStreamProcessorDelegate;
import com.bytechef.component.datastream.item.ItemStreamReaderDelegate;
import com.bytechef.component.datastream.item.ItemStreamWriterDelegate;
import com.bytechef.component.datastream.listener.DataStreamJobExecutionListener;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.Map;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Factory for creating isolated in-memory Spring Batch job infrastructure per execution. Each call to
 * {@link #runJob(JobParameters)} creates a fresh repository, job launcher, step, and job, ensuring complete isolation
 * between concurrent executions.
 *
 * @author Ivica Cardic
 */
@Component
public class InMemoryBatchJobFactory {

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ContextFactory contextFactory;
    private final DataStreamJobExecutionListener jobExecutionListener;

    public InMemoryBatchJobFactory(
        ClusterElementDefinitionService clusterElementDefinitionService, ContextFactory contextFactory,
        DataStreamJobExecutionListener jobExecutionListener) {

        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.contextFactory = contextFactory;
        this.jobExecutionListener = jobExecutionListener;
    }

    /**
     * Creates and runs a job with fresh in-memory infrastructure. Each invocation creates isolated repository, job
     * launcher, step, and job instances.
     *
     * @param jobParameters the job parameters
     * @return the job execution result
     * @throws Exception if job execution fails
     */
    public JobExecution runJob(JobParameters jobParameters) throws Exception {
        JobRepository jobRepository = new ResourcelessJobRepository();
        PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

        Step step = new StepBuilder("inMemoryStep", jobRepository)
            .<Map<String, Object>, Map<String, Object>>chunk(10, transactionManager)
            .reader(new ItemStreamReaderDelegate(clusterElementDefinitionService, contextFactory))
            .processor(new ItemStreamProcessorDelegate(clusterElementDefinitionService, contextFactory))
            .writer(new ItemStreamWriterDelegate(clusterElementDefinitionService, contextFactory))
            .build();

        Job job = new JobBuilder("inMemoryDataStreamJob", jobRepository)
            .listener(jobExecutionListener)
            .incrementer(new RunIdIncrementer())
            .start(step)
            .build();

        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();

        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.afterPropertiesSet();

        return jobLauncher.run(job, jobParameters);
    }
}
