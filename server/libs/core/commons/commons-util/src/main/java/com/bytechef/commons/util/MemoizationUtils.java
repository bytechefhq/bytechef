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

package com.bytechef.commons.util;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * @author Ivica Cardic
 */
public class MemoizationUtils {

    public static <T> Supplier<T> memoize(Supplier<T> supplier) {
        return new Supplier<>() {
            private final Lock lock = new ReentrantLock();

            private volatile T value;

            @Override
            public T get() {
                T result = value;

                if (result == null) {
                    lock.lock();

                    try {
                        if (value == null) {
                            value = supplier.get();
                        }

                        result = value;
                    } finally {
                        lock.unlock();
                    }
                }

                return result;
            }
        };
    }
}
