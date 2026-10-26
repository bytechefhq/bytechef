/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import com.bytechef.commons.util.ConvertUtils;
import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Converts plain Java maps, lists and scalars to protobuf {@link Struct} and {@link Value} and back. Integral numbers
 * come back as {@link Integer} when they fit, else {@link Long}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class ProtoValues {

    private ProtoValues() {
    }

    public static Map<String, Object> toMap(Struct struct) {
        Map<String, Object> map = new HashMap<>();

        for (Map.Entry<String, Value> entry : struct.getFieldsMap()
            .entrySet()) {

            map.put(entry.getKey(), toObject(entry.getValue()));
        }

        return map;
    }

    public static @Nullable Object toObject(Value value) {
        return switch (value.getKindCase()) {
            case BOOL_VALUE -> value.getBoolValue();
            case LIST_VALUE -> toList(value.getListValue());
            case NUMBER_VALUE -> toNumber(value.getNumberValue());
            case STRING_VALUE -> value.getStringValue();
            case STRUCT_VALUE -> toMap(value.getStructValue());
            case NULL_VALUE, KIND_NOT_SET -> null;
        };
    }

    public static Struct toStruct(@Nullable Map<String, ?> map) {
        Struct.Builder builder = Struct.newBuilder();

        if (map != null) {
            for (Map.Entry<String, ?> entry : map.entrySet()) {
                builder.putFields(entry.getKey(), toValue(entry.getValue()));
            }
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    public static Value toValue(@Nullable Object object) {
        Value.Builder builder = Value.newBuilder();

        if (object == null) {
            return builder.setNullValue(NullValue.NULL_VALUE)
                .build();
        }

        if (object instanceof Boolean booleanValue) {
            return builder.setBoolValue(booleanValue)
                .build();
        }

        if (object instanceof Number number) {
            return builder.setNumberValue(number.doubleValue())
                .build();
        }

        if (object instanceof CharSequence charSequence) {
            return builder.setStringValue(charSequence.toString())
                .build();
        }

        if (object instanceof Map<?, ?> map) {
            return builder.setStructValue(toStruct((Map<String, ?>) map))
                .build();
        }

        if (object instanceof Iterable<?> iterable) {
            ListValue.Builder listValueBuilder = ListValue.newBuilder();

            for (Object item : iterable) {
                listValueBuilder.addValues(toValue(item));
            }

            return builder.setListValue(listValueBuilder)
                .build();
        }

        return toValue(ConvertUtils.convertValue(object, Object.class));
    }

    private static List<@Nullable Object> toList(ListValue listValue) {
        List<@Nullable Object> list = new ArrayList<>();

        for (Value value : listValue.getValuesList()) {
            list.add(toObject(value));
        }

        return list;
    }

    private static Number toNumber(double number) {
        if (number == Math.rint(number) && !Double.isInfinite(number) && Math.abs(number) < 0x1p53) {
            long longValue = (long) number;

            if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                return (int) longValue;
            }

            return longValue;
        }

        return number;
    }
}
