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

import java.util.List;

/**
 * @author Igor Beslic
 */
public class PlatformException extends AbstractException {

    public PlatformException(String message, ErrorType errorType) {
        super(message, errorType);
    }

    public PlatformException(String message, ErrorType errorType, List<?> errorMessageArguments) {
        super(message, errorType, errorMessageArguments);
    }

    public PlatformException(Throwable cause, ErrorType errorType) {
        super(cause, errorType);
    }

    public PlatformException(Throwable cause, ErrorType errorType, List<?> errorMessageArguments) {
        super(cause, errorType, errorMessageArguments);
    }

    public PlatformException(String message, Throwable cause, ErrorType errorType) {
        super(message, cause, errorType);
    }

    public PlatformException(
        String message, Throwable cause, ErrorType errorType, List<?> errorMessageArguments) {

        super(message, cause, errorType, errorMessageArguments);
    }
}
