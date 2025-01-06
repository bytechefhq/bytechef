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

package com.bytechef.encryption.exception;

import com.bytechef.encryption.Encryption;
import com.bytechef.exception.AbstractErrorType;
import com.bytechef.exception.AbstractException;

/**
 * @author Ivica Cardic
 */
public class InvalidEncryptionKeyException extends AbstractException {

    public InvalidEncryptionKeyException(String message, Exception exception) {
        super(message, exception, InvalidEncryptionKeyErrorType.INVALID_ENCRYPTION_KEY);
    }

    private static class InvalidEncryptionKeyErrorType extends AbstractErrorType {

        private static final InvalidEncryptionKeyErrorType INVALID_ENCRYPTION_KEY =
            new InvalidEncryptionKeyErrorType(100);

        private InvalidEncryptionKeyErrorType(int errorKey) {
            super(Encryption.class, errorKey);
        }
    }
}
