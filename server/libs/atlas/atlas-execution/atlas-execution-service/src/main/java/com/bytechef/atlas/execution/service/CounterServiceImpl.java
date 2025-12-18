/*
 * Copyright 2025 ByteChef
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
 */

package com.bytechef.atlas.execution.service;

import com.bytechef.atlas.execution.repository.CounterRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Transactional
public class CounterServiceImpl implements CounterService {

    private final CounterRepository counterRepository;

    @SuppressFBWarnings("EI2")
    public CounterServiceImpl(CounterRepository counterRepository) {
        this.counterRepository = counterRepository;
    }

    /**
     * Decrement the specified counter by 1.
     *
     * @param id the id of the counter
     * @return the new value
     */
    @Override
    public long decrement(long id) {
        return counterRepository.decrementAndGet(id);
    }

    @Override
    public void delete(long id) {
        counterRepository.deleteById(id);
    }

    /**
     * Set the counter to the give value.
     *
     * @param id the id of the counter
     */
    @Override
    public void set(long id, long value) {
        counterRepository.setAtomic(id, value);
    }
}
