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

package com.bytechef.task.dispatcher.condition.constant;

/**
 * @author Ivica Cardic
 */
public class ConditionTaskDispatcherConstants {

    public static final String RAW_EXPRESSION = "rawExpression";
    public static final String EXPRESSION = "expression";
    public static final String CONDITION = "condition";
    public static final String CONDITIONS = "conditions";
    public static final String BOOLEAN = "boolean";
    public static final String VALUE_1 = "value1";
    public static final String OPERATION = "operation";
    public static final String VALUE_2 = "value2";
    public static final String DATE_TIME = "dateTime";
    public static final String STRING = "string";
    public static final String NUMBER = "number";
    public static final String CASE_TRUE = "caseTrue";
    public static final String CASE_FALSE = "caseFalse";
    public static final String TYPE = "type";

    public enum Operation {
        EQUALS,
        EQUALS_IGNORE_CASE,
        NOT_EQUALS,
        AFTER,
        BEFORE,
        LESS,
        LESS_EQUALS,
        GREATER,
        GREATER_EQUALS,
        CONTAINS,
        NOT_CONTAINS,
        STARTS_WITH,
        ENDS_WITH,
        REGEX,
        EMPTY,
    }
}
