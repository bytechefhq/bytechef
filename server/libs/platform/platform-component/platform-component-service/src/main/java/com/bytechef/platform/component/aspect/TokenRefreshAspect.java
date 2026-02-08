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

package com.bytechef.platform.component.aspect;

import com.bytechef.component.definition.Context;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.exception.ErrorType;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.annotation.WithTokenRefresh;
import com.bytechef.platform.component.context.ContextFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Aspect that intercepts methods annotated with {@link WithTokenRefresh} and automatically handles OAuth2 token refresh
 * on credential errors. When a method fails with an exception matching the refreshOn patterns, the aspect will refresh
 * the credentials and retry the method.
 *
 * @author Ivica Cardic
 */
@Aspect
@Component
public class TokenRefreshAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenRefreshAspect.class);

    private final ContextFactory contextFactory;
    private final TokenRefreshHandler tokenRefreshHandler;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public TokenRefreshAspect(ContextFactory contextFactory, TokenRefreshHandler tokenRefreshHandler) {
        this.contextFactory = contextFactory;
        this.tokenRefreshHandler = tokenRefreshHandler;
    }

    /**
     * Intercepts methods annotated with {@link WithTokenRefresh} and handles token refresh on credential errors.
     *
     * @param joinPoint        the join point
     * @param withTokenRefresh the annotation
     * @return the result of the method execution
     * @throws Throwable if the method execution fails
     */
    @Around("@annotation(withTokenRefresh)")
    public Object handleTokenRefresh(ProceedingJoinPoint joinPoint, WithTokenRefresh withTokenRefresh)
        throws Throwable {

        try {
            return joinPoint.proceed();
        } catch (Exception exception) {
            return handleException(joinPoint, withTokenRefresh, exception);
        }
    }

    private Object handleException(
        ProceedingJoinPoint joinPoint, WithTokenRefresh withTokenRefresh, Exception exception) throws Throwable {

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();
        Method method = methodSignature.getMethod();
        Parameter[] parameters = method.getParameters();

        String componentName =
            findAnnotatedArgument(parameters, args, WithTokenRefresh.ComponentNameParam.class, String.class);

        if (componentName == null) {
            throw new IllegalStateException(
                "Method annotated with @WithTokenRefresh must declare a parameter annotated with " +
                    "@WithTokenRefresh.ComponentNameParam of type String: " + method.toGenericString());
        }

        ComponentConnection componentConnection = findConnectionArgument(parameters, args);

        if (componentConnection == null || componentConnection.authorizationType() == null) {
            throw exception;
        }

        if (!tokenRefreshHandler.shouldRefresh(componentName, componentConnection, exception)) {
            throw wrapException(exception, withTokenRefresh);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                "Token refresh triggered for component {} due to exception: {}",
                componentName, exception.getMessage());
        }

        Context context = createContext(componentName, componentConnection);

        ComponentConnection refreshedConnection = tokenRefreshHandler.refreshCredentials(
            componentConnection, context);

        Object[] updatedArgs = updateArgsWithRefreshedConnection(args, parameters, refreshedConnection);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Retrying method {} with refreshed credentials", method.getName());
        }

        try {
            return joinPoint.proceed(updatedArgs);
        } catch (Exception retryException) {
            throw wrapException(retryException, withTokenRefresh);
        }
    }

    @SuppressWarnings("unchecked")
    private @Nullable ComponentConnection findConnectionArgument(Parameter[] parameters, Object[] args) {
        int index = findAnnotatedParameterIndex(parameters, WithTokenRefresh.ConnectionParam.class);

        if (index < 0) {
            return null;
        }

        Object value = args[index];

        if (value == null) {
            return null;
        }

        if (value instanceof ComponentConnection componentConnection) {
            return componentConnection;
        }

        if (value instanceof Map<?, ?> map) {
            Map<String, ComponentConnection> connectionMap = (Map<String, ComponentConnection>) map;

            if (connectionMap.isEmpty()) {
                return null;
            }

            return connectionMap.values()
                .iterator()
                .next();
        }

        return null;
    }

    private <T> @Nullable T findAnnotatedArgument(
        Parameter[] parameters, Object[] args, Class<?> annotationClass, Class<T> type) {

        int index = findAnnotatedParameterIndex(parameters, annotationClass);

        if (index < 0) {
            return null;
        }

        Object value = args[index];

        if (value == null) {
            return null;
        }

        if (type.isInstance(value)) {
            return type.cast(value);
        }

        if (type == Integer.class && value instanceof Number number) {
            return type.cast(number.intValue());
        }

        return null;
    }

    private int findAnnotatedParameterIndex(Parameter[] parameters, Class<?> annotationClass) {
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(annotationClass.asSubclass(java.lang.annotation.Annotation.class))) {
                return i;
            }
        }

        return -1;
    }

    @SuppressWarnings("unchecked")
    private Object[] updateArgsWithRefreshedConnection(
        Object[] args, Parameter[] parameters, ComponentConnection refreshedConnection) {

        Object[] updatedArgs = args.clone();
        int index = findAnnotatedParameterIndex(parameters, WithTokenRefresh.ConnectionParam.class);

        if (index < 0) {
            return updatedArgs;
        }

        Object currentValue = args[index];

        if (currentValue instanceof ComponentConnection) {
            updatedArgs[index] = refreshedConnection;
        } else if (currentValue instanceof Map<?, ?> map) {
            Map<String, ComponentConnection> connectionMap = (Map<String, ComponentConnection>) map;

            if (!connectionMap.isEmpty()) {
                String firstKey = connectionMap.keySet()
                    .iterator()
                    .next();

                Map<String, ComponentConnection> updatedMap = new HashMap<>(connectionMap);

                updatedMap.put(firstKey, refreshedConnection);

                updatedArgs[index] = updatedMap;
            }
        }

        return updatedArgs;
    }

    private Context createContext(String componentName, ComponentConnection componentConnection) {
        return contextFactory.createContext(componentName, componentConnection);
    }

    private Exception wrapException(Exception exception, WithTokenRefresh withTokenRefresh) {
        if (exception instanceof ProviderException) {
            ErrorType errorType = getErrorType(withTokenRefresh);

            if (errorType != null) {
                return new ConfigurationException(exception, errorType);
            }
        }

        if (exception instanceof ConfigurationException || exception instanceof ExecutionException) {
            return exception;
        }

        return exception;
    }

    private @Nullable ErrorType getErrorType(WithTokenRefresh withTokenRefresh) {
        Class<?> errorTypeClass = withTokenRefresh.errorTypeClass();
        String errorTypeField = withTokenRefresh.errorTypeField();

        if (errorTypeClass == Void.class || errorTypeField.isEmpty()) {
            return null;
        }

        try {
            Field field = errorTypeClass.getField(errorTypeField);

            return (ErrorType) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.warn("Could not get error type from {} field {}", errorTypeClass.getName(), errorTypeField, e);

            return null;
        }
    }
}
