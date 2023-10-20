
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

package com.bytechef.hermes.component.util;

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.util.ComponentContextThreadLocal.ComponentContext;

import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public final class ComponentContextSupplier {

    public static <T, E extends Exception> T get(
        Context context, ComponentDefinition componentDefinition, Supplier<T, E> supplier) throws E {

        Objects.requireNonNull(context, "'triggerContext' must not be null");
        Objects.requireNonNull(componentDefinition, "'componentDefinition' must not be null");
        Objects.requireNonNull(supplier, "'supplier' must not be null");

        ComponentContextThreadLocal.set(new ComponentContext(context, componentDefinition));

        try {
            return supplier.get();
        } finally {
            ComponentContextThreadLocal.remove();
        }
    }

    @FunctionalInterface
    public interface Supplier<T, E extends Exception> {

        T get() throws E;
    }
}
