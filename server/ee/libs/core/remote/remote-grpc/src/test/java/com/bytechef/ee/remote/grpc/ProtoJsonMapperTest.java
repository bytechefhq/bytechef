/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.MessageOptions;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Int64Value;
import java.util.ArrayList;
import java.util.List;
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
    void testBytesFieldsCarryJackson() throws Descriptors.DescriptorValidationException {
        Descriptors.Descriptor descriptor = holderDescriptor();

        Holder holder = new Holder(
            "parent", Map.of("id", 9_007_199_254_740_993L, "ratio", 5.0),
            List.of(new Holder("child", Map.of("nested", Map.of("count", 5)), List.of(), Map.of(), List.of())),
            Map.of(), List.of());

        DynamicMessage dynamicMessage = protoJsonMapper.toProto(holder, DynamicMessage.newBuilder(descriptor));

        Holder readHolder = protoJsonMapper.toObject(dynamicMessage, Holder.class);

        assertThat(readHolder).isEqualTo(holder);
        assertThat(readHolder.payload()
            .get("id")).isInstanceOf(Long.class);
        assertThat(readHolder.payload()
            .get("ratio")).isInstanceOf(Double.class);
    }

    @Test
    void testMapAndRepeatedBytesCarryJackson() throws Descriptors.DescriptorValidationException {
        List<Object> items = new ArrayList<>();

        items.add(9_007_199_254_740_993L);
        items.add(null);
        items.add(Map.of("ratio", 5.0));

        Holder holder = new Holder(
            "parent", null, List.of(), Map.of("big", 9_007_199_254_740_993L, "nested", Map.of("count", 5)), items);

        DynamicMessage dynamicMessage = protoJsonMapper.toProto(holder, DynamicMessage.newBuilder(holderDescriptor()));

        Holder readHolder = protoJsonMapper.toObject(dynamicMessage, Holder.class);

        assertThat(readHolder.extras()).isEqualTo(holder.extras());
        assertThat(readHolder.extras()
            .get("big")).isInstanceOf(Long.class);
        assertThat(readHolder.items()).isEqualTo(items);
    }

    @Test
    void testUnsetBytesFieldReadsAsNull() throws Descriptors.DescriptorValidationException {
        DynamicMessage dynamicMessage = protoJsonMapper.toProto(
            new Holder("parent", null, List.of(), Map.of(), List.of()), DynamicMessage.newBuilder(holderDescriptor()));

        assertThat(protoJsonMapper.toObject(dynamicMessage, Holder.class)
            .payload()).isNull();
    }

    @Test
    void testToProtoKeepsLargeIntegersInInt64Fields() {
        Int64Value int64Value = protoJsonMapper.toProto(9_007_199_254_740_993L, Int64Value.newBuilder());

        assertThat(int64Value.getValue()).isEqualTo(9_007_199_254_740_993L);
    }

    @Test
    void testMessageWithoutBytesFieldsRoundTrips() throws Descriptors.DescriptorValidationException {
        Node node = new Node(
            "parent", 9_007_199_254_740_993L, List.of(new Node("child", 5L, List.of(), Map.of("key", "value"))),
            Map.of());

        DynamicMessage dynamicMessage = protoJsonMapper.toProto(node, DynamicMessage.newBuilder(nodeDescriptor()));

        Node readNode = protoJsonMapper.toObject(dynamicMessage, Node.class);

        assertThat(readNode).isEqualTo(node);
        assertThat(dynamicMessage.getField(dynamicMessage.getDescriptorForType()
            .findFieldByName("count"))).isEqualTo(9_007_199_254_740_993L);
    }

    private static Descriptors.Descriptor nodeDescriptor() throws Descriptors.DescriptorValidationException {
        DescriptorProto nodeProto = DescriptorProto.newBuilder()
            .setName("Node")
            .addField(
                field("name", 1, FieldDescriptorProto.Type.TYPE_STRING, FieldDescriptorProto.Label.LABEL_OPTIONAL))
            .addField(
                field("count", 2, FieldDescriptorProto.Type.TYPE_INT64, FieldDescriptorProto.Label.LABEL_OPTIONAL))
            .addField(
                field("children", 3, FieldDescriptorProto.Type.TYPE_MESSAGE, FieldDescriptorProto.Label.LABEL_REPEATED)
                    .toBuilder()
                    .setTypeName(".test.Node")
                    .build())
            .addField(
                field("labels", 4, FieldDescriptorProto.Type.TYPE_MESSAGE, FieldDescriptorProto.Label.LABEL_REPEATED)
                    .toBuilder()
                    .setTypeName(".test.Node.LabelsEntry")
                    .build())
            .addNestedType(
                DescriptorProto.newBuilder()
                    .setName("LabelsEntry")
                    .addField(
                        field("key", 1, FieldDescriptorProto.Type.TYPE_STRING,
                            FieldDescriptorProto.Label.LABEL_OPTIONAL))
                    .addField(
                        field("value", 2, FieldDescriptorProto.Type.TYPE_STRING,
                            FieldDescriptorProto.Label.LABEL_OPTIONAL))
                    .setOptions(
                        MessageOptions.newBuilder()
                            .setMapEntry(true)))
            .build();

        FileDescriptorProto fileDescriptorProto = FileDescriptorProto.newBuilder()
            .setName("node.proto")
            .setPackage("test")
            .setSyntax("proto3")
            .addMessageType(nodeProto)
            .build();

        Descriptors.FileDescriptor fileDescriptor = Descriptors.FileDescriptor.buildFrom(
            fileDescriptorProto, new Descriptors.FileDescriptor[0]);

        return fileDescriptor.findMessageTypeByName("Node");
    }

    private static Descriptors.Descriptor holderDescriptor() throws Descriptors.DescriptorValidationException {
        DescriptorProto holderProto = DescriptorProto.newBuilder()
            .setName("Holder")
            .addField(
                field("name", 1, FieldDescriptorProto.Type.TYPE_STRING, FieldDescriptorProto.Label.LABEL_OPTIONAL))
            .addField(
                field("payload", 2, FieldDescriptorProto.Type.TYPE_BYTES, FieldDescriptorProto.Label.LABEL_OPTIONAL))
            .addField(
                field("children", 3, FieldDescriptorProto.Type.TYPE_MESSAGE, FieldDescriptorProto.Label.LABEL_REPEATED)
                    .toBuilder()
                    .setTypeName(".test.Holder")
                    .build())
            .addField(
                field("extras", 4, FieldDescriptorProto.Type.TYPE_MESSAGE, FieldDescriptorProto.Label.LABEL_REPEATED)
                    .toBuilder()
                    .setTypeName(".test.Holder.ExtrasEntry")
                    .build())
            .addField(
                field("items", 5, FieldDescriptorProto.Type.TYPE_BYTES, FieldDescriptorProto.Label.LABEL_REPEATED))
            .addNestedType(
                DescriptorProto.newBuilder()
                    .setName("ExtrasEntry")
                    .addField(
                        field("key", 1, FieldDescriptorProto.Type.TYPE_STRING,
                            FieldDescriptorProto.Label.LABEL_OPTIONAL))
                    .addField(
                        field("value", 2, FieldDescriptorProto.Type.TYPE_BYTES,
                            FieldDescriptorProto.Label.LABEL_OPTIONAL))
                    .setOptions(
                        MessageOptions.newBuilder()
                            .setMapEntry(true)))
            .build();

        FileDescriptorProto fileDescriptorProto = FileDescriptorProto.newBuilder()
            .setName("holder.proto")
            .setPackage("test")
            .setSyntax("proto3")
            .addMessageType(holderProto)
            .build();

        Descriptors.FileDescriptor fileDescriptor = Descriptors.FileDescriptor.buildFrom(
            fileDescriptorProto, new Descriptors.FileDescriptor[0]);

        return fileDescriptor.findMessageTypeByName("Holder");
    }

    private static FieldDescriptorProto field(
        String name, int number, FieldDescriptorProto.Type type, FieldDescriptorProto.Label label) {

        return FieldDescriptorProto.newBuilder()
            .setName(name)
            .setNumber(number)
            .setType(type)
            .setLabel(label)
            .build();
    }

    record Node(String name, long count, List<Node> children, Map<String, String> labels) {
    }

    record Holder(
        String name, Map<String, Object> payload, List<Holder> children, Map<String, Object> extras,
        List<Object> items) {
    }
}
