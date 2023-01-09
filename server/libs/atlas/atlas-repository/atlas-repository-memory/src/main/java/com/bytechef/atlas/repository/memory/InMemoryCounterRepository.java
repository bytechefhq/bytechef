
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

import com.bytechef.atlas.domain.Counter;
import com.bytechef.atlas.repository.CounterRepository;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Feb, 21 2020
 */
public class InMemoryCounterRepository implements CounterRepository {

    private final Map<Long, Long> counters = new HashMap<>();

    @Override
    public void deleteById(Long id) {
        counters.remove(id);
    }

    @Override
    public Iterable<Counter> findAll() {
        return counters.entrySet()
            .stream()
            .map(entry -> new Counter(entry.getKey(), entry.getValue()))
            .toList();
    }

    @Override
    public Long findValueByIdForUpdate(Long id) {
        return counters.get(id);
    }

    @Override
    public Counter save(Counter counter) {
        counters.put(counter.getId(), counter.getValue());

        return counter;
    }

    @Override
    public void update(Long id, long value) {
        counters.put(id, value);
    }
}
