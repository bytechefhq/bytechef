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

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

/**
 * @author Igor Beslic
 */
public abstract class ProviderException extends RuntimeException {

    @Nonnull
    public static ProviderException fromHttpResponseCode(int code, String message) {
        return switch (code) {
            case 400 -> new BadRequestException(message);
            case 401 -> new AuthorizationFailedException(message);
            case 403 -> new UnauthorizedRequestException(message);
            case 404 -> new NotFoundException(message);
            default -> new GeneralException(message);
        };
    }

    /**
     * Returns specialized ProviderException based on content of exception message. Content is scanned for http response
     * code 4xx.
     *
     * @param message the content of exception message
     * @return specialized ProviderException if 4xx pattern is resolved in message, or null if http error code can't be
     *         resolved
     */
    public static ProviderException fromExceptionMessage(@Nonnull String message) {
        Matcher matcher = http4nnPattern.matcher(message);
        if (matcher.matches()) {
            if (matcher.group(1) != null) {
                return fromHttpResponseCode(Integer.parseInt(matcher.group(1)), message);
            }
        }

        return null;
    }

    public static ProviderException fromException(@Nonnull Exception exception) {
        String exceptionMessage = exception.getMessage();

        if (!exceptionMessage.startsWith("{") || !exceptionMessage.endsWith("}")) {
            throw new UnsupportedOperationException("Unable to convert " + exceptionMessage);
        }

        Map<String, ?> map = JsonUtils.readMap(exception.getMessage());

        AuthorizationFailedException exceptionMessage1 = new AuthorizationFailedException(
            MapUtils.getString(map,"exceptionMessage"), new ProviderErrorType());

        exceptionMessage1.withComponentName(MapUtils.getString(map, "componentName"));

        return exceptionMessage1;
    }

    private static Pattern http4nnPattern = Pattern.compile("^.*(4\\d\\d)(\\s(Unauthorized)?.*)?$", Pattern.DOTALL);

    public static boolean hasAuthorizationFailedExceptionContent(@Nonnull Exception exception) {
        ProviderException providerException = fromExceptionMessage(exception.getMessage());

        if (Objects.equals(AuthorizationFailedException.class, providerException.getClass())) {
            return true;
        }

        return false;
    }

    public static String getComponentName(@Nonnull Exception exception) {
        if (exception instanceof AuthorizationFailedException) {
            ProviderException providerException = (ProviderException) exception;

            return providerException.getComponentName();
        }

        if (!hasAuthorizationFailedExceptionContent(exception)) {
            Map<String, ?> map = JsonUtils.readMap(exception.getMessage());

            return (String) map.get("componentName");
        }

        return null;
    }

    public ProviderException withComponentName(@Nonnull String componentName) {
        this.componentName = componentName;

        return this;
    }

    public String getComponentName() {
        return componentName;
    }

    @Nonnull
    @Override
    public String getMessage() {
        return JsonUtils.write(Map.of("componentName", getComponentName(),
            "exceptionMessage", super.getMessage(),
            "exceptionClass", getClass().getName()));
    }

    private String componentName;

    public ProviderException(String message) {
        super(message);
    }

    public static class UnauthorizedRequestException extends ProviderException {
        public UnauthorizedRequestException(String message) {
            super(message);
        }
    }

    public static class AuthorizationFailedException extends ProviderException {
        public AuthorizationFailedException(String message) {
            super(message);
        }
    }

    public static class AuthenticationFailedException extends ProviderException {
        public AuthenticationFailedException(String message) {
            super(message);
        }
    }

    public static class BadRequestException extends ProviderException {
        public BadRequestException(String message,) {
            super(message);
        }
    }

    public static class NotFoundException extends ProviderException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    public static class GeneralException extends ProviderException {
        public GeneralException(String message) {
            super(message);
        }
    }

}
