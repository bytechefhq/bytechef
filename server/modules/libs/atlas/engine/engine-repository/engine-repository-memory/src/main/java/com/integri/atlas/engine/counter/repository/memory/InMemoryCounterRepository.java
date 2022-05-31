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

package com.integri.atlas.engine.counter.repository.memory;

import com.integri.atlas.engine.counter.repository.CounterRepository;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Arik Cohen
 * @since Feb, 21 2020
 */
public class InMemoryCounterRepository implements CounterRepository {

    private final Map<String, Long> counters = new HashMap<>();

    @Override
    public void set(String counterName, long value) {
        counters.put(counterName, value);
    }

    @Override
    public long decrement(String counterName) {
        Long value = counters.getOrDefault(counterName, 0L) - 1;

        counters.put(counterName, value);

        return value;
    }

    @Override
    public void delete(String counterName) {
        counters.remove(counterName);
    }
}
