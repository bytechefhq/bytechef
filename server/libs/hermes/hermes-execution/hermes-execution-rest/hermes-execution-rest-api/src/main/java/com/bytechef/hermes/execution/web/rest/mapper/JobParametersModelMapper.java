
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.execution.web.rest.mapper;

import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.hermes.configuration.connection.WorkflowConnection;
import com.bytechef.hermes.execution.web.rest.mapper.config.WorkflowExecutionMapperSpringConfig;
import com.bytechef.hermes.execution.web.rest.model.JobParametersModel;
import com.bytechef.hermes.execution.web.rest.model.JobConnectionModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.core.convert.converter.Converter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Mapper(config = WorkflowExecutionMapperSpringConfig.class)
public interface JobParametersModelMapper extends Converter<JobParametersModel, JobParameters> {

    @Override
    @Mapping(target = "metadata", qualifiedByName = "metadata", source = ".")
    JobParameters convert(JobParametersModel jobParametersModel);

    @Named("metadata")
    default Map<String, Object> getMetadata(JobParametersModel jobParametersModel) {
        Map<String, Map<String, Map<String, Long>>> taskConnectionMap = new HashMap<>();

        if (jobParametersModel.getConnections() != null) {
            for (JobConnectionModel jobConnectionModel : jobParametersModel.getConnections()) {
                Map<String, Map<String, Long>> workflowConnection = taskConnectionMap.computeIfAbsent(
                    jobConnectionModel.getTaskName(), key -> new HashMap<>());

                workflowConnection.put(
                    jobConnectionModel.getKey(), Map.of(WorkflowConnection.ID, jobConnectionModel.getId()));
            }
        }

        return Map.of(WorkflowConnection.CONNECTIONS, taskConnectionMap);
    }
}
