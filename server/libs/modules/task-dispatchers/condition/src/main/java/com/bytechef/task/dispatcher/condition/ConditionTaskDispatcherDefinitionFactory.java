/*
 * Copyright 2025 ByteChef
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

import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.array;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.bool;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.dateTime;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.number;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.object;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.option;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.string;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.task;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.taskDispatcher;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.BOOLEAN;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.CASE_FALSE;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.CASE_TRUE;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.CONDITION;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.CONDITIONS;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.DATE_TIME;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.EXPRESSION;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.NUMBER;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.OPERATION;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.RAW_EXPRESSION;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.STRING;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.TYPE;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.VALUE_1;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.VALUE_2;

import com.bytechef.platform.workflow.task.dispatcher.TaskDispatcherDefinitionFactory;
import com.bytechef.platform.workflow.task.dispatcher.definition.Property;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition;
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
                .label("Raw Expression")
                .description("Set condition as raw expression or list of conditions.")
                .expressionEnabled(false)
                .required(true)
                .defaultValue(false),
            array(CONDITIONS)
                .label("OR Conditions")
                .placeholder("Add OR Condition")
                .description("OR Condition array that contains AND Condition arrays")
                .expressionEnabled(false)
                .displayCondition("%s == false".formatted(RAW_EXPRESSION))
                .items(
                    array()
                        .label("AND Condition")
                        .placeholder("Add AND Condition")
                        .expressionEnabled(false)
                        .description("AND Condition array that contains expression items")
                        .items(
                            object("boolean")
                                .label("Boolean Expression")
                                .expressionEnabled(false)
                                .properties(
                                    string(TYPE)
                                        .defaultValue(BOOLEAN)
                                        .hidden(true),
                                    bool(VALUE_1)
                                        .label("Value 1")
                                        .description("The boolean value to compare with the second one.")
                                        .required(true)
                                        .defaultValue(false),
                                    string(OPERATION)
                                        .label("Operation")
                                        .description("Compare operation to decide where to map data.")
                                        .options(
                                            option("Equals", Operation.EQUALS.name()),
                                            option("Not Equals", Operation.NOT_EQUALS.name()))
                                        .required(true),
                                    bool(VALUE_2)
                                        .label("Value 2")
                                        .description("The boolean value to compare with the first one.")
                                        .required(true)
                                        .defaultValue(false)),
                            object("dateTime")
                                .label("Date & Time Expression")
                                .expressionEnabled(false)
                                .properties(
                                    string(TYPE)
                                        .defaultValue(DATE_TIME)
                                        .hidden(true),
                                    dateTime(VALUE_1)
                                        .label("Value 1")
                                        .description("The date & time value to compare with the second one.")
                                        .required(true)
                                        .defaultValue(null),
                                    string(OPERATION)
                                        .label("Operation")
                                        .description("Compare operation to decide where to map data.")
                                        .options(
                                            option("After", Operation.AFTER.name()),
                                            option("Before", Operation.BEFORE.name()))
                                        .required(true),
                                    dateTime(VALUE_2)
                                        .label("Value 2")
                                        .description("The date & time value to compare with the first one.")
                                        .required(true)
                                        .defaultValue(null)),
                            object("number")
                                .label("Number Expression")
                                .expressionEnabled(false)
                                .properties(
                                    string(TYPE)
                                        .defaultValue(NUMBER)
                                        .hidden(true),
                                    number(VALUE_1)
                                        .label("Value 1")
                                        .description("The number value to compare with the second one.")
                                        .required(true)
                                        .defaultValue(0),
                                    string(OPERATION)
                                        .label("Operation")
                                        .description("Compare operation to decide where to map data.")
                                        .options(
                                            option("Less", Operation.LESS.name()),
                                            option("Less or Equals", Operation.LESS_EQUALS.name()),
                                            option("Equals", Operation.EQUALS.name()),
                                            option("Not Equals", Operation.NOT_EQUALS.name()),
                                            option("Greater", Operation.GREATER.name()),
                                            option("Greater or Equals", Operation.GREATER_EQUALS.name()),
                                            option("Empty", Operation.EMPTY.name()))
                                        .required(true),
                                    number(VALUE_2)
                                        .label("Value 2")
                                        .description("The number value to compare with the first one.")
                                        .required(true)
                                        .defaultValue(0)
                                        .displayCondition(
                                            "conditions[index][index].%s != '%s'".formatted(
                                                OPERATION, Operation.EMPTY.name()))),
                            object("string")
                                .label("String Expression")
                                .expressionEnabled(false)
                                .properties(
                                    string(TYPE)
                                        .defaultValue(STRING)
                                        .hidden(true),
                                    string(VALUE_1)
                                        .label("Value 1")
                                        .description("The string value to compare with the second one.")
                                        .required(true)
                                        .defaultValue(""),
                                    string(OPERATION)
                                        .label("Operation")
                                        .description("Compare operation to decide where to map data.")
                                        .options(
                                            option("Equals", Operation.EQUALS.name()),
                                            option("Not Equals", Operation.NOT_EQUALS.name()),
                                            option("Contains", Operation.CONTAINS.name()),
                                            option("Not Contains", Operation.NOT_CONTAINS.name()),
                                            option("Starts With", Operation.STARTS_WITH.name()),
                                            option("Ends With", Operation.ENDS_WITH.name()),
                                            option("Regex", Operation.REGEX.name()),
                                            option("Empty", Operation.EMPTY.name()))
                                        .required(true),
                                    string(VALUE_2)
                                        .label("Value 2")
                                        .description("The string value to compare with the first one.")
                                        .required(true)
                                        .defaultValue("")
                                        .displayCondition(
                                            "!contains({'%s','%s'}, conditions[index][index].%s)".formatted(
                                                Operation.EMPTY.name(), Operation.REGEX.name(), OPERATION)),
                                    string(VALUE_2)
                                        .label("Regex")
                                        .description("The regex value to compare with the first one.")
                                        .placeholder("/text/i")
                                        .required(true)
                                        .defaultValue("")
                                        .displayCondition(
                                            "conditions[index][index].%s == '%s'".formatted(
                                                OPERATION, Operation.REGEX.name()))))
                        .required(true)),
            string(EXPRESSION)
                .label("Expression")
                .description("The raw expression.")
                .controlType(Property.ControlType.TEXT_AREA)
                .placeholder("Write expression, e.g. =1 + 1 == 2")
                .displayCondition("%s == true".formatted(RAW_EXPRESSION))
                .required(true))
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
