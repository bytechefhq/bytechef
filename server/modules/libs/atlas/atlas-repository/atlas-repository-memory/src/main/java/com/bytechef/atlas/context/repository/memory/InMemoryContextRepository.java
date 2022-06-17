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

package com.bytechef.atlas.context.repository.memory;

import com.bytechef.atlas.context.domain.Context;
import com.bytechef.atlas.context.repository.ContextRepository;
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

    private final Map<String, Deque<Context>> contexts = new HashMap<>();

    @Override
    public void delete(String stackId) {
        contexts.remove(stackId);
    }

    @Override
    public void push(String stackId, Context context) {
        Deque<Context> stack = contexts.get(stackId);
        if (stack == null) {
            stack = new LinkedList<>();

            contexts.put(stackId, stack);
        }
        stack.push(context);
    }

    @Override
    public Context peek(String stackId) {
        Deque<Context> linkedList = contexts.get(stackId);
        Assert.notNull(linkedList, "unknown stack: " + stackId);

        return linkedList.peek();
    }
}
