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

package com.bytechef.component.data.storage.constant;

import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.definition.Option;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class DataStorageConstants {

    public static final String APPEND_LIST_AS_SINGLE_ITEM = "appendListAsSingleItem";
    public static final String DEFAULT_VALUE = "defaultValue";
    public static final String DEFAULT_VALUE_LABEL = "Default Value";
    public static final String INDEX = "index";
    public static final String KEY = "key";
    public static final String SCOPE = "scope";
    public static final String TIMEOUT = "timeout";
    public static final String TYPE = "type";
    public static final String VALUE = "value";
    public static final String VALUE_TO_ADD = "valueToAdd";

    public static final List<Option<String>> SCOPE_OPTIONS = List.of(
        option("Current Execution", "CURRENT_EXECUTION", "The value is accessible only within the current execution."),
        option("Workflow", "WORKFLOW", "The value is shared across all executions of the same workflow."),
        option("Deployment", "PRINCIPAL", "The value is shared across all workflows within the same deployment."),
        option("Account", "ACCOUNT",
            "The value is shared across all deployments and workflows under the same account."));

    public static final List<Option<String>> TYPE_OPTIONS = List.of(
        option("Array", ValueType.ARRAY.name()),
        option("Boolean", ValueType.BOOLEAN.name()),
        option("Date", ValueType.DATE.name()),
        option("Date Time", ValueType.DATE_TIME.name()),
        option("Integer", ValueType.INTEGER.name()),
        option("Nullable", ValueType.NULL.name()),
        option("Number", ValueType.NUMBER.name()),
        option("Object", ValueType.OBJECT.name()),
        option("String", ValueType.STRING.name()),
        option("Time", ValueType.TIME.name()));
}
