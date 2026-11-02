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

package com.bytechef.platform.notification.delivery;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Ivica Cardic
 */
final class WebhookDeliveryMessages {

    static final int MAX_LOGGED_LABELS = 20;

    private static final int MAX_FAILURE_MESSAGE_LENGTH = 200;
    private static final Pattern URL_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9+.-]*://[^\\s\"'<>]+");

    private WebhookDeliveryMessages() {
    }

    static String describe(Throwable throwable) {
        String description = describeThrowable(throwable);

        Throwable mostSpecificCause = NestedExceptionUtils.getMostSpecificCause(throwable);

        boolean statusDescribed = throwable instanceof WebhookDeliveryException webhookDeliveryException &&
            webhookDeliveryException.getStatusCode() != null;

        if (mostSpecificCause != throwable && !statusDescribed) {
            description = description + " (" + describeThrowable(mostSpecificCause) + ")";
        }

        String redirectDescription = describeRedirect(throwable);

        if (redirectDescription != null) {
            description = description + ", " + redirectDescription;
        }

        return abbreviate(description);
    }

    static String describeLabels(List<String> labels) {
        if (labels.size() <= MAX_LOGGED_LABELS) {
            return String.valueOf(labels);
        }

        return labels.subList(0, MAX_LOGGED_LABELS) + " and " + (labels.size() - MAX_LOGGED_LABELS) + " more";
    }

    static String host(String url) {
        String host = getHost(url);

        return host == null ? "<invalid url>" : host;
    }

    static String redact(String text) {
        Matcher matcher = URL_PATTERN.matcher(text);

        return matcher.replaceAll(matchResult -> {
            String host = getHost(matchResult.group());

            return Matcher.quoteReplacement(host == null ? "<url>" : host);
        });
    }

    private static String abbreviate(String text) {
        String singleLineText = text.replaceAll("\\s+", " ");

        singleLineText = singleLineText.strip();

        if (singleLineText.length() <= MAX_FAILURE_MESSAGE_LENGTH) {
            return singleLineText;
        }

        return singleLineText.substring(0, MAX_FAILURE_MESSAGE_LENGTH) + "...";
    }

    private static @Nullable String describeRedirect(Throwable throwable) {
        if (!(throwable.getCause() instanceof RestClientResponseException restClientResponseException)) {
            return null;
        }

        HttpStatusCode statusCode = restClientResponseException.getStatusCode();

        if (!statusCode.is3xxRedirection()) {
            return null;
        }

        HttpHeaders responseHeaders = restClientResponseException.getResponseHeaders();

        URI location = responseHeaders == null ? null : responseHeaders.getLocation();
        String locationHost = location == null ? null : getHost(location);

        return locationHost == null ? "redirect without a location host" : "redirect to host " + locationHost;
    }

    private static String describeThrowable(Throwable throwable) {
        String message = throwable.getMessage();

        if (throwable instanceof WebhookDeliveryException && message != null && !message.isBlank()) {
            return redact(message);
        }

        Class<? extends Throwable> throwableClass = throwable.getClass();

        if (message == null || message.isBlank()) {
            return throwableClass.getSimpleName();
        }

        return throwableClass.getSimpleName() + ": " + redact(message);
    }

    private static @Nullable String getHost(String url) {
        String host = parseHost(url);

        if (host != null) {
            return host;
        }

        try {
            return getHost(URI.create(url));
        } catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }

    private static @Nullable String parseHost(String url) {
        try {
            UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(url);

            UriComponents uriComponents = uriComponentsBuilder.build();

            String host = uriComponents.getHost();

            return host == null || host.isEmpty() ? null : host;
        } catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }

    private static @Nullable String getHost(URI uri) {
        String host = uri.getHost();

        if (host == null && uri.isAbsolute()) {
            try {
                URL url = uri.toURL();

                host = url.getHost();
            } catch (IllegalArgumentException | MalformedURLException exception) {
                return null;
            }
        }

        return host == null || host.isEmpty() ? null : host;
    }
}
