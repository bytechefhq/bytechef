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

package com.bytechef.platform.workflow.execution.web.rest.mapper;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.web.rest.mapper.config.PlatformWorkflowExecutionMapperSpringConfig;
import com.bytechef.platform.workflow.execution.web.rest.model.JobBasicModel;
import com.bytechef.platform.workflow.execution.web.rest.model.JobModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
public class JobMapper {

    @Mapper(config = PlatformWorkflowExecutionMapperSpringConfig.class)
    public interface JobDTOToJobModelMapper extends Converter<JobDTO, JobModel> {

        @Override
        JobModel convert(JobDTO jobDTO);
    }

    @Mapper(config = PlatformWorkflowExecutionMapperSpringConfig.class)
    public interface JobToJobBasicModelMapper extends Converter<Job, JobBasicModel> {

        @Override
        JobBasicModel convert(Job job);
    }

    @Mapper(config = PlatformWorkflowExecutionMapperSpringConfig.class)
    public interface JobToJobModelMapper extends Converter<Job, JobModel> {

        @Override
        @Mapping(target = "outputs", ignore = true)
        @Mapping(target = "taskExecutions", ignore = true)
        JobModel convert(Job job);
    }
}
