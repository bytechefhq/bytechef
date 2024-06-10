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

package com.bytechef.platform.user.exception;

import com.bytechef.platform.exception.ErrorType;
import com.bytechef.platform.user.domain.User;

/**
 * @author Ivica Cardic
 */
public enum UserErrorType implements ErrorType {

    USER_NOT_FOUND(100), EMAIL_ALREADY_USED(101), LOGIN_ALREADY_USED(102), AUTHORITY_ALREADY_USED(103),
    INVALID_PASSWORD(104), USER_ALREADY_EXISTS(105), INVALID_EMAIL(106);

    private final int errorKey;

    UserErrorType(int errorKey) {
        this.errorKey = errorKey;
    }

    @Override
    public Class<?> getErrorClass() {
        return User.class;
    }

    @Override
    public int getErrorKey() {
        return errorKey;
    }
}
