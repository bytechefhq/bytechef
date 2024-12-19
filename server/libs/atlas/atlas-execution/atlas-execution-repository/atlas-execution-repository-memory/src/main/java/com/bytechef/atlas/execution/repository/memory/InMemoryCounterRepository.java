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

import com.bytechef.atlas.execution.domain.Counter;
import com.bytechef.atlas.execution.repository.CounterRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Feb, 21 2020
 */
public class InMemoryCounterRepository implements CounterRepository {

    private static final Cache<Long, Long> COUNTERS =
        Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    @Override
    public void deleteById(Long id) {
        COUNTERS.invalidate(id);
    }

    @Override
    public Optional<Long> findValueByIdForUpdate(Long id) {
        return Optional.ofNullable(COUNTERS.getIfPresent(id));
    }

    @Override
    public Counter save(Counter counter) {
        COUNTERS.put(counter.getId(), counter.getValue());

        return counter;
    }

    @Override
    public void update(Long id, long value) {
        COUNTERS.put(id, value);
    }
}
