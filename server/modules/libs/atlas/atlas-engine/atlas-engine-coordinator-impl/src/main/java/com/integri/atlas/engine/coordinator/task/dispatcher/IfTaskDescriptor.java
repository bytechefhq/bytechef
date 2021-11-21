/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.coordinator.task.dispatcher;

import static com.integri.atlas.engine.core.task.TaskDescriptor.task;
import static com.integri.atlas.engine.core.task.description.TaskDescription.property;
import static com.integri.atlas.engine.core.task.description.TaskProperty.hide;
import static com.integri.atlas.engine.core.task.description.TaskProperty.multipleValues;
import static com.integri.atlas.engine.core.task.description.TaskProperty.properties;
import static com.integri.atlas.engine.core.task.description.TaskProperty.show;
import static com.integri.atlas.engine.core.task.description.TaskPropertyOption.propertyGroup;
import static com.integri.atlas.engine.core.task.description.TaskPropertyOption.propertyOption;
import static com.integri.atlas.engine.core.task.description.TaskPropertyType.BOOLEAN;
import static com.integri.atlas.engine.core.task.description.TaskPropertyType.COLLECTION;
import static com.integri.atlas.engine.core.task.description.TaskPropertyType.DATE_TIME;
import static com.integri.atlas.engine.core.task.description.TaskPropertyType.NUMBER;
import static com.integri.atlas.engine.core.task.description.TaskPropertyType.SELECT;
import static com.integri.atlas.engine.core.task.description.TaskPropertyType.STRING;

import com.integri.atlas.engine.core.task.TaskDescriptor;
import com.integri.atlas.engine.core.task.description.TaskDescription;

/**
 * @author Ivica Cardic
 */
public class IfTaskDescriptor implements TaskDescriptor {

    private static final TaskDescription TASK_DESCRIPTION = task("if")
        .displayName("IF")
        .description("Directs a stream based on true/false results of comparisons")
        .properties(
            property("conditions")
                .displayName("Conditions")
                .placeholder("Add Condition")
                .type(COLLECTION)
                .propertyTypeOption(multipleValues(true))
                .description("The type of values to compare.")
                .propertyOptions(
                    propertyGroup(
                        "boolean",
                        "Boolean",
                        properties(
                            property("value1")
                                .displayName("Value 1")
                                .type(BOOLEAN)
                                .description("The boolean value to compare with the second one.")
                                .defaultValue(false),
                            property("operation")
                                .displayName("Operation")
                                .type(SELECT)
                                .description("Compare operation to decide where to map data.")
                                .propertyOptions(
                                    propertyOption("Equal", "equal"),
                                    propertyOption("Not Equal", "notEqual")
                                )
                                .defaultValue("equal"),
                            property("value2")
                                .displayName("Value 2")
                                .type(BOOLEAN)
                                .description("The boolean value to compare with the first one.")
                                .defaultValue(false)
                        )
                    ),
                    propertyGroup(
                        "dateTime",
                        "Date & Time",
                        properties(
                            property("value1")
                                .displayName("Value 1")
                                .type(DATE_TIME)
                                .description("The date & time value to compare with the second one.")
                                .defaultValue(""),
                            property("operation")
                                .displayName("Operation")
                                .type(SELECT)
                                .description("Compare operation to decide where to map data.")
                                .propertyOptions(propertyOption("After", "after"), propertyOption("Before", "before"))
                                .defaultValue("after"),
                            property("value2")
                                .displayName("Value 2")
                                .type(DATE_TIME)
                                .description("The date & time value to compare with the first one.")
                                .defaultValue("")
                        )
                    ),
                    propertyGroup(
                        "number",
                        "Number",
                        properties(
                            property("value1")
                                .displayName("Value 1")
                                .type(NUMBER)
                                .description("The number value to compare with the second one.")
                                .defaultValue(0),
                            property("operation")
                                .displayName("Operation")
                                .type(SELECT)
                                .description("Compare operation to decide where to map data.")
                                .propertyOptions(
                                    propertyOption("Smaller", "smaller"),
                                    propertyOption("Smaller or Equal", "smallerEqual"),
                                    propertyOption("Equal", "equal"),
                                    propertyOption("Not Equal", "notEqual"),
                                    propertyOption("Larger", "larger"),
                                    propertyOption("Larger or Equal", "largerEqual"),
                                    propertyOption("Empty", "empty")
                                )
                                .defaultValue("smaller"),
                            property("value2")
                                .displayName("Value 2")
                                .type(NUMBER)
                                .description("The number value to compare with the first one.")
                                .defaultValue(0)
                                .displayOption(hide("operation"))
                        )
                    ),
                    propertyGroup(
                        "string",
                        "String",
                        properties(
                            property("value1")
                                .displayName("Value 1")
                                .type(STRING)
                                .description("The string value to compare with the second one.")
                                .defaultValue(""),
                            property("operation")
                                .displayName("Operation")
                                .type(SELECT)
                                .description("Compare operation to decide where to map data.")
                                .propertyOptions(
                                    propertyOption("Equal", "equal"),
                                    propertyOption("Not Equal", "notEqual"),
                                    propertyOption("Contains", "contains"),
                                    propertyOption("Not Contains", "notContains"),
                                    propertyOption("Starts With", "startsWith"),
                                    propertyOption("Ends With", "endsWith"),
                                    propertyOption("Regex", "regex"),
                                    propertyOption("Empty", "empty")
                                )
                                .defaultValue("equal"),
                            property("value2")
                                .displayName("Value 2")
                                .type(STRING)
                                .description("The string value to compare with the first one.")
                                .defaultValue("")
                                .displayOption(hide("operation", "empty", "regex")),
                            property("value2")
                                .displayName("Regex")
                                .type(STRING)
                                .description("The regex value to compare with the first one.")
                                .placeholder("/text/i")
                                .defaultValue("")
                                .displayOption(show("operation", "regex"))
                        )
                    )
                ),
            property("combineOperation")
                .displayName("Combine")
                .type(SELECT)
                .propertyOptions(
                    propertyOption(
                        "All",
                        "all",
                        "Only if all conditions are met, the workflow goes into \"true\" branch."
                    ),
                    propertyOption("Any", "any", "If all conditions are met, the workflow goes into \"true\" branch.")
                )
                .description(
                    """
                            If multiple conditions are set, this setting decides if it is true as soon as ANY condition
                             matches or only if ALL are met.
                            """
                )
                .defaultValue("all")
        );

    @Override
    public TaskDescription getDescription() {
        return TASK_DESCRIPTION;
    }
}
