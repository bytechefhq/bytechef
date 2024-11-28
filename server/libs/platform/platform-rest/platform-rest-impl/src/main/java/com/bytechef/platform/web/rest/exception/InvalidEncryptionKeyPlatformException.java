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

package com.bytechef.platform.web.rest.exception;

import com.bytechef.encryption.Encryption;
import com.bytechef.encryption.exception.InvalidEncryptionKeyException;
import com.bytechef.platform.exception.ErrorType;
import com.bytechef.platform.exception.PlatformException;

public class InvalidEncryptionKeyPlatformException extends PlatformException {

    private InvalidEncryptionKeyPlatformException(InvalidEncryptionKeyException exception) {
        super(exception, InvalidEncryptionKeyErrorType.INVALID_ENCRYPTION_KEY);
    }

    public static InvalidEncryptionKeyPlatformException toInvalidEncryptionKeyPlatformException(
        InvalidEncryptionKeyException exception) {

        return new InvalidEncryptionKeyPlatformException(exception);
    }

    private enum InvalidEncryptionKeyErrorType implements ErrorType {

        INVALID_ENCRYPTION_KEY(100);

        private final int errorKey;

        InvalidEncryptionKeyErrorType(int errorKey) {
            this.errorKey = errorKey;
        }

        @Override
        public Class<?> getErrorClass() {
            return Encryption.class;
        }

        @Override
        public int getErrorKey() {
            return errorKey;
        }
    }
}
