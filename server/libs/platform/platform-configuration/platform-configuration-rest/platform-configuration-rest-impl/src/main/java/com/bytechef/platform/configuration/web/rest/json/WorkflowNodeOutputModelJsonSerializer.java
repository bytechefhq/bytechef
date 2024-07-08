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

package com.bytechef.platform.configuration.web.rest.json;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.configuration.web.rest.model.WorkflowNodeOutputModel;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import org.springframework.boot.jackson.JsonComponent;

/**
 * @author Ivica Cardic
 */
@JsonComponent
public class WorkflowNodeOutputModelJsonSerializer extends JsonSerializer<WorkflowNodeOutputModel> {
    @Override
    public void serialize(WorkflowNodeOutputModel value, JsonGenerator jsonGenerator, SerializerProvider serializers)
        throws IOException {

        jsonGenerator.writeStartObject();

        if (value.getActionDefinition() != null) {
            jsonGenerator.writeObjectField("actionDefinition", value.getActionDefinition());
        }

        if (value.getOutputSchema() != null) {
            jsonGenerator.writeObjectField("outputSchema", value.getOutputSchema());
        }

        if (value.getTaskDispatcherDefinition() != null) {
            jsonGenerator.writeObjectField("taskDispatcherDefinition", value.getTaskDispatcherDefinition());
        }

        if (value.getTriggerDefinition() != null) {
            jsonGenerator.writeObjectField("triggerDefinition", value.getTriggerDefinition());
        }

        jsonGenerator.writeStringField("workflowNodeName", value.getWorkflowNodeName());

        // Include null values in sampleOutput

        if (value.getSampleOutput() != null) {
            jsonGenerator.writeFieldName("sampleOutput");
            jsonGenerator.writeRaw(":");
            jsonGenerator.writeRaw(JsonUtils.write(value.getSampleOutput(), true));
        }

        jsonGenerator.writeEndObject();
    }
}
