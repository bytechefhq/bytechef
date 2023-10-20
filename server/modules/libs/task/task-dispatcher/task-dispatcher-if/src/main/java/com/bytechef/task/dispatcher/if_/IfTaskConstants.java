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

/**
 * @author Ivica Cardic
 */
public class IfTaskConstants {

    public static final String PROPERTY_RAW_CONDITIONS = "rawConditions";
    public static final String PROPERTY_CONDITIONS = "conditions";
    public static final String PROPERTY_BOOLEAN = "boolean";
    public static final String PROPERTY_VALUE_1 = "value1";
    public static final String PROPERTY_OPERATION = "operation";
    public static final String PROPERTY_VALUE_2 = "value2";
    public static final String PROPERTY_DATE_TIME = "dateTime";
    public static final String PROPERTY_STRING = "string";
    public static final String PROPERTY_COMBINE_OPERATION = "combineOperation";
    public static final String PROPERTY_NUMBER = "number";
    public static final String PROPERTY_CASE_TRUE = "caseTrue";
    public static final String PROPERTY_CASE_FALSE = "caseFalse";
    public static final String TASK_IF = "if";

    public enum Operation {
        EQUALS,
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

    public enum CombineOperation {
        ALL,
        ANY,
    }
}
