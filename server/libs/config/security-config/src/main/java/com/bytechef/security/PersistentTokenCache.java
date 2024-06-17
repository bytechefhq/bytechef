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

package com.bytechef.security;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple time-limited cache for login tokens, necessary to avoid concurrent requests invalidating one another. It uses
 * a {@link java.util.LinkedHashMap} to keep the tokens in order of expiration. During access any entries which have
 * expired are automatically purged.
 *
 * @author Ivica Cardic
 */
public class PersistentTokenCache<T> {

    private final long expireMillis;

    private final Map<String, Value> map;
    private long latestWriteTime;

    /**
     * Construct a new TokenCache.
     *
     * @param expireMillis Delay until tokens expire, in millis.
     * @throws java.lang.IllegalArgumentException if expireMillis is non-positive.
     */
    @SuppressFBWarnings("CT")
    public PersistentTokenCache(long expireMillis) {
        if (expireMillis <= 0L) {
            throw new IllegalArgumentException();
        }

        this.expireMillis = expireMillis;

        map = new LinkedHashMap<>(64, 0.75f);
        latestWriteTime = System.currentTimeMillis();
    }

    /**
     * Get a token from the cache.
     *
     * @param key The key to look for.
     * @return The token, if present and not yet expired, or null otherwise.
     */
    public T get(String key) {
        purge();

        Value val = map.get(key);

        long time = System.currentTimeMillis();

        return val != null && time < val.expire ? val.token : null;
    }

    /**
     * Put a token in the cache. If a token already exists for the given key, it is replaced.
     *
     * @param key   The key to insert for.
     * @param token The token to insert.
     */
    public void put(String key, T token) {
        purge();
        if (map.containsKey(key)) {
            map.remove(key);
        }
        long time = System.currentTimeMillis();
        map.put(key, new Value(token, time + expireMillis));
        latestWriteTime = time;
    }

    /**
     * Get the number of tokens in the cache. Note, this may include expired tokens, unless {@link #purge()} is invoked
     * first.
     *
     * @return The size of the cache.
     */
    public int size() {
        return map.size();
    }

    /**
     * Remove expired entries from the map. This will be called automatically before read/write access, but could be
     * manually invoked if desired.
     */
    public void purge() {
        long time = System.currentTimeMillis();

        if (time - latestWriteTime > expireMillis) {
            // Everything in the map is expired, clear all at once
            map.clear();
        } else {
            // Iterate and remove until the first non-expired token
            Iterator<Value> values = map.values()
                .iterator();

            while (values.hasNext()) {
                if (time >= values.next().expire) {
                    values.remove();
                } else {
                    break;
                }
            }
        }
    }

    private class Value {

        private final T token;
        private final long expire;

        Value(T token, long expire) {
            this.token = token;
            this.expire = expire;
        }
    }

}
