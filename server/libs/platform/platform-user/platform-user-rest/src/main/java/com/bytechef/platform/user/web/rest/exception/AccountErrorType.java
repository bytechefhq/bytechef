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

package com.bytechef.platform.user.web.rest.exception;

import com.bytechef.exception.AbstractErrorType;
import com.bytechef.platform.user.dto.AdminUserDTO;
import org.springframework.http.HttpStatus;

/**
 * @author Ivica Cardic
 */
public class AccountErrorType extends AbstractErrorType {

    public static final AccountErrorType USER_NOT_FOUND = new AccountErrorType(100, HttpStatus.INTERNAL_SERVER_ERROR);
    public static final AccountErrorType ORGANIZATION_ALREADY_EXISTS = new AccountErrorType(
        101, HttpStatus.BAD_REQUEST);
    public static final AccountErrorType SIGN_UP_DISABLED = new AccountErrorType(102, HttpStatus.BAD_REQUEST);
    public static final AccountErrorType PROVIDER_UNLINK_NOT_ALLOWED = new AccountErrorType(
        103, HttpStatus.BAD_REQUEST);
    public static final AccountErrorType INVALID_TOTP_CODE = new AccountErrorType(104, HttpStatus.BAD_REQUEST);
    public static final AccountErrorType INVALID_PASSWORD = new AccountErrorType(105, HttpStatus.BAD_REQUEST);
    public static final AccountErrorType QR_CODE_GENERATION_FAILED = new AccountErrorType(
        106, HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    private AccountErrorType(int errorKey, HttpStatus status) {
        super(AdminUserDTO.class, errorKey);

        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
