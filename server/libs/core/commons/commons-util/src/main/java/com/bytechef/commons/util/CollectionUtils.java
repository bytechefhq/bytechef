
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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Ivica Cardic
 */
public final class CollectionUtils {

    private CollectionUtils() {
    }

    public static <T> List<T> concat(List<T> list1, T item) {
        Objects.requireNonNull(list1, "'list1' must not be null");
        Objects.requireNonNull(item, "'item' must not be null");

        return Stream.concat(list1.stream(), Stream.of(item))
            .toList();
    }

    public static <T> List<T> concat(List<T> list1, List<T> list2) {
        Objects.requireNonNull(list1, "'list1' must not be null");
        Objects.requireNonNull(list2, "'list2' must not be null");

        return Stream.concat(list1.stream(), list2.stream())
            .toList();
    }

    public static <T> List<T> concat(Stream<T> stream1, Stream<T> stream2) {
        Objects.requireNonNull(stream1, "'stream1' must not be null");
        Objects.requireNonNull(stream1, "'stream1' must not be null");

        return Stream.concat(stream1, stream2)
            .toList();
    }

    public static <T> List<T> concatDistinct(List<T> list1, List<T> list2) {
        Objects.requireNonNull(list1, "'list1' must not be null");
        Objects.requireNonNull(list2, "'list2' must not be null");

        return Stream.concat(list1.stream(), list2.stream())
            .distinct()
            .toList();
    }

    public static <T> boolean contains(List<T> list, T item) {
        Objects.requireNonNull(list, "'list' must not be null");

        return list.contains(item);
    }

    public static <T> long count(Iterable<T> iterable) {
        Objects.requireNonNull(iterable, "'iterable' must not be null");

        return StreamSupport.stream(iterable.spliterator(), false)
            .count();
    }

    public static <T> List<T> filter(List<T> list, Predicate<? super T> filter) {
        Objects.requireNonNull(list, "'list' must not be null");
        Objects.requireNonNull(filter, "'filter' must not be null");

        return list.stream()
            .filter(filter)
            .toList();
    }

    public static <T> T findFirstOrElse(Collection<T> list, Predicate<? super T> filter, T elseObject) {
        Objects.requireNonNull(list, "'list' must not be null");
        Objects.requireNonNull(filter, "'filter' must not be null");

        return OptionalUtils.orElse(
            list.stream()
                .filter(filter)
                .findFirst(),
            elseObject);
    }

    public static <T, R> List<? extends R> flatMap(
        List<T> list, Function<? super T, ? extends Collection<? extends R>> mapper) {

        Objects.requireNonNull(list, "'list' must not be null");
        Objects.requireNonNull(mapper, "'mapper' must not be null");

        return list.stream()
            .flatMap(item -> stream(mapper.apply(item)))
            .toList();
    }

    public static <T> T getFirst(Collection<T> list, Predicate<? super T> filter) {
        Objects.requireNonNull(list, "'list' must not be null");
        Objects.requireNonNull(filter, "'filter' must not be null");

        return OptionalUtils.get(
            list.stream()
                .filter(filter)
                .findFirst());
    }

    public static <T, U> U getFirst(
        Collection<T> list, Predicate<? super T> filter, Function<? super T, ? extends U> mapper) {

        Objects.requireNonNull(list, "'list' must not be null");
        Objects.requireNonNull(filter, "'filter' must not be null");
        Objects.requireNonNull(mapper, "'mapper' must not be null");

        return OptionalUtils.get(
            list.stream()
                .filter(filter)
                .map(mapper)
                .findFirst());
    }

    public static <T, R> List<R> map(List<T> list, Function<? super T, R> mapper) {
        Objects.requireNonNull(list, "'list' must not be null");
        Objects.requireNonNull(mapper, "'mapper' must not be null");

        return list.stream()
            .map(mapper)
            .toList();
    }

    public static int size(Collection<?> collection) {
        Objects.requireNonNull(collection, "'collection' must not be null");

        return collection.size();
    }

    public static <T> List<T> sorted(Collection<T> collection, Comparator<? super T> comparator) {
        Objects.requireNonNull(collection, "'collection' must not be null");
        Objects.requireNonNull(comparator, "'comparator' must not be null");

        return collection.stream()
            .sorted(comparator)
            .toList();
    }

    public static <T> Stream<T> stream(Collection<T> collection) {
        Objects.requireNonNull(collection, "'collection' must not be null");

        return collection.stream();
    }

    public static <T> Stream<T> stream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    public static <T> List<T> toList(Iterable<T> iterable) {
        Stream<T> stream = StreamSupport.stream(iterable.spliterator(), false);

        return stream.toList();
    }

    public static <T> List<T> toList(Stream<T> stream) {
        return stream.toList();
    }
}
