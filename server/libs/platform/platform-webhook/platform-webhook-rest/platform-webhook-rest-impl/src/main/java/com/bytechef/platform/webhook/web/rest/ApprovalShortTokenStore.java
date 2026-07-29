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

package com.bytechef.platform.webhook.web.rest;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ApprovalShortTokenStore {

    private static final int MAX_ENTRIES = 100_000;
    private static final long TTL_SECONDS = 70L * 24 * 60 * 60;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public String mint(String token) {
        purgeExpired();

        if (entries.size() >= MAX_ENTRIES) {
            evictOldest();
        }

        byte[] bytes = new byte[16];

        secureRandom.nextBytes(bytes);

        String shortId = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes);

        entries.put(shortId, new Entry(token, Instant.now()
            .getEpochSecond() + TTL_SECONDS));

        return shortId;
    }

    public @Nullable String resolve(String shortId) {
        Entry entry = entries.get(shortId);

        if (entry == null) {
            return null;
        }

        if (entry.expiresAtEpochSecond() < Instant.now()
            .getEpochSecond()) {

            entries.remove(shortId);

            return null;
        }

        return entry.token();
    }

    private void purgeExpired() {
        long now = Instant.now()
            .getEpochSecond();

        Iterator<Map.Entry<String, Entry>> iterator = entries.entrySet()
            .iterator();

        while (iterator.hasNext()) {
            if (iterator.next()
                .getValue()
                .expiresAtEpochSecond() < now) {

                iterator.remove();
            }
        }
    }

    private void evictOldest() {
        entries.entrySet()
            .stream()
            .min(Map.Entry.comparingByValue(
                (left, right) -> Long.compare(left.expiresAtEpochSecond(), right.expiresAtEpochSecond())))
            .map(Map.Entry::getKey)
            .ifPresent(entries::remove);
    }

    private record Entry(String token, long expiresAtEpochSecond) {
    }
}
