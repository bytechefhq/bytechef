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

package com.integri.atlas.engine.repository.memory.counter;

import com.integri.atlas.engine.core.counter.repository.CounterRepository;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Arik Cohen
 * @since Feb, 21 2020
 */
public class InMemoryCounterRepository implements CounterRepository {

    private final Map<String, Long> counters = new HashMap<>();

    @Override
    public void set(String aCounterName, long aValue) {
        counters.put(aCounterName, aValue);
    }

    @Override
    public long decrement(String aCounterName) {
        Long value = counters.getOrDefault(aCounterName, 0L) - 1;
        counters.put(aCounterName, value);
        return value;
    }

    @Override
    public void delete(String aCounterName) {
        counters.remove(aCounterName);
    }
}
