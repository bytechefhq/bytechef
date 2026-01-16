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

package com.bytechef.component.definition.datastream;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Object representing a context for an {@link ItemStream}. It is a thin wrapper for a map that allows optionally for
 * type safety on reads. It also allows for dirty checking by setting a 'dirty' flag whenever any put is called.
 * <p>
 * Non-transient entries should be serializable, otherwise a custom serializer should be used. Note that putting
 * <code>null</code> value is equivalent to removing the entry for the given key.
 *
 * @author Ivica Cardic
 */
public interface ExecutionContext {

    /**
     * Adds a String value to the context. Putting <code>null</code> value for a given key removes the key.
     *
     * @param key   Key to add to context
     * @param value Value to associate with key
     */
    void putString(String key, String value);

    /**
     * Adds a Long value to the context.
     *
     * @param key   Key to add to context
     * @param value Value to associate with key
     */
    void putLong(String key, long value);

    /**
     * Adds an Integer value to the context.
     *
     * @param key   Key to add to context
     * @param value Value to associate with key
     */
    void putInt(String key, int value);

    /**
     * Add a Double value to the context.
     *
     * @param key   Key to add to context
     * @param value Value to associate with key
     */
    void putDouble(String key, double value);

    /**
     * Add an Object value to the context. Putting <code>null</code> value for a given key removes the key.
     *
     * @param key   Key to add to context
     * @param value Value to associate with key
     */
    void put(String key, @Nullable Object value);

    /**
     * Indicates if context has been changed with a "put" operation since the dirty flag was last cleared. Note that the
     * last time the flag was cleared might correspond to creation of the context.
     *
     * @return True if "put" operation has occurred since flag was last cleared
     */
    boolean isDirty();

    /**
     * Typesafe Getter for the String represented by the provided key.
     *
     * @param key The key to get a value for
     * @return The <code>String</code> value
     */
    String getString(String key);

    /**
     * Typesafe Getter for the String represented by the provided key with default value to return if key is not
     * represented.
     *
     * @param key           The key to get a value for
     * @param defaultString Default to return if key is not represented
     * @return The <code>String</code> value if key is represented, specified default otherwise
     */
    String getString(String key, String defaultString);

    /**
     * Typesafe Getter for the Long represented by the provided key.
     *
     * @param key The key to get a value for
     * @return The <code>Long</code> value
     */
    long getLong(String key);

    /**
     * Typesafe Getter for the Long represented by the provided key with default value to return if key is not
     * represented.
     *
     * @param key         The key to get a value for
     * @param defaultLong Default to return if key is not represented
     * @return The <code>long</code> value if key is represented, specified default otherwise
     */
    long getLong(String key, long defaultLong);

    /**
     * Typesafe Getter for the Integer represented by the provided key.
     *
     * @param key The key to get a value for
     * @return The <code>Integer</code> value
     */
    int getInt(String key);

    /**
     * Typesafe Getter for the Integer represented by the provided key with default value to return if key is not
     * represented.
     *
     * @param key        The key to get a value for
     * @param defaultInt Default to return if key is not represented
     * @return The <code>int</code> value if key is represented, specified default otherwise
     */
    int getInt(String key, int defaultInt);

    /**
     * Typesafe Getter for the Double represented by the provided key.
     *
     * @param key The key to get a value for
     * @return The <code>Double</code> value
     */
    double getDouble(String key);

    /**
     * Typesafe Getter for the Double represented by the provided key with default value to return if key is not
     * represented.
     *
     * @param key           The key to get a value for
     * @param defaultDouble Default to return if key is not represented
     * @return The <code>double</code> value if key is represented, specified default otherwise
     */
    double getDouble(String key, double defaultDouble);

    /**
     * Getter for the value represented by the provided key.
     *
     * @param key The key to get a value for
     * @return The value represented by the given key or {@code null} if the key is not present
     */
    Optional<Object> get(String key);

    /**
     * Typesafe getter for the value represented by the provided key, with cast to given class.
     *
     * @param key  The key to get a value for
     * @param type The class of return type
     * @param <V>  Type of returned value
     * @return The value of given type represented by the given key or {@code null} if the key is not present
     */
    <V> Optional<V> get(String key, Class<V> type);

    /**
     * Typesafe getter for the value represented by the provided key, with cast to given class.
     *
     * @param key          The key to get a value for
     * @param type         The class of return type
     * @param defaultValue Default value in case element is not present
     * @param <V>          Type of returned value
     * @return The value of given type represented by the given key or the default value if the key is not present
     */
    <V> Optional<V> get(String key, Class<V> type, @Nullable V defaultValue);

    /**
     * Indicates whether or not the context is empty.
     *
     * @return True if the context has no entries, false otherwise.
     * @see java.util.Map#isEmpty()
     */
    boolean isEmpty();

    /**
     * Clears the dirty flag.
     */
    void clearDirtyFlag();

    /**
     * Returns the entry set containing the contents of this context.
     *
     * @return An unmodifiable set representing the contents of the context
     * @see java.util.Map#entrySet()
     */
    Set<Map.Entry<String, Object>> entrySet();

    /**
     * Returns the internal map as read-only.
     *
     * @return An unmodifiable map containing all contents.
     * @see java.util.Map
     */
    Map<String, Object> toMap();

    /**
     * Indicates whether or not a key is represented in this context.
     *
     * @param key Key to check existence for
     * @return True if key is represented in context, false otherwise
     * @see java.util.Map#containsKey(Object)
     */
    boolean containsKey(String key);

    /**
     * Removes the mapping for a key from this context if it is present.
     *
     * @param key {@link String} that identifies the entry to be removed from the context.
     * @return the value that was removed from the context.
     *
     * @see java.util.Map#remove(Object)
     */
    Optional<Object> remove(String key);

    /**
     * Indicates whether or not a value is represented in this context.
     *
     * @param value Value to check existence for
     * @return True if value is represented in context, false otherwise
     * @see java.util.Map#containsValue(Object)
     */
    boolean containsValue(Object value);

    /**
     * Returns number of entries in the context
     *
     * @return Number of entries in the context
     * @see java.util.Map#size()
     */
    int size();
}
