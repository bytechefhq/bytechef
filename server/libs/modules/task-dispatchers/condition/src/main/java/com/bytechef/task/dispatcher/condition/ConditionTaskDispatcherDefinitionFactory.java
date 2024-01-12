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

package com.bytechef.task.dispatcher.condition;

import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.array;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.bool;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.dateTime;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.number;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.object;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.option;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.string;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.task;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.taskDispatcher;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.BOOLEAN;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.CASE_FALSE;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.CASE_TRUE;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.COMBINE_OPERATION;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.CONDITION;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.CONDITIONS;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.DATE_TIME;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.EXPRESSION;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.NUMBER;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.OPERATION;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.RAW_EXPRESSION;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.STRING;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.VALUE_1;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.VALUE_2;

import com.bytechef.platform.workflow.task.dispatcher.TaskDispatcherDefinitionFactory;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition;
import com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.CombineOperation;
import com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.Operation;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ConditionTaskDispatcherDefinitionFactory implements TaskDispatcherDefinitionFactory {

    private static final TaskDispatcherDefinition TASK_DISPATCHER_DEFINITION = taskDispatcher(CONDITION)
        .title("Condition")
        .description("Directs a stream based on true/false results of comparisons.")
        .icon("path:assets/condition.svg")
        .properties(
            bool(RAW_EXPRESSION)
                .label("Raw expression")
                .description("Set condition as raw expression or list of conditions.")
                .defaultValue(false),
            array(CONDITIONS)
                .label("Conditions")
                .placeholder("Add Condition")
                .description("The type of values to compare.")
                .displayCondition("%s === true".formatted(RAW_EXPRESSION))
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
                                .displayCondition("%s !== '%s'".formatted(OPERATION, Operation.EMPTY.name()))),
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
                                .displayCondition("!['%s','%s'].includes('%s')".formatted(
                                    Operation.EMPTY.name(),
                                    Operation.REGEX.name(),
                                    OPERATION)),
                            string(VALUE_2)
                                .label("Regex")
                                .description(
                                    "The regex value to compare with the first one.")
                                .placeholder("/text/i")
                                .defaultValue("")
                                .displayCondition("%s === '%s'".formatted(OPERATION, Operation.REGEX.name())))),
            string(COMBINE_OPERATION)
                .label("Combine")
                .description(
                    """
                        If multiple conditions are set, this setting decides if it is true as soon as ANY condition
                         matches or only if ALL are met.
                        """)
                .displayCondition("%s === false".formatted(RAW_EXPRESSION))
                .options(
                    option(
                        "All",
                        CombineOperation.ALL.name(),
                        "Only if all conditions are met, the workflow goes into \"true\" branch."),
                    option(
                        "Any",
                        CombineOperation.ANY.name(),
                        "If any condition is met, the workflow goes into \"true\" branch."))
                .defaultValue(CombineOperation.ALL.name()),
            string(EXPRESSION)
                .label("Expression")
                .description("The raw expression."))
        .taskProperties(
            array(CASE_TRUE)
                .description(
                    "The list of tasks to execute sequentially if the result of evaluating expression is true.")
                .items(task()),
            array(CASE_FALSE)
                .description(
                    "The list of tasks to execute sequentially if the result of evaluating expression is false.")
                .items(task()));

    @Override
    public TaskDispatcherDefinition getDefinition() {
        return TASK_DISPATCHER_DEFINITION;
    }
}
