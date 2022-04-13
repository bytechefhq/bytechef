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

package com.integri.atlas.engine.core.counter.repository;

/**
 * A repository that can be used to atomically set
 * a counter value.
 *
 * @author Arik Cohen
 */
public interface CounterRepository {
    /**
     * Set the counter to the give value.
     * @param aCounterName the name of the counter
     * @param aValue the value to set the counter to.
     */
    void set(String aCounterName, long aValue);

    /**
     * Decrement the specified counter by 1.
     * @param aCounterName the name of the counter
     * @return the new value
     */
    long decrement(String aCounterName);

    /**
     * Delete the specified counter.
     * @param aCounterName the name of the counter
     */
    void delete(String aCounterName);
}
