
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.atlas.service.impl;

import com.bytechef.atlas.domain.Counter;
import com.bytechef.atlas.repository.CounterRepository;
import com.bytechef.atlas.service.CounterService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

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
    public long decrement(String id) {
        Assert.notNull(id, "id cannot be null.");

        Long value = counterRepository.findValueByIdForUpdate(id);

        if (value == null) {
            throw new IllegalStateException("Unable to locate counter with id " + id);
        }

        value = value - 1;

        counterRepository.update(id, value);

        return value;
    }

    @Override
    public void delete(String id) {
        Assert.notNull(id, "id cannot be null.");

        counterRepository.deleteById(id);
    }

    /**
     * Set the counter to the give value.
     *
     * @param id the id of the counter
     */
    @Override
    public void set(String id, long value) {
        Assert.notNull(id, "id cannot be null.");

        Long selectedValue = counterRepository.findValueByIdForUpdate(id);

        if (selectedValue == null) {
            Counter counter = new Counter();

            counter.setId(id);
            counter.setNew(true);
            counter.setValue(value);

            counterRepository.save(counter);
        } else {
            counterRepository.update(id, value);
        }
    }
}
