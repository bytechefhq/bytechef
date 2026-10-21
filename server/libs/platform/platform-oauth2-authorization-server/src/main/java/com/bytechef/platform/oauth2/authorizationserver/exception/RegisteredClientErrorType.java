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

package com.bytechef.platform.oauth2.authorizationserver.exception;

import com.bytechef.exception.AbstractErrorType;

/**
 * @author Ivica Cardic
 */
public class RegisteredClientErrorType extends AbstractErrorType {

    public static final RegisteredClientErrorType DELETE_SHARED_REGISTERED_CLIENT = new RegisteredClientErrorType(100);
    public static final RegisteredClientErrorType GRANT_NOT_FOUND = new RegisteredClientErrorType(101);
    public static final RegisteredClientErrorType CLIENT_ID_ALREADY_REGISTERED = new RegisteredClientErrorType(102);
    public static final RegisteredClientErrorType INVALID_REDIRECT_URI = new RegisteredClientErrorType(103);
    public static final RegisteredClientErrorType INVALID_SCOPE = new RegisteredClientErrorType(104);
    public static final RegisteredClientErrorType REGISTERED_CLIENT_NOT_FOUND = new RegisteredClientErrorType(105);
    public static final RegisteredClientErrorType REGISTERED_CLIENT_NOT_UPDATABLE = new RegisteredClientErrorType(106);
    public static final RegisteredClientErrorType SCOPES_NOT_UPDATABLE = new RegisteredClientErrorType(107);
    public static final RegisteredClientErrorType REGISTERED_CLIENT_NOT_DELETABLE = new RegisteredClientErrorType(108);
    public static final RegisteredClientErrorType REGISTERED_CLIENT_MANAGED_BY_SERVER =
        new RegisteredClientErrorType(109);

    private RegisteredClientErrorType(int errorKey) {
        super(RegisteredClientErrorType.class, errorKey);
    }
}
