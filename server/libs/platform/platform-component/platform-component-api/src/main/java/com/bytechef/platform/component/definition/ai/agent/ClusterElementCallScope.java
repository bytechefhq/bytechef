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

package com.bytechef.platform.component.definition.ai.agent;

import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * Carries the call a piece of code runs under to code on the same thread, so nested calls can name their parent.
 *
 * @author Ivica Cardic
 */
public final class ClusterElementCallScope {

    private static final ThreadLocal<ClusterElementCallRef> CURRENT = new ThreadLocal<>();

    private ClusterElementCallScope() {
    }

    public static @Nullable ClusterElementCallRef current() {
        return CURRENT.get();
    }

    public static <T> T callWith(@Nullable ClusterElementCallRef ref, Supplier<T> supplier) {
        ClusterElementCallRef previous = CURRENT.get();

        if (ref == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(ref);
        }

        try {
            return supplier.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
