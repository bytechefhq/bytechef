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

package com.bytechef.exception;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractException extends RuntimeException {

    protected final Class<?> entityClass;
    protected final int errorKey;

    @Nullable
    protected List<?> errorMessageArguments;

    @Nullable
    protected Map<String, ?> inputParameters;

    public AbstractException(String message, ErrorType errorType) {
        super(message);

        this.entityClass = errorType.getErrorClass();
        this.errorKey = errorType.getErrorKey();
    }

    public AbstractException(
        String message, @Nullable Map<String, ?> inputParameters, ErrorType errorType) {

        this(message, errorType);

        this.inputParameters =
            inputParameters == null ? Collections.emptyMap() : Collections.unmodifiableMap(inputParameters);
    }

    public AbstractException(String message, ErrorType errorType, List<?> errorMessageArguments) {
        super(message);

        this.entityClass = errorType.getErrorClass();
        this.errorKey = errorType.getErrorKey();
        this.errorMessageArguments = Collections.unmodifiableList(errorMessageArguments);
    }

    public AbstractException(Throwable cause, ErrorType errorType) {
        super(cause);

        this.entityClass = errorType.getErrorClass();
        this.errorKey = errorType.getErrorKey();
    }

    public AbstractException(
        Throwable cause, @Nullable Map<String, ?> inputParameters, ErrorType errorType) {

        this(cause, errorType);

        this.inputParameters =
            inputParameters == null ? Collections.emptyMap() : Collections.unmodifiableMap(inputParameters);
    }

    public AbstractException(String message, Throwable cause, ErrorType errorType) {
        super(message, cause);

        this.entityClass = errorType.getErrorClass();
        this.errorKey = errorType.getErrorKey();
    }

    public AbstractException(
        String message, Throwable cause, @Nullable Map<String, ?> inputParameters, ErrorType errorType) {

        this(message, cause, errorType);

        this.inputParameters =
            inputParameters == null ? Collections.emptyMap() : Collections.unmodifiableMap(inputParameters);
    }

    public AbstractException(
        Throwable cause, ErrorType errorType, List<?> errorMessageArguments) {

        super(cause);

        this.entityClass = errorType.getErrorClass();
        this.errorKey = errorType.getErrorKey();
        this.errorMessageArguments = Collections.unmodifiableList(errorMessageArguments);
    }

    public AbstractException(
        String message, Throwable cause, ErrorType errorType, List<?> errorMessageArguments) {

        super(message, cause);

        this.entityClass = errorType.getErrorClass();
        this.errorKey = errorType.getErrorKey();
        this.errorMessageArguments = Collections.unmodifiableList(errorMessageArguments);
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public int getErrorKey() {
        return errorKey;
    }

    public List<?> getErrorMessageArguments() {
        return errorMessageArguments == null ? List.of() : Collections.unmodifiableList(errorMessageArguments);
    }

    public String getErrorMessageCode() {
        return getErrorMessageCode(entityClass, errorKey);
    }

    private static String getErrorMessageCode(Class<?> entityClass, int errorKey) {
        return "error." + StringUtils.uncapitalize(entityClass.getSimpleName()) + "." + errorKey;
    }

    public Map<String, ?> getInputParameters() {
        return inputParameters == null ? Collections.emptyMap() : Collections.unmodifiableMap(inputParameters);
    }
}
