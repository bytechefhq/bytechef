/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.grpc.mapper;

import com.bytechef.ee.platform.connection.remote.grpc.ConnectionProto;
import com.bytechef.ee.platform.connection.remote.grpc.ConnectionStatusProto;
import com.bytechef.ee.platform.connection.remote.grpc.ConnectionVisibilityProto;
import com.bytechef.ee.platform.connection.remote.grpc.PlatformTypeProto;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Maps the {@link Connection} domain object to and from its gRPC {@link ConnectionProto}. Hybrid marshalling: stable
 * scalar/enum fields are copied field-by-field; the dynamic {@code parameters} map travels as a
 * {@link com.google.protobuf.Struct}.
 *
 * <p>
 * Enums are mapped by {@link Enum#name()} (robust to ordinal drift), with the proto {@code *_UNSPECIFIED} sentinel used
 * only when a domain value is absent. Note the documented {@code Struct} limitation: JSON/Struct numbers are doubles,
 * so an integral parameter value round-trips as a {@link Double} unless normalized — {@link #structToMap(Struct)}
 * narrows integral doubles back to {@link Long} so common integer parameters survive.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class ConnectionProtoMapper {

    private ConnectionProtoMapper() {
    }

    public static ConnectionProto toProto(Connection connection) {
        ConnectionProto.Builder builder = ConnectionProto.newBuilder()
            .setConnectionVersion(connection.getConnectionVersion())
            .setEnvironment(connection.getEnvironmentId())
            .setVersion(connection.getVersion());

        Long id = connection.getId();

        if (id != null) {
            builder.setId(id);
        }

        String name = connection.getName();

        if (name != null) {
            builder.setName(name);
        }

        String componentName = connection.getComponentName();

        if (componentName != null) {
            builder.setComponentName(componentName);
        }

        ConnectionStatus status = connection.getStatus();

        if (status != null) {
            builder.setStatus(ConnectionStatusProto.valueOf(status.name()));
        }

        ResourceVisibility visibility = connection.getVisibility();

        if (visibility != null) {
            builder.setVisibility(ConnectionVisibilityProto.valueOf(visibility.name()));
        }

        PlatformType type = connection.getType();

        if (type != null) {
            builder.setType(PlatformTypeProto.valueOf(type.name()));
        }

        List<Long> tagIds = connection.getTagIds();

        if (tagIds != null) {
            builder.addAllTagIds(tagIds);
        }

        Map<String, ?> parameters = connection.getParameters();

        if (parameters != null) {
            builder.setParameters(mapToStruct(parameters));
        }

        return builder.build();
    }

    public static Connection toDomain(ConnectionProto proto) {
        Connection connection = new Connection();

        if (proto.getId() != 0) {
            connection.setId(proto.getId());
        }

        connection.setName(proto.getName());
        connection.setComponentName(proto.getComponentName());
        connection.setConnectionVersion(proto.getConnectionVersion());
        connection.setEnvironmentId(proto.getEnvironment());
        connection.setVersion(proto.getVersion());
        connection.setTagIds(new ArrayList<>(proto.getTagIdsList()));

        if (proto.getStatus() != ConnectionStatusProto.CONNECTION_STATUS_UNSPECIFIED) {
            connection.setStatus(ConnectionStatus.valueOf(proto.getStatus()
                .name()));
        }

        if (proto.getVisibility() != ConnectionVisibilityProto.CONNECTION_VISIBILITY_UNSPECIFIED) {
            connection.setVisibility(ResourceVisibility.valueOf(proto.getVisibility()
                .name()));
        }

        if (proto.getType() != PlatformTypeProto.PLATFORM_TYPE_UNSPECIFIED) {
            connection.setType(PlatformType.valueOf(proto.getType()
                .name()));
        }

        if (proto.hasParameters()) {
            connection.setParameters(structToMap(proto.getParameters()));
        }

        return connection;
    }

    static Struct mapToStruct(Map<String, ?> map) {
        Struct.Builder builder = Struct.newBuilder();

        for (Map.Entry<String, ?> entry : map.entrySet()) {
            builder.putFields(entry.getKey(), toValue(entry.getValue()));
        }

        return builder.build();
    }

    static Map<String, Object> structToMap(Struct struct) {
        Map<String, Object> map = new LinkedHashMap<>();

        for (Map.Entry<String, Value> entry : struct.getFieldsMap()
            .entrySet()) {

            map.put(entry.getKey(), fromValue(entry.getValue()));
        }

        return map;
    }

    private static Value toValue(@Nullable Object object) {
        if (object == null) {
            return Value.newBuilder()
                .setNullValue(NullValue.NULL_VALUE)
                .build();
        }

        if (object instanceof String string) {
            return Value.newBuilder()
                .setStringValue(string)
                .build();
        }

        if (object instanceof Boolean bool) {
            return Value.newBuilder()
                .setBoolValue(bool)
                .build();
        }

        if (object instanceof Number number) {
            return Value.newBuilder()
                .setNumberValue(number.doubleValue())
                .build();
        }

        if (object instanceof Map<?, ?> nestedMap) {
            Struct.Builder structBuilder = Struct.newBuilder();

            for (Map.Entry<?, ?> entry : nestedMap.entrySet()) {
                structBuilder.putFields(String.valueOf(entry.getKey()), toValue(entry.getValue()));
            }

            return Value.newBuilder()
                .setStructValue(structBuilder.build())
                .build();
        }

        if (object instanceof Iterable<?> iterable) {
            ListValue.Builder listBuilder = ListValue.newBuilder();

            for (Object element : iterable) {
                listBuilder.addValues(toValue(element));
            }

            return Value.newBuilder()
                .setListValue(listBuilder.build())
                .build();
        }

        return Value.newBuilder()
            .setStringValue(String.valueOf(object))
            .build();
    }

    private static @Nullable Object fromValue(Value value) {
        return switch (value.getKindCase()) {
            case STRING_VALUE -> value.getStringValue();
            case BOOL_VALUE -> value.getBoolValue();
            case NUMBER_VALUE -> narrowNumber(value.getNumberValue());
            case STRUCT_VALUE -> structToMap(value.getStructValue());
            case LIST_VALUE -> {
                List<Object> list = new ArrayList<>();

                for (Value element : value.getListValue()
                    .getValuesList()) {

                    list.add(fromValue(element));
                }

                yield list;
            }
            case NULL_VALUE, KIND_NOT_SET -> null;
        };
    }

    private static Object narrowNumber(double number) {
        if (number == Math.rint(number) && !Double.isInfinite(number)) {
            return (long) number;
        }

        return number;
    }
}
