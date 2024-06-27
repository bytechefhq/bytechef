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

package com.bytechef.platform.component.registry.exception;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.platform.exception.ErrorType;

/**
 * @author Ivica Cardic
 */
public enum ActionDefinitionErrorType implements ErrorType {

    EXECUTE_DYNAMIC_PROPERTIES(100), EXECUTE_WORKFLOW_NODE_DESCRIPTION(101), EXECUTE_OPTIONS(102), EXECUTE_OUTPUT(103),
    EXECUTE_PERFORM(104), EXECUTE_PROCESS_ERROR_RESPONSE(105);

    private final int errorKey;

    ActionDefinitionErrorType(int errorKey) {
        this.errorKey = errorKey;
    }

    @Override
    public Class<?> getErrorClass() {
        return ActionDefinition.class;
    }

    @Override
    public int getErrorKey() {
        return errorKey;
    }
}
