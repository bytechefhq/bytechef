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

package com.bytechef.platform.workflow.execution.web.rest.mapper;

import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.web.rest.mapper.config.PlatformWorkflowExecutionMapperSpringConfig;
import com.bytechef.platform.workflow.execution.web.rest.model.JobConnectionModel;
import com.bytechef.platform.workflow.execution.web.rest.model.JobParametersModel;
import com.bytechef.platform.workflow.execution.web.rest.model.TriggerOutputModel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
@Mapper(config = PlatformWorkflowExecutionMapperSpringConfig.class)
public abstract class JobParametersModelMapper implements Converter<JobParametersModel, JobParameters> {

    @Override
    @Mapping(target = "inputs", qualifiedByName = "inputs", source = ".")
    @Mapping(target = "metadata", qualifiedByName = "metadata", source = ".")
    public abstract JobParameters convert(JobParametersModel jobParametersModel);

    @Named("inputs")
    public Map<String, Object> getInputs(JobParametersModel jobParametersModel) {
        return getInputs(
            jobParametersModel.getInputs() == null ? Map.of() : jobParametersModel.getInputs(),
            jobParametersModel.getTriggerOutputs() == null ? List.of() : jobParametersModel.getTriggerOutputs());
    }

    @Named("metadata")
    public Map<String, Object> getMetadata(JobParametersModel jobParametersModel) {
        return getMetadata(
            jobParametersModel.getConnections() == null ? List.of() : jobParametersModel.getConnections());
    }

    private static Map<String, Object> getMetadata(List<JobConnectionModel> jobConnectionModels) {
        Map<String, Map<String, Long>> connectionIdsMap = new HashMap<>();

        for (JobConnectionModel jobConnectionModel : jobConnectionModels) {
            Map<String, Long> connectionIdMap = connectionIdsMap.computeIfAbsent(
                jobConnectionModel.getTaskName(), key -> new HashMap<>());

            connectionIdMap.put(jobConnectionModel.getKey(), jobConnectionModel.getId());
        }

        return Map.of(MetadataConstants.CONNECTION_IDS, connectionIdsMap);
    }

    private static Map<String, Object> getInputs(
        Map<String, Object> inputs, List<TriggerOutputModel> triggerOutputModels) {

        inputs = new HashMap<>(inputs);

        for (TriggerOutputModel triggerOutputModel : triggerOutputModels) {
            inputs.put(triggerOutputModel.getTriggerName(), triggerOutputModel.getValue());
        }

        return inputs;
    }
}
