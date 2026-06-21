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

package com.bytechef.commons.util;

/**
 * Thrown when a URL fails outbound-request (SSRF) validation.
 *
 * @author Ivica Cardic
 */
public class UrlValidationException extends RuntimeException {

    public UrlValidationException(String message) {
        super(message);
    }

    public UrlValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
