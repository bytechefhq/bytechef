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
 * In-memory map from a short opaque id to a (long) tokenized resume id, for approval channels whose interactive-button
 * payload is too small to carry the whole token — Telegram ({@code callback_data} 64 bytes) and Discord
 * ({@code custom_id} 100 chars). The channel mints a short id at send time (via {@link ApprovalShortTokenController})
 * and the interactivity handler resolves it back.
 *
 * <p>
 * The map is process-local and lost on restart. That is acceptable because those channels also keep the hosted-form
 * link in the message: a lost mapping degrades the in-place buttons to the form link, it does not strand the approval.
 * A distributed EE deployment with multiple coordinator replicas would need a shared store (Redis / DB) instead — see
 * the HITL backlog. Entries are bounded by {@link #MAX_ENTRIES} (oldest evicted) and a TTL matching the maximum
 * approval expiry.
 * </p>
 *
 * @author Ivica Cardic
 */
@Component
public class ApprovalShortTokenStore {

    private static final int MAX_ENTRIES = 100_000;
    private static final long TTL_SECONDS = 70L * 24 * 60 * 60;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    /**
     * Stores the token under a fresh random id and returns the id. Purges expired entries opportunistically and evicts
     * the oldest when the map is full.
     */
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

    /**
     * Returns the token for the id when present and unexpired, otherwise {@code null}.
     */
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
