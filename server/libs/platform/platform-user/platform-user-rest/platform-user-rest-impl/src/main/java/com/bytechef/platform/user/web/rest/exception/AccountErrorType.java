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

package com.bytechef.platform.user.web.rest.exception;

import com.bytechef.platform.exception.ErrorType;
import com.bytechef.platform.user.dto.AdminUserDTO;
import org.springframework.http.HttpStatus;

public enum AccountErrorType implements ErrorType {

    USER_NOT_FOUND(100, HttpStatus.INTERNAL_SERVER_ERROR), ORGANIZATION_ALREADY_EXISTS(101, HttpStatus.BAD_REQUEST);

    private final int errorKey;
    private final HttpStatus status;

    AccountErrorType(int errorKey, HttpStatus status) {
        this.errorKey = errorKey;
        this.status = status;
    }

    @Override
    public Class<?> getErrorClass() {
        return AdminUserDTO.class;
    }

    @Override
    public int getErrorKey() {
        return errorKey;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
