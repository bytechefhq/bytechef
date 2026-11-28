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

package com.bytechef.platform.workflow.task.dispatcher.exception;

import com.bytechef.exception.AbstractErrorType;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition;

/**
 * @author Ivica Cardic
 */
public class TaskDispatcherDefinitionErrorType extends AbstractErrorType {

    public static final TaskDispatcherDefinitionErrorType EXECUTE_DYNAMIC_PROPERTIES =
        new TaskDispatcherDefinitionErrorType(100);
    public static final TaskDispatcherDefinitionErrorType EXECUTE_OPTIONS =
        new TaskDispatcherDefinitionErrorType(101);
    public static final TaskDispatcherDefinitionErrorType EXECUTE_OUTPUT =
        new TaskDispatcherDefinitionErrorType(102);
    public static final TaskDispatcherDefinitionErrorType EXECUTE_VARIABLE_PROPERTIES =
        new TaskDispatcherDefinitionErrorType(103);
    public static final TaskDispatcherDefinitionErrorType GET_TASK_DISPATCHER =
        new TaskDispatcherDefinitionErrorType(104);
    public static final TaskDispatcherDefinitionErrorType GET_TASK_DISPATCHER_DEFINITION =
        new TaskDispatcherDefinitionErrorType(105);
    public static final TaskDispatcherDefinitionErrorType GET_TASK_DISPATCHER_OUTPUT =
        new TaskDispatcherDefinitionErrorType(106);
    public static final TaskDispatcherDefinitionErrorType GET_TASK_DISPATCHER_PROPERTIES =
        new TaskDispatcherDefinitionErrorType(107);
    public static final TaskDispatcherDefinitionErrorType LIST_TASK_DISPATCHERS =
        new TaskDispatcherDefinitionErrorType(108);
    public static final TaskDispatcherDefinitionErrorType SEARCH_TASK_DISPATCHERS =
        new TaskDispatcherDefinitionErrorType(109);

    private TaskDispatcherDefinitionErrorType(int errorKey) {
        super(TaskDispatcherDefinition.class, errorKey);
    }
}
