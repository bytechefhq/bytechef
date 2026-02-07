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

package com.bytechef.platform.component.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should automatically handle token refresh on credential errors. The aspect will
 * intercept these methods and on specific exceptions (matching refreshOn patterns), will refresh the OAuth2 credentials
 * and retry the method.
 *
 * <p>
 * Use {@link ComponentNameParam} and {@link ConnectionParam} annotations on method parameters to identify the component
 * name and connection parameters for the token refresh operation.
 * </p>
 *
 * @author Ivica Cardic
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface WithTokenRefresh {

    /**
     * The error type class to use when wrapping exceptions (e.g., ActionDefinitionErrorType.class). Use Void.class to
     * indicate no error type wrapping should be performed.
     *
     * @return the class containing the error type constants, or Void.class for no wrapping
     */
    Class<?> errorTypeClass() default Void.class;

    /**
     * The name of the static field in errorTypeClass to use for the error type. Leave empty to indicate no error type
     * wrapping should be performed.
     *
     * @return the name of the static field containing the error type, or empty string for no wrapping
     */
    String errorTypeField() default "";

    /**
     * Marks a method parameter as the component name for token refresh operations. Used in conjunction with
     * {@link WithTokenRefresh} to identify which parameter contains the component name without relying on parameter
     * name reflection.
     *
     * @author Ivica Cardic
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface ComponentNameParam {
    }

    /**
     * Marks a method parameter as the connection for token refresh operations. Used in conjunction with
     * {@link WithTokenRefresh} to identify which parameter contains the ComponentConnection (or Map of
     * ComponentConnections) without relying on parameter name reflection.
     *
     * @author Ivica Cardic
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface ConnectionParam {
    }
}
