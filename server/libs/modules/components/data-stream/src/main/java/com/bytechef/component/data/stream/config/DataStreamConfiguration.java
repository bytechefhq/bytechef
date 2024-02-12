/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.data.stream.config;

import com.bytechef.component.data.stream.item.DataStreamItemProcessor;
import com.bytechef.component.data.stream.item.DataStreamItemReader;
import com.bytechef.component.data.stream.item.DataStreamItemWriter;
import com.bytechef.component.data.stream.listener.DataStreamJobExecutionListener;
import java.util.Map;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

/**
 * @author Ivica Cardic
 */
@Configuration
public class DataStreamConfiguration {

    @Bean
    public DataStreamItemReader dataStreamReader() {
        return new DataStreamItemReader();
    }

    @Bean
    public DataStreamItemProcessor dataStreamProcessor() {
        return new DataStreamItemProcessor();
    }

    @Bean
    public DataStreamItemWriter dataStreamWriter() {
        return new DataStreamItemWriter();
    }

    @Bean
    public Job dataStreamJob(JobRepository jobRepository, Step step1, DataStreamJobExecutionListener listener) {
        return new JobBuilder("dataStreamJob", jobRepository)
            .listener(listener)
            .start(step1)
            .build();
    }

    @Bean
    public Step step1(
        JobRepository jobRepository, DataSourceTransactionManager transactionManager,
        DataStreamItemReader reader, DataStreamItemProcessor processor, DataStreamItemWriter writer) {

        return new StepBuilder("step1", jobRepository)
            .<Map<String, ?>, Map<String, ?>>chunk(10, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }
}
