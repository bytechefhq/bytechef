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

package com.bytechef.commons.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public final class ClientCacheUtils {

    private static final Logger log = LoggerFactory.getLogger(ClientCacheUtils.class);

    private ClientCacheUtils() {
    }

    public static <K, V> Cache<K, V> createClientCache(Consumer<? super V> clientCloser) {
        return createClientCache(ClientCacheSettings.defaults(), clientCloser);
    }

    public static <K, V> Cache<K, V> createClientCache(
        ClientCacheSettings clientCacheSettings, Consumer<? super V> clientCloser) {

        long maximumSize = clientCacheSettings.maximumSize();
        Executor sizeEvictionCloseExecutor = clientCacheSettings.sizeEvictionCloseExecutor();

        return Caffeine.newBuilder()
            .executor(clientCacheSettings.executor())
            .expireAfterAccess(ClientCacheSettings.IDLE_TIMEOUT)
            .maximumSize(maximumSize)
            .scheduler(clientCacheSettings.scheduler())
            .ticker(clientCacheSettings.ticker())
            .removalListener((K key, V client, RemovalCause removalCause) -> {
                if (client == null) {
                    return;
                }

                if (removalCause == RemovalCause.SIZE) {
                    log.warn(
                        "Client cache exceeded its maximum size of {} and evicted the client for {}; closing it in {}",
                        maximumSize, key, ClientCacheSettings.SIZE_EVICTION_CLOSE_DELAY);

                    sizeEvictionCloseExecutor.execute(() -> closeClient(key, client, clientCloser));
                } else {
                    closeClient(key, client, clientCloser);
                }
            })
            .build();
    }

    private static <K, V> void closeClient(K key, V client, Consumer<? super V> clientCloser) {
        try {
            clientCloser.accept(client);
        } catch (RuntimeException runtimeException) {
            log.warn("Failed to close the cached client for {}", key, runtimeException);
        }
    }
}
