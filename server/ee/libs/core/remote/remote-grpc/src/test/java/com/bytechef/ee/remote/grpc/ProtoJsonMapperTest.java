/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Int64Value;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProtoJsonMapperTest {

    private final ProtoJsonMapper protoJsonMapper = new ProtoJsonMapper(
        JsonMapper.builder()
            .build());

    @Test
    void testToProtoKeepsLargeIntegersInStructsAsStrings() {
        Struct struct = protoJsonMapper.toProto(
            Map.of("count", 5, "id", 9_007_199_254_740_993L), Struct.newBuilder());

        Map<String, Value> fields = struct.getFieldsMap();

        assertThat(fields.get("count")
            .getNumberValue()).isEqualTo(5.0);
        assertThat(fields.get("id")
            .getStringValue()).isEqualTo("9007199254740993");
    }

    @Test
    void testToProtoKeepsLargeIntegersInInt64Fields() {
        Int64Value int64Value = protoJsonMapper.toProto(9_007_199_254_740_993L, Int64Value.newBuilder());

        assertThat(int64Value.getValue()).isEqualTo(9_007_199_254_740_993L);
    }
}
