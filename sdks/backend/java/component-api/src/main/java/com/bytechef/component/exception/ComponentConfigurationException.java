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

import java.util.Collections;
import java.util.Map;

/**
 * @author Igor Beslic
 */
public class ComponentConfigurationException extends RuntimeException {

    private final Map<String, ?> inputParameters;

    public ComponentConfigurationException(String message) {
        this(message, Map.of());
    }

    public ComponentConfigurationException(Throwable cause) {
        this(cause, Map.of());
    }

    public ComponentConfigurationException(
        String message, Map<String, ?> inputParameters) {

        this(message, null, inputParameters);
    }

    public ComponentConfigurationException(
        Throwable cause, Map<String, ?> inputParameters) {

        this(null, cause, inputParameters);
    }

    public ComponentConfigurationException(String message, Throwable cause, Map<String, ?> inputParameters) {
        super(message, cause);

        this.inputParameters = Collections.unmodifiableMap(inputParameters);
    }

    public Map<String, ?> getInputParameters() {
        return inputParameters;
    }

    @Override
    public String toString() {
        return "ComponentConfigurationException{" +
            "inputParameters=" + inputParameters +
            "} " + super.toString();
    }
}
