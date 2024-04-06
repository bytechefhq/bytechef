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

package com.bytechef.platform.component.exception;

import com.bytechef.platform.exception.AbstractException;
import com.bytechef.platform.exception.ErrorType;
import java.util.Collections;
import java.util.Map;

/**
 * @author Igor Beslic
 */
public class ComponentExecutionException extends AbstractException {

    private Map<String, ?> inputParameters;

    public ComponentExecutionException(String message, ErrorType errorType) {
        super(message, errorType);
    }

    public ComponentExecutionException(
        String message, Map<String, ?> inputParameters, ErrorType errorType) {

        super(message, errorType);

        this.inputParameters = Collections.unmodifiableMap(inputParameters);
    }

    public ComponentExecutionException(Throwable cause, ErrorType errorType) {
        super(cause, errorType);
    }

    public ComponentExecutionException(
        Throwable cause, Map<String, ?> inputParameters, ErrorType errorType) {

        super(cause, errorType);

        this.inputParameters = Collections.unmodifiableMap(inputParameters);
    }

    public ComponentExecutionException(String message, Throwable cause, ErrorType errorType) {
        super(message, cause, errorType);
    }

    public ComponentExecutionException(
        String message, Throwable cause, Map<String, ?> inputParameters, ErrorType errorType) {

        super(message, cause, errorType);

        this.inputParameters = Collections.unmodifiableMap(inputParameters);
    }

    public Map<String, ?> getInputParameters() {
        return Collections.unmodifiableMap(inputParameters);
    }

    @Override
    public String toString() {
        return "ComponentExecutionException{" +
            "inputParameters=" + inputParameters +
            "} " + super.toString();
    }
}
