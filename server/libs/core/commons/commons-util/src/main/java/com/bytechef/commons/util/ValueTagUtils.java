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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * Encodes and decodes values whose Java type would otherwise be erased by a round trip through untyped JSON, wrapping
 * each such value in a {@code {"@bytechefType": ..., "@bytechefValue": ...}} tag that {@link #untag(Object)}
 * reconstructs. Temporal values are normalized via {@link TemporalValueUtils#normalize(Object)} before being tagged;
 * numeric values are tagged as-is.
 *
 * <p>
 * Payload data can itself be a map of exactly that shape -- an API response whose body happens to use those two field
 * names. Such a map is escaped on write by wrapping it in a {@code MAP} tag, so {@link #untag(Object)} can tell an
 * encoder-written tag from user data that merely looks like one and neither is mistaken for the other.
 *
 * @author Ivica Cardic
 */
public final class ValueTagUtils {

    private static final String TYPE_KEY = "@bytechefType";
    private static final String VALUE_KEY = "@bytechefValue";

    private static final String MAP_TYPE = "MAP";

    private static final Map<String, Function<String, Object>> PARSERS = Map.ofEntries(
        Map.entry("ZONED_DATE_TIME", ZonedDateTime::parse),
        Map.entry("LOCAL_DATE", LocalDate::parse),
        Map.entry("LOCAL_DATE_TIME", LocalDateTime::parse),
        Map.entry("LOCAL_TIME", LocalTime::parse),
        Map.entry("OFFSET_TIME", OffsetTime::parse),
        Map.entry("BIG_DECIMAL", BigDecimal::new),
        Map.entry("BIG_INTEGER", BigInteger::new),
        Map.entry("LONG", Long::parseLong),
        Map.entry("FLOAT", Float::parseFloat),
        Map.entry("SHORT", Short::parseShort),
        Map.entry("BYTE", Byte::parseByte));

    private ValueTagUtils() {
    }

    public static @Nullable Object tag(@Nullable Object value) {
        return tagNormalized(TemporalValueUtils.normalize(value));
    }

    public static @Nullable Object untag(@Nullable Object value) {
        return switch (value) {
            case null -> null;
            case Map<?, ?> map -> untagMap(map);
            case List<?> list -> {
                List<@Nullable Object> untaggedList = new ArrayList<>(list.size());

                for (Object item : list) {
                    untaggedList.add(untag(item));
                }

                yield untaggedList;
            }
            default -> value;
        };
    }

    private static @Nullable Object tagNormalized(@Nullable Object value) {
        return switch (value) {
            case null -> null;
            case ZonedDateTime zonedDateTime -> tagOf("ZONED_DATE_TIME", zonedDateTime.toInstant()
                .toString());
            case LocalDate localDate -> tagOf("LOCAL_DATE", localDate.toString());
            case LocalDateTime localDateTime -> tagOf("LOCAL_DATE_TIME", localDateTime.toString());
            case LocalTime localTime -> tagOf("LOCAL_TIME", localTime.toString());
            case OffsetTime offsetTime -> tagOf("OFFSET_TIME", offsetTime.toString());
            case BigDecimal bigDecimal -> tagOf("BIG_DECIMAL", bigDecimal.toPlainString());
            case BigInteger bigInteger -> tagOf("BIG_INTEGER", bigInteger.toString());
            case Long longValue -> tagOf("LONG", longValue.toString());
            case Float floatValue -> tagOf("FLOAT", floatValue.toString());
            case Short shortValue -> tagOf("SHORT", shortValue.toString());
            case Byte byteValue -> tagOf("BYTE", byteValue.toString());
            case Map<?, ?> map -> {
                Map<String, @Nullable Object> taggedMap = new LinkedHashMap<>();

                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    taggedMap.put(String.valueOf(entry.getKey()), tagNormalized(entry.getValue()));
                }

                yield isReconstructable(taggedMap) ? tagOf(MAP_TYPE, taggedMap) : taggedMap;
            }
            case List<?> list -> {
                List<@Nullable Object> taggedList = new ArrayList<>(list.size());

                for (Object item : list) {
                    taggedList.add(tagNormalized(item));
                }

                yield taggedList;
            }
            default -> value;
        };
    }

    private static Map<String, Object> tagOf(String type, Object value) {
        Map<String, Object> tag = new LinkedHashMap<>();

        tag.put(TYPE_KEY, type);
        tag.put(VALUE_KEY, value);

        return tag;
    }

    private static Object untagMap(Map<?, ?> map) {
        if (isTagShaped(map) && MAP_TYPE.equals(map.get(TYPE_KEY)) && map.get(VALUE_KEY) instanceof Map<?, ?> escaped) {
            return untagEntries(escaped);
        }

        Object reconstructed = reconstruct(map);

        if (reconstructed != null) {
            return reconstructed;
        }

        return untagEntries(map);
    }

    private static Map<String, @Nullable Object> untagEntries(Map<?, ?> map) {
        Map<String, @Nullable Object> untaggedMap = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            untaggedMap.put(String.valueOf(entry.getKey()), untag(entry.getValue()));
        }

        return untaggedMap;
    }

    private static boolean isTagShaped(Map<?, ?> map) {
        return map.size() == 2 && map.containsKey(TYPE_KEY) && map.containsKey(VALUE_KEY);
    }

    /**
     * Whether {@link #untagMap(Map)} would read this map back as something other than a plain map, which is exactly
     * when a map carrying user data of the same shape has to be escaped on write.
     */
    private static boolean isReconstructable(Map<?, ?> map) {
        if (!isTagShaped(map) || !(map.get(TYPE_KEY) instanceof String type)) {
            return false;
        }

        Object value = map.get(VALUE_KEY);

        return (PARSERS.containsKey(type) && value instanceof String) ||
            (MAP_TYPE.equals(type) && value instanceof Map);
    }

    private static @Nullable Object reconstruct(Map<?, ?> map) {
        if (!isTagShaped(map)) {
            return null;
        }

        if (!(map.get(TYPE_KEY) instanceof String type) || !(map.get(VALUE_KEY) instanceof String value)) {
            return null;
        }

        Function<String, Object> parser = PARSERS.get(type);

        if (parser == null) {
            return null;
        }

        try {
            return parser.apply(value);
        } catch (DateTimeParseException | NumberFormatException exception) {
            return null;
        }
    }
}
