/*
 * Copyright 2021 <your company/name>.
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
 */

package com.integri.atlas.task.dispatcher.if_;

import static com.integri.atlas.engine.core.task.description.TaskSpecification.task;
import static com.integri.atlas.engine.core.task.description.TaskProperty.BOOLEAN_PROPERTY;
import static com.integri.atlas.engine.core.task.description.TaskProperty.COLLECTION_PROPERTY;
import static com.integri.atlas.engine.core.task.description.TaskProperty.DATE_TIME_PROPERTY;
import static com.integri.atlas.engine.core.task.description.TaskProperty.GROUP_PROPERTY;
import static com.integri.atlas.engine.core.task.description.TaskProperty.NUMBER_PROPERTY;
import static com.integri.atlas.engine.core.task.description.TaskProperty.SELECT_PROPERTY;
import static com.integri.atlas.engine.core.task.description.TaskProperty.STRING_PROPERTY;
import static com.integri.atlas.engine.core.task.description.TaskProperty.hide;
import static com.integri.atlas.engine.core.task.description.TaskProperty.multipleValues;
import static com.integri.atlas.engine.core.task.description.TaskProperty.show;
import static com.integri.atlas.engine.core.task.description.TaskPropertyOption.option;

import com.integri.atlas.engine.core.task.TaskDefinition;
import com.integri.atlas.engine.core.task.description.TaskSpecification;

/**
 * @author Ivica Cardic
 */
public class IfTaskDefinition implements TaskDefinition {

    private static final TaskSpecification TASK_DESCRIPTION = task("if")
        .displayName("IF")
        .description("Directs a stream based on true/false results of comparisons")
        .properties(
            COLLECTION_PROPERTY("conditions")
                .displayName("Conditions")
                .placeholder("Add Condition")
                .description("The type of values to compare.")
                .typeOption(multipleValues(true))
                .options(
                    GROUP_PROPERTY("boolean")
                        .displayName("Boolean")
                        .fields(
                            BOOLEAN_PROPERTY("value1")
                                .displayName("Value 1")
                                .description("The boolean value to compare with the second one.")
                                .defaultValue(false),
                            SELECT_PROPERTY("operation")
                                .displayName("Operation")
                                .description("Compare operation to decide where to map data.")
                                .options(option("Equal", "equal"), option("Not Equal", "notEqual"))
                                .defaultValue("equal"),
                            BOOLEAN_PROPERTY("value2")
                                .displayName("Value 2")
                                .description("The boolean value to compare with the first one.")
                                .defaultValue(false)
                        ),
                    GROUP_PROPERTY("dateTime")
                        .displayName("Date & Time")
                        .fields(
                            DATE_TIME_PROPERTY("value1")
                                .displayName("Value 1")
                                .description("The date & time value to compare with the second one.")
                                .defaultValue(null),
                            SELECT_PROPERTY("operation")
                                .displayName("Operation")
                                .description("Compare operation to decide where to map data.")
                                .options(option("After", "after"), option("Before", "before"))
                                .defaultValue("after"),
                            DATE_TIME_PROPERTY("value2")
                                .displayName("Value 2")
                                .description("The date & time value to compare with the first one.")
                                .defaultValue(null)
                        ),
                    GROUP_PROPERTY("number")
                        .displayName("Number")
                        .fields(
                            NUMBER_PROPERTY("value1")
                                .displayName("Value 1")
                                .description("The number value to compare with the second one.")
                                .defaultValue(0),
                            SELECT_PROPERTY("operation")
                                .displayName("Operation")
                                .description("Compare operation to decide where to map data.")
                                .options(
                                    option("Smaller", "smaller"),
                                    option("Smaller or Equal", "smallerEqual"),
                                    option("Equal", "equal"),
                                    option("Not Equal", "notEqual"),
                                    option("Larger", "larger"),
                                    option("Larger or Equal", "largerEqual"),
                                    option("Empty", "empty")
                                )
                                .defaultValue("smaller"),
                            NUMBER_PROPERTY("value2")
                                .displayName("Value 2")
                                .description("The number value to compare with the first one.")
                                .defaultValue(0)
                                .displayOption(hide("operation"))
                        ),
                    GROUP_PROPERTY("string")
                        .displayName("String")
                        .fields(
                            STRING_PROPERTY("value1")
                                .displayName("Value 1")
                                .description("The string value to compare with the second one.")
                                .defaultValue(""),
                            SELECT_PROPERTY("operation")
                                .displayName("Operation")
                                .description("Compare operation to decide where to map data.")
                                .options(
                                    option("Equal", "equal"),
                                    option("Not Equal", "notEqual"),
                                    option("Contains", "contains"),
                                    option("Not Contains", "notContains"),
                                    option("Starts With", "startsWith"),
                                    option("Ends With", "endsWith"),
                                    option("Regex", "regex"),
                                    option("Empty", "empty")
                                )
                                .defaultValue("equal"),
                            STRING_PROPERTY("value2")
                                .displayName("Value 2")
                                .description("The string value to compare with the first one.")
                                .defaultValue("")
                                .displayOption(hide("operation", "empty", "regex")),
                            STRING_PROPERTY("value2")
                                .displayName("Regex")
                                .description("The regex value to compare with the first one.")
                                .placeholder("/text/i")
                                .defaultValue("")
                                .displayOption(show("operation", "regex"))
                        )
                ),
            SELECT_PROPERTY("combineOperation")
                .displayName("Combine")
                .options(
                    option("All", "all", "Only if all conditions are met, the workflow goes into \"true\" branch."),
                    option("Any", "any", "If any condition is met, the workflow goes into \"true\" branch.")
                )
                .description(
                    """
                            If multiple conditions are set, this setting decides if it is true as soon as ANY condition
                             matches or only if ALL are met.
                            """
                )
                .defaultValue("all"),
            COLLECTION_PROPERTY("caseTrue")
                .description("List of tasks that will be executed when result of resolving all conditions is TRUE")
                .displayName("Case TRUE")
                .placeholder("Add Task"),
            COLLECTION_PROPERTY("caseFalse")
                .description("List of tasks that will be executed when result of resolving all conditions is FALSE")
                .displayName("Case FALSE")
                .placeholder("Add Task")
        );

    @Override
    public TaskSpecification getSpecification() {
        return TASK_DESCRIPTION;
    }
}
