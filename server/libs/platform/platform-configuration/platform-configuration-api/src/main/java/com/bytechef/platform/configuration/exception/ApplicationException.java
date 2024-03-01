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

package com.bytechef.platform.configuration.exception;

import com.bytechef.platform.exception.AbstractException;
import java.util.List;

/**
 * @author Igor Beslic
 */
public class ApplicationException extends AbstractException {

    public ApplicationException(String message, Class<?> entityClass, int errorKey) {
        super(message, entityClass, errorKey);
    }

    public ApplicationException(String message, Class<?> entityClass, int errorKey, List<?> errorMessageArguments) {
        super(message, entityClass, errorKey, errorMessageArguments);
    }

    public ApplicationException(Throwable cause, Class<?> entityClass, int errorKey) {
        super(cause, entityClass, errorKey);
    }

    public ApplicationException(Throwable cause, Class<?> entityClass, int errorKey, List<?> errorMessageArguments) {
        super(cause, entityClass, errorKey, errorMessageArguments);
    }

    public ApplicationException(String message, Throwable cause, Class<?> entityClass, int errorKey) {
        super(message, cause, entityClass, errorKey);
    }

    public ApplicationException(
        String message, Throwable cause, Class<?> entityClass, int errorKey, List<?> errorMessageArguments) {

        super(message, cause, entityClass, errorKey, errorMessageArguments);
    }
}
