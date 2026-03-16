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

package com.bytechef.ai.mcp.tool.platform.exception;

import com.bytechef.exception.AbstractErrorType;

/**
 * @author Ivica Cardic
 */
public class TaskDispatcherToolErrorType extends AbstractErrorType {

    public static final TaskDispatcherToolErrorType GET_TASK_DISPATCHER = new TaskDispatcherToolErrorType(100);
    public static final TaskDispatcherToolErrorType GET_TASK_DISPATCHER_DEFINITION =
        new TaskDispatcherToolErrorType(101);
    public static final TaskDispatcherToolErrorType GET_TASK_DISPATCHER_OUTPUT = new TaskDispatcherToolErrorType(102);
    public static final TaskDispatcherToolErrorType GET_TASK_DISPATCHER_PROPERTIES =
        new TaskDispatcherToolErrorType(103);
    public static final TaskDispatcherToolErrorType LIST_TASK_DISPATCHERS = new TaskDispatcherToolErrorType(104);
    public static final TaskDispatcherToolErrorType SEARCH_TASK_DISPATCHERS = new TaskDispatcherToolErrorType(105);

    private TaskDispatcherToolErrorType(int errorKey) {
        super(TaskDispatcherToolErrorType.class, errorKey);
    }
}
