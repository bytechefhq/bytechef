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
public class ComponentToolErrorType extends AbstractErrorType {

    public static final ComponentToolErrorType GENERATE_ACTION_DEFINITION = new ComponentToolErrorType(100);
    public static final ComponentToolErrorType GENERATE_TRIGGER_DEFINITION = new ComponentToolErrorType(101);
    public static final ComponentToolErrorType GET_ACTION = new ComponentToolErrorType(102);
    public static final ComponentToolErrorType GET_COMPONENT = new ComponentToolErrorType(103);
    public static final ComponentToolErrorType GET_OUTPUT_PROPERTIES = new ComponentToolErrorType(104);
    public static final ComponentToolErrorType GET_PROPERTIES = new ComponentToolErrorType(105);
    public static final ComponentToolErrorType GET_TRIGGER = new ComponentToolErrorType(106);
    public static final ComponentToolErrorType LIST_ACTIONS = new ComponentToolErrorType(107);
    public static final ComponentToolErrorType LIST_COMPONENTS = new ComponentToolErrorType(108);
    public static final ComponentToolErrorType LIST_TRIGGERS = new ComponentToolErrorType(109);
    public static final ComponentToolErrorType OPERATION_NOT_FOUND = new ComponentToolErrorType(110);
    public static final ComponentToolErrorType SEARCH_ACTIONS = new ComponentToolErrorType(111);
    public static final ComponentToolErrorType SEARCH_COMPONENTS = new ComponentToolErrorType(112);
    public static final ComponentToolErrorType SEARCH_TRIGGERS = new ComponentToolErrorType(113);

    private ComponentToolErrorType(int errorKey) {
        super(ComponentToolErrorType.class, errorKey);
    }
}
