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
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import org.jspecify.annotations.Nullable;

/**
 * @author Arik Cohen
 * @since Feb, 21 2020
 */
public class InMemoryContextRepository implements ContextRepository {

    private static final String NO_SUB_STACK = "";

    private final ConcurrentHashMap<String, ConcurrentMap<String, Deque<FileEntry>>> cache =
        new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public List<Context> findAllByStackIdAndClassnameId(long stackId, int classnameId) {
        Map<String, Deque<FileEntry>> subStacks = cache.get(getStackKey(stackId, classnameId));

        if (subStacks == null) {
            return List.of();
        }

        List<Context> contexts = new ArrayList<>();

        for (Map.Entry<String, Deque<FileEntry>> entry : subStacks.entrySet()) {
            Integer subStackId = toSubStackId(entry.getKey());

            for (FileEntry fileEntry : entry.getValue()) {
                contexts.add(new Context(stackId, subStackId, Context.Classname.values()[classnameId], fileEntry));
            }
        }

        return contexts;
    }

    @Override
    public void deleteAllByStackIdAndClassnameId(long stackId, int classnameId) {
        cache.remove(getStackKey(stackId, classnameId));
    }

    @Override
    public Optional<Context> findTop1ByStackIdAndClassnameIdOrderByCreatedDateDesc(long stackId, int classnameId) {
        return findTop(stackId, null, classnameId);
    }

    @Override
    public Optional<Context> findTop1ByStackIdAndSubStackIdAndClassnameIdOrderByCreatedDateDesc(
        long stackId, int subStackId, int classnameId) {

        return findTop(stackId, subStackId, classnameId);
    }

    @Override
    public Context save(Context context) {
        long stackId = context.getStackId();
        int classnameId = context.getClassnameId();
        String subStackKey = getSubStackKey(context.getSubStackId());

        String lockKey = TenantCacheKeyUtils.getKey(stackId, context.getSubStackId(), classnameId);

        ReentrantLock lock = locks.computeIfAbsent(lockKey, key -> new ReentrantLock());

        try {
            lock.lock();

            Map<String, Deque<FileEntry>> subStacks = cache.computeIfAbsent(
                getStackKey(stackId, classnameId), key -> new ConcurrentHashMap<>());

            Deque<FileEntry> stack = subStacks.computeIfAbsent(subStackKey, key -> new ConcurrentLinkedDeque<>());

            if (context.isNew()) {
                context.setId(Math.abs(Math.max(RandomUtils.nextLong(), Long.MIN_VALUE + 1)));
            }

            stack.push(context.getValue());
        } finally {
            lock.unlock();

            if (!lock.isLocked() && !lock.hasQueuedThreads()) {
                locks.remove(lockKey, lock);
            }
        }

        return context;
    }

    private Optional<Context> findTop(long stackId, @Nullable Integer subStackId, int classnameId) {
        Map<String, Deque<FileEntry>> subStacks = cache.get(getStackKey(stackId, classnameId));

        if (subStacks == null) {
            return Optional.empty();
        }

        Deque<FileEntry> stack = subStacks.get(getSubStackKey(subStackId));

        if (stack == null) {
            return Optional.empty();
        }

        FileEntry fileEntry = stack.peek();

        return fileEntry == null ? Optional.empty() : Optional.of(new Context(fileEntry));
    }

    private static String getStackKey(long stackId, int classnameId) {
        return TenantCacheKeyUtils.getKey(stackId, classnameId);
    }

    private static String getSubStackKey(@Nullable Integer subStackId) {
        return subStackId == null ? NO_SUB_STACK : String.valueOf(subStackId);
    }

    private static @Nullable Integer toSubStackId(String subStackKey) {
        return subStackKey.equals(NO_SUB_STACK) ? null : Integer.valueOf(subStackKey);
    }
}
