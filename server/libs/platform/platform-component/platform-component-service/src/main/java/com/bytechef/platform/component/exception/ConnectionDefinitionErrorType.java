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

package com.bytechef.platform.component.exception;

import com.bytechef.component.definition.ConnectionDefinition;
import com.bytechef.exception.AbstractErrorType;

/**
 * @author Ivica Cardic
 */
public class ConnectionDefinitionErrorType extends AbstractErrorType {

    public static final ConnectionDefinitionErrorType GET_DEFAULT_AUTHORIZATION_CALLBACK_FUNCTION =
        new ConnectionDefinitionErrorType(100);
    public static final ConnectionDefinitionErrorType GET_DEFAULT_REFRESH_URL = new ConnectionDefinitionErrorType(101);
    public static final ConnectionDefinitionErrorType EXECUTE_AUTHORIZATION_APPLY =
        new ConnectionDefinitionErrorType(102);
    public static final ConnectionDefinitionErrorType EXECUTE_AUTHORIZATION_CALLBACK =
        new ConnectionDefinitionErrorType(103);
    public static final ConnectionDefinitionErrorType GET_OAUTH2_AUTHORIZATION_PARAMETERS =
        new ConnectionDefinitionErrorType(104);
    public static final ConnectionDefinitionErrorType EXECUTE_AUTHORIZATION_REFRESH = new ConnectionDefinitionErrorType(
        105);
    public static final ConnectionDefinitionErrorType EXECUTE_ACQUIRE = new ConnectionDefinitionErrorType(106);

    private ConnectionDefinitionErrorType(int errorKey) {
        super(ConnectionDefinition.class, errorKey);
    }
}
