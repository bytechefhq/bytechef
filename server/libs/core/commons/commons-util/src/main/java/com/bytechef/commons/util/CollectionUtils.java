
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

package com.bytechef.commons.util;

import org.springframework.util.Assert;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Ivica Cardic
 */
public final class CollectionUtils {

    private CollectionUtils() {
    }

    public static <T> List<T> concat(List<T> list1, List<T> list2) {
        Assert.notNull(list1, "'list1' must not be null");
        Assert.notNull(list2, "'list2' must not be null");

        return Stream.concat(list1.stream(), list2.stream())
            .toList();
    }

    public static <T> List<T> concat(List<T> list, Stream<T> stream) {
        Assert.notNull(list, "'list' must not be null");
        Assert.notNull(stream, "'stream' must not be null");

        return Stream.concat(list.stream(), stream)
            .toList();
    }

    public static <K, V> Map<K, V> concat(Map<K, V> map1, Map<K, V> map2) {
        Assert.notNull(map1, "'map1' must not be null");
        Assert.notNull(map2, "'map2' must not be null");

        return Stream.concat(stream(map1), stream(map2))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v2));
    }

    public static <T> List<T> concatDistinct(List<T> list1, List<T> list2) {
        Assert.notNull(list1, "'list1' must not be null");
        Assert.notNull(list2, "'list2' must not be null");

        return Stream.concat(list1.stream(), list2.stream())
            .distinct()
            .toList();
    }

    public static <T> boolean contains(List<T> list, T item) {
        return org.springframework.util.CollectionUtils.contains(list.iterator(), item);
    }

    public static <T> List<T> filter(List<T> list, Predicate<? super T> predicate) {
        return list.stream()
            .filter(predicate)
            .toList();
    }

    public static <T> T findFirst(List<T> list, Predicate<? super T> predicate) {
        return OptionalUtils.get(list.stream()
            .filter(predicate)
            .findFirst());
    }

    public static <T, R> List<R> map(List<T> list, Function<? super T, R> mapper) {
        Assert.notNull(list, "'list' must not be null");

        return list.stream()
            .map(mapper)
            .toList();
    }

    public static int size(Collection<?> collection) {
        Assert.notNull(collection, "'collection' must not be null");

        return collection.size();
    }

    public static <T> Stream<T> stream(Collection<T> collection) {
        Assert.notNull(collection, "'collection' must not be null");

        return collection.stream();
    }

    public static <K, V> Stream<Map.Entry<K, V>> stream(Map<K, V> map) {
        Assert.notNull(map, "'map' must not be null");

        Set<Map.Entry<K, V>> entry = map.entrySet();

        return entry.stream();
    }

    public static <T> List<T> toList(Iterable<T> iterable) {
        Stream<T> stream = StreamSupport.stream(iterable.spliterator(), false);

        return stream.toList();
    }

    public static <T> List<T> toList(Stream<T> stream) {
        return stream.toList();
    }
}
