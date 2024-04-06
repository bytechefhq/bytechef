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

package com.bytechef.platform.component.registry.constant;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.platform.exception.ErrorType;

/**
 * @author Ivica Cardic
 */
public enum TriggerDefinitionErrorType implements ErrorType {

    EXECUTE_DYNAMIC_PROPERTIES(100), EXECUTE_WORKFLOW_NODE_DESCRIPTION(101), EXECUTE_OPTIONS(102),
    EXECUTE_LISTENER_DISABLE(103), EXECUTE_LISTENER_ENABLE(104), EXECUTE_DYNAMIC_WEBHOOK_TRIGGER(105),
    EXECUTE_POLLING_TRIGGER(106), EXECUTE_STATIC_WEBHOOK_ERROR_TYPE(107), EXECUTE_DYNAMIC_WEBHOOK_DISABLE(108),
    EXECUTE_DYNAMIC_WEBHOOK_ENABLE(109);

    private final int errorKey;

    TriggerDefinitionErrorType(int errorKey) {
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
