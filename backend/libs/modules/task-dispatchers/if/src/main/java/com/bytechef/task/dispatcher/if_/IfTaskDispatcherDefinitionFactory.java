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

import static com.bytechef.hermes.task.dispatcher.TaskDispatcherDSL.array;
import static com.bytechef.hermes.task.dispatcher.TaskDispatcherDSL.bool;
import static com.bytechef.hermes.task.dispatcher.TaskDispatcherDSL.create;
import static com.bytechef.hermes.task.dispatcher.TaskDispatcherDSL.dateTime;
import static com.bytechef.hermes.task.dispatcher.TaskDispatcherDSL.display;
import static com.bytechef.hermes.task.dispatcher.TaskDispatcherDSL.hideWhen;
import static com.bytechef.hermes.task.dispatcher.TaskDispatcherDSL.number;
import static com.bytechef.hermes.task.dispatcher.TaskDispatcherDSL.object;
import static com.bytechef.hermes.task.dispatcher.TaskDispatcherDSL.option;
import static com.bytechef.hermes.task.dispatcher.TaskDispatcherDSL.showWhen;
import static com.bytechef.hermes.task.dispatcher.TaskDispatcherDSL.string;
import static com.bytechef.task.dispatcher.if_.constants.IfTaskDispatcherConstants.BOOLEAN;
import static com.bytechef.task.dispatcher.if_.constants.IfTaskDispatcherConstants.COMBINE_OPERATION;
import static com.bytechef.task.dispatcher.if_.constants.IfTaskDispatcherConstants.CONDITIONS;
import static com.bytechef.task.dispatcher.if_.constants.IfTaskDispatcherConstants.DATE_TIME;
import static com.bytechef.task.dispatcher.if_.constants.IfTaskDispatcherConstants.IF;
import static com.bytechef.task.dispatcher.if_.constants.IfTaskDispatcherConstants.NUMBER;
import static com.bytechef.task.dispatcher.if_.constants.IfTaskDispatcherConstants.OPERATION;
import static com.bytechef.task.dispatcher.if_.constants.IfTaskDispatcherConstants.RAW_CONDITIONS;
import static com.bytechef.task.dispatcher.if_.constants.IfTaskDispatcherConstants.STRING;
import static com.bytechef.task.dispatcher.if_.constants.IfTaskDispatcherConstants.VALUE_1;
import static com.bytechef.task.dispatcher.if_.constants.IfTaskDispatcherConstants.VALUE_2;

import com.bytechef.hermes.task.dispatcher.TaskDispatcherDefinitionFactory;
import com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDefinition;
import com.bytechef.task.dispatcher.if_.constants.IfTaskDispatcherConstants;
import com.bytechef.task.dispatcher.if_.constants.IfTaskDispatcherConstants.Operation;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class IfTaskDispatcherDefinitionFactory implements TaskDispatcherDefinitionFactory {

    private static final TaskDispatcherDefinition TASK_DISPATCHER_DEFINITION = create(IF)
            .display(display("If").description("Directs a stream based on true/false results of comparisons."))
            .display(display("Boolean Condition"))
            .inputs(
                    array(CONDITIONS)
                            .label("Conditions")
                            .placeholder("Add Condition")
                            .description("The type of values to compare.")
                            .items(
                                    object(BOOLEAN)
                                            .label("Boolean")
                                            .properties(
                                                    bool(VALUE_1)
                                                            .label("Value 1")
                                                            .description(
                                                                    "The boolean value to compare with the second one.")
                                                            .defaultValue(false),
                                                    string(OPERATION)
                                                            .label("Operation")
                                                            .description(
                                                                    "Compare operation to decide where to map data.")
                                                            .options(
                                                                    option("Equals", Operation.EQUALS.name()),
                                                                    option("Not Equals", Operation.NOT_EQUALS.name()))
                                                            .defaultValue(Operation.EQUALS.name()),
                                                    bool(VALUE_2)
                                                            .label("Value 2")
                                                            .description(
                                                                    "The boolean value to compare with the first one.")
                                                            .defaultValue(false)),
                                    object(DATE_TIME)
                                            .label("Date & Time")
                                            .properties(
                                                    dateTime(VALUE_1)
                                                            .label("Value 1")
                                                            .description(
                                                                    "The date & time value to compare with the second one.")
                                                            .defaultValue(null),
                                                    string(OPERATION)
                                                            .label("Operation")
                                                            .description(
                                                                    "Compare operation to decide where to map data.")
                                                            .options(
                                                                    option("After", Operation.AFTER.name()),
                                                                    option("Before", Operation.BEFORE.name()))
                                                            .defaultValue(Operation.AFTER.name()),
                                                    dateTime(VALUE_2)
                                                            .label("Value 2")
                                                            .description(
                                                                    "The date & time value to compare with the first one.")
                                                            .defaultValue(null)),
                                    object(NUMBER)
                                            .label("Number")
                                            .properties(
                                                    number(VALUE_1)
                                                            .label("Value 1")
                                                            .description(
                                                                    "The number value to compare with the second one.")
                                                            .defaultValue(0),
                                                    string(OPERATION)
                                                            .label("Operation")
                                                            .description(
                                                                    "Compare operation to decide where to map data.")
                                                            .options(
                                                                    option("Less", Operation.LESS.name()),
                                                                    option(
                                                                            "Less or Equals",
                                                                            Operation.LESS_EQUALS.name()),
                                                                    option("Equals", Operation.EQUALS.name()),
                                                                    option("Not Equals", Operation.NOT_EQUALS.name()),
                                                                    option("Greater", Operation.GREATER.name()),
                                                                    option(
                                                                            "Greater or Equals",
                                                                            Operation.GREATER_EQUALS.name()),
                                                                    option("Empty", Operation.EMPTY.name()))
                                                            .defaultValue(Operation.LESS.name()),
                                                    number(VALUE_2)
                                                            .label("Value 2")
                                                            .description(
                                                                    "The number value to compare with the first one.")
                                                            .defaultValue(0)
                                                            .displayOption(hideWhen(OPERATION))),
                                    object(STRING)
                                            .label("String")
                                            .properties(
                                                    string(VALUE_1)
                                                            .label("Value 1")
                                                            .description(
                                                                    "The string value to compare with the second one.")
                                                            .defaultValue(""),
                                                    string(OPERATION)
                                                            .label("Operation")
                                                            .description(
                                                                    "Compare operation to decide where to map data.")
                                                            .options(
                                                                    option("Equals", Operation.EQUALS.name()),
                                                                    option("Not Equals", Operation.NOT_EQUALS.name()),
                                                                    option("Contains", Operation.CONTAINS.name()),
                                                                    option(
                                                                            "Not Contains",
                                                                            Operation.NOT_CONTAINS.name()),
                                                                    option("Starts With", Operation.STARTS_WITH.name()),
                                                                    option("Ends With", Operation.ENDS_WITH.name()),
                                                                    option("Regex", Operation.REGEX.name()),
                                                                    option("Empty", Operation.EMPTY.name()))
                                                            .defaultValue(Operation.EQUALS.name()),
                                                    string(VALUE_2)
                                                            .label("Value 2")
                                                            .description(
                                                                    "The string value to compare with the first one.")
                                                            .defaultValue("")
                                                            .displayOption(hideWhen(OPERATION)
                                                                    .in(
                                                                            Operation.EMPTY.name(),
                                                                            Operation.REGEX.name())),
                                                    string(VALUE_2)
                                                            .label("Regex")
                                                            .description(
                                                                    "The regex value to compare with the first one.")
                                                            .placeholder("/text/i")
                                                            .defaultValue("")
                                                            .displayOption(showWhen(OPERATION)
                                                                    .eq(Operation.REGEX.name())))),
                    string(COMBINE_OPERATION)
                            .label("Combine")
                            .description(
                                    """
                            If multiple conditions are set, this setting decides if it is true as soon as ANY condition
                             matches or only if ALL are met.
                            """)
                            .displayOption(showWhen(RAW_CONDITIONS).eq(false))
                            .options(
                                    option(
                                            "All",
                                            IfTaskDispatcherConstants.CombineOperation.ALL.name(),
                                            "Only if all conditions are met, the workflow goes into \"true\" branch."),
                                    option(
                                            "Any",
                                            IfTaskDispatcherConstants.CombineOperation.ANY.name(),
                                            "If any condition is met, the workflow goes into \"true\" branch."))
                            .defaultValue(IfTaskDispatcherConstants.CombineOperation.ALL.name()));

    @Override
    public TaskDispatcherDefinition getDefinition() {
        return TASK_DISPATCHER_DEFINITION;
    }
}
