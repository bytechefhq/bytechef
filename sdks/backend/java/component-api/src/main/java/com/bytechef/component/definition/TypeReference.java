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

package com.bytechef.component.definition;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Captures a full generic type at compile time so it can be recovered at runtime, working around Java's type erasure.
 * Instances are created as anonymous subclasses (for example, {@code new TypeReference<List<String>>() {}}) so the
 * actual type argument can be read from the generic superclass and used to drive type-safe conversions.
 *
 * @param <T> the referenced type
 *
 * @author Ivica Cardic
 */
public abstract class TypeReference<T> implements Comparable<TypeReference<T>> {

    protected final Type type;

    /**
     * Constructs a type reference, capturing the actual type argument {@code T} from the anonymous subclass's generic
     * superclass.
     *
     * @throws IllegalArgumentException if the reference is constructed without supplying actual type information
     */
    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    protected TypeReference() {
        Type superClass = getClass().getGenericSuperclass();

        if (superClass instanceof Class<?>) { // sanity check, should never happen
            throw new IllegalArgumentException(
                "Internal error: TypeReference constructed without actual type information");
        }

        type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
    }

    /**
     * Returns the captured generic {@link Type}.
     *
     * @return the referenced type
     */
    public Type getType() {
        return type;
    }

    /**
     * The only reason we define this method (and require implementation of <code>Comparable</code>) is to prevent
     * constructing a reference without type information.
     *
     * @param o the other type reference to compare against
     * @return always {@code 0}; ordering is not meaningful for type references
     */
    @Override
    public int compareTo(TypeReference<T> o) {
        return 0;
    }
}
