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
 * Default implementation. Filters to spans that carry at least one {@code gen_ai.*} attribute — OTel-instrumented
 * services emit plenty of unrelated spans (DB queries, HTTP middleware) that we do not want to persist as LLM traces.
 *
 * <p>
 * Trace and span IDs are encoded as lowercase hex per the OTLP spec (16 bytes / 32 hex chars for traceId, 8 bytes / 16
 * hex chars for spanId). The protobuf wire shape carries them as raw {@link ByteString}; rendering with
 * {@link ByteString#toStringUtf8()} would silently replace invalid UTF-8 byte sequences with U+FFFD and collapse
 * distinct binary IDs onto the same string — making cross-tenant trace lookups corruptable. {@link HexFormat} is the
 * canonical W3C Trace Context representation and round-trips losslessly.
 *
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

    /**
     * Maximum number of per-span warn-level rejection log lines emitted in a single {@link #toBatch} call. A
     * malformed-batch attack (or a misconfigured exporter looping retries) can otherwise emit thousands of warn lines
     * per request — DoS-ing structured-log pipelines that pay per-line. Lines beyond the cap are aggregated into a
     * single summary so the volume is still visible without flooding.
     */
    private static final int PER_REQUEST_REJECTION_LOG_CAP = 5;

    /**
     * Hard ceiling on {@link #unwrap} recursion depth. protobuf-java imposes a 100-level recursion limit on parsing by
     * default, but {@code unwrap} can still chain {@code KVLIST}/{@code ARRAY} variants pathologically. The per-span
     * {@code catch (RuntimeException)} in {@link #toBatch} would NOT catch a {@link StackOverflowError}, so an
     * unbounded recursion would abort the whole batch and bypass the partial-success contract. Throwing
     * {@link IllegalArgumentException} past this limit lets the per-span guard reject the offending span without
     * affecting peers. {@code 32} comfortably accommodates legitimate nested attribute structures while bounding the
     * stack walk to a fraction of the default thread stack.
     */
    private static final int MAX_UNWRAP_DEPTH = 32;

    /**
     * Sentinel for unset OTLP array slots, surfaced through the otlp-api module so downstream consumers comparing
     * attribute values by identity do not need a transitive dependency on this mapper module. See
     * {@link OtelSpanAttributes.UnsetAttribute} for the rationale.
     */
    private static final Object UNSET_ATTRIBUTE_VALUE = OtelSpanAttributes.UNSET_ATTRIBUTE_VALUE;

    @Override
    public OtelSpanBatch toBatch(ExportTraceServiceRequest request) {
        List<OtelGenAiSpan> spans = new ArrayList<>();
        List<String> mapperWarnings = new ArrayList<>();
        int dropped = 0;
        int rejected = 0;
        int loggedRejections = 0;

        for (ResourceSpans resourceSpans : request.getResourceSpansList()) {
            // Resource-level attribute decode is wrapped in its own try/catch with a graceful fallback to empty
            // attributes rather than rejecting every span under this ResourceSpans. A malformed resource attribute
            // (deeply nested KvlistValue, future protobuf wire-format change, ARRAY/KVLIST/BYTES recursion fault)
            // would otherwise propagate uncaught and abort the entire batch upstream of the per-span guard below.
            // Graceful fallback preserves the per-span partial-success contract: spans get persisted without
            // resource-level enrichment, which is strictly better than losing them entirely. The breadcrumb is
            // also added to mapperWarnings so the wire response advertises the degraded outcome to the client —
            // a counter-only signal would let an exporter believe its 100-span batch was 100% accepted while
            // service.name was silently stripped from every persisted row.
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
                    // Per-span guard wraps the entire span construction including per-span attribute decode, so a
                    // malformed span attribute does not abort the whole batch. Catch RuntimeException (not
                    // IllegalArgumentException) because the construction chain can throw: ArithmeticException from
                    // Instant.ofEpochSecond on nanosecond overflow, NullPointerException from Map.copyOf if a null
                    // slipped through, and the OtelGenAiSpan compact constructor's strict boundary validation (32-hex
                    // traceId, 16-hex spanId, endTime >= startTime). All are upstream-data faults that should reject
                    // the offending span without aborting peers.
                    try {
                        AttributeBuildResult attributeBuild = toAttributeMapWithGenAiFlag(span.getAttributesList());

                        if (!attributeBuild.hasGenAi) {
                            dropped++;

                            continue;
                        }

                        Map<String, Object> attributes = attributeBuild.values;

                        // OTLP parent_span_id is OPTIONAL; protobuf default-encodes absent bytes as empty, but
                        // several real-world exporters (older OTel Java agents, hand-rolled shims) emit 8 zero
                        // bytes instead. Treating zero-bytes as "no parent" keeps those legitimate root spans
                        // out of the all-zero sentinel guard in OtelHexIds.validate — otherwise the per-span
                        // catch below would bucket them as REJECTED_BY_MAPPER and a whole class of valid root
                        // spans would silently disappear depending on the exporter. The zero sentinel is
                        // correctly enforced for traceId/spanId (those are mandatory OTLP) but parent_span_id
                        // is optional and zero-bytes is semantically equivalent to absent.
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

                        // Log spanId / traceId / service.name even on rejection — but cap per-request. The toHex
                        // calls are safe for any byte length (HexFormat does not validate); empty-bytes paths
                        // render as empty strings.
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

        // Surface the truncation explicitly so operators reading the log can see "we hit the cap" rather than
        // wondering why mass rejections from a misbehaving exporter only produced 5 lines.
        if (rejected > loggedRejections) {
            log.warn(
                "OTLP mapper rejected {} additional span(s) in this request (only first {} logged with detail)",
                rejected - loggedRejections, PER_REQUEST_REJECTION_LOG_CAP);
        }

        return new OtelSpanBatch(spans, dropped, rejected, mapperWarnings);
    }

    /**
     * Returns true when every byte in {@code bytes} is zero. OTLP parent_span_id is optional; the wire-format default
     * for an absent field is empty bytes, but several exporters emit an explicit 8-byte zero string for "no parent."
     * Both encodings must be treated as "absent" so the canonical-id sentinel guard does not reject them.
     */
    private static boolean isAllZeroBytes(ByteString bytes) {
        for (int index = 0; index < bytes.size(); index++) {
            if (bytes.byteAt(index) != 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Maximum bytes a malformed-id payload may consume in the rejection log before truncation. A canonical OTLP trace
     * id is 16 bytes (32 hex chars) and a span id is 8 bytes (16 hex chars). A malformed exporter could send
     * megabyte-scale BYTES masquerading as a trace id; without this cap, the per-rejection log allocates 2*N hex
     * characters and the {@code PER_REQUEST_REJECTION_LOG_CAP=5} multiplier turns a 10MB payload into a 100MB heap
     * spike. 64 bytes is generous (4× the canonical 16-byte trace id) and bounded.
     */
    private static final int SAFE_HEX_BYTE_CAP = 64;

    private static String safeHex(ByteString bytes) {
        if (bytes.isEmpty()) {
            return "<empty>";
        }

        if (bytes.size() <= SAFE_HEX_BYTE_CAP) {
            return HEX.formatHex(bytes.toByteArray());
        }

        // Allocate only the prefix and append a truncation marker so operators can still see the start of the
        // malformed id. ByteString.substring is a view; toByteArray() on the slice copies just the prefix.
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

    /**
     * Builds the attribute map and tracks whether any key starts with {@code gen_ai.} in the same pass. Avoids the
     * second O(n) prefix scan over {@code keySet()} that would otherwise be needed in the hot per-span loop.
     *
     * <p>
     * The {@code gen_ai.} flag is set on the KEY, not on the VALUE — a span carrying {@code gen_ai.system} as
     * {@code VALUE_NOT_SET} (legal protobuf) MUST be forwarded to the ingest facade so it gets rejected with
     * {@link RejectionCode#REJECTED_NO_SYSTEM} rather than silently classified as a non-gen_ai span and dropped. The
     * dropped path collapses two distinct operational signals — "exporter intentionally omitted gen_ai" vs. "exporter
     * sent gen_ai but the value was VALUE_NOT_SET" — onto the same dashboard bucket; preserving the flag based on key
     * presence keeps those signals distinguishable downstream. Map.copyOf still rejects null values, so VALUE_NOT_SET
     * entries are not added to the map — the value goes to the ingest facade as null and the
     * {@code missing gen_ai.system attribute} branch fires there.
     */
    private static AttributeBuildResult toAttributeMapWithGenAiFlag(List<KeyValue> keyValues) {
        Map<String, Object> result = new LinkedHashMap<>();
        boolean hasGenAi = false;

        for (KeyValue keyValue : keyValues) {
            String key = keyValue.getKey();

            if (!hasGenAi && key.startsWith("gen_ai.")) {
                hasGenAi = true;
            }

            Object unwrapped = unwrap(keyValue.getValue());

            // Skip VALUE_NOT_SET attributes entirely. OtelGenAiSpan's constructor passes the map through
            // Map.copyOf, which throws on null values; semantically "no value" == "no attribute" anyway. The
            // hasGenAi flag was already updated above based on the key, so the span still flows downstream
            // and a missing gen_ai.system value surfaces as REJECTED_NO_SYSTEM at the ingest layer.
            if (unwrapped == null) {
                continue;
            }

            result.put(key, unwrapped);
        }

        return new AttributeBuildResult(result, hasGenAi);
    }

    /**
     * Unwraps an OTLP {@link AnyValue} into a Java type. Each protobuf variant maps to a distinct Java shape so callers
     * can pattern-match by runtime type rather than re-parsing strings:
     * <ul>
     * <li>{@code STRING_VALUE} / {@code BOOL_VALUE} / {@code INT_VALUE} / {@code DOUBLE_VALUE} → scalar Java
     * types.</li>
     * <li>{@code ARRAY_VALUE} → {@code List<Object>}, recursively unwrapped. Slots that were emitted as
     * {@code VALUE_NOT_SET} are preserved as {@link #UNSET_ATTRIBUTE_VALUE} so positional indices stay stable for
     * downstream consumers (e.g. {@code gen_ai.input.messages[N]}).</li>
     * <li>{@code KVLIST_VALUE} → {@code Map<String, Object>}, recursively unwrapped. Unset values are dropped because
     * map access is key-addressed and positional shifting does not apply.</li>
     * <li>{@code BYTES_VALUE} → lowercase hex string (lossless, matches the trace/span id encoding).</li>
     * <li>{@code VALUE_NOT_SET} → {@code null}, since the wire shape explicitly says "no value".</li>
     * </ul>
     * The recursive shape preserves the original Java type of each attribute so a numeric token attribute lands as
     * {@code Long} / {@code Double} downstream rather than as a stringified composite — which would otherwise force a
     * lossy reparse that drops the breadcrumb back to the original wire value.
     */
    private static Object unwrap(AnyValue anyValue) {
        return unwrap(anyValue, 0);
    }

    private static Object unwrap(AnyValue anyValue, int depth) {
        if (depth > MAX_UNWRAP_DEPTH) {
            // Throw IAE rather than letting the JVM unwind via StackOverflowError. The per-span guard in
            // toBatch catches RuntimeException, so the offending span is rejected with REJECTED_BY_MAPPER and
            // peer spans persist normally — preserving the partial-success contract under attack.
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

            // Preserve unset slots as a sentinel so positional indices are stable. Dropping the slot would
            // silently shift every subsequent index — an array like [set, set, NOT_SET, set] would surface
            // as [a, b, d] and a downstream consumer reading [3] would get the wrong value.
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

    /**
     * Counter for resource-attribute decode failures. The WARN log alone leaves operators with no aggregate signal — a
     * misconfigured OTel collector emitting malformed resource attributes per request silently strips
     * {@code service.name} from every persisted span until someone tails logs.
     *
     * <p>
     * The increment is wrapped in try/catch mirroring the discipline in
     * {@link com.bytechef.ee.automation.ai.observability.facade.AiObservabilityOtlpIngestFacadeImpl}: a meter registry
     * RuntimeException (cardinality cap, shutdown race) escaping out of this helper would propagate up the resource-
     * decode catch block and abort the whole ResourceSpans loop — turning a recoverable "service.name stripped" event
     * into a batch-wide rejection. Counter failures are operationally noisy but not data-correctness affecting; degrade
     * gracefully so the per-span hot path keeps running.
     */
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
