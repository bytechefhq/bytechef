/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.dataset.domain;

import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A single input/expected-output row in a {@link AiEvalDatasetVersion}. {@code input}, {@code expectedOutput}, and
 * {@code metadata} all store raw JSON; decoding is deferred to the service/DTO layer. {@code sourceTraceId} optionally
 * links the item back to the trace it was derived from when the item was seeded from production traffic.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Table("ai_eval_dataset_item")
public class AiEvalDatasetItem {

    @CreatedDate
    @Column("created_date")
    private Instant createdDate;

    @Column("dataset_id")
    private Long datasetId;

    @Column("dataset_version_id")
    private Long datasetVersionId;

    @Column("expected_output")
    private String expectedOutput;

    @Id
    private Long id;

    private String input;

    private String metadata;

    @Column("source_trace_id")
    private Long sourceTraceId;

    private AiEvalDatasetItem() {
    }

    public AiEvalDatasetItem(Long datasetId, Long datasetVersionId, String input) {
        Validate.notNull(datasetId, "datasetId must not be null");
        Validate.notNull(datasetVersionId, "datasetVersionId must not be null");
        Validate.isTrue(StringUtils.isNotBlank(input), "input must not be blank");

        this.datasetId = datasetId;
        this.datasetVersionId = datasetVersionId;
        this.input = input;
    }

    /**
     * Creation-time factory that accepts the optional {@code expectedOutput} and {@code metadata} alongside the
     * required {@code input}. Preferred over {@code new AiEvalDatasetItem(...)} + {@code setExpectedOutput} +
     * {@code setMetadata} from cross-package callers because the public mutators carry a runtime "id == null" guard
     * that future refactors of the service might trip on. This factory writes the optional fields via direct field
     * access (same package as {@link AiEvalDatasetItem}) so the persistence-state guard stays unviolated for the
     * legitimate creation path.
     */
    public static AiEvalDatasetItem newItem(
        Long datasetId, Long datasetVersionId, String input, String expectedOutput, String metadata) {

        AiEvalDatasetItem item = new AiEvalDatasetItem(datasetId, datasetVersionId, input);

        item.expectedOutput = expectedOutput;
        item.metadata = metadata;

        return item;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiEvalDatasetItem aiEvalDatasetItem)) {
            return false;
        }

        return Objects.equals(id, aiEvalDatasetItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Long getDatasetId() {
        return datasetId;
    }

    public Long getDatasetVersionId() {
        return datasetVersionId;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public Long getId() {
        return id;
    }

    public String getInput() {
        return input;
    }

    public String getMetadata() {
        return metadata;
    }

    public Long getSourceTraceId() {
        return sourceTraceId;
    }

    /**
     * Package-private. {@code datasetId} is the parent-dataset anchor and is set once at construction. Reparenting an
     * item between datasets is never legitimate business logic — it would silently break the dataset-membership
     * invariant and leak items across tenants. Kept package-visible so Spring Data JDBC reflection and same-package
     * tests work; service-layer code in other packages must construct a new item instead.
     */
    void setDatasetId(Long datasetId) {
        this.datasetId = datasetId;
    }

    /**
     * Package-private. {@code datasetVersionId} is the immutability anchor: once a {@link AiEvalDatasetVersion} is
     * frozen, its item set must not change. A public setter would let a caller reparent an item out of (or into) a
     * frozen version, breaking the comparison contract every consumer that pinned the snapshot relies on. Kept
     * package-visible for Spring Data JDBC reflection and same-package tests.
     */
    void setDatasetVersionId(Long datasetVersionId) {
        this.datasetVersionId = datasetVersionId;
    }

    /**
     * Package-private. Spring Data JDBC reflective hydration uses this on row load; legitimate cross-package edits go
     * through the service-layer "load parent → assert !frozen → delete-and-reinsert" pattern. Public exposure would
     * surface a runtime guard ({@link #assertNotPersisted}) where the compile-time boundary is the safer signal.
     */
    void setExpectedOutput(String expectedOutput) {
        assertNotPersisted("expectedOutput");

        this.expectedOutput = expectedOutput;
    }

    /**
     * Package-private. Spring Data JDBC reflective hydration uses this on row load; legitimate cross-package edits go
     * through the service-layer "load parent → assert !frozen → delete-and-reinsert" pattern.
     */
    void setInput(String input) {
        assertNotPersisted("input");
        Validate.isTrue(StringUtils.isNotBlank(input), "input must not be blank");

        this.input = input;
    }

    /**
     * Package-private. Spring Data JDBC reflective hydration uses this on row load; legitimate cross-package edits go
     * through the service-layer "load parent → assert !frozen → delete-and-reinsert" pattern.
     */
    void setMetadata(String metadata) {
        assertNotPersisted("metadata");

        this.metadata = metadata;
    }

    /**
     * Pre-persistence-only guard. The honest invariant we want to enforce is "the parent {@link AiEvalDatasetVersion}
     * is not frozen", but the item cannot reach the parent locally without an extra DB round-trip; we settle for
     * "{@code id == null}" as a strict-but-imperfect proxy. The trade-off is intentional: stricter than the real
     * invariant requires (legitimate edits to an unfrozen version's items must go through the service layer's "load
     * parent → verify → save new item" pattern), but the asymmetry is the safer side to err on — relaxing this to "load
     * the parent and check" would silently widen the surface where a frozen-version item can be mutated under a stale
     * read of the parent's frozen flag.
     *
     * <p>
     * <strong>Service-layer responsibility for legitimate edits.</strong> Editing a persisted item on an unfrozen
     * version is supported via the aggregate-rooted update pattern owned by {@code AiEvalDatasetService} /
     * {@code AiEvalDatasetVersionService}: load the parent version under pessimistic lock, assert {@code !frozen}, then
     * delete-and-reinsert (or stage on a fresh unfrozen version). The mutators are package-private so cross-package
     * callers cannot reach them; the runtime guard remains as a defence-in-depth signal for same-package callers
     * (Spring Data JDBC's reflective row-load path writes fields directly, not through these setters, so it is
     * unaffected).
     */
    private void assertNotPersisted(String fieldName) {
        if (id != null) {
            throw new IllegalStateException(
                "Cannot mutate '" + fieldName + "' on persisted AiEvalDatasetItem id=" + id
                    + " — load the parent version, verify it is not frozen, and route the update through the service "
                    + "layer (delete-and-reinsert or stage on a fresh unfrozen version)");
        }
    }

    /**
     * Records the source-trace provenance link in a one-way fashion: the link can be set at most once. Subsequent calls
     * with the same id are idempotent; calls with a different id throw, since rewriting provenance after-the-fact would
     * silently invalidate consumers (audit trails, dataset lineage UIs) that already pinned the original trace. Use
     * this method in service-layer code; the package-private {@link #setSourceTraceId} setter exists only for Spring
     * Data JDBC reflection.
     */
    public void linkSourceTrace(Long traceId) {
        Validate.notNull(traceId, "traceId must not be null");

        if (this.sourceTraceId != null && !Objects.equals(this.sourceTraceId, traceId)) {
            throw new IllegalStateException(
                "AiEvalDatasetItem source trace is already linked to " + this.sourceTraceId
                    + "; cannot relink to " + traceId);
        }

        this.sourceTraceId = traceId;
    }

    /**
     * Package-private. Direct setter exists for Spring Data JDBC reflective hydration; application code uses
     * {@link #linkSourceTrace(Long)} so a same-tenant provenance link cannot be silently overwritten.
     */
    void setSourceTraceId(Long sourceTraceId) {
        this.sourceTraceId = sourceTraceId;
    }

    @Override
    public String toString() {
        return "AiEvalDatasetItem{" +
            "id=" + id +
            ", datasetId=" + datasetId +
            ", datasetVersionId=" + datasetVersionId +
            ", sourceTraceId=" + sourceTraceId +
            '}';
    }
}
