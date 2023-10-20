/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.worker.context;

import com.integri.atlas.engine.core.context.Context;
import com.integri.atlas.engine.core.context.ContextRepository;
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
    public void push(String aStackId, Context aContext) {
        Deque<Context> stack = contexts.get(aStackId);
        if (stack == null) {
            stack = new LinkedList<>();
            contexts.put(aStackId, stack);
        }
        stack.push(aContext);
    }

    @Override
    public Context peek(String aStackId) {
        Deque<Context> linkedList = contexts.get(aStackId);
        Assert.notNull(linkedList, "unknown stack: " + aStackId);
        return linkedList.peek();
    }
}
