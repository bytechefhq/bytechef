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
 * @author Ivica Cardic
 */
public class ProviderException extends RuntimeException {

    private Integer statusCode;

    /**
     *
     * @param message
     */
    public ProviderException(String message) {
        super(message);
    }

    /**
     *
     * @param statusCode
     */
    public ProviderException(Integer statusCode) {
        this.statusCode = statusCode;
    }

    /**
     *
     * @param statusCode
     * @param message
     */
    public ProviderException(Integer statusCode, String message) {
        super(message);

        this.statusCode = statusCode;
    }

    /**
     *
     * @return
     */
    public Integer getStatusCode() {
        return statusCode;
    }

    /**
     *
     */
    public static class BadRequestException extends ProviderException {

        /**
         *
         * @param message
         */
        public BadRequestException(String message) {
            super(400, message);
        }
    }

    /**
     *
     */
    public static class ForbiddenException extends ProviderException {

        /**
         *
         * @param message
         */
        public ForbiddenException(String message) {
            super(403, message);
        }

    }

    /**
     *
     */
    public static class NotFoundException extends ProviderException {

        /**
         *
         * @param message
         */
        public NotFoundException(String message) {
            super(404, message);
        }
    }

    /**
     *
     */
    public static class UnauthorizedException extends ProviderException {

        /**
         *
         * @param message
         */
        public UnauthorizedException(String message) {
            super(401, message);
        }
    }
}
