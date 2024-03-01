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

import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractException extends RuntimeException {

    private final Class<?> entityClass;
    private final int errorKey;
    private List<?> errorMessageArguments;

    public AbstractException(String message, Class<?> entityClass, int errorKey) {
        super(message);

        this.entityClass = entityClass;
        this.errorKey = errorKey;
    }

    public AbstractException(String message, Class<?> entityClass, int errorKey, List<?> errorMessageArguments) {
        super(message);

        this.entityClass = entityClass;
        this.errorKey = errorKey;
        this.errorMessageArguments = Collections.unmodifiableList(errorMessageArguments);
    }

    public AbstractException(Throwable cause, Class<?> entityClass, int errorKey) {
        super(cause);

        this.entityClass = entityClass;
        this.errorKey = errorKey;
    }

    public AbstractException(String message, Throwable cause, Class<?> entityClass, int errorKey) {
        super(message, cause);

        this.entityClass = entityClass;
        this.errorKey = errorKey;
    }

    public AbstractException(
        Throwable cause, Class<?> entityClass, int errorKey, List<?> errorMessageArguments) {

        super(cause);

        this.entityClass = entityClass;
        this.errorKey = errorKey;
        this.errorMessageArguments = Collections.unmodifiableList(errorMessageArguments);
    }

    public AbstractException(
        String message, Throwable cause, Class<?> entityClass, int errorKey, List<?> errorMessageArguments) {

        super(message, cause);

        this.entityClass = entityClass;
        this.errorKey = errorKey;
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

    @Override
    public String toString() {
        return "AbstractException{" +
            "entityClass=" + entityClass +
            ", errorKey=" + errorKey +
            ", errorMessageArguments=" + errorMessageArguments +
            "} " + super.toString();
    }
}
