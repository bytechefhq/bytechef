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

import com.bytechef.component.definition.ConnectionDefinition;
import com.bytechef.platform.exception.ErrorType;

/**
 * @author Ivica Cardic
 */
public enum ConnectionDefinitionErrorType implements ErrorType {

    GET_DEFAULT_AUTHORIZATION_CALLBACK_FUNCTION(100), GET_DEFAULT_REFRESH_URL(101), EXECUTE_AUTHORIZATION_APPLY(102),
    EXECUTE_AUTHORIZATION_CALLBACK(103), GET_OAUTH2_AUTHORIZATION_PARAMETERS(104);

    private final int errorKey;

    ConnectionDefinitionErrorType(int errorKey) {
        this.errorKey = errorKey;
    }

    @Override
    public Class<?> getErrorClass() {
        return ConnectionDefinition.class;
    }

    @Override
    public int getErrorKey() {
        return errorKey;
    }
}
