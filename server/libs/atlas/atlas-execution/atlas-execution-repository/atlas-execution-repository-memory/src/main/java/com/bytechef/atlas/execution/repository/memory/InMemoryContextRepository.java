/*
 * Copyright 2016-2020 the original author or authors.
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
 *
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.atlas.execution.repository.memory;

import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.repository.ContextRepository;
import com.bytechef.commons.util.RandomUtils;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * @author Arik Cohen
 * @since Feb, 21 2020
 */
public class InMemoryContextRepository implements ContextRepository {

    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final String CACHE = InMemoryContextRepository.class.getName() + ".context";

    private final CacheManager cacheManager;

    public InMemoryContextRepository(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public Optional<Context> findTop1ByStackIdAndClassnameIdOrderByCreatedDateDesc(long stackId, int classnameId) {
        Deque<FileEntry> stack = getStack(getKey(stackId, null, classnameId));

        return Optional.ofNullable(stack == null ? null : new Context(stack.peek()));
    }

    @Override
    public Optional<Context> findTop1ByStackIdAndSubStackIdAndClassnameIdOrderByCreatedDateDesc(
        long stackId, int subStackId, int classnameId) {

        Deque<FileEntry> linkedList = getStack(getKey(stackId, subStackId, classnameId));

        return Optional.ofNullable(linkedList == null ? null : new Context(linkedList.peek()));
    }

    @Override
    public Context save(Context context) {
        String key = getKey(context.getStackId(), context.getSubStackId(), context.getClassnameId());

        try {
            LOCK.lock();

            Deque<FileEntry> stack = getStack(key);

            if (context.isNew()) {
                context.setId(Math.abs(Math.max(RandomUtils.nextLong(), Long.MIN_VALUE + 1)));
            }

            stack.push(context.getValue());

            Cache cache = Objects.requireNonNull(cacheManager.getCache(CACHE));

            cache.put(key, stack);
        } finally {
            LOCK.unlock();
        }

        return context;
    }

    private static String getKey(long stackId, Integer subStackId, int classnameId) {
        return TenantCacheKeyUtils.getKey(stackId, subStackId, classnameId);
    }

    private Deque<FileEntry> getStack(String key) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(CACHE));

        return cache.get(key, LinkedList::new);
    }
}
