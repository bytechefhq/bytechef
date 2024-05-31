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

package com.bytechef.platform.exception;

/**
 * @author Igor Beslic
 */
public abstract class ProviderException extends AbstractException {

    public static ProviderException getByHttpResponseCode(int code, String message) {
        return switch (code) {
            case 400 -> new BadRequestException(message, new ProviderErrorType());
            case 401 -> new AuthorizationFailedException(message, new ProviderErrorType());
            case 403 -> new UnauthorizedRequestException(message, new ProviderErrorType());
            case 404 -> new NotFoundException(message, new ProviderErrorType());
            default -> new GeneralException(message, new ProviderErrorType());
        };
    }

    public ProviderException(String message, ErrorType errorType) {
        super(message, errorType);
    }

    public static class UnauthorizedRequestException extends ProviderException {
        public UnauthorizedRequestException(String message, ErrorType errorType) {
            super(message, errorType);
        }
    }

    public static class AuthorizationFailedException extends ProviderException {
        public AuthorizationFailedException(String message, ErrorType errorType) {
            super(message, errorType);
        }
    }

    public static class AuthenticationFailedException extends ProviderException {
        public AuthenticationFailedException(String message, ErrorType errorType) {
            super(message, errorType);
        }
    }

    public static class BadRequestException extends ProviderException {
        public BadRequestException(String message, ErrorType errorType) {
            super(message, errorType);
        }
    }

    public static class NotFoundException extends ProviderException {
        public NotFoundException(String message, ErrorType errorType) {
            super(message, errorType);
        }
    }

    public static class GeneralException extends ProviderException {
        public GeneralException(String message, ErrorType errorType) {
            super(message, errorType);
        }
    }

    public static class ProviderErrorType implements ErrorType {
        @Override
        public Class<?> getErrorClass() {
            return getClass();
        }

        @Override
        public int getErrorKey() {
            return 10000;
        }
    }

}
