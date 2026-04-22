/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.otlp.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.ai.gateway.otlp.dto.OtelGenAiSpan;
import com.bytechef.ee.platform.ai.gateway.otlp.dto.OtelSpanAttributes;
import com.bytechef.ee.platform.ai.gateway.otlp.dto.OtelSpanBatch;
import com.bytechef.ee.platform.ai.gateway.otlp.dto.OtelSpanStatus;
import com.google.protobuf.ByteString;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 * @version ee
 */
class OtlpProtobufMapperTest {

    private static final HexFormat HEX = HexFormat.of();

    /** Canonical OTel trace-id length: 16 bytes. */
    private static final int TRACE_ID_BYTES = 16;

    /** Canonical OTel span-id length: 8 bytes. */
    private static final int SPAN_ID_BYTES = 8;

    private final OtlpProtobufMapper mapper = new OtlpProtobufMapperImpl();

    /**
     * Produces a canonical 16-byte protobuf trace id by padding the seed bytes with trailing zeros (or truncating if
     * the seed is longer). The mapper is the only thing under test; padding lets each test stay readable (seed =
     * "trace-aaaa") while satisfying the OTel canonical 16-byte width that the mapper now validates.
     */
    private static ByteString traceIdBytes(String seed) {
        return padded(seed, TRACE_ID_BYTES);
    }

    /** Same idea as {@link #traceIdBytes} but for the canonical 8-byte span id width. */
    private static ByteString spanIdBytes(String seed) {
        return padded(seed, SPAN_ID_BYTES);
    }

    private static ByteString padded(String seed, int width) {
        byte[] seedBytes = seed.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[width];

        System.arraycopy(seedBytes, 0, result, 0, Math.min(seedBytes.length, width));

        return ByteString.copyFrom(result);
    }

    private static String hex(String seed) {
        return HEX.formatHex(padded(seed, TRACE_ID_BYTES).toByteArray());
    }

    private static String spanIdHex(String seed) {
        return HEX.formatHex(padded(seed, SPAN_ID_BYTES).toByteArray());
    }

    @Test
    void testMapsChatCompletionSpan() {
        ExportTraceServiceRequest request = requestWithOneGenAiSpan();

        OtelSpanBatch batch = mapper.toBatch(request);

        assertThat(batch.size()).isEqualTo(1);

        OtelGenAiSpan span = batch.spans()
            .getFirst();

        assertThat(span.systemAttr()).isEqualTo("openai");
        assertThat(span.requestModelAttr()).isEqualTo("gpt-4o");
        assertThat(span.inputTokensAttr()).isEqualTo(100);
        assertThat(span.outputTokensAttr()).isEqualTo(42);
        assertThat(span.status()).isEqualTo(OtelSpanStatus.OK);
    }

    @Test
    void testSkipsSpansWithoutGenAiSystem() {
        ExportTraceServiceRequest request = ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(ResourceSpans.newBuilder()
                .addScopeSpans(ScopeSpans.newBuilder()
                    .addSpans(Span.newBuilder()
                        .setTraceId(traceIdBytes("t1234567890abcdef"))
                        .setSpanId(spanIdBytes("s1234567"))
                        .setName("db.query")
                        .setStartTimeUnixNano(1_000_000L)
                        .setEndTimeUnixNano(2_000_000L)))
                .build())
            .build();

        OtelSpanBatch batch = mapper.toBatch(request);

        assertThat(batch.isEmpty()).isTrue();
    }

    @Test
    void testMapsMultipleResourceSpans() {
        // Two ResourceSpans, one gen_ai span each — verifies the outer for-loop visits all of them.
        ExportTraceServiceRequest request = ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(resourceSpansWithSingleGenAiSpan("trace-aaaa", "span-aaaa"))
            .addResourceSpans(resourceSpansWithSingleGenAiSpan("trace-bbbb", "span-bbbb"))
            .build();

        OtelSpanBatch batch = mapper.toBatch(request);

        assertThat(batch.size()).isEqualTo(2);
        assertThat(batch.spans()
            .get(0)
            .traceIdHex()).isEqualTo(hex("trace-aaaa"));
        assertThat(batch.spans()
            .get(1)
            .traceIdHex()).isEqualTo(hex("trace-bbbb"));
    }

    @Test
    void testMapsMultipleScopeSpansPerResource() {
        // Single ResourceSpans containing two ScopeSpans each with one gen_ai span — verifies the
        // middle for-loop visits all ScopeSpans.
        ExportTraceServiceRequest request = ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(ResourceSpans.newBuilder()
                .setResource(Resource.newBuilder()
                    .addAttributes(keyValue("service.name", "chat-service")))
                .addScopeSpans(scopeSpansWithGenAiSpan("trace-cccc", "span-c1"))
                .addScopeSpans(scopeSpansWithGenAiSpan("trace-cccc", "span-c2"))
                .build())
            .build();

        OtelSpanBatch batch = mapper.toBatch(request);

        assertThat(batch.size()).isEqualTo(2);
        assertThat(batch.spans()
            .get(0)
            .spanIdHex()).isEqualTo(spanIdHex("span-c1"));
        assertThat(batch.spans()
            .get(1)
            .spanIdHex()).isEqualTo(spanIdHex("span-c2"));
    }

    @Test
    void testMapsErrorStatusCode() {
        ExportTraceServiceRequest request = ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(ResourceSpans.newBuilder()
                .addScopeSpans(ScopeSpans.newBuilder()
                    .addSpans(Span.newBuilder()
                        .setTraceId(traceIdBytes("trace-err"))
                        .setSpanId(spanIdBytes("span-err"))
                        .setName("chat")
                        .setStartTimeUnixNano(1_000_000_000L)
                        .setEndTimeUnixNano(2_000_000_000L)
                        .setStatus(Status.newBuilder()
                            .setCode(Status.StatusCode.STATUS_CODE_ERROR))
                        .addAttributes(keyValue("gen_ai.system", "openai"))))
                .build())
            .build();

        OtelSpanBatch batch = mapper.toBatch(request);

        assertThat(batch.size()).isEqualTo(1);
        assertThat(batch.spans()
            .getFirst()
            .status()).isEqualTo(OtelSpanStatus.ERROR);
    }

    @Test
    void testExposesBothRequestAndResponseModelIndependently() {
        // gen_ai.request.model and gen_ai.response.model are independent accessors. The facade picks
        // one over the other (response wins), but the mapper must surface both faithfully so a future
        // consumer can branch differently.
        ExportTraceServiceRequest request = ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(ResourceSpans.newBuilder()
                .addScopeSpans(ScopeSpans.newBuilder()
                    .addSpans(Span.newBuilder()
                        .setTraceId(traceIdBytes("trace-dd"))
                        .setSpanId(spanIdBytes("span-dd"))
                        .setName("chat")
                        .setStartTimeUnixNano(1_000_000_000L)
                        .setEndTimeUnixNano(2_000_000_000L)
                        .addAttributes(keyValue("gen_ai.system", "openai"))
                        .addAttributes(keyValue("gen_ai.request.model", "gpt-4o"))
                        .addAttributes(keyValue("gen_ai.response.model", "gpt-4o-mini"))))
                .build())
            .build();

        OtelSpanBatch batch = mapper.toBatch(request);

        OtelGenAiSpan span = batch.spans()
            .getFirst();

        assertThat(span.requestModelAttr()).isEqualTo("gpt-4o");
        assertThat(span.responseModelAttr()).isEqualTo("gpt-4o-mini");
    }

    @Test
    void testPropagatesParentSpanId() {
        ExportTraceServiceRequest request = ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(ResourceSpans.newBuilder()
                .addScopeSpans(ScopeSpans.newBuilder()
                    .addSpans(Span.newBuilder()
                        .setTraceId(traceIdBytes("trace-pp"))
                        .setSpanId(spanIdBytes("span-pp"))
                        .setParentSpanId(spanIdBytes("parent-pp"))
                        .setName("chat")
                        .setStartTimeUnixNano(1_000_000_000L)
                        .setEndTimeUnixNano(2_000_000_000L)
                        .addAttributes(keyValue("gen_ai.system", "openai"))))
                .build())
            .build();

        OtelSpanBatch batch = mapper.toBatch(request);

        assertThat(batch.spans()
            .getFirst()
            .parentSpanIdHex()).isEqualTo(spanIdHex("parent-pp"));
    }

    @Test
    void testEmptyExportRequestProducesEmptyBatch() {
        ExportTraceServiceRequest request = ExportTraceServiceRequest.newBuilder()
            .build();

        OtelSpanBatch batch = mapper.toBatch(request);

        assertThat(batch.isEmpty()).isTrue();
        assertThat(batch.droppedSpans()).isZero();
    }

    @Test
    void testEncodesBinaryTraceAndSpanIdsAsHex() {
        // OTel spec requires 16-byte trace ids and 8-byte span ids; the bytes are commonly random and
        // almost never valid UTF-8. Without canonical hex encoding, two distinct binary ids that share a
        // UTF-8 representation (because of U+FFFD replacement on invalid continuation bytes) silently
        // collide on the database unique index.
        byte[] traceIdBytes = new byte[] {
            (byte) 0xFF, (byte) 0xFE, (byte) 0x00, (byte) 0x01, (byte) 0xAA, (byte) 0xBB, (byte) 0xCC,
            (byte) 0xDD, (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x66,
            (byte) 0x77, (byte) 0x88
        };
        byte[] spanIdBytes = new byte[] {
            (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78
        };

        ExportTraceServiceRequest request = ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(ResourceSpans.newBuilder()
                .addScopeSpans(ScopeSpans.newBuilder()
                    .addSpans(Span.newBuilder()
                        .setTraceId(ByteString.copyFrom(traceIdBytes))
                        .setSpanId(ByteString.copyFrom(spanIdBytes))
                        .setName("chat")
                        .setStartTimeUnixNano(1_000_000_000L)
                        .setEndTimeUnixNano(2_000_000_000L)
                        .addAttributes(keyValue("gen_ai.system", "openai"))))
                .build())
            .build();

        OtelSpanBatch batch = mapper.toBatch(request);

        assertThat(batch.spans()
            .getFirst()
            .traceIdHex()).isEqualTo(HEX.formatHex(traceIdBytes));
        assertThat(batch.spans()
            .getFirst()
            .spanIdHex()).isEqualTo(HEX.formatHex(spanIdBytes));
    }

    @Test
    void testUnwrapsArrayKvListBytesAndUnsetAttributes() {
        // Without recursive unwrap dispatch, ARRAY_VALUE / KVLIST_VALUE / BYTES_VALUE / VALUE_NOT_SET all
        // collapse to AnyValue.toString(), producing misleading attribute values that downstream parsers
        // silently mishandle. This test pins the per-variant Java type mapping so a regression collapsing
        // them again is caught.
        ExportTraceServiceRequest request = ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(ResourceSpans.newBuilder()
                .addScopeSpans(ScopeSpans.newBuilder()
                    .addSpans(Span.newBuilder()
                        .setTraceId(traceIdBytes("trace-mix"))
                        .setSpanId(spanIdBytes("span-mix"))
                        .setName("chat")
                        .setStartTimeUnixNano(1_000_000_000L)
                        .setEndTimeUnixNano(2_000_000_000L)
                        .addAttributes(keyValue("gen_ai.system", "openai"))
                        .addAttributes(KeyValue.newBuilder()
                            .setKey("custom.array")
                            .setValue(AnyValue.newBuilder()
                                .setArrayValue(io.opentelemetry.proto.common.v1.ArrayValue.newBuilder()
                                    .addValues(AnyValue.newBuilder()
                                        .setStringValue("a"))
                                    .addValues(AnyValue.newBuilder()
                                        .setIntValue(7L))))
                            .build())
                        .addAttributes(KeyValue.newBuilder()
                            .setKey("custom.kvlist")
                            .setValue(AnyValue.newBuilder()
                                .setKvlistValue(io.opentelemetry.proto.common.v1.KeyValueList.newBuilder()
                                    .addValues(keyValue("nested", "ok"))))
                            .build())
                        .addAttributes(KeyValue.newBuilder()
                            .setKey("custom.bytes")
                            .setValue(AnyValue.newBuilder()
                                .setBytesValue(ByteString.copyFrom(new byte[] {
                                    (byte) 0xCA, (byte) 0xFE
                                })))
                            .build())
                        .addAttributes(KeyValue.newBuilder()
                            .setKey("custom.unset")
                            .setValue(AnyValue.getDefaultInstance())
                            .build())))
                .build())
            .build();

        OtelGenAiSpan span = mapper.toBatch(request)
            .spans()
            .getFirst();

        assertThat(span.attributes()
            .get("custom.array")).isEqualTo(java.util.List.of("a", 7L));
        assertThat(span.attributes()
            .get("custom.kvlist")).isEqualTo(java.util.Map.of("nested", "ok"));
        assertThat(span.attributes()
            .get("custom.bytes")).isEqualTo("cafe");
        // VALUE_NOT_SET drops the attribute entirely at the top-level kvList. (Inside an ARRAY_VALUE the
        // sentinel is preserved instead — see testArrayPreservesPositionalIndicesForUnsetSlots — because list
        // semantics require positional stability.)
        assertThat(span.attributes()
            .values()).doesNotContainKey("custom.unset");
    }

    @Test
    void testArrayPreservesPositionalIndicesForUnsetSlots() {
        // Pin the array-index-stability invariant: VALUE_NOT_SET slots inside an ARRAY_VALUE must be preserved
        // as the OtelSpanAttributes.UNSET_ATTRIBUTE_VALUE sentinel rather than dropped, otherwise downstream
        // consumers reading {@code gen_ai.input.messages[3]} after a single missing earlier entry would
        // silently get the wrong element.
        ExportTraceServiceRequest request = ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(ResourceSpans.newBuilder()
                .addScopeSpans(ScopeSpans.newBuilder()
                    .addSpans(Span.newBuilder()
                        .setTraceId(traceIdBytes("trace-arr"))
                        .setSpanId(spanIdBytes("span-arr"))
                        .setName("chat")
                        .setStartTimeUnixNano(1_000_000_000L)
                        .setEndTimeUnixNano(2_000_000_000L)
                        .addAttributes(keyValue("gen_ai.system", "openai"))
                        .addAttributes(KeyValue.newBuilder()
                            .setKey("gen_ai.input.messages")
                            .setValue(AnyValue.newBuilder()
                                .setArrayValue(io.opentelemetry.proto.common.v1.ArrayValue.newBuilder()
                                    .addValues(AnyValue.newBuilder()
                                        .setStringValue("first"))
                                    .addValues(AnyValue.newBuilder()
                                        .setStringValue("second"))
                                    .addValues(AnyValue.getDefaultInstance())
                                    .addValues(AnyValue.newBuilder()
                                        .setStringValue("fourth"))))
                            .build())))
                .build())
            .build();

        OtelGenAiSpan span = mapper.toBatch(request)
            .spans()
            .getFirst();

        java.util.List<?> messages = (java.util.List<?>) span.attributes()
            .get("gen_ai.input.messages");

        assertThat(messages).hasSize(4);
        assertThat(messages.get(0)).isEqualTo("first");
        assertThat(messages.get(1)).isEqualTo("second");
        assertThat(messages.get(2)).isSameAs(OtelSpanAttributes.UNSET_ATTRIBUTE_VALUE);
        assertThat(messages.get(3)).isEqualTo("fourth");
    }

    @Test
    void testRecursivelyUnwrapsNestedArraysAndKvLists() {
        // Pins the recursion depth of the unwrap() switch. The depth-1 test
        // (testUnwrapsArrayKvListBytesAndUnsetAttributes) covers each variant individually, but without this
        // depth-2/3 case a regression returning {@code anyValue.toString()} from the inner case branches
        // would still pass — the outer ARRAY_VALUE / KVLIST_VALUE branches would call unwrap() on nested
        // values and stringify them rather
        // than recursing properly. Constructs ARRAY-of-KVLIST and KVLIST-of-ARRAY shapes (depth 2) and
        // asserts the resulting Java types remain typed all the way down.
        ExportTraceServiceRequest request = ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(ResourceSpans.newBuilder()
                .addScopeSpans(ScopeSpans.newBuilder()
                    .addSpans(Span.newBuilder()
                        .setTraceId(traceIdBytes("trace-deep"))
                        .setSpanId(spanIdBytes("span-deep"))
                        .setName("chat")
                        .setStartTimeUnixNano(1_000_000_000L)
                        .setEndTimeUnixNano(2_000_000_000L)
                        .addAttributes(keyValue("gen_ai.system", "openai"))
                        // ARRAY_VALUE containing a KVLIST_VALUE element
                        .addAttributes(KeyValue.newBuilder()
                            .setKey("custom.array_of_kvlist")
                            .setValue(AnyValue.newBuilder()
                                .setArrayValue(io.opentelemetry.proto.common.v1.ArrayValue.newBuilder()
                                    .addValues(AnyValue.newBuilder()
                                        .setKvlistValue(io.opentelemetry.proto.common.v1.KeyValueList.newBuilder()
                                            .addValues(keyValue("inner", "value"))))))
                            .build())
                        // KVLIST_VALUE containing an ARRAY_VALUE element
                        .addAttributes(KeyValue.newBuilder()
                            .setKey("custom.kvlist_of_array")
                            .setValue(AnyValue.newBuilder()
                                .setKvlistValue(io.opentelemetry.proto.common.v1.KeyValueList.newBuilder()
                                    .addValues(KeyValue.newBuilder()
                                        .setKey("nested_array")
                                        .setValue(AnyValue.newBuilder()
                                            .setArrayValue(io.opentelemetry.proto.common.v1.ArrayValue.newBuilder()
                                                .addValues(AnyValue.newBuilder()
                                                    .setStringValue("a"))
                                                .addValues(AnyValue.newBuilder()
                                                    .setIntValue(7L)))))))
                            .build())))
                .build())
            .build();

        OtelGenAiSpan span = mapper.toBatch(request)
            .spans()
            .getFirst();

        // ARRAY-of-KVLIST: outer is List, the only element is the typed inner Map (NOT a stringified shape).
        assertThat(span.attributes()
            .get("custom.array_of_kvlist"))
                .isEqualTo(java.util.List.of(java.util.Map.of("inner", "value")));

        // KVLIST-of-ARRAY: outer is Map, the value of "nested_array" is the typed inner List (NOT a string).
        assertThat(span.attributes()
            .get("custom.kvlist_of_array"))
                .isEqualTo(java.util.Map.of("nested_array", java.util.List.of("a", 7L)));
    }

    @Test
    void testRecursivelyUnwrapsKvListOfArrayOfBytes() {
        // Pins the depth-3 KvList(Array(Bytes)) shape. Without recursive BYTES unwrap, an ARRAY_VALUE branch
        // that calls anyValue.toString() on its elements silently corrupts every BYTES leaf into the AnyValue
        // toString form ("bytes_value: \"...\"") rather than a hex/base16 representation of the bytes
        // themselves. The depth-2 testRecursivelyUnwrapsNestedArraysAndKvLists asserts ARRAY-of-KVLIST and
        // KVLIST-of-ARRAY but does not
        // exercise a BYTES leaf — this is the missing assertion that the recursive unwrap reaches the canonical
        // hex form for protobuf Bytes attributes embedded deep in nested kv-list / array attribute shapes.
        byte[] payload = {
            0x01, 0x02, 0x03, 0x04
        };

        ExportTraceServiceRequest request = ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(ResourceSpans.newBuilder()
                .addScopeSpans(ScopeSpans.newBuilder()
                    .addSpans(Span.newBuilder()
                        .setTraceId(traceIdBytes("trace-bytes"))
                        .setSpanId(spanIdBytes("span-bytes"))
                        .setName("chat")
                        .setStartTimeUnixNano(1_000_000_000L)
                        .setEndTimeUnixNano(2_000_000_000L)
                        .addAttributes(keyValue("gen_ai.system", "openai"))
                        .addAttributes(KeyValue.newBuilder()
                            .setKey("custom.kvlist_of_array_of_bytes")
                            .setValue(AnyValue.newBuilder()
                                .setKvlistValue(io.opentelemetry.proto.common.v1.KeyValueList.newBuilder()
                                    .addValues(KeyValue.newBuilder()
                                        .setKey("payloads")
                                        .setValue(AnyValue.newBuilder()
                                            .setArrayValue(io.opentelemetry.proto.common.v1.ArrayValue.newBuilder()
                                                .addValues(AnyValue.newBuilder()
                                                    .setBytesValue(ByteString.copyFrom(payload))))))))
                            .build())))
                .build())
            .build();

        OtelGenAiSpan span = mapper.toBatch(request)
            .spans()
            .getFirst();

        Object outerValue = span.attributes()
            .get("custom.kvlist_of_array_of_bytes");

        assertThat(outerValue).isInstanceOf(java.util.Map.class);

        java.util.Map<?, ?> outerMap = (java.util.Map<?, ?>) outerValue;
        Object payloadsValue = outerMap.get("payloads");

        assertThat(payloadsValue).isInstanceOf(java.util.List.class);

        java.util.List<?> payloads = (java.util.List<?>) payloadsValue;

        assertThat(payloads).hasSize(1);

        // The leaf must be the canonical hex form ("01020304") of the underlying bytes — NOT the AnyValue
        // toString() output (which would look like "bytes_value: \"...\"" with U+FFFD replacement chars).
        assertThat(payloads.getFirst()).isEqualTo(HEX.formatHex(payload));
    }

    @Test
    void testRejectsAttributeUnwrapDeeperThanMaxDepth() {
        // Defense against malformed exporters that submit pathologically nested KVLIST chains. The current
        // MAX_UNWRAP_DEPTH = 32 ceiling exists because protobuf-java's parse-time recursion limit (default 100)
        // does not bound post-parse traversal — an attacker could craft a depth-200 nested AnyValue and the
        // unwrap recursion would blow the stack mid-batch, aborting peer spans. A regression that loosens the
        // cap (or removes the depth check entirely) re-enables this DoS vector. We construct a depth-33 nested
        // KVLIST chain plus a known-good span as ballast and assert: (a) the bad span lands in rejectedSpans,
        // (b) the good span survives in spans (proving per-span boundary holds), (c) total accepted/rejected
        // counts stay self-consistent.
        AnyValue.Builder deeplyNested = AnyValue.newBuilder()
            .setStringValue("leaf");

        // Wrap 33 layers of KVLIST around the leaf. MAX_UNWRAP_DEPTH = 32, so depth 33 must trip the guard.
        for (int depth = 0; depth < 33; depth++) {
            deeplyNested = AnyValue.newBuilder()
                .setKvlistValue(io.opentelemetry.proto.common.v1.KeyValueList.newBuilder()
                    .addValues(KeyValue.newBuilder()
                        .setKey("level_" + depth)
                        .setValue(deeplyNested)));
        }

        ExportTraceServiceRequest request = ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(ResourceSpans.newBuilder()
                .addScopeSpans(ScopeSpans.newBuilder()
                    .addSpans(Span.newBuilder()
                        .setTraceId(traceIdBytes("trace-deep"))
                        .setSpanId(spanIdBytes("span-deep"))
                        .setName("chat")
                        .setStartTimeUnixNano(1_000_000_000L)
                        .setEndTimeUnixNano(2_000_000_000L)
                        .addAttributes(keyValue("gen_ai.system", "openai"))
                        .addAttributes(KeyValue.newBuilder()
                            .setKey("custom.deep")
                            .setValue(deeplyNested)
                            .build()))
                    .addSpans(Span.newBuilder()
                        .setTraceId(traceIdBytes("trace-good"))
                        .setSpanId(spanIdBytes("span-good"))
                        .setName("chat")
                        .setStartTimeUnixNano(1_000_000_000L)
                        .setEndTimeUnixNano(2_000_000_000L)
                        .addAttributes(keyValue("gen_ai.system", "openai")))))
            .build();

        OtelSpanBatch batch = mapper.toBatch(request);

        // The deeply-nested span MUST land in rejectedSpans, NOT abort the batch.
        assertThat(batch.rejectedSpans()).isEqualTo(1);

        // The peer span MUST survive — partial-success contract holds at the per-span boundary.
        assertThat(batch.size()).isEqualTo(1);
        assertThat(batch.spans()
            .getFirst()
            .spanIdHex()).isEqualTo(spanIdHex("span-good"));
    }

    @Test
    void testDroppedSpansCounted() {
        ExportTraceServiceRequest request = ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(ResourceSpans.newBuilder()
                .addScopeSpans(ScopeSpans.newBuilder()
                    .addSpans(Span.newBuilder()
                        .setTraceId(traceIdBytes("t"))
                        .setSpanId(spanIdBytes("s"))
                        .setName("db.query")
                        .setStartTimeUnixNano(1L)
                        .setEndTimeUnixNano(2L))))
            .build();

        OtelSpanBatch batch = mapper.toBatch(request);

        assertThat(batch.isEmpty()).isTrue();
        assertThat(batch.droppedSpans()).isEqualTo(1);
    }

    @Test
    void testRejectsMalformedSpanIdAndCountsRejection() {
        // 7-byte span id violates the canonical 8-byte / 16-hex-char OTel width that
        // OtelGenAiSpan's compact constructor enforces. A regression that removes the per-span
        // try/catch in OtlpProtobufMapperImpl would let this single malformed span abort the
        // whole batch upstream of the ingest facade's per-span boundary — invisible to the
        // facade tests because they only ever see post-mapper OtelGenAiSpan records.
        ByteString malformedSpanId = ByteString.copyFrom(new byte[SPAN_ID_BYTES - 1]);

        ExportTraceServiceRequest request = ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(ResourceSpans.newBuilder()
                .addScopeSpans(ScopeSpans.newBuilder()
                    .addSpans(Span.newBuilder()
                        .setTraceId(traceIdBytes("trace-bad"))
                        .setSpanId(malformedSpanId)
                        .setName("chat")
                        .setStartTimeUnixNano(1_000_000_000L)
                        .setEndTimeUnixNano(2_000_000_000L)
                        .addAttributes(keyValue("gen_ai.system", "openai")))
                    .addSpans(Span.newBuilder()
                        .setTraceId(traceIdBytes("trace-good"))
                        .setSpanId(spanIdBytes("span-good"))
                        .setName("chat")
                        .setStartTimeUnixNano(1_000_000_000L)
                        .setEndTimeUnixNano(2_000_000_000L)
                        .addAttributes(keyValue("gen_ai.system", "openai")))))
            .build();

        OtelSpanBatch batch = mapper.toBatch(request);

        assertThat(batch.size()).isEqualTo(1);
        assertThat(batch.droppedSpans()).isZero();
        assertThat(batch.rejectedSpans()).isEqualTo(1);
        assertThat(batch.spans()
            .getFirst()
            .spanIdHex()).isEqualTo(spanIdHex("span-good"));
    }

    @Test
    void testRejectsInvertedTimestampsAndCountsRejection() {
        // Pins the per-span boundary for the timestamp-ordering validation in OtelGenAiSpan's compact
        // constructor (endTime must not be before startTime). Different validation rule than the malformed
        // spanId case but the same partial-success requirement: one bad span must not abort siblings.
        // Together with testRejectsMalformedSpanIdAndCountsRejection, this exercises both major branches of
        // the constructor's validation that the mapper-side try/catch is meant to absorb.
        ExportTraceServiceRequest request = ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(ResourceSpans.newBuilder()
                .addScopeSpans(ScopeSpans.newBuilder()
                    .addSpans(Span.newBuilder()
                        .setTraceId(traceIdBytes("trace-inverted"))
                        .setSpanId(spanIdBytes("span-inv"))
                        .setName("chat")
                        .setStartTimeUnixNano(2_000_000_000L)
                        .setEndTimeUnixNano(1_000_000_000L)
                        .addAttributes(keyValue("gen_ai.system", "openai")))
                    .addSpans(Span.newBuilder()
                        .setTraceId(traceIdBytes("trace-good"))
                        .setSpanId(spanIdBytes("span-good"))
                        .setName("chat")
                        .setStartTimeUnixNano(1_000_000_000L)
                        .setEndTimeUnixNano(2_000_000_000L)
                        .addAttributes(keyValue("gen_ai.system", "openai")))))
            .build();

        OtelSpanBatch batch = mapper.toBatch(request);

        assertThat(batch.size()).isEqualTo(1);
        assertThat(batch.rejectedSpans()).isEqualTo(1);
        assertThat(batch.spans()
            .getFirst()
            .spanIdHex()).isEqualTo(spanIdHex("span-good"));
    }

    /**
     * Pins the {@code PER_REQUEST_REJECTION_LOG_CAP = 5} contract. A malformed-batch attack submitting 100s of bad
     * spans must NOT flood the structured-log pipeline (operators pay per line, and it trains them to ignore noise).
     * The cap is enforced for per-line rejections; one trailing summary line surfaces the truncated count. A regression
     * that removes or raises the cap would silently re-enable the DoS the constant defends against.
     */
    @Test
    void testCapsPerRequestRejectionLog() {
        ExportTraceServiceRequest.Builder requestBuilder = ExportTraceServiceRequest.newBuilder();
        ResourceSpans.Builder resourceBuilder = ResourceSpans.newBuilder();
        ScopeSpans.Builder scopeBuilder = ScopeSpans.newBuilder();

        // 20 spans, each with a 7-byte (malformed) span id so every one is rejected by the per-span try/catch.
        ByteString malformedSpanId = ByteString.copyFrom(new byte[SPAN_ID_BYTES - 1]);

        for (int spanIndex = 0; spanIndex < 20; spanIndex++) {
            scopeBuilder.addSpans(Span.newBuilder()
                .setTraceId(traceIdBytes("trace-" + spanIndex))
                .setSpanId(malformedSpanId)
                .setName("chat")
                .setStartTimeUnixNano(1_000_000_000L)
                .setEndTimeUnixNano(2_000_000_000L)
                .addAttributes(keyValue("gen_ai.system", "openai")));
        }

        ExportTraceServiceRequest request = requestBuilder
            .addResourceSpans(resourceBuilder.addScopeSpans(scopeBuilder))
            .build();

        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
            .getLogger(OtlpProtobufMapperImpl.class);
        ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender =
            new ch.qos.logback.core.read.ListAppender<>();

        appender.start();
        logger.addAppender(appender);

        try {
            OtelSpanBatch batch = mapper.toBatch(request);

            assertThat(batch.rejectedSpans()).isEqualTo(20);
            assertThat(batch.size()).isZero();

            // Cap = 5 per-line breadcrumbs + 1 trailing summary line. Anything beyond 6 indicates the cap was
            // bypassed — exactly the regression that re-enables the log-flood DoS.
            long warnLines = appender.list.stream()
                .filter(event -> event.getLevel() == ch.qos.logback.classic.Level.WARN)
                .count();

            assertThat(warnLines)
                .as("Per-line breadcrumbs capped at 5 plus one trailing summary")
                .isLessThanOrEqualTo(6L);

            assertThat(appender.list.stream()
                .anyMatch(event -> event.getFormattedMessage()
                    .contains("additional span(s) in this request")))
                        .as("Trailing summary line surfaces the truncated count")
                        .isTrue();
        } finally {
            logger.detachAppender(appender);
        }
    }

    /**
     * Pins the {@code SAFE_HEX_BYTE_CAP = 64} contract directly on {@code safeHex}. A malformed exporter could send a
     * megabyte-scale BYTES payload masquerading as a trace id; without this cap, the per-rejection log allocates 2*N
     * hex characters and the {@code PER_REQUEST_REJECTION_LOG_CAP=5} multiplier turns a 10MB payload into a 100MB heap
     * spike. A regression that raises the cap to {@code Integer.MAX_VALUE} or removes the truncation marker re-enables
     * the megabyte-log DoS the constant defends against.
     */
    @Test
    void testSafeHexCapsByteLength() throws ReflectiveOperationException {
        java.lang.reflect.Method method =
            OtlpProtobufMapperImpl.class.getDeclaredMethod("safeHex", ByteString.class);

        method.setAccessible(true);

        // Empty bytes: sentinel "<empty>".
        assertThat((String) method.invoke(null, ByteString.EMPTY)).isEqualTo("<empty>");

        // Exactly at the cap: no truncation marker, 2*64 = 128 hex chars.
        ByteString atCap = ByteString.copyFrom(new byte[64]);

        String atCapHex = (String) method.invoke(null, atCap);

        assertThat(atCapHex)
            .as("64-byte input must hex without truncation marker")
            .hasSize(128)
            .doesNotContain("truncated");

        // Just over the cap: marker fires, prefix bounded at 2*64=128 hex chars + the truncation marker.
        ByteString justOverCap = ByteString.copyFrom(new byte[65]);

        String justOverCapHex = (String) method.invoke(null, justOverCap);

        assertThat(justOverCapHex)
            .as("65-byte input triggers truncation marker for the 1 byte beyond the cap")
            .startsWith("0".repeat(128))
            .contains("<truncated 1 bytes>");

        // Well over the cap (1024 bytes — the megabyte-log DoS lower bound used in the SAFE_HEX_BYTE_CAP Javadoc):
        // hex prefix bounded at 128 chars + truncation marker. Without the cap, output would be 2048 hex chars.
        ByteString wellOverCap = ByteString.copyFrom(new byte[1024]);

        String wellOverCapHex = (String) method.invoke(null, wellOverCap);

        assertThat(wellOverCapHex.length())
            .as("Output length stays bounded — must not scale with input size")
            .isLessThan(200);

        assertThat(wellOverCapHex).contains("<truncated 960 bytes>");

        // 16 bytes — the canonical OTel trace-id width. Should hex cleanly with no truncation marker. Pins the
        // common-case path: a regression that fires the marker for legitimate trace ids would corrupt every
        // rejection breadcrumb in production.
        ByteString canonicalTraceId = ByteString.copyFrom(new byte[16]);

        String canonicalTraceIdHex = (String) method.invoke(null, canonicalTraceId);

        assertThat(canonicalTraceIdHex)
            .hasSize(32)
            .doesNotContain("truncated");
    }

    private ExportTraceServiceRequest requestWithOneGenAiSpan() {
        return ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(ResourceSpans.newBuilder()
                .setResource(Resource.newBuilder()
                    .addAttributes(keyValue("service.name", "chat-service")))
                .addScopeSpans(ScopeSpans.newBuilder()
                    .addSpans(Span.newBuilder()
                        .setTraceId(traceIdBytes("trace-aaaaaaaaaaa"))
                        .setSpanId(spanIdBytes("span-bbbbbb"))
                        .setName("chat")
                        .setStartTimeUnixNano(1_000_000_000L)
                        .setEndTimeUnixNano(2_000_000_000L)
                        .setStatus(Status.newBuilder()
                            .setCode(Status.StatusCode.STATUS_CODE_OK))
                        .addAttributes(keyValue("gen_ai.system", "openai"))
                        .addAttributes(keyValue("gen_ai.request.model", "gpt-4o"))
                        .addAttributes(keyValue("gen_ai.usage.input_tokens", 100L))
                        .addAttributes(keyValue("gen_ai.usage.output_tokens", 42L))))
                .build())
            .build();
    }

    private ResourceSpans resourceSpansWithSingleGenAiSpan(String traceId, String spanId) {
        return ResourceSpans.newBuilder()
            .setResource(Resource.newBuilder()
                .addAttributes(keyValue("service.name", "chat-service")))
            .addScopeSpans(scopeSpansWithGenAiSpan(traceId, spanId))
            .build();
    }

    private ScopeSpans.Builder scopeSpansWithGenAiSpan(String traceId, String spanId) {
        return ScopeSpans.newBuilder()
            .addSpans(Span.newBuilder()
                .setTraceId(traceIdBytes(traceId))
                .setSpanId(spanIdBytes(spanId))
                .setName("chat")
                .setStartTimeUnixNano(1_000_000_000L)
                .setEndTimeUnixNano(2_000_000_000L)
                .setStatus(Status.newBuilder()
                    .setCode(Status.StatusCode.STATUS_CODE_OK))
                .addAttributes(keyValue("gen_ai.system", "openai"))
                .addAttributes(keyValue("gen_ai.request.model", "gpt-4o")));
    }

    private KeyValue keyValue(String key, String value) {
        return KeyValue.newBuilder()
            .setKey(key)
            .setValue(AnyValue.newBuilder()
                .setStringValue(value))
            .build();
    }

    private KeyValue keyValue(String key, long value) {
        return KeyValue.newBuilder()
            .setKey(key)
            .setValue(AnyValue.newBuilder()
                .setIntValue(value))
            .build();
    }

    /**
     * Pins {@link OtelSpanAttributes.UnsetAttribute#INSTANCE} singleton identity across Java native serialization. A
     * future refactor that demotes the enum back to a plain class (or a raw {@code Object}) would silently break
     * identity comparisons against {@link OtelSpanAttributes#UNSET_ATTRIBUTE_VALUE} after any serializer touches the
     * attribute map (Spring Session, distributed cache, kryo-backed snapshot tooling).
     *
     * <p>
     * Java native serialization is the strictest contract — Jackson and Kryo both preserve enum identity by name — so
     * passing this test guarantees the others do too.
     */
    @SuppressFBWarnings(
        value = "OBJECT_DESERIALIZATION",
        justification = "Test round-trips a single internally-defined enum singleton through native serialization to "
            + "verify identity preservation; the input is the test's own ByteArrayOutputStream output, never " +
            "untrusted data, so the find-sec-bugs deserialization-of-untrusted-data warning does not apply.")
    @Test
    void testUnsetAttributeSingletonSurvivesNativeSerializationRoundTrip() throws Exception {
        Object original = OtelSpanAttributes.UNSET_ATTRIBUTE_VALUE;

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            outputStream.writeObject(original);
        }

        Object roundTripped;

        try (ObjectInputStream inputStream =
            new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()))) {

            roundTripped = inputStream.readObject();
        }

        assertThat(roundTripped)
            .as("Native serialization MUST preserve singleton identity for the UnsetAttribute sentinel — "
                + "any consumer doing identity comparison against UNSET_ATTRIBUTE_VALUE depends on this.")
            .isSameAs(original);
    }

    /**
     * Pins that pathological Unicode in attribute values (4-byte emoji, RTL marks, embedded NUL) round-trips through
     * the mapper without corruption. Caller-controlled attribute values can carry user prompts, customer-id strings
     * with non-Latin scripts, or hostile NUL-padded inputs from compromised exporters; silent truncation / replacement
     * at the mapper boundary would corrupt downstream search and dashboards.
     */
    @Test
    void testPreservesPathologicalUnicodeInAttributeValues() {
        String emoji = "hello 👋 world"; // 4-byte UTF-16 surrogate pair (waving hand emoji)
        String rtl = "abc‮def"; // right-to-left override mark
        String nulEmbedded = "before\u0000after"; // embedded NUL byte

        ExportTraceServiceRequest request = ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(ResourceSpans.newBuilder()
                .addScopeSpans(ScopeSpans.newBuilder()
                    .addSpans(Span.newBuilder()
                        .setTraceId(traceIdBytes("trace-uni"))
                        .setSpanId(spanIdBytes("span-uni"))
                        .setName("chat")
                        .setStartTimeUnixNano(1_000_000_000L)
                        .setEndTimeUnixNano(2_000_000_000L)
                        .addAttributes(keyValue("gen_ai.system", "openai"))
                        .addAttributes(keyValue("gen_ai.prompt", emoji))
                        .addAttributes(keyValue("custom.rtl", rtl))
                        .addAttributes(keyValue("custom.nul", nulEmbedded))
                        .setStatus(Status.newBuilder()
                            .setCode(Status.StatusCode.STATUS_CODE_OK)))))
            .build();

        OtelGenAiSpan span = mapper.toBatch(request)
            .spans()
            .getFirst();

        assertThat(span.attributes()
            .get("gen_ai.prompt"))
                .as("Surrogate-pair emoji must round-trip byte-identically through the mapper")
                .isEqualTo(emoji);
        assertThat(span.attributes()
            .get("custom.rtl"))
                .as("RTL override mark must not be stripped or normalised")
                .isEqualTo(rtl);
        assertThat(span.attributes()
            .get("custom.nul"))
                .as("Embedded NUL byte must survive — Postgres TEXT storage can reject this downstream, but "
                    + "the mapper itself must not silently truncate at NUL")
                .isEqualTo(nulEmbedded);
    }
}
