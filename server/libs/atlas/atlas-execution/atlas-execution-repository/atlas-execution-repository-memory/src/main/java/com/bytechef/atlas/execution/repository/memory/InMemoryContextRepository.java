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
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import org.apache.commons.lang3.Validate;

/**
 * @author Arik Cohen
 * @since Feb, 21 2020
 */
public class InMemoryContextRepository implements ContextRepository {

    private static final Random RANDOM = new Random();

    private final Map<String, Deque<FileEntry>> contexts = new HashMap<>();

    @Override
    public Iterable<Context> findAll() {
        return contexts.values()
            .stream()
            .flatMap(deque -> deque.stream()
                .map(Context::new))
            .toList();
    }

    @Override
    public Context findTop1ByStackIdAndClassnameIdOrderByCreatedDateDesc(long stackId, int classnameId) {
        Deque<FileEntry> linkedList = contexts.get(getKey(stackId, null, classnameId));

        Validate.notNull(linkedList, "unknown stack: %s", stackId);

        return new Context(linkedList.peek());
    }

    @Override
    public Context findTop1ByStackIdAndSubStackIdAndClassnameIdOrderByCreatedDateDesc(
        long stackId, int subStackId, int classnameId) {
        Deque<FileEntry> linkedList = contexts.get(getKey(stackId, subStackId, classnameId));

        Validate.notNull(linkedList, "unknown stack: " + stackId);

        return new Context(linkedList.peek());
    }

    @Override
    public Context save(Context context) {
        Deque<FileEntry> stack = contexts.computeIfAbsent(
            getKey(context.getStackId(), context.getSubStackId(), context.getClassnameId()), k -> new LinkedList<>());

        if (context.isNew()) {
            context.setId(RANDOM.nextLong());
        }

        stack.push(context.getValue());

        return context;
    }

    private static String getKey(long stackId, Integer subStackId, int classnameId) {
        return "" + stackId + subStackId + classnameId;
    }
}
