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

package com.bytechef.platform.component.registry.exception;

import java.util.Collections;
import java.util.Map;

/**
 * @author Igor Beslic
 */
public class ComponentExecutionException extends RuntimeException {

    private final Class<?> entityClass;
    private final int errorKey;
    private Map<String, ?> inputParameters;

    public ComponentExecutionException(String message, Class<?> entityClass, int errorKey) {
        super(message);

        this.entityClass = entityClass;
        this.errorKey = errorKey;
    }

    public ComponentExecutionException(
        String message, Map<String, ?> inputParameters, Class<?> entityClass, int errorKey) {

        super(message);

        this.entityClass = entityClass;
        this.errorKey = errorKey;
        this.inputParameters = Collections.unmodifiableMap(inputParameters);
    }

    public ComponentExecutionException(Throwable cause, Class<?> entityClass, int errorKey) {
        super(cause);

        this.entityClass = entityClass;
        this.errorKey = errorKey;
    }

    public ComponentExecutionException(
        Throwable cause, Map<String, ?> inputParameters, Class<?> entityClass, int errorKey) {

        super(cause);

        this.entityClass = entityClass;
        this.errorKey = errorKey;
        this.inputParameters = Collections.unmodifiableMap(inputParameters);
    }

    public ComponentExecutionException(String message, Throwable cause, Class<?> entityClass, int errorKey) {
        super(message, cause);

        this.entityClass = entityClass;
        this.errorKey = errorKey;
    }

    public ComponentExecutionException(
        String message, Throwable cause, Map<String, ?> inputParameters, Class<?> entityClass, int errorKey) {

        super(message, cause);

        this.entityClass = entityClass;
        this.errorKey = errorKey;
        this.inputParameters = Collections.unmodifiableMap(inputParameters);
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public int getErrorKey() {
        return errorKey;
    }

    public Map<String, ?> getInputParameters() {
        return Collections.unmodifiableMap(inputParameters);
    }

    @Override
    public String toString() {
        return "ComponentExecutionException{" +
            "entityClass=" + entityClass +
            ", errorKey=" + errorKey +
            ", inputParameters=" + inputParameters +
            ", message=" + super.toString() +
            "} ";
    }
}
