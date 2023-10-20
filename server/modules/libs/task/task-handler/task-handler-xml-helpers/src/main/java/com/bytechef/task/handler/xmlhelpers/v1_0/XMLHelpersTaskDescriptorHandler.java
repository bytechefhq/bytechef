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

package com.bytechef.task.handler.xmlhelpers.v1_0;

import static com.bytechef.hermes.descriptor.domain.DSL.ANY_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.ARRAY_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.OBJECT_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.OPERATION;
import static com.bytechef.hermes.descriptor.domain.DSL.STRING_PROPERTY;

import com.bytechef.hermes.descriptor.domain.DSL;
import com.bytechef.hermes.descriptor.domain.TaskDescriptor;
import com.bytechef.hermes.descriptor.handler.TaskDescriptorHandler;
import com.bytechef.task.handler.xmlhelpers.XMLHelpersTaskConstants;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class XMLHelpersTaskDescriptorHandler implements TaskDescriptorHandler {

    private static final TaskDescriptor TASK_DESCRIPTOR = DSL.createTaskDescriptor(XMLHelpersTaskConstants.XML_HELPERS)
            .displayName("XML Helpers")
            .description("Converts between XML string and object/array.")
            .version(XMLHelpersTaskConstants.VERSION_1_0)
            .operations(
                    OPERATION(XMLHelpersTaskConstants.XML_PARSE)
                            .displayName("Convert from XML string")
                            .description("Converts the XML string to object/array.")
                            .inputs(STRING_PROPERTY(XMLHelpersTaskConstants.SOURCE)
                                    .displayName("Source")
                                    .description("The XML string to convert to the data.")
                                    .required(true))
                            .outputs(OBJECT_PROPERTY()),
                    OPERATION(XMLHelpersTaskConstants.XML_STRINGIFY)
                            .displayName("Convert to XML string")
                            .description("Writes the object/array to a XML string.")
                            .inputs(ANY_PROPERTY(XMLHelpersTaskConstants.SOURCE)
                                    .displayName("Source")
                                    .description("The data to convert to XML string.")
                                    .required(true)
                                    .types(ARRAY_PROPERTY(), OBJECT_PROPERTY()))
                            .outputs(STRING_PROPERTY()));

    @Override
    public TaskDescriptor getTaskDescriptor() {
        return TASK_DESCRIPTOR;
    }
}
