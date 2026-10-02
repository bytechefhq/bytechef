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

package com.bytechef.platform.ai.sensitivedata.tokenization;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public final class PiiTokenSession {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AtomicInteger nextOrdinal = new AtomicInteger(1);
    private final String sessionId;
    private final Map<String, String> tokenToValue = new ConcurrentHashMap<>();
    private final Map<String, String> valueToToken = new ConcurrentHashMap<>();

    private PiiTokenSession(String sessionId) {
        this.sessionId = sessionId;
    }

    public static PiiTokenSession create() {
        StringBuilder builder = new StringBuilder(PiiToken.SESSION_ID_LENGTH);

        for (int index = 0; index < PiiToken.SESSION_ID_LENGTH; index++) {
            builder.append(PiiToken.SESSION_ID_ALPHABET.charAt(
                SECURE_RANDOM.nextInt(PiiToken.SESSION_ID_ALPHABET.length())));
        }

        return new PiiTokenSession(builder.toString());
    }

    public String tokenFor(String category, String value) {
        return valueToToken.computeIfAbsent(value, presentValue -> {
            String token = new PiiToken(category, nextOrdinal.getAndIncrement(), sessionId).text();

            tokenToValue.put(token, presentValue);

            return token;
        });
    }

    public @Nullable String restore(@Nullable String text) {
        return restoreWithUnresolvedCount(text).text();
    }

    public RestoreResult restoreWithUnresolvedCount(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return new RestoreResult(text, 0);
        }

        Matcher matcher = PiiToken.pattern()
            .matcher(text);

        StringBuilder builder = new StringBuilder();
        int unresolvedCount = 0;

        while (matcher.find()) {
            String value = tokenToValue.get(matcher.group());

            if (value == null) {
                unresolvedCount++;
            }

            matcher.appendReplacement(builder, Matcher.quoteReplacement(value == null ? matcher.group() : value));
        }

        matcher.appendTail(builder);

        return new RestoreResult(builder.toString(), unresolvedCount);
    }

    public String sessionId() {
        return sessionId;
    }

    public int size() {
        return tokenToValue.size();
    }

    public Map<String, String> tokens() {
        return Map.copyOf(tokenToValue);
    }

    public void close() {
        tokenToValue.clear();
        valueToToken.clear();
    }

    public static PiiTokenSession rehydrate(String sessionId, Map<String, String> tokens) {
        PiiTokenSession session = new PiiTokenSession(sessionId);
        int highestOrdinal = 0;

        for (Map.Entry<String, String> entry : tokens.entrySet()) {
            Optional<PiiToken> parsed = PiiToken.parse(entry.getKey());

            if (parsed.isEmpty()) {
                continue;
            }

            PiiToken token = parsed.get();

            session.tokenToValue.put(entry.getKey(), entry.getValue());
            session.valueToToken.put(entry.getValue(), entry.getKey());

            highestOrdinal = Math.max(highestOrdinal, token.ordinal());
        }

        session.nextOrdinal.set(highestOrdinal + 1);

        return session;
    }

    public record RestoreResult(@Nullable String text, int unresolvedCount) {
    }
}
