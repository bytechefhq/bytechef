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

package com.bytechef.component.exception;

/**
 * @author Igor Beslic
 */
public abstract class ProviderException extends RuntimeException {

    public static ProviderException getByHttpResponseCode(int code, String message) {
        return switch (code) {
            case 400 -> new BadRequestException(message);
            case 401 -> new AuthorizationFailedException(message);
            case 403 -> new UnauthorizedRequestException(message);
            case 404 -> new NotFoundException(message);
            case 429 -> new LimitExceedException(message);
            case 500 -> new InternalErrorException(message);
            default -> throw new IllegalArgumentException("Response code %s is not supported.".formatted(code));
        };
    }

    public ProviderException(String message) {
        super(message);
    }

    public static class UnauthorizedRequestException extends ProviderException {
        public UnauthorizedRequestException(String message) {
            super(message);
        }
    }

    public static class AuthorizationFailedException extends ProviderException {
        public AuthorizationFailedException(String message) {
            super(message);
        }
    }

    public static class BadRequestException extends ProviderException {
        public BadRequestException(String message) {
            super(message);
        }
    }

    public static class InternalErrorException extends ProviderException {
        public InternalErrorException(String message) {
            super(message);
        }
    }

    public static class LimitExceedException extends ProviderException {
        public LimitExceedException(String message) {
            super(message);
        }
    }

    public static class NotFoundException extends ProviderException {
        public NotFoundException(String message) {
            super(message);
        }
    }
}
