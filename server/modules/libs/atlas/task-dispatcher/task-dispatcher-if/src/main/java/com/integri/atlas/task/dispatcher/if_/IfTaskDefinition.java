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

package com.integri.atlas.task.dispatcher.if_;

import static com.integri.atlas.task.definition.dsl.TaskProperty.BOOLEAN_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.COLLECTION_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.DATE_TIME_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.GROUP_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.NUMBER_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.SELECT_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.STRING_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.hide;
import static com.integri.atlas.task.definition.dsl.TaskProperty.multipleValues;
import static com.integri.atlas.task.definition.dsl.TaskProperty.show;
import static com.integri.atlas.task.definition.dsl.TaskPropertyOption.option;
import static com.integri.atlas.task.dispatcher.if_.IfTaskConstants.CombineOperation;
import static com.integri.atlas.task.dispatcher.if_.IfTaskConstants.Operation;
import static com.integri.atlas.task.dispatcher.if_.IfTaskConstants.PROPERTY_BOOLEAN;
import static com.integri.atlas.task.dispatcher.if_.IfTaskConstants.PROPERTY_COMBINE_OPERATION;
import static com.integri.atlas.task.dispatcher.if_.IfTaskConstants.PROPERTY_CONDITIONS;
import static com.integri.atlas.task.dispatcher.if_.IfTaskConstants.PROPERTY_DATE_TIME;
import static com.integri.atlas.task.dispatcher.if_.IfTaskConstants.PROPERTY_NUMBER;
import static com.integri.atlas.task.dispatcher.if_.IfTaskConstants.PROPERTY_OPERATION;
import static com.integri.atlas.task.dispatcher.if_.IfTaskConstants.PROPERTY_RAW_CONDITIONS;
import static com.integri.atlas.task.dispatcher.if_.IfTaskConstants.PROPERTY_STRING;
import static com.integri.atlas.task.dispatcher.if_.IfTaskConstants.PROPERTY_VALUE_1;
import static com.integri.atlas.task.dispatcher.if_.IfTaskConstants.PROPERTY_VALUE_2;
import static com.integri.atlas.task.dispatcher.if_.IfTaskConstants.TASK_IF;

import com.integri.atlas.task.definition.TaskDefinition;
import com.integri.atlas.task.definition.dsl.TaskSpecification;

/**
 * @author Ivica Cardic
 */
public class IfTaskDefinition implements TaskDefinition {

    public static final TaskSpecification TASK_SPECIFICATION = TaskSpecification
        .create(TASK_IF)
        .displayName("If")
        .description("Directs a stream based on true/false results of comparisons")
        .properties(
            BOOLEAN_PROPERTY(PROPERTY_RAW_CONDITIONS)
                .displayName("RAW Conditions")
                .description("If the conditions should be set via the key-value pair in UI or as an raw expression).")
                .defaultValue(false),
            STRING_PROPERTY(PROPERTY_CONDITIONS)
                .displayName("Conditions")
                .description("The conditions expressed as an expression.")
                .displayOption(show("rawConditions", true)),
            COLLECTION_PROPERTY(PROPERTY_CONDITIONS)
                .displayName("Conditions")
                .placeholder("Add Condition")
                .description("The type of values to compare.")
                .displayOption(show(PROPERTY_RAW_CONDITIONS, false))
                .typeOption(multipleValues(true))
                .options(
                    GROUP_PROPERTY(PROPERTY_BOOLEAN)
                        .displayName("Boolean")
                        .groupProperties(
                            BOOLEAN_PROPERTY(PROPERTY_VALUE_1)
                                .displayName("Value 1")
                                .description("The boolean value to compare with the second one.")
                                .defaultValue(false),
                            SELECT_PROPERTY(PROPERTY_OPERATION)
                                .displayName("Operation")
                                .description("Compare operation to decide where to map data.")
                                .options(
                                    option("Equals", Operation.EQUALS.name()),
                                    option("Not Equals", Operation.NOT_EQUALS.name())
                                )
                                .defaultValue(Operation.EQUALS.name()),
                            BOOLEAN_PROPERTY(PROPERTY_VALUE_2)
                                .displayName("Value 2")
                                .description("The boolean value to compare with the first one.")
                                .defaultValue(false)
                        ),
                    GROUP_PROPERTY(PROPERTY_DATE_TIME)
                        .displayName("Date & Time")
                        .groupProperties(
                            DATE_TIME_PROPERTY(PROPERTY_VALUE_1)
                                .displayName("Value 1")
                                .description("The date & time value to compare with the second one.")
                                .defaultValue(null),
                            SELECT_PROPERTY(PROPERTY_OPERATION)
                                .displayName("Operation")
                                .description("Compare operation to decide where to map data.")
                                .options(
                                    option("After", Operation.AFTER.name()),
                                    option("Before", Operation.BEFORE.name())
                                )
                                .defaultValue(Operation.AFTER.name()),
                            DATE_TIME_PROPERTY(PROPERTY_VALUE_2)
                                .displayName("Value 2")
                                .description("The date & time value to compare with the first one.")
                                .defaultValue(null)
                        ),
                    GROUP_PROPERTY(PROPERTY_NUMBER)
                        .displayName("Number")
                        .groupProperties(
                            NUMBER_PROPERTY(PROPERTY_VALUE_1)
                                .displayName("Value 1")
                                .description("The number value to compare with the second one.")
                                .defaultValue(0),
                            SELECT_PROPERTY(PROPERTY_OPERATION)
                                .displayName("Operation")
                                .description("Compare operation to decide where to map data.")
                                .options(
                                    option("Less", Operation.LESS.name()),
                                    option("Less or Equals", Operation.LESS_EQUALS.name()),
                                    option("Equals", Operation.EQUALS.name()),
                                    option("Not Equals", Operation.NOT_EQUALS.name()),
                                    option("Greater", Operation.GREATER.name()),
                                    option("Greater or Equals", Operation.GREATER_EQUALS.name()),
                                    option("Empty", Operation.EMPTY.name())
                                )
                                .defaultValue(Operation.LESS.name()),
                            NUMBER_PROPERTY(PROPERTY_VALUE_2)
                                .displayName("Value 2")
                                .description("The number value to compare with the first one.")
                                .defaultValue(0)
                                .displayOption(hide(PROPERTY_OPERATION))
                        ),
                    GROUP_PROPERTY(PROPERTY_STRING)
                        .displayName("String")
                        .groupProperties(
                            STRING_PROPERTY(PROPERTY_VALUE_1)
                                .displayName("Value 1")
                                .description("The string value to compare with the second one.")
                                .defaultValue(""),
                            SELECT_PROPERTY(PROPERTY_OPERATION)
                                .displayName("Operation")
                                .description("Compare operation to decide where to map data.")
                                .options(
                                    option("Equals", Operation.EQUALS.name()),
                                    option("Not Equals", Operation.NOT_EQUALS.name()),
                                    option("Contains", Operation.CONTAINS.name()),
                                    option("Not Contains", Operation.NOT_CONTAINS.name()),
                                    option("Starts With", Operation.STARTS_WITH.name()),
                                    option("Ends With", Operation.ENDS_WITH.name()),
                                    option("Regex", Operation.REGEX.name()),
                                    option("Empty", Operation.EMPTY.name())
                                )
                                .defaultValue(Operation.EQUALS.name()),
                            STRING_PROPERTY(PROPERTY_VALUE_2)
                                .displayName("Value 2")
                                .description("The string value to compare with the first one.")
                                .defaultValue("")
                                .displayOption(
                                    hide(PROPERTY_OPERATION, Operation.EMPTY.name(), Operation.REGEX.name())
                                ),
                            STRING_PROPERTY(PROPERTY_VALUE_2)
                                .displayName("Regex")
                                .description("The regex value to compare with the first one.")
                                .placeholder("/text/i")
                                .defaultValue("")
                                .displayOption(show(PROPERTY_OPERATION, Operation.REGEX.name()))
                        )
                ),
            SELECT_PROPERTY(PROPERTY_COMBINE_OPERATION)
                .displayName("Combine")
                .description(
                    """
                    If multiple conditions are set, this setting decides if it is true as soon as ANY condition
                     matches or only if ALL are met.
                    """
                )
                .displayOption(show(PROPERTY_RAW_CONDITIONS, false))
                .options(
                    option(
                        "All",
                        CombineOperation.ALL.name(),
                        "Only if all conditions are met, the workflow goes into \"true\" branch."
                    ),
                    option(
                        "Any",
                        CombineOperation.ANY.name(),
                        "If any condition is met, the workflow goes into \"true\" branch."
                    )
                )
                .defaultValue(CombineOperation.ALL.name())
        );

    @Override
    public TaskSpecification getSpecification() {
        return TASK_SPECIFICATION;
    }
}
