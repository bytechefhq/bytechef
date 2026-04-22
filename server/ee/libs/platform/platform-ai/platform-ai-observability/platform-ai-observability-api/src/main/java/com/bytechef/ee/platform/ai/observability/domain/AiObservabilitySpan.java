/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.domain;

import com.bytechef.ee.platform.ai.llm.usage.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Table("ai_observability_span")
public class AiObservabilitySpan {

    private static final AiObservabilitySpanLevel[] LEVEL_VALUES = AiObservabilitySpanLevel.values();
    private static final AiObservabilitySpanStatus[] STATUS_VALUES = AiObservabilitySpanStatus.values();
    private static final AiObservabilitySpanType[] TYPE_VALUES = AiObservabilitySpanType.values();

    private BigDecimal cost;

    @CreatedDate
    private Instant createdDate;

    private Instant endTime;

    /**
     * Optional external identifier (e.g. OTLP {@code span_id} as 16 hex chars). Combined with {@code traceId} via a
     * partial unique index, this provides idempotency for OTLP retries — exporters retry whole batches on transient
     * failures and would otherwise double-insert. Internal spans created by the gateway itself leave this null.
     */
    private String externalSpanId;

    @Id
    private Long id;

    private String input;

    private Integer inputTokens;

    @LastModifiedDate
    private Instant lastModifiedDate;

    private Integer latencyMs;

    private int level;

    private String metadata;

    private String model;

    private String name;

    private String output;

    private Integer outputTokens;

    private Long parentSpanId;

    private Long promptId;

    private Long promptVersionId;

    private String provider;

    private Instant startTime;

    private int status;

    private Long traceId;

    private int type;

    @Version
    private int version;

    private AiObservabilitySpan() {
    }

    public AiObservabilitySpan(Long traceId, AiObservabilitySpanType type) {
        Validate.notNull(traceId, "traceId must not be null");
        Validate.notNull(type, "type must not be null");

        this.level = AiObservabilitySpanLevel.DEFAULT.ordinal();
        this.startTime = Instant.now();
        this.status = AiObservabilitySpanStatus.ACTIVE.ordinal();
        this.traceId = traceId;
        this.type = type.ordinal();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiObservabilitySpan aiObservabilitySpan)) {
            return false;
        }

        return Objects.equals(id, aiObservabilitySpan.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public BigDecimal getCost() {
        return cost;
    }

    /**
     * Currency-aware view of {@link #getCost()}. Prefer this when summing span costs for a trace or aggregating across
     * workspaces. Single-currency USD today — the {@link Money} abstraction catches a future per-provider currency
     * change at the call site.
     */
    public Money getCostAsMoney() {
        return cost == null ? null : Money.usd(cost);
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public String getExternalSpanId() {
        return externalSpanId;
    }

    public Long getId() {
        return id;
    }

    public String getInput() {
        return input;
    }

    public Integer getInputTokens() {
        return inputTokens;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public AiObservabilitySpanLevel getLevel() {
        return safeOrdinalLookup("level", level, LEVEL_VALUES);
    }

    public String getMetadata() {
        return metadata;
    }

    public String getModel() {
        return model;
    }

    public String getName() {
        return name;
    }

    public String getOutput() {
        return output;
    }

    public Integer getOutputTokens() {
        return outputTokens;
    }

    public Long getParentSpanId() {
        return parentSpanId;
    }

    public Long getPromptId() {
        return promptId;
    }

    public Long getPromptVersionId() {
        return promptVersionId;
    }

    public String getProvider() {
        return provider;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public AiObservabilitySpanStatus getStatus() {
        return safeOrdinalLookup("status", status, STATUS_VALUES);
    }

    public Long getTraceId() {
        return traceId;
    }

    public AiObservabilitySpanType getType() {
        return safeOrdinalLookup("type", type, TYPE_VALUES);
    }

    /**
     * Defensive ordinal lookup so a corrupted column value (caused by an enum reordering or a manual DB edit) surfaces
     * as a precise diagnostic with the row id and field name, rather than {@link ArrayIndexOutOfBoundsException} from a
     * raw {@code values[ordinal]} access. Mirrors the discipline applied to {@code AiEvalScore.getDataType()} so the
     * gateway domain surface stays consistent under enum drift.
     */
    private <E extends Enum<E>> E safeOrdinalLookup(String field, int ordinal, E[] values) {
        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalStateException(
                "AiObservabilitySpan id=" + id + " has corrupt " + field + " ordinal " + ordinal +
                    " (valid range 0.." + (values.length - 1) + ")");
        }

        return values[ordinal];
    }

    /**
     * Guards every mutation path on the span. Post-close mutation would silently overwrite a finalized observation row,
     * breaking the partial-success ingestion contract (the same span row could be re-ingested by an OTLP retry after
     * close and emerge with different cost / latency / status on the dashboard). Construction-phase setters are
     * reachable freely; once {@link #close(Instant, AiObservabilitySpanStatus)} or
     * {@link #setStatus(AiObservabilitySpanStatus)} has stamped a terminal status, every setter — including the
     * time/status finalization triad ({@link #setEndTime(Instant)}, {@link #setStartTime(Instant)},
     * {@link #setStatus(AiObservabilitySpanStatus)}) — throws.
     *
     * <p>
     * {@link #close(Instant, AiObservabilitySpanStatus)} bypasses this guard for the {@code status} write specifically
     * by performing a direct field assignment after {@code setEndTime}; the close() entry point performs its own active
     * check first.
     */
    private void assertActive(String fieldName) {
        if (this.status != AiObservabilitySpanStatus.ACTIVE.ordinal()) {
            throw new IllegalStateException(
                "AiObservabilitySpan id=" + id + " is in terminal state " + getStatus() +
                    "; cannot mutate '" + fieldName + "' after close()");
        }
    }

    public int getVersion() {
        return version;
    }

    /**
     * Stamps cost. Token, cost, and latency setters are individually mutable for callers (e.g. {@code AiGatewayFacade})
     * that compute these via wall-clock {@code System.currentTimeMillis()} or upstream cost feeds rather than from the
     * {@code (startTime, endTime)} pair. Callers that prefer atomic terminal transitions should use
     * {@link #close(Instant, AiObservabilitySpanStatus)}, which sets endTime, recomputes latencyMs, and stamps the
     * terminal status in one call.
     *
     * <p>
     * Throws {@link IllegalStateException} if the span has already been closed — see {@link #assertActive(String)}.
     */
    public void setCost(BigDecimal cost) {
        assertActive("cost");

        this.cost = cost;
    }

    /**
     * Internal endTime mutator. Demoted to {@code private} so the only public terminal transition path is
     * {@link #close(Instant, AiObservabilitySpanStatus)} — that keeps {@code (endTime, latencyMs, status)} atomically
     * consistent. Spring Data JDBC's reflective row hydration still reaches this setter via {@code setAccessible}, so
     * the {@code end_time} column round-trip is unaffected. Null is accepted only for the hydration path (open spans
     * have a nullable {@code end_time} column); business code reaches it only through {@link #close} which validates
     * non-null.
     *
     * <p>
     * If {@code latencyMs} has not yet been set and {@code startTime} is non-null, {@code latencyMs} is auto-derived
     * from {@code endTime - startTime} so the pair stays consistent for callers that don't otherwise stamp it.
     */
    private void setEndTime(Instant endTime) {
        assertActive("endTime");

        if (endTime != null && startTime != null && endTime.isBefore(startTime)) {
            throw new IllegalArgumentException(
                "endTime (" + endTime + ") must not be before startTime (" + startTime + ")");
        }

        this.endTime = endTime;

        if (endTime != null && startTime != null && this.latencyMs == null) {
            long durationMillis = java.time.Duration.between(startTime, endTime)
                .toMillis();

            this.latencyMs = (int) Math.max(0L, durationMillis);
        }
    }

    /**
     * Stamps the external (OTLP) span identifier. The {@code (trace_id, external_span_id)} partial unique index is the
     * idempotency mechanism: an OTLP exporter retrying a batch must hit the same row, not insert a second row with a
     * drifted hex id. To keep that guarantee at the type level, this setter rejects re-stamping with a different
     * non-null value — once the field is set, a divergent non-null write throws {@link IllegalStateException}. Same
     * non-null value (re-stamping the OTel span_id from a retry) is a no-op so callers are not forced to branch on
     * already-set state. Null writes (clearing during an internal-span build) remain free.
     *
     * <p>
     * Spring Data JDBC hydrates a freshly-constructed instance, so {@code this.externalSpanId} is null when the setter
     * fires for a row read; the guard therefore does not interfere with row mapping. The constraint applies only to
     * subsequent business-code mutations on an already-hydrated span.
     */
    public void setExternalSpanId(String externalSpanId) {
        assertActive("externalSpanId");

        if (externalSpanId != null && this.externalSpanId != null
            && !externalSpanId.equals(this.externalSpanId)) {

            throw new IllegalStateException(
                "AiObservabilitySpan id=" + id + " already has externalSpanId='" + this.externalSpanId
                    + "'; cannot re-stamp to '" + externalSpanId
                    + "' — the (trace_id, external_span_id) idempotency contract forbids drift");
        }

        this.externalSpanId = externalSpanId;
    }

    /**
     * Atomically transitions the span to a terminal state: sets endTime, derives latencyMs (only when the caller has
     * not already stamped one — see {@link #setEndTime(Instant)}), and sets status. Prefer this over calling the
     * individual setters so the three fields stay consistent.
     *
     * <p>
     * Rejects re-closing: a span whose status is already terminal cannot be closed again. This avoids silently
     * re-stamping endTime / latencyMs / status on a span that another caller already finalized (e.g. an at-least-once
     * span ingest path or a buggy retry loop).
     */
    public void close(Instant endTime, AiObservabilitySpanStatus terminalStatus) {
        Validate.notNull(endTime, "endTime must not be null");
        Validate.notNull(terminalStatus, "terminalStatus must not be null");
        Validate.isTrue(
            terminalStatus != AiObservabilitySpanStatus.ACTIVE,
            "close() requires a terminal status, not ACTIVE");

        if (this.status != AiObservabilitySpanStatus.ACTIVE.ordinal()) {
            throw new IllegalStateException(
                "Span " + id + " is already in terminal state " + STATUS_VALUES[this.status] +
                    " and cannot be re-closed");
        }

        setEndTime(endTime);

        this.status = terminalStatus.ordinal();
    }

    public void setInput(String input) {
        assertActive("input");

        this.input = input;
    }

    public void setInputTokens(Integer inputTokens) {
        assertActive("inputTokens");

        this.inputTokens = inputTokens;
    }

    public void setLatencyMs(Integer latencyMs) {
        assertActive("latencyMs");

        this.latencyMs = latencyMs;
    }

    public void setLevel(AiObservabilitySpanLevel level) {
        assertActive("level");

        this.level = level.ordinal();
    }

    public void setMetadata(String metadata) {
        assertActive("metadata");

        this.metadata = metadata;
    }

    public void setModel(String model) {
        assertActive("model");

        this.model = model;
    }

    public void setName(String name) {
        assertActive("name");

        this.name = name;
    }

    public void setOutput(String output) {
        assertActive("output");

        this.output = output;
    }

    public void setOutputTokens(Integer outputTokens) {
        assertActive("outputTokens");

        this.outputTokens = outputTokens;
    }

    public void setParentSpanId(Long parentSpanId) {
        assertActive("parentSpanId");

        this.parentSpanId = parentSpanId;
    }

    public void setPromptId(Long promptId) {
        assertActive("promptId");

        this.promptId = promptId;
    }

    public void setPromptVersionId(Long promptVersionId) {
        assertActive("promptVersionId");

        this.promptVersionId = promptVersionId;
    }

    public void setProvider(String provider) {
        assertActive("provider");

        this.provider = provider;
    }

    /**
     * Sets {@code startTime}. Null is accepted only for Spring Data JDBC reflective hydration paths; business code
     * should never pass {@code null} — the constructor auto-stamps {@code startTime} to {@code Instant.now()} and the
     * column is non-nullable in the DB schema. A null write in business code would silently leave a row that cannot
     * derive {@code latencyMs} on subsequent {@link #setEndTime(Instant)}.
     */
    public void setStartTime(Instant startTime) {
        assertActive("startTime");

        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new IllegalArgumentException(
                "startTime (" + startTime + ") must not be after existing endTime (" + endTime + ")");
        }

        this.startTime = startTime;
    }

    public void setStatus(AiObservabilitySpanStatus status) {
        Validate.notNull(status, "status must not be null");

        // Reject re-mutation after the span has reached a terminal status — the post-close immutability
        // guarantee applies to status too. Going ACTIVE → terminal IS allowed here (assertActive passes
        // because the current state is still ACTIVE), so existing callers that finalize via setStatus(...)
        // instead of close() continue to work; the difference is they no longer get a free post-close revert.
        assertActive("status");

        this.status = status.ordinal();
    }

    @Override
    public String toString() {
        return "AiObservabilitySpan{" +
            "id=" + id +
            ", traceId=" + traceId +
            ", type=" + getType() +
            ", status=" + getStatus() +
            ", level=" + getLevel() +
            ", startTime=" + startTime +
            '}';
    }
}
