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

import com.bytechef.exception.AbstractErrorType;
import com.bytechef.platform.user.domain.User;

/**
 * @author Ivica Cardic
 */
public class UserErrorType extends AbstractErrorType {

    public static final UserErrorType USER_NOT_FOUND = new UserErrorType(100);
    public static final UserErrorType EMAIL_ALREADY_USED = new UserErrorType(101);
    public static final UserErrorType LOGIN_ALREADY_USED = new UserErrorType(102);
    public static final UserErrorType AUTHORITY_ALREADY_USED = new UserErrorType(103);
    public static final UserErrorType INVALID_PASSWORD = new UserErrorType(104);
    public static final UserErrorType USER_ALREADY_EXISTS = new UserErrorType(105);
    public static final UserErrorType INVALID_EMAIL = new UserErrorType(106);

    private UserErrorType(int errorKey) {
        super(User.class, errorKey);
    }
}
