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

import java.io.InputStream;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
final class JsonParserStream implements Stream<Map<String, ?>> {

    private final JsonParser jsonParser;
    private final Stream<Map<String, ?>> stream;

    public JsonParserStream(InputStream inputStream, ObjectMapper objectMapper) {
        this.jsonParser = new JsonFactory().createParser(ObjectReadContext.empty(), inputStream);
        this.stream = StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(new JsonIterator(jsonParser, objectMapper), Spliterator.ORDERED),
            false);
    }

    @Override
    public Stream<Map<String, ?>> filter(Predicate<? super Map<String, ?>> predicate) {
        return stream.filter(predicate);
    }

    @Override
    public <R> Stream<R> map(Function<? super Map<String, ?>, ? extends R> mapper) {
        return stream.map(mapper);
    }

    @Override
    public IntStream mapToInt(ToIntFunction<? super Map<String, ?>> mapper) {
        return stream.mapToInt(mapper);
    }

    @Override
    public LongStream mapToLong(ToLongFunction<? super Map<String, ?>> mapper) {
        return stream.mapToLong(mapper);
    }

    @Override
    public DoubleStream mapToDouble(ToDoubleFunction<? super Map<String, ?>> mapper) {
        return stream.mapToDouble(mapper);
    }

    @Override
    public <R> Stream<R> flatMap(Function<? super Map<String, ?>, ? extends Stream<? extends R>> mapper) {
        return stream.flatMap(mapper);
    }

    @Override
    public IntStream flatMapToInt(Function<? super Map<String, ?>, ? extends IntStream> mapper) {
        return stream.flatMapToInt(mapper);
    }

    @Override
    public LongStream flatMapToLong(Function<? super Map<String, ?>, ? extends LongStream> mapper) {
        return stream.flatMapToLong(mapper);
    }

    @Override
    public DoubleStream flatMapToDouble(Function<? super Map<String, ?>, ? extends DoubleStream> mapper) {
        return stream.flatMapToDouble(mapper);
    }

    @Override
    public Stream<Map<String, ?>> distinct() {
        return stream.distinct();
    }

    @Override
    public Stream<Map<String, ?>> sorted() {
        return stream.sorted();
    }

    @Override
    public Stream<Map<String, ?>> sorted(Comparator<? super Map<String, ?>> comparator) {
        return stream.sorted(comparator);
    }

    @Override
    public Stream<Map<String, ?>> peek(Consumer<? super Map<String, ?>> action) {
        return stream.peek(action);
    }

    @Override
    public Stream<Map<String, ?>> limit(long maxSize) {
        return stream.limit(maxSize);
    }

    @Override
    public Stream<Map<String, ?>> skip(long n) {
        return stream.skip(n);
    }

    @Override
    public void forEach(Consumer<? super Map<String, ?>> action) {
        stream.forEach(action);
    }

    @Override
    public void forEachOrdered(Consumer<? super Map<String, ?>> action) {
        stream.forEachOrdered(action);
    }

    @Override
    public Object[] toArray() {
        return stream.toArray();
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        return stream.toArray(generator);
    }

    @Override
    public Map<String, ?> reduce(Map<String, ?> identity, BinaryOperator<Map<String, ?>> accumulator) {
        return stream.reduce(identity, accumulator);
    }

    @Override
    public Optional<Map<String, ?>> reduce(BinaryOperator<Map<String, ?>> accumulator) {
        return stream.reduce(accumulator);
    }

    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super Map<String, ?>, U> accumulator, BinaryOperator<U> combiner) {
        return stream.reduce(identity, accumulator, combiner);
    }

    @Override
    public <R> R collect(
        Supplier<R> supplier, BiConsumer<R, ? super Map<String, ?>> accumulator, BiConsumer<R, R> combiner) {
        return stream.collect(supplier, accumulator, combiner);
    }

    @Override
    public <R, A> R collect(Collector<? super Map<String, ?>, A, R> collector) {
        return stream.collect(collector);
    }

    @Override
    public Optional<Map<String, ?>> min(Comparator<? super Map<String, ?>> comparator) {
        return stream.min(comparator);
    }

    @Override
    public Optional<Map<String, ?>> max(Comparator<? super Map<String, ?>> comparator) {
        return stream.max(comparator);
    }

    @Override
    public long count() {
        return stream.count();
    }

    @Override
    public boolean anyMatch(Predicate<? super Map<String, ?>> predicate) {
        return stream.anyMatch(predicate);
    }

    @Override
    public boolean allMatch(Predicate<? super Map<String, ?>> predicate) {
        return stream.allMatch(predicate);
    }

    @Override
    public boolean noneMatch(Predicate<? super Map<String, ?>> predicate) {
        return stream.noneMatch(predicate);
    }

    @Override
    public Optional<Map<String, ?>> findFirst() {
        return stream.findFirst();
    }

    @Override
    public Optional<Map<String, ?>> findAny() {
        return stream.findAny();
    }

    @Override
    public Iterator<Map<String, ?>> iterator() {
        return stream.iterator();
    }

    @Override
    public Spliterator<Map<String, ?>> spliterator() {
        return stream.spliterator();
    }

    @Override
    public boolean isParallel() {
        return stream.isParallel();
    }

    @Override
    public Stream<Map<String, ?>> sequential() {
        return stream.sequential();
    }

    @Override
    public Stream<Map<String, ?>> parallel() {
        return stream.parallel();
    }

    @Override
    public Stream<Map<String, ?>> unordered() {
        return stream.unordered();
    }

    @Override
    public Stream<Map<String, ?>> onClose(Runnable closeHandler) {
        return stream.onClose(closeHandler);
    }

    @Override
    public void close() {
        if (jsonParser != null) {
            jsonParser.close();
        }
    }
}
