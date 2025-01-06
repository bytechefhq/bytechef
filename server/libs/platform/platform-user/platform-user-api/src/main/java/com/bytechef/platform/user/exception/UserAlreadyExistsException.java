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

import com.bytechef.exception.AbstractException;

/**
 * @author Ivica Cardic
 */
public class UserAlreadyExistsException extends AbstractException {

    public UserAlreadyExistsException() {
        super("A new user cannot already have an ID", UserErrorType.USER_ALREADY_EXISTS);
    }
}
