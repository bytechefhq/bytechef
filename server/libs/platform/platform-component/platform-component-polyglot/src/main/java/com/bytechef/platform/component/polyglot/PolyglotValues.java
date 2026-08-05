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

package com.bytechef.platform.component.polyglot;

import com.bytechef.commons.util.ConvertUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyDate;
import org.graalvm.polyglot.proxy.ProxyInstant;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.graalvm.polyglot.proxy.ProxyTime;

/**
 * Marshalling helpers shared by every guest polyglot execution: convert host-native values into guest-facing proxy
 * representations and back.
 *
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
public final class PolyglotValues {

    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    private PolyglotValues() {
    }

    /**
     * Recursively converts data structures originating from a polyglot context into Java-native objects. Handles
     * conversions for common data structures such as maps and lists, while leaving other object types unmodified.
     *
     * @param object the object to be copied and converted from the polyglot context. This may be a map, list, or any
     *               other data type.
     * @return a Java-native representation of the input object. If the input is a map or list, it is recursively copied
     *         and converted. Other objects are returned as is.
     */
    public static Object copyFromPolyglotContext(Object object) {
        switch (object) {
            case null -> {
                return null;
            }
            case Map<?, ?> map -> {
                Map<String, Object> hashMap = new HashMap<>();

                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    hashMap.put((String) entry.getKey(), copyFromPolyglotContext(entry.getValue()));
                }

                return hashMap;
            }
            case List<?> list -> {
                List<Object> arrayList = new ArrayList<>();

                for (Object item : list) {
                    arrayList.add(copyFromPolyglotContext(item));
                }

                return arrayList;
            }
            default -> {
            }
        }

        return object;
    }

    public static Object copyToJavaValue(Value value) {
        if (value == null) {
            return null;
        }

        if (value.isBoolean()) {
            return value.asBoolean();
        } else if (value.isDate()) {
            return value.asDate();
        } else if (value.isHostObject()) {
            return value.asHostObject();
        } else if (value.isInstant()) {
            return value.asInstant();
        } else if (value.isNull()) {
            return null;
        } else if (value.isNumber()) {
            return value.as(Number.class);
        } else if (value.isTime()) {
            return value.asTime();
        } else if (value.isString()) {
            return value.asString();
        } else if (value.hasArrayElements()) {
            return value.as(List.class);
        } else if (value.hasMembers()) {
            return value.as(Map.class);
        } else if (value.isProxyObject()) {
            return value.asProxyObject();
        }

        throw new IllegalArgumentException("Cannot copy value %s to java type.".formatted(value));
    }

    public static Object copyToGuestValue(Object value, String languageId) {
        if (value == null) {
            return null;
        }

        Class<?> valueClass = value.getClass();

        if (valueClass.isArray()) {
            if (value instanceof byte[] bytes) {
                return ENCODER.encodeToString(bytes);
            }

            return ProxyArray.fromArray((Object[]) value);
        } else if (value instanceof Boolean bool) {
            return bool;
        } else if (value instanceof Collection<?> collection) {
            List<Object> proxyList = new ArrayList<>();

            for (Object item : collection) {
                proxyList.add(copyToGuestValue(item, languageId));
            }

            return ProxyArray.fromList(proxyList);
        } else if (value instanceof Date date) {
            return ProxyInstant.from(date.toInstant());
        } else if (value instanceof Instant instant) {
            return ProxyInstant.from(instant);
        } else if (value instanceof LocalDate localDate) {
            return ProxyDate.from(localDate);
        } else if (value instanceof LocalTime localTime) {
            return ProxyTime.from(localTime);
        } else if (value instanceof Map<?, ?> map) {
            Map<String, Object> proxyMap = new HashMap<>();

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                proxyMap.put((String) entry.getKey(), copyToGuestValue(entry.getValue(), languageId));
            }

            return ProxyObject.fromMap(proxyMap);
        } else if (value instanceof Number number) {
            return number;
        } else if (value instanceof String string) {
            return string;
        } else if (ConvertUtils.canConvert(value, Map.class)) {
            Map<String, Object> proxyMap = new HashMap<>();

            Map<?, ?> map = ConvertUtils.convertValue(value, Map.class);

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                proxyMap.put((String) entry.getKey(), copyToGuestValue(entry.getValue(), languageId));
            }

            return ProxyObject.fromMap(proxyMap);
        } else {
            throw new IllegalArgumentException("Cannot copy value %s to %s type.".formatted(value, languageId));
        }
    }
}
