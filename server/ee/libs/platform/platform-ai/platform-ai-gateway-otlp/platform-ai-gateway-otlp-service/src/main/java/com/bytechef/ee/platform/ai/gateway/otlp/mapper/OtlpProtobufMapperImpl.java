/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.otlp.mapper;

import com.bytechef.ee.platform.ai.gateway.otlp.dto.OtelGenAiSpan;
import com.bytechef.ee.platform.ai.gateway.otlp.dto.OtelResourceAttributes;
import com.bytechef.ee.platform.ai.gateway.otlp.dto.OtelSpanAttributes;
import com.bytechef.ee.platform.ai.gateway.otlp.dto.OtelSpanBatch;
import com.bytechef.ee.platform.ai.gateway.otlp.dto.OtelSpanStatus;
import com.google.protobuf.ByteString;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.ArrayValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.common.v1.KeyValueList;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 * @version ee
 */
public class OtlpProtobufMapperImpl implements OtlpProtobufMapper {

    private static final HexFormat HEX = HexFormat.of();
    private static final Logger log = LoggerFactory.getLogger(OtlpProtobufMapperImpl.class);

    private static final String RESOURCE_DECODE_FAILED_METRIC = "bytechef_ai_otlp_resource_decode_failed";

    private final MeterRegistry meterRegistry;

    public OtlpProtobufMapperImpl() {
        this(null);
    }

    public OtlpProtobufMapperImpl(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    private static final int PER_REQUEST_REJECTION_LOG_CAP = 5;

    private static final int MAX_UNWRAP_DEPTH = 32;

    private static final Object UNSET_ATTRIBUTE_VALUE = OtelSpanAttributes.UNSET_ATTRIBUTE_VALUE;

    @Override
    public OtelSpanBatch toBatch(ExportTraceServiceRequest request) {
        List<OtelGenAiSpan> spans = new ArrayList<>();
        List<String> mapperWarnings = new ArrayList<>();
        int dropped = 0;
        int rejected = 0;
        int loggedRejections = 0;

        for (ResourceSpans resourceSpans : request.getResourceSpansList()) {
            Map<String, Object> resourceAttributesMap;
            String resourceServiceName;
            OtelResourceAttributes resourceAttributes;

            int scopeSpansCount = resourceSpans.getScopeSpansCount();

            try {
                List<KeyValue> resourceAttributesList = resourceSpans.getResource()
                    .getAttributesList();

                resourceAttributesMap = toAttributeMap(resourceAttributesList);
                resourceServiceName = stringAttr(resourceAttributesMap, "service.name");
                resourceAttributes = OtelResourceAttributes.of(resourceAttributesMap);
            } catch (RuntimeException invalidResource) {
                log.warn(
                    "OTLP resource attribute decode failed; falling back to empty resource attributes for {} scope(s)",
                    scopeSpansCount, invalidResource);

                incrementResourceDecodeFailedCounter();

                String summary = invalidResource.getClass()
                    .getSimpleName();

                mapperWarnings.add(
                    "resource attribute decode failed for " + scopeSpansCount + " scope(s); " +
                        "spans persisted without resource enrichment (" + summary + ")");

                resourceAttributesMap = Map.of();
                resourceServiceName = null;
                resourceAttributes = OtelResourceAttributes.of(resourceAttributesMap);
            }

            for (ScopeSpans scopeSpans : resourceSpans.getScopeSpansList()) {
                for (Span span : scopeSpans.getSpansList()) {
                    try {
                        AttributeBuildResult attributeBuild = toAttributeMapWithGenAiFlag(span.getAttributesList());

                        if (!attributeBuild.hasGenAi) {
                            dropped++;

                            continue;
                        }

                        Map<String, Object> attributes = attributeBuild.values;

                        ByteString parentSpanIdBytes = span.getParentSpanId();
                        String parentHex = (parentSpanIdBytes.isEmpty() || isAllZeroBytes(parentSpanIdBytes))
                            ? null : toHex(parentSpanIdBytes);

                        spans.add(OtelGenAiSpan.ofHex(
                            toHex(span.getTraceId()),
                            toHex(span.getSpanId()),
                            parentHex,
                            span.getName(),
                            Instant.ofEpochSecond(0L, span.getStartTimeUnixNano()),
                            Instant.ofEpochSecond(0L, span.getEndTimeUnixNano()),
                            toStatus(span.getStatus()),
                            OtelSpanAttributes.of(attributes),
                            resourceAttributes,
                            stringAttr(attributes, "gen_ai.prompt"),
                            stringAttr(attributes, "gen_ai.completion")));
                    } catch (RuntimeException invalidSpan) {
                        rejected++;

                        if (loggedRejections < PER_REQUEST_REJECTION_LOG_CAP) {
                            log.warn(
                                "OTLP span rejected by mapper traceId={} spanId={} serviceName={}",
                                safeHex(span.getTraceId()), safeHex(span.getSpanId()), resourceServiceName,
                                invalidSpan);

                            loggedRejections++;
                        }
                    }
                }
            }
        }

        if (rejected > loggedRejections) {
            log.warn(
                "OTLP mapper rejected {} additional span(s) in this request (only first {} logged with detail)",
                rejected - loggedRejections, PER_REQUEST_REJECTION_LOG_CAP);
        }

        return new OtelSpanBatch(spans, dropped, rejected, mapperWarnings);
    }

    private static boolean isAllZeroBytes(ByteString bytes) {
        for (int index = 0; index < bytes.size(); index++) {
            if (bytes.byteAt(index) != 0) {
                return false;
            }
        }

        return true;
    }

    private static final int SAFE_HEX_BYTE_CAP = 64;

    private static String safeHex(ByteString bytes) {
        if (bytes.isEmpty()) {
            return "<empty>";
        }

        if (bytes.size() <= SAFE_HEX_BYTE_CAP) {
            return HEX.formatHex(bytes.toByteArray());
        }

        ByteString prefix = bytes.substring(0, SAFE_HEX_BYTE_CAP);
        byte[] prefixBytes = prefix.toByteArray();

        return HEX.formatHex(prefixBytes) + "...<truncated " + (bytes.size() - SAFE_HEX_BYTE_CAP) + " bytes>";
    }

    private static String toHex(ByteString bytes) {
        return HEX.formatHex(bytes.toByteArray());
    }

    private static Map<String, Object> toAttributeMap(List<KeyValue> keyValues) {
        return toAttributeMapWithGenAiFlag(keyValues).values;
    }

    private static AttributeBuildResult toAttributeMapWithGenAiFlag(List<KeyValue> keyValues) {
        Map<String, Object> result = new LinkedHashMap<>();
        boolean hasGenAi = false;

        for (KeyValue keyValue : keyValues) {
            String key = keyValue.getKey();

            if (!hasGenAi && key.startsWith("gen_ai.")) {
                hasGenAi = true;
            }

            Object unwrapped = unwrap(keyValue.getValue());

            if (unwrapped == null) {
                continue;
            }

            result.put(key, unwrapped);
        }

        return new AttributeBuildResult(result, hasGenAi);
    }

    private static Object unwrap(AnyValue anyValue) {
        return unwrap(anyValue, 0);
    }

    private static Object unwrap(AnyValue anyValue, int depth) {
        if (depth > MAX_UNWRAP_DEPTH) {
            throw new IllegalArgumentException(
                "OTLP attribute unwrap exceeded max depth " + MAX_UNWRAP_DEPTH);
        }

        return switch (anyValue.getValueCase()) {
            case STRING_VALUE -> anyValue.getStringValue();
            case BOOL_VALUE -> anyValue.getBoolValue();
            case INT_VALUE -> anyValue.getIntValue();
            case DOUBLE_VALUE -> anyValue.getDoubleValue();
            case ARRAY_VALUE -> unwrapArray(anyValue.getArrayValue(), depth + 1);
            case KVLIST_VALUE -> unwrapKvList(anyValue.getKvlistValue(), depth + 1);
            case BYTES_VALUE -> {
                ByteString bytesValue = anyValue.getBytesValue();
                byte[] rawBytes = bytesValue.toByteArray();

                yield HEX.formatHex(rawBytes);
            }
            case VALUE_NOT_SET -> null;
        };
    }

    private static List<Object> unwrapArray(ArrayValue arrayValue, int depth) {
        List<Object> result = new ArrayList<>(arrayValue.getValuesCount());

        for (AnyValue value : arrayValue.getValuesList()) {
            Object unwrapped = unwrap(value, depth);

            result.add(unwrapped == null ? UNSET_ATTRIBUTE_VALUE : unwrapped);
        }

        return result;
    }

    private static Map<String, Object> unwrapKvList(KeyValueList kvList, int depth) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (KeyValue keyValue : kvList.getValuesList()) {
            Object unwrapped = unwrap(keyValue.getValue(), depth);

            if (unwrapped == null) {
                continue;
            }

            result.put(keyValue.getKey(), unwrapped);
        }

        return result;
    }

    private static OtelSpanStatus toStatus(Status status) {
        return switch (status.getCode()) {
            case STATUS_CODE_OK -> OtelSpanStatus.OK;
            case STATUS_CODE_ERROR -> OtelSpanStatus.ERROR;
            case STATUS_CODE_UNSET, UNRECOGNIZED -> OtelSpanStatus.UNSET;
        };
    }

    private static String stringAttr(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);

        return value == null ? null : String.valueOf(value);
    }

    private record AttributeBuildResult(Map<String, Object> values, boolean hasGenAi) {
    }

    private void incrementResourceDecodeFailedCounter() {
        if (meterRegistry == null) {
            return;
        }

        try {
            Counter.builder(RESOURCE_DECODE_FAILED_METRIC)
                .register(meterRegistry)
                .increment();
        } catch (RuntimeException meterFailure) {
            log.debug("Resource-decode-failed counter increment failed", meterFailure);
        }
    }
}
