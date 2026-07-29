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

package com.bytechef.component.exception;

import com.bytechef.component.definition.HttpStatus;
import java.util.List;
import java.util.Map;

/**
 * Represents an error returned by an external provider while a component action or trigger interacts with it. It
 * carries the provider's HTTP status code and any {@code Retry-After} hint, and offers specialized subtypes for common
 * client-error statuses along with helpers to classify errors as retryable.
 *
 * @author Igor Beslic
 * @author Ivica Cardic
 */
public class ProviderException extends RuntimeException {

    private Integer statusCode;
    private String retryAfter;

    /**
     * Creates a provider exception wrapping the given underlying exception.
     *
     * @param exception the cause of this provider error
     */
    public ProviderException(Exception exception) {
        super(exception);
    }

    /**
     * Creates a provider exception with the given detail message.
     *
     * @param message the detail message describing the error
     */
    public ProviderException(String message) {
        super(message);
    }

    /**
     * Creates a provider exception with the given detail message and underlying cause.
     *
     * @param message   the detail message describing the error
     * @param exception the cause of this provider error
     */
    public ProviderException(String message, Exception exception) {
        super(message, exception);
    }

    /**
     * Creates a provider exception carrying the given HTTP status code and no detail message.
     *
     * @param statusCode the HTTP status code returned by the provider
     */
    public ProviderException(Integer statusCode) {
        this(statusCode, null);
    }

    /**
     * Creates a provider exception carrying the given HTTP status code and detail message.
     *
     * @param statusCode the HTTP status code returned by the provider
     * @param message    the detail message describing the error
     */
    public ProviderException(Integer statusCode, String message) {
        super(message);

        this.statusCode = statusCode;
    }

    /**
     * Returns the HTTP status code the provider responded with, if any.
     *
     * @return the HTTP status code, or {@code null} when none was set
     */
    public Integer getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the raw {@code Retry-After} header value the provider sent with this error, if any.
     *
     * @return the {@code Retry-After} value (delay-seconds or an HTTP-date), or {@code null} when absent
     */
    public String getRetryAfter() {
        return retryAfter;
    }

    /**
     * Whether this provider error is transient and worth retrying later rather than surfacing as a hard failure: an
     * explicit rate limit (HTTP 429), a temporary unavailability (HTTP 503), or any response carrying a
     * {@code Retry-After} header.
     *
     * @return true if the error is transient/retryable
     */
    public boolean isRetryable() {
        if (retryAfter != null) {
            return true;
        }

        if (statusCode == null) {
            return false;
        }

        return statusCode == HttpStatus.TOO_MANY_REQUESTS.getValue() ||
            statusCode == HttpStatus.SERVICE_UNAVAILABLE.getValue();
    }

    /**
     * Walks the cause chain of the given throwable and reports whether any {@link ProviderException} in it is
     * {@link #isRetryable() retryable}.
     *
     * @param throwable the throwable to inspect (may be {@code null})
     * @return true if a retryable provider error is present anywhere in the cause chain
     */
    public static boolean isRetryable(Throwable throwable) {
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof ProviderException providerException && providerException.isRetryable()) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    /**
     * A provider error corresponding to an HTTP 400 Bad Request response.
     */
    public static class BadRequestException extends ProviderException {

        /**
         * Creates a bad-request provider exception with the given detail message.
         *
         * @param message the detail message describing the error
         */
        public BadRequestException(String message) {
            super(400, message);
        }
    }

    /**
     * A provider error corresponding to an HTTP 403 Forbidden response.
     */
    public static class ForbiddenException extends ProviderException {

        /**
         * Creates a forbidden provider exception with the given detail message.
         *
         * @param message the detail message describing the error
         */
        public ForbiddenException(String message) {
            super(403, message);
        }

    }

    /**
     * A provider error corresponding to an HTTP 404 Not Found response.
     */
    public static class NotFoundException extends ProviderException {

        /**
         * Creates a not-found provider exception with the given detail message.
         *
         * @param message the detail message describing the error
         */
        public NotFoundException(String message) {
            super(404, message);
        }
    }

    /**
     * A provider error corresponding to an HTTP 401 Unauthorized response.
     */
    public static class UnauthorizedException extends ProviderException {

        /**
         * Creates an unauthorized provider exception with the given detail message.
         *
         * @param message the detail message describing the error
         */
        public UnauthorizedException(String message) {
            super(401, message);
        }
    }

    /**
     * Builds the most specific {@link ProviderException} subtype for the given HTTP status code, incorporating the
     * response body into the message.
     *
     * @param statusCode the HTTP status code returned by the provider
     * @param body       the response body, if any
     * @return a provider exception matching the status code (a specific subtype for 400, 401, 403, and 404; a generic
     *         {@link ProviderException} otherwise)
     */
    public static ProviderException getProviderException(int statusCode, Object body) {
        String message = String.valueOf(statusCode);

        message += body == null ? "" : ": " + body;

        return switch (statusCode) {
            case 400 -> new ProviderException.BadRequestException(message);
            case 401 -> new ProviderException.UnauthorizedException(message);
            case 403 -> new ProviderException.ForbiddenException(message);
            case 404 -> new ProviderException.NotFoundException(message);
            default -> new ProviderException(statusCode, message);
        };
    }

    /**
     * Builds the most specific {@link ProviderException} subtype for the given HTTP status code, additionally capturing
     * the {@code Retry-After} header from the response so retryability can be determined.
     *
     * @param statusCode the HTTP status code returned by the provider
     * @param body       the response body, if any
     * @param headers    the response headers, keyed by name
     * @return a provider exception matching the status code, with any {@code Retry-After} value attached
     */
    public static ProviderException getProviderException(
        int statusCode, Object body, Map<String, List<String>> headers) {

        ProviderException providerException = getProviderException(statusCode, body);

        providerException.retryAfter = getRetryAfter(headers);

        return providerException;
    }

    private static String getRetryAfter(Map<String, List<String>> headers) {
        if (headers == null) {
            return null;
        }

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if ("retry-after".equalsIgnoreCase(entry.getKey())) {
                List<String> values = entry.getValue();

                if (values != null && !values.isEmpty()) {
                    return values.getFirst();
                }
            }
        }

        return null;
    }
}
