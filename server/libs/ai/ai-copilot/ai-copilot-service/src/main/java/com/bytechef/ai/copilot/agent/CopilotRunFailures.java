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

package com.bytechef.ai.copilot.agent;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

/**
 * @author Ivica Cardic
 */
final class CopilotRunFailures {

    static final String NO_PROVIDER = "No AI provider is set up for this environment, so the chat can't answer.";
    static final String OUT_OF_CREDITS =
        "The AI provider account is out of credits. Ask an administrator to top it up or switch providers.";
    static final String RATE_LIMITED = "The AI provider is rate limiting requests right now. Try again in a minute.";
    static final String OVERLOADED = "The AI provider is overloaded right now. Try again in a minute.";
    static final String API_KEY_REJECTED = "The AI provider rejected its API key. Ask an administrator to check it.";
    static final String CONVERSATION_TOO_LONG = "This conversation is too long for the AI model. Start a new chat.";

    private static final Pattern MISSING_API_KEY = Pattern.compile(
        "\\b(?:no|missing)\\s+api[ _-]?key|api[ _-]?key\\b[^.]*\\b(?:not set|must be set|is required|not configured|" +
            "is missing)",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern RATE_LIMIT_STATUS = httpStatusPattern(429);
    private static final Pattern OVERLOADED_STATUS = httpStatusPattern(529);
    private static final Pattern UNAUTHORIZED_STATUS = httpStatusPattern(401);

    private CopilotRunFailures() {
    }

    static @Nullable String describe(Throwable throwable) {
        Set<Throwable> visited = new HashSet<>();

        for (Throwable current = throwable; current != null && visited.add(current); current = current.getCause()) {
            String description = describeOne(current);

            if (description != null) {
                return description;
            }
        }

        return null;
    }

    private static @Nullable String describeOne(Throwable throwable) {
        if (throwable instanceof AuthenticationException || throwable instanceof AccessDeniedException) {
            return null;
        }

        Class<? extends Throwable> throwableClass = throwable.getClass();

        String className = throwableClass.getSimpleName()
            .toLowerCase(Locale.ROOT);
        String message = throwable.getMessage() == null ? "" : throwable.getMessage();

        String lowerCaseMessage = message.toLowerCase(Locale.ROOT);

        if (lowerCaseMessage.startsWith("no chat provider is activated")) {
            return NO_PROVIDER;
        }

        if (lowerCaseMessage.contains("credit balance is too low") || lowerCaseMessage.contains("insufficient_quota")
            || lowerCaseMessage.contains("exceeded your current quota")) {

            return OUT_OF_CREDITS;
        }

        if (className.contains("ratelimit") || lowerCaseMessage.contains("rate limit")
            || lowerCaseMessage.contains("rate_limit") || RATE_LIMIT_STATUS.matcher(message)
                .find()) {

            return RATE_LIMITED;
        }

        if (className.contains("overloaded") || lowerCaseMessage.contains("overloaded")
            || OVERLOADED_STATUS.matcher(message)
                .find()) {

            return OVERLOADED;
        }

        if (MISSING_API_KEY.matcher(message)
            .find()) {

            return null;
        }

        if (className.contains("unauthorized") || className.contains("authentication")
            || lowerCaseMessage.contains("api key") || lowerCaseMessage.contains("api_key")
            || lowerCaseMessage.contains("x-api-key") || UNAUTHORIZED_STATUS.matcher(message)
                .find()) {

            return API_KEY_REJECTED;
        }

        if (lowerCaseMessage.contains("context_length") || lowerCaseMessage.contains("context length")
            || lowerCaseMessage.contains("prompt is too long") || lowerCaseMessage.contains("too many tokens")) {

            return CONVERSATION_TOO_LONG;
        }

        return null;
    }

    private static Pattern httpStatusPattern(int statusCode) {
        return Pattern.compile(
            "(?:^|\\bHTTP(?:/[\\d.]+)?\\s*|\\bstatus(?:\\s+code)?\\s*[:=]?\\s*)" + statusCode + "\\b",
            Pattern.CASE_INSENSITIVE);
    }
}
