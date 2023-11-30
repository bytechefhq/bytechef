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

package com.bytechef.helios.execution.web.rest.mapper.util;

import com.bytechef.helios.execution.web.rest.model.TaskConnectionModel;
import com.bytechef.helios.execution.web.rest.model.TriggerOutputModel;
import com.bytechef.hermes.configuration.constant.MetadataConstants;
import com.bytechef.hermes.configuration.domain.WorkflowConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ConverterUtils {

    public static Map<String, Object> getMetadata(List<TaskConnectionModel> taskConnectionModels) {
        Map<String, Map<String, Map<String, Long>>> connectionMap = new HashMap<>();

        for (TaskConnectionModel testConnectionModel : taskConnectionModels) {
            Map<String, Map<String, Long>> connection = connectionMap.computeIfAbsent(
                testConnectionModel.getTaskName(), key -> new HashMap<>());

            connection.put(testConnectionModel.getKey(), Map.of(WorkflowConnection.ID, testConnectionModel.getId()));
        }

        return Map.of(MetadataConstants.CONNECTIONS, connectionMap);
    }

    public static Map<String, Object> getInputs(
        Map<String, Object> inputs, List<TriggerOutputModel> triggerOutputModels) {

        inputs = new HashMap<>(inputs);

        for (TriggerOutputModel triggerOutputModel : triggerOutputModels) {
            inputs.put(triggerOutputModel.getTriggerName(), triggerOutputModel.getValue());
        }

        return inputs;
    }
}
