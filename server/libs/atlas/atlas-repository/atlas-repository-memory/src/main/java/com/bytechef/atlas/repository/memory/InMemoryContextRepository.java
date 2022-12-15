
/*
 * Copyright 2016-2018 the original author or authors.
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
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.repository.memory;

import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.repository.ContextRepository;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 * @since Feb, 21 2020
 */
public class InMemoryContextRepository implements ContextRepository {

    private final Map<String, Deque<Map<String, Object>>> contexts = new HashMap<>();

    @Override
    public void deleteById(String id) {
        contexts.remove(id);
    }

    @Override
    public Iterable<Context> findAll() {
        return contexts.values()
            .stream()
            .flatMap(deque -> deque.stream()
                .map(Context::new))
            .toList();
    }

    @Override
    public Context findTop1ByStackIdOrderByCreatedDateDesc(String stackId) {
        Deque<Map<String, Object>> linkedList = contexts.get(stackId);
        Assert.notNull(linkedList, "unknown stack: " + stackId);

        return new Context(linkedList.peek());
    }

    @Override
    public Context save(Context context) {
        Deque<Map<String, Object>> stack = contexts.get(context.getStackId());

        if (stack == null) {
            stack = new LinkedList<>();

            contexts.put(context.getStackId(), stack);
        }

        stack.push(context.getValue());

        return context;
    }
}
