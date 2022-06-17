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

package com.bytechef.task.dispatcher.if_;

import static com.bytechef.hermes.descriptor.domain.DSL.ARRAY_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.BOOLEAN_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.DATE_TIME_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.NUMBER_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.OBJECT_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.OPERATION;
import static com.bytechef.hermes.descriptor.domain.DSL.STRING_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.hideWhen;
import static com.bytechef.hermes.descriptor.domain.DSL.showWhen;

import com.bytechef.hermes.descriptor.domain.DSL;
import com.bytechef.hermes.descriptor.domain.TaskDescriptor;
import com.bytechef.hermes.descriptor.handler.TaskDescriptorHandler;

/**
 * @author Ivica Cardic
 */
public class IfTaskDescriptorHandler implements TaskDescriptorHandler {

    private static final TaskDescriptor TASK_DESCRIPTOR = DSL.createTaskDescriptor(IfTaskConstants.TASK_IF)
            .displayName("If")
            .description("Directs a stream based on true/false results of comparisons")
            .operations(OPERATION("Branch")
                    .inputs(
                            BOOLEAN_PROPERTY(IfTaskConstants.PROPERTY_RAW_CONDITIONS)
                                    .displayName("RAW Conditions")
                                    .description(
                                            "If the conditions should be set via the key-value pair in UI or as an raw expression).")
                                    .defaultValue(false),
                            STRING_PROPERTY(IfTaskConstants.PROPERTY_CONDITIONS)
                                    .displayName("Conditions")
                                    .description("The conditions expressed as an expression.")
                                    .displayOption(showWhen(IfTaskConstants.PROPERTY_RAW_CONDITIONS)
                                            .eq(true)),
                            ARRAY_PROPERTY(IfTaskConstants.PROPERTY_CONDITIONS)
                                    .displayName("Conditions")
                                    .placeholder("Add Condition")
                                    .description("The type of values to compare.")
                                    .displayOption(showWhen(IfTaskConstants.PROPERTY_RAW_CONDITIONS)
                                            .eq(false))
                                    .options(
                                            DSL.option("Boolean", IfTaskConstants.PROPERTY_BOOLEAN),
                                            DSL.option("Date & Time", IfTaskConstants.PROPERTY_DATE_TIME),
                                            DSL.option("Number", IfTaskConstants.PROPERTY_NUMBER),
                                            DSL.option("String", IfTaskConstants.PROPERTY_STRING))
                                    .items(
                                            OBJECT_PROPERTY(IfTaskConstants.PROPERTY_BOOLEAN)
                                                    .displayName("Boolean")
                                                    .properties(
                                                            BOOLEAN_PROPERTY(IfTaskConstants.PROPERTY_VALUE_1)
                                                                    .displayName("Value 1")
                                                                    .description(
                                                                            "The boolean value to compare with the second one.")
                                                                    .defaultValue(false),
                                                            STRING_PROPERTY(IfTaskConstants.PROPERTY_OPERATION)
                                                                    .displayName("Operation")
                                                                    .description(
                                                                            "Compare operation to decide where to map data.")
                                                                    .options(
                                                                            DSL.option(
                                                                                    "Equals",
                                                                                    IfTaskConstants.Operation.EQUALS
                                                                                            .name()),
                                                                            DSL.option(
                                                                                    "Not Equals",
                                                                                    IfTaskConstants.Operation.NOT_EQUALS
                                                                                            .name()))
                                                                    .defaultValue(
                                                                            IfTaskConstants.Operation.EQUALS.name()),
                                                            BOOLEAN_PROPERTY(IfTaskConstants.PROPERTY_VALUE_2)
                                                                    .displayName("Value 2")
                                                                    .description(
                                                                            "The boolean value to compare with the first one.")
                                                                    .defaultValue(false)),
                                            OBJECT_PROPERTY(IfTaskConstants.PROPERTY_DATE_TIME)
                                                    .displayName("Date & Time")
                                                    .properties(
                                                            DATE_TIME_PROPERTY(IfTaskConstants.PROPERTY_VALUE_1)
                                                                    .displayName("Value 1")
                                                                    .description(
                                                                            "The date & time value to compare with the second one.")
                                                                    .defaultValue(null),
                                                            STRING_PROPERTY(IfTaskConstants.PROPERTY_OPERATION)
                                                                    .displayName("Operation")
                                                                    .description(
                                                                            "Compare operation to decide where to map data.")
                                                                    .options(
                                                                            DSL.option(
                                                                                    "After",
                                                                                    IfTaskConstants.Operation.AFTER
                                                                                            .name()),
                                                                            DSL.option(
                                                                                    "Before",
                                                                                    IfTaskConstants.Operation.BEFORE
                                                                                            .name()))
                                                                    .defaultValue(
                                                                            IfTaskConstants.Operation.AFTER.name()),
                                                            DATE_TIME_PROPERTY(IfTaskConstants.PROPERTY_VALUE_2)
                                                                    .displayName("Value 2")
                                                                    .description(
                                                                            "The date & time value to compare with the first one.")
                                                                    .defaultValue(null)),
                                            OBJECT_PROPERTY(IfTaskConstants.PROPERTY_NUMBER)
                                                    .displayName("Number")
                                                    .properties(
                                                            NUMBER_PROPERTY(IfTaskConstants.PROPERTY_VALUE_1)
                                                                    .displayName("Value 1")
                                                                    .description(
                                                                            "The number value to compare with the second one.")
                                                                    .defaultValue(0),
                                                            STRING_PROPERTY(IfTaskConstants.PROPERTY_OPERATION)
                                                                    .displayName("Operation")
                                                                    .description(
                                                                            "Compare operation to decide where to map data.")
                                                                    .options(
                                                                            DSL.option(
                                                                                    "Less",
                                                                                    IfTaskConstants.Operation.LESS
                                                                                            .name()),
                                                                            DSL.option(
                                                                                    "Less or Equals",
                                                                                    IfTaskConstants.Operation
                                                                                            .LESS_EQUALS
                                                                                            .name()),
                                                                            DSL.option(
                                                                                    "Equals",
                                                                                    IfTaskConstants.Operation.EQUALS
                                                                                            .name()),
                                                                            DSL.option(
                                                                                    "Not Equals",
                                                                                    IfTaskConstants.Operation.NOT_EQUALS
                                                                                            .name()),
                                                                            DSL.option(
                                                                                    "Greater",
                                                                                    IfTaskConstants.Operation.GREATER
                                                                                            .name()),
                                                                            DSL.option(
                                                                                    "Greater or Equals",
                                                                                    IfTaskConstants.Operation
                                                                                            .GREATER_EQUALS
                                                                                            .name()),
                                                                            DSL.option(
                                                                                    "Empty",
                                                                                    IfTaskConstants.Operation.EMPTY
                                                                                            .name()))
                                                                    .defaultValue(
                                                                            IfTaskConstants.Operation.LESS.name()),
                                                            NUMBER_PROPERTY(IfTaskConstants.PROPERTY_VALUE_2)
                                                                    .displayName("Value 2")
                                                                    .description(
                                                                            "The number value to compare with the first one.")
                                                                    .defaultValue(0)
                                                                    .displayOption(hideWhen(
                                                                            IfTaskConstants.PROPERTY_OPERATION))),
                                            OBJECT_PROPERTY(IfTaskConstants.PROPERTY_STRING)
                                                    .displayName("String")
                                                    .properties(
                                                            STRING_PROPERTY(IfTaskConstants.PROPERTY_VALUE_1)
                                                                    .displayName("Value 1")
                                                                    .description(
                                                                            "The string value to compare with the second one.")
                                                                    .defaultValue(""),
                                                            STRING_PROPERTY(IfTaskConstants.PROPERTY_OPERATION)
                                                                    .displayName("Operation")
                                                                    .description(
                                                                            "Compare operation to decide where to map data.")
                                                                    .options(
                                                                            DSL.option(
                                                                                    "Equals",
                                                                                    IfTaskConstants.Operation.EQUALS
                                                                                            .name()),
                                                                            DSL.option(
                                                                                    "Not Equals",
                                                                                    IfTaskConstants.Operation.NOT_EQUALS
                                                                                            .name()),
                                                                            DSL.option(
                                                                                    "Contains",
                                                                                    IfTaskConstants.Operation.CONTAINS
                                                                                            .name()),
                                                                            DSL.option(
                                                                                    "Not Contains",
                                                                                    IfTaskConstants.Operation
                                                                                            .NOT_CONTAINS
                                                                                            .name()),
                                                                            DSL.option(
                                                                                    "Starts With",
                                                                                    IfTaskConstants.Operation
                                                                                            .STARTS_WITH
                                                                                            .name()),
                                                                            DSL.option(
                                                                                    "Ends With",
                                                                                    IfTaskConstants.Operation.ENDS_WITH
                                                                                            .name()),
                                                                            DSL.option(
                                                                                    "Regex",
                                                                                    IfTaskConstants.Operation.REGEX
                                                                                            .name()),
                                                                            DSL.option(
                                                                                    "Empty",
                                                                                    IfTaskConstants.Operation.EMPTY
                                                                                            .name()))
                                                                    .defaultValue(
                                                                            IfTaskConstants.Operation.EQUALS.name()),
                                                            STRING_PROPERTY(IfTaskConstants.PROPERTY_VALUE_2)
                                                                    .displayName("Value 2")
                                                                    .description(
                                                                            "The string value to compare with the first one.")
                                                                    .defaultValue("")
                                                                    .displayOption(hideWhen(
                                                                                    IfTaskConstants.PROPERTY_OPERATION)
                                                                            .in(
                                                                                    IfTaskConstants.Operation.EMPTY
                                                                                            .name(),
                                                                                    IfTaskConstants.Operation.REGEX
                                                                                            .name())),
                                                            STRING_PROPERTY(IfTaskConstants.PROPERTY_VALUE_2)
                                                                    .displayName("Regex")
                                                                    .description(
                                                                            "The regex value to compare with the first one.")
                                                                    .placeholder("/text/i")
                                                                    .defaultValue("")
                                                                    .displayOption(showWhen(
                                                                                    IfTaskConstants.PROPERTY_OPERATION)
                                                                            .eq(
                                                                                    IfTaskConstants.Operation.REGEX
                                                                                            .name())))),
                            STRING_PROPERTY(IfTaskConstants.PROPERTY_COMBINE_OPERATION)
                                    .displayName("Combine")
                                    .description(
                                            """
                            If multiple conditions are set, this setting decides if it is true as soon as ANY condition
                             matches or only if ALL are met.
                            """)
                                    .displayOption(showWhen(IfTaskConstants.PROPERTY_RAW_CONDITIONS)
                                            .eq(false))
                                    .options(
                                            DSL.option(
                                                    "All",
                                                    IfTaskConstants.CombineOperation.ALL.name(),
                                                    "Only if all conditions are met, the workflow goes into \"true\" branch."),
                                            DSL.option(
                                                    "Any",
                                                    IfTaskConstants.CombineOperation.ANY.name(),
                                                    "If any condition is met, the workflow goes into \"true\" branch."))
                                    .defaultValue(IfTaskConstants.CombineOperation.ALL.name())));

    @Override
    public TaskDescriptor getTaskDescriptor() {
        return TASK_DESCRIPTOR;
    }
}
