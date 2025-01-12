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

import com.bytechef.exception.AbstractException;
import com.bytechef.exception.ErrorType;
import java.util.Map;

/**
 * @author Igor Beslic
 */
public class ExecutionException extends AbstractException {

    public ExecutionException(String message, ErrorType errorType) {
        super(message, errorType);
    }

    public ExecutionException(
        String message, Map<String, ?> inputParameters, ErrorType errorType) {

        super(message, inputParameters, errorType);
    }

    public ExecutionException(Throwable cause, ErrorType errorType) {
        super(cause, errorType);
    }

    public ExecutionException(
        Throwable cause, Map<String, ?> inputParameters, ErrorType errorType) {

        super(cause, inputParameters, errorType);
    }

    public ExecutionException(String message, Throwable cause, ErrorType errorType) {
        super(message, cause, errorType);
    }

    public ExecutionException(
        String message, Throwable cause, Map<String, ?> inputParameters, ErrorType errorType) {

        super(message, cause, inputParameters, errorType);
    }

    @Override
    public String toString() {
        return "ExecutionException{" +
            "entityClass=" + entityClass +
            ", errorKey=" + errorKey +
            ", errorMessageArguments=" + errorMessageArguments +
            ", inputParameters=" + inputParameters +
            "} " + super.toString();
    }
}
