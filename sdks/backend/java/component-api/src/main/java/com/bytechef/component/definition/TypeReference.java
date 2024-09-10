/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.definition;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @param <T>
 */
public abstract class TypeReference<T> implements Comparable<TypeReference<T>> {

    protected final Type type;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    protected TypeReference() {
        Type superClass = getClass().getGenericSuperclass();

        if (superClass instanceof Class<?>) { // sanity check, should never happen
            throw new IllegalArgumentException(
                "Internal error: TypeReference constructed without actual type information");
        }

        type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
    }

    public Type getType() {
        return type;
    }

    /**
     * The only reason we define this method (and require implementation of <code>Comparable</code>) is to prevent
     * constructing a reference without type information.
     */
    @Override
    public int compareTo(TypeReference<T> o) {
        return 0;
    }
}
