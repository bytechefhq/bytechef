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

package com.bytechef.component.datastream.config;

import com.bytechef.component.datastream.converter.MapToStringConverter;
import com.bytechef.component.datastream.converter.StringToMapConverter;
import com.bytechef.component.datastream.item.ItemStreamProcessorDelegate;
import com.bytechef.component.datastream.item.ItemStreamReaderDelegate;
import com.bytechef.component.datastream.item.ItemStreamWriterDelegate;
import com.bytechef.component.datastream.listener.DataStreamJobExecutionListener;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

/**
 * @author Ivica Cardic
 */
@Configuration
public class DataStreamConfiguration extends DefaultBatchConfiguration {

    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI")
    public DataStreamConfiguration(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    @NonNull
    protected ConfigurableConversionService getConversionService() {
        ConfigurableConversionService conversionService = super.getConversionService();

        conversionService.addConverter(new MapToStringConverter(objectMapper));
        conversionService.addConverter(new StringToMapConverter(objectMapper));

        return conversionService;
    }

    @Bean
    public Job dataStreamJob(JobRepository jobRepository, Step step1, DataStreamJobExecutionListener listener) {
        return new JobBuilder("dataStreamJob", jobRepository)
            .listener(listener)
            .incrementer(new RunIdIncrementer())
            .start(step1)
            .build();
    }

    @Bean
    @JobScope
    public Step step1(
        ClusterElementDefinitionService clusterElementDefinitionService, ContextFactory contextFactory,
        JobRepository jobRepository, DataSourceTransactionManager transactionManager) {

        return new StepBuilder("step1", jobRepository)
            .<Map<String, Object>, Map<String, Object>>chunk(10, transactionManager)
            .reader(new ItemStreamReaderDelegate(clusterElementDefinitionService, contextFactory))
            .processor(new ItemStreamProcessorDelegate(clusterElementDefinitionService, contextFactory))
            .writer(new ItemStreamWriterDelegate(clusterElementDefinitionService, contextFactory))
            .build();
    }
}
