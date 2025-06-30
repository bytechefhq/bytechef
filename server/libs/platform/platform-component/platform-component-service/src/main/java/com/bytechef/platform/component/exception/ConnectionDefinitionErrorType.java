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

package com.bytechef.platform.component.exception;

import com.bytechef.component.definition.ConnectionDefinition;
import com.bytechef.exception.AbstractErrorType;

/**
 * @author Ivica Cardic
 */
public class ConnectionDefinitionErrorType extends AbstractErrorType {

    public static final ConnectionDefinitionErrorType INVALID_CLAIM =
        new ConnectionDefinitionErrorType(100);
    public static final ConnectionDefinitionErrorType TOKEN_REFRESH_FAILED = new ConnectionDefinitionErrorType(101);
    public static final ConnectionDefinitionErrorType AUTHORIZATION_APPLY_FAILED =
        new ConnectionDefinitionErrorType(102);
    public static final ConnectionDefinitionErrorType AUTHORIZATION_CALLBACK_FAILED =
        new ConnectionDefinitionErrorType(103);
    public static final ConnectionDefinitionErrorType INVALID_OAUTH2_AUTHORIZATION_PARAMETERS =
        new ConnectionDefinitionErrorType(104);
    public static final ConnectionDefinitionErrorType OAUTH_TOKEN_REFRESH_FAILED = new ConnectionDefinitionErrorType(
        105);
    public static final ConnectionDefinitionErrorType ACQUIRE_FAILED = new ConnectionDefinitionErrorType(106);

    private ConnectionDefinitionErrorType(int errorKey) {
        super(ConnectionDefinition.class, errorKey);
    }
}
