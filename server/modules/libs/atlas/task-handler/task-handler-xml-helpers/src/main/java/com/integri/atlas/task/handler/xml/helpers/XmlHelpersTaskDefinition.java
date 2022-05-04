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

package com.integri.atlas.task.handler.xml.helpers;

import static com.integri.atlas.task.definition.dsl.TaskParameterValue.parameterValues;
import static com.integri.atlas.task.definition.dsl.TaskProperty.JSON_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.SELECT_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.STRING_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.show;
import static com.integri.atlas.task.definition.dsl.TaskPropertyOption.option;
import static com.integri.atlas.task.handler.xml.helpers.XmlHelpersTaskConstants.*;

import com.integri.atlas.task.definition.TaskDefinition;
import com.integri.atlas.task.definition.dsl.TaskSpecification;
import com.integri.atlas.task.handler.xml.helpers.XmlHelpersTaskConstants.Operation;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class XmlHelpersTaskDefinition implements TaskDefinition {

    public static final TaskSpecification TASK_SPECIFICATION = TaskSpecification
        .create(TASK_XML_HELPERS)
        .displayName("XML Helpers")
        .description("Converts between XML string and object/array.")
        .properties(
            SELECT_PROPERTY(PROPERTY_OPERATION)
                .displayName("Operation")
                .description("The operation to perform.")
                .options(
                    option(
                        "Convert from XML string",
                        Operation.XML_TO_JSON.name(),
                        "Converts the XML string to object/array."
                    ),
                    option("Convert to XML string", "JSON_TO_XML", "Writes the object/array to a XML string.")
                )
                .defaultValue(Operation.XML_TO_JSON.name())
                .required(true),
            STRING_PROPERTY(PROPERTY_SOURCE)
                .displayName("Source")
                .description("The XML string to convert to the data.")
                .displayOption(show(PROPERTY_OPERATION, parameterValues(Operation.XML_TO_JSON.name())))
                .required(true),
            JSON_PROPERTY(PROPERTY_SOURCE)
                .displayName("Source")
                .description("The data to convert to XML string.")
                .displayOption(show(PROPERTY_OPERATION, parameterValues(Operation.JSON_TO_XML.name())))
                .required(true)
        );

    @Override
    public TaskSpecification getSpecification() {
        return TASK_SPECIFICATION;
    }
}
