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
 * Modifications copyright (C) 2023 ByteChef Inc.
 */

package com.bytechef.atlas.execution.repository.memory;

import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.repository.ContextRepository;
import com.bytechef.file.storage.domain.FileEntry;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.security.SecureRandom;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Arik Cohen
 * @since Feb, 21 2020
 */
public class InMemoryContextRepository implements ContextRepository {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Cache<String, Deque<FileEntry>> CONTEXTS =
        Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    @Override
    public Optional<Context> findTop1ByStackIdAndClassnameIdOrderByCreatedDateDesc(long stackId, int classnameId) {
        Deque<FileEntry> linkedList = CONTEXTS.getIfPresent(getKey(stackId, null, classnameId));

        return Optional.ofNullable(linkedList == null ? null : new Context(linkedList.peek()));
    }

    @Override
    public Optional<Context> findTop1ByStackIdAndSubStackIdAndClassnameIdOrderByCreatedDateDesc(
        long stackId, int subStackId, int classnameId) {

        Deque<FileEntry> linkedList = CONTEXTS.getIfPresent(getKey(stackId, subStackId, classnameId));

        return Optional.ofNullable(linkedList == null ? null : new Context(linkedList.peek()));
    }

    @Override
    public Context save(Context context) {
        String key = getKey(context.getStackId(), context.getSubStackId(), context.getClassnameId());

        synchronized (CONTEXTS) {
            Deque<FileEntry> stack = CONTEXTS.get(key, k -> new LinkedList<>());

            if (context.isNew()) {
                context.setId(Math.abs(Math.max(RANDOM.nextLong(), Long.MIN_VALUE + 1)));
            }

            stack.push(context.getValue());

            CONTEXTS.put(key, stack);
        }

        return context;
    }

    private static String getKey(long stackId, Integer subStackId, int classnameId) {
        return "" + stackId + subStackId + classnameId;
    }
}
