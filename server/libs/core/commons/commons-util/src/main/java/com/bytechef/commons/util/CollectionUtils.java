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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public final class CollectionUtils {

    private CollectionUtils() {
    }

    public static <T> boolean anyMatch(Collection<T> list, Predicate<? super T> predicate) {
        Validate.notNull(list, "'list' must not be null");
        Validate.notNull(predicate, "'predicate' must not be null");

        return list.stream()
            .anyMatch(predicate);
    }

    @SafeVarargs
    public static <T> List<T> concat(List<T> list1, T... items) {
        Validate.notNull(list1, "'list1' must not be null");
        Validate.notNull(items, "'items' must not be null");

        return Stream.concat(list1.stream(), Stream.of(items))
            .toList();
    }

    public static <T> List<T> concat(List<T> list1, List<T> list2) {
        Validate.notNull(list1, "'list1' must not be null");
        Validate.notNull(list2, "'list2' must not be null");

        return Stream.concat(list1.stream(), list2.stream())
            .toList();
    }

    public static <T> List<T> concat(Stream<T> stream1, Stream<T> stream2) {
        Validate.notNull(stream1, "'stream1' must not be null");
        Validate.notNull(stream1, "'stream1' must not be null");

        return Stream.concat(stream1, stream2)
            .toList();
    }

    public static <T> List<T> concatDistinct(List<T> list1, List<T> list2) {
        Validate.notNull(list1, "'list1' must not be null");
        Validate.notNull(list2, "'list2' must not be null");

        return Stream.concat(list1.stream(), list2.stream())
            .distinct()
            .toList();
    }

    public static <T> boolean contains(List<T> list, T item) {
        Validate.notNull(list, "'list' must not be null");

        return list.contains(item);
    }

    public static <T> long count(Iterable<T> iterable) {
        Validate.notNull(iterable, "'iterable' must not be null");

        return StreamSupport.stream(iterable.spliterator(), false)
            .count();
    }

    public static <T> List<T> filter(List<T> list, Predicate<? super T> filter) {
        Validate.notNull(list, "'list' must not be null");
        Validate.notNull(filter, "'filter' must not be null");

        return list.stream()
            .filter(filter)
            .toList();
    }

    public static <T> Optional<T> findFirst(Collection<T> list) {
        return list.stream()
            .findFirst();
    }

    public static <T> Optional<T> findFirst(Collection<T> list, Predicate<? super T> filter) {
        return list.stream()
            .filter(filter)
            .findFirst();
    }

    public static <T> T findFirstOrElse(Collection<T> list, @Nullable T elseObject) {
        Validate.notNull(list, "'list' must not be null");

        return list.stream()
            .findFirst()
            .orElse(elseObject);
    }

    public static <T> T findFirstFilterOrElse(Collection<T> list, Predicate<? super T> filter, T elseObject) {
        Validate.notNull(list, "'list' must not be null");
        Validate.notNull(filter, "'filter' must not be null");

        return list.stream()
            .filter(filter)
            .findFirst()
            .orElse(elseObject);
    }

    public static <T, R> R findFirstMapOrElse(Collection<T> list, Function<? super T, R> mapper, R elseObject) {
        Validate.notNull(list, "'list' must not be null");
        Validate.notNull(mapper, "'mapper' must not be null");

        return list.stream()
            .map(mapper)
            .findFirst()
            .orElse(elseObject);
    }

    public static <T, R> List<R> flatMap(
        List<T> list, Function<? super T, ? extends Collection<R>> mapper) {

        Validate.notNull(list, "'list' must not be null");
        Validate.notNull(mapper, "'mapper' must not be null");

        return list.stream()
            .flatMap(item -> stream(mapper.apply(item)))
            .toList();
    }

    public static <T> T getFirst(Collection<T> collection) {
        return collection.stream()
            .findFirst()
            .orElseThrow();
    }

    public static <T> T getFirst(Collection<T> collection, Predicate<? super T> filter) {
        Validate.notNull(collection, "'collection' must not be null");
        Validate.notNull(filter, "'filter' must not be null");

        return collection.stream()
            .filter(filter)
            .findFirst()
            .orElseThrow();
    }

    public static <T> T getFirst(Collection<T> collection, Predicate<? super T> filter, String exceptionMessage) {
        Validate.notNull(collection, "'collection' must not be null");
        Validate.notNull(filter, "'filter' must not be null");
        Validate.notNull(exceptionMessage, "'exceptionMessage' must not be null");

        return collection.stream()
            .filter(filter)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(exceptionMessage));
    }

    public static <T, U> U getFirstFilter(
        Collection<T> collection, Predicate<? super T> filter, Function<? super T, ? extends U> mapper) {

        Validate.notNull(collection, "'list' must not be null");
        Validate.notNull(filter, "'filter' must not be null");
        Validate.notNull(mapper, "'mapper' must not be null");

        return collection.stream()
            .filter(filter)
            .map(mapper)
            .findFirst()
            .orElseThrow();
    }

    public static <T, U> U getFirstMap(Collection<T> collection, Function<? super T, ? extends U> mapper) {
        Validate.notNull(collection, "'collection' must not be null");
        Validate.notNull(mapper, "'mapper' must not be null");

        return collection.stream()
            .map(mapper)
            .findFirst()
            .orElseThrow();
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static <T, R> List<R> map(List<T> list, Function<? super T, R> mapper) {
        Validate.notNull(list, "'list' must not be null");
        Validate.notNull(mapper, "'mapper' must not be null");

        return new ArrayList<>(list)
            .stream()
            .map(mapper)
            .toList();
    }

    public static <R, T> List<R> map(Set<T> set, Function<? super T, R> mapper) {
        Validate.notNull(set, "'set' must not be null");
        Validate.notNull(mapper, "'mapper' must not be null");

        return new ArrayList<>(set)
            .stream()
            .map(mapper)
            .toList();
    }

    public static <T> boolean noneMatch(Collection<T> list, Predicate<? super T> predicate) {
        Validate.notNull(list, "'list' must not be null");
        Validate.notNull(predicate, "'predicate' must not be null");

        return list.stream()
            .noneMatch(predicate);
    }

    public static int size(Collection<?> collection) {
        Validate.notNull(collection, "'collection' must not be null");

        return collection.size();
    }

    public static <T> List<T> sort(List<T> workflowIds) {
        return workflowIds.stream()
            .sorted()
            .toList();
    }

    public static <T> List<T> sort(Collection<T> collection, Comparator<? super T> comparator) {
        Validate.notNull(collection, "'collection' must not be null");
        Validate.notNull(comparator, "'comparator' must not be null");

        return collection.stream()
            .sorted(comparator)
            .toList();
    }

    public static <T> Stream<T> stream(Collection<T> collection) {
        Validate.notNull(collection, "'collection' must not be null");

        return collection.stream();
    }

    public static <T> Stream<T> stream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    public static List<String> toList(Enumeration<String> enumeration) {
        List<String> list = new ArrayList<>();

        while (enumeration.hasMoreElements()) {
            list.add(enumeration.nextElement());
        }

        return list;
    }

    public static <T> List<T> toList(Iterable<T> iterable) {
        Stream<T> stream = StreamSupport.stream(iterable.spliterator(), false);

        return stream.toList();
    }

    public static <T> List<T> toList(Stream<T> stream) {
        return stream.toList();
    }

    public static String toString(Collection<?> collection) {
        return collection.stream()
            .map(Object::toString)
            .collect(Collectors.joining(", ", "[", "]"));
    }
}
