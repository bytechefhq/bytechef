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

package com.bytechef.atlas.execution.repository;

import com.bytechef.atlas.execution.domain.Counter;
import java.util.Optional;

/**
 * A repository that can be used to update a counter value.
 *
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public interface CounterRepository {

    /**
     * Delete the specified counter.
     *
     * @param id the id of the counter
     */
    void deleteById(Long id);

    Optional<Long> findValueByIdForUpdate(Long id);

    Counter save(Counter counter);

    /**
     * Set the counter to the give value.
     *
     * @param id    the id of the counter
     * @param value the value to set the counter to.
     */
    void update(Long id, long value);
}
