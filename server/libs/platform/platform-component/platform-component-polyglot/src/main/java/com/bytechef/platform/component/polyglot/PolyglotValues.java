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
import java.util.Set;
import java.util.function.Function;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyDate;
import org.graalvm.polyglot.proxy.ProxyInstant;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.graalvm.polyglot.proxy.ProxyTime;

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
public final class PolyglotValues {

    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    private static final Set<String> HASH_ACCESS_LANGUAGE_IDS = Set.of("python");

    private PolyglotValues() {
    }

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
            return copyArrayToJavaValue(value);
        } else if (value.hasHashEntries()) {
            return copyHashEntriesToJavaValue(value);
        } else if (value.canExecute()) {
            return toHostFunction(value);
        } else if (value.hasMembers()) {
            return copyMembersToJavaValue(value);
        } else if (value.isProxyObject()) {
            return value.asProxyObject();
        }

        throw new IllegalArgumentException("Cannot copy value %s to java type.".formatted(value));
    }

    private static List<Object> copyArrayToJavaValue(Value value) {
        List<Object> list = new ArrayList<>();

        for (long index = 0; index < value.getArraySize(); index++) {
            list.add(copyToJavaValue(value.getArrayElement(index)));
        }

        return list;
    }

    private static Map<String, Object> copyHashEntriesToJavaValue(Value value) {
        Map<String, Object> map = new HashMap<>();

        Value iterator = value.getHashEntriesIterator();

        while (iterator.hasIteratorNextElement()) {
            Value entry = iterator.getIteratorNextElement();

            map.put(entry.getArrayElement(0)
                .asString(), copyToJavaValue(entry.getArrayElement(1)));
        }

        return map;
    }

    private static Function<Object[], Object> toHostFunction(Value value) {
        return arguments -> copyToJavaValue(value.execute(arguments == null ? new Object[0] : arguments));
    }

    private static Map<String, Object> copyMembersToJavaValue(Value value) {
        Map<String, Object> map = new HashMap<>();

        for (String key : value.getMemberKeys()) {
            map.put(key, copyToJavaValue(value.getMember(key)));
        }

        return map;
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

            return toGuestMap(proxyMap, languageId);
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

            return toGuestMap(proxyMap, languageId);
        } else {
            throw new IllegalArgumentException("Cannot copy value %s to %s type.".formatted(value, languageId));
        }
    }

    public static Object copyToGuestMemberAndHashMap(Map<String, ?> map, String languageId) {
        Map<String, Object> proxyMap = new HashMap<>();

        for (Map.Entry<String, ?> entry : map.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof Map<?, ?> nestedMap) {
                proxyMap.put(entry.getKey(), copyToGuestMemberAndHashMap(toStringKeyMap(nestedMap), languageId));
            } else {
                proxyMap.put(entry.getKey(), copyToGuestValue(value, languageId));
            }
        }

        return new MemberAndHashMapProxy(proxyMap);
    }

    private static Map<String, Object> toStringKeyMap(Map<?, ?> map) {
        Map<String, Object> stringKeyMap = new HashMap<>();

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            stringKeyMap.put((String) entry.getKey(), entry.getValue());
        }

        return stringKeyMap;
    }

    private static Object toGuestMap(Map<String, Object> proxyMap, String languageId) {
        if (HASH_ACCESS_LANGUAGE_IDS.contains(languageId)) {
            return new MemberAndHashMapProxy(proxyMap);
        }

        return ProxyObject.fromMap(proxyMap);
    }
}
