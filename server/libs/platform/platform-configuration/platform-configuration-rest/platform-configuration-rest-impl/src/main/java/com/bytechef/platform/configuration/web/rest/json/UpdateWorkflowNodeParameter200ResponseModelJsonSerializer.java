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
import com.bytechef.platform.configuration.web.rest.model.UpdateWorkflowNodeParameter200ResponseModel;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import org.springframework.boot.jackson.JsonComponent;

@JsonComponent
public class UpdateWorkflowNodeParameter200ResponseModelJsonSerializer
    extends JsonSerializer<UpdateWorkflowNodeParameter200ResponseModel> {

    @Override
    public void serialize(
        UpdateWorkflowNodeParameter200ResponseModel value, JsonGenerator jsonGenerator, SerializerProvider serializers)
        throws IOException {

        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField("displayConditions", value.getDisplayConditions());
        jsonGenerator.writeObjectField("metadata", value.getMetadata());
        jsonGenerator.writeFieldName("parameters");
        jsonGenerator.writeRaw(":");
        jsonGenerator.writeRaw(JsonUtils.write(value.getParameters(), true));
        jsonGenerator.writeEndObject();
    }
}
