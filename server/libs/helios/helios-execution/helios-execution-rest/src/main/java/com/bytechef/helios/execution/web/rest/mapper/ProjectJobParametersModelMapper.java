
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

package com.bytechef.helios.execution.web.rest.mapper;

import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.helios.execution.web.rest.mapper.config.ProjectExecutionMapperSpringConfig;
import com.bytechef.helios.execution.web.rest.model.JobParametersModel;
import com.bytechef.helios.execution.web.rest.model.TaskConnectionModel;
import com.bytechef.hermes.configuration.connection.WorkflowConnection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.core.convert.converter.Converter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Mapper(config = ProjectExecutionMapperSpringConfig.class)
public interface ProjectJobParametersModelMapper extends Converter<JobParametersModel, JobParameters> {

    @Override
    @Mapping(target = "metadata", qualifiedByName = "metadata", source = ".")
    JobParameters convert(JobParametersModel jobParametersModel);

    @Named("metadata")
    default Map<String, Object> getMetadata(JobParametersModel jobParametersModel) {
        Map<String, Map<String, Map<String, Long>>> connectionMap = new HashMap<>();

        if (jobParametersModel.getConnections() != null) {
            for (TaskConnectionModel taskConnectionModel : jobParametersModel.getConnections()) {
                Map<String, Map<String, Long>> connection = connectionMap.computeIfAbsent(
                    taskConnectionModel.getTaskName(), key -> new HashMap<>());

                connection.put(
                    taskConnectionModel.getKey(), Map.of(WorkflowConnection.ID, taskConnectionModel.getId()));
            }
        }

        return Map.of(WorkflowConnection.CONNECTIONS, connectionMap);
    }
}
