/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.Validate;

/**
 * Response for single-score endpoints. Sealed sum type with two variants:
 * <ul>
 * <li>{@link Accepted}: carries the persisted {@code scoreId}.</li>
 * <li>{@link Rejected}: carries the {@code rejectionReason}.</li>
 * </ul>
 *
 * <p>
 * Use the {@link #accepted(Long)} / {@link #rejected(String)} factories — they construct the appropriate variant
 * directly. Callers dispatch via exhaustive {@code switch} on the sealed type, eliminating the previous record's
 * runtime cross-field validation in favour of compile-time guarantees.
 *
 * <p>
 * Wire shape: each variant serializes only its populated field; absent fields are omitted via
 * {@link JsonInclude.Include#NON_NULL}. Clients reading the JSON discriminate by which key is present ({@code scoreId}
 * vs. {@code rejectionReason}). No discriminator field is added.
 *
 * <p>
 * <strong>One-way (response-only) contract:</strong> this type is produced by the gateway as a response payload only.
 * Without {@code @JsonTypeInfo}, Jackson cannot reconstruct the correct sealed-interface variant from the wire bytes
 * (the absence of {@code scoreId} vs. {@code rejectionReason} is the discriminator, but Jackson's default deserialiser
 * has no rule for "pick the variant whose canonical field is present"). Promoting this to a request body — i.e.
 * accepting an {@code AiExternalScoreResult} as input — requires either a custom deserialiser keyed on which canonical
 * field is present, or {@code @JsonTypeInfo(use = DEDUCTION)} on the interface plus {@code @JsonSubTypes} on the
 * permits clause. Today no controller deserialises this type; if that changes, add the deserialisation rule before
 * exposing it as input or the type system stops helping at the wire boundary.
 *
 * @author Ivica Cardic
 * @version ee
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public sealed interface AiExternalScoreResult permits AiExternalScoreResult.Accepted, AiExternalScoreResult.Rejected {

    static AiExternalScoreResult accepted(Long scoreId) {
        return new Accepted(scoreId);
    }

    static AiExternalScoreResult rejected(String reason) {
        return new Rejected(reason);
    }

    record Accepted(Long scoreId) implements AiExternalScoreResult {

        public Accepted {
            Validate.notNull(scoreId, "accepted result must carry a scoreId");
        }
    }

    record Rejected(String rejectionReason) implements AiExternalScoreResult {

        public Rejected {
            Validate.notBlank(rejectionReason, "rejected result must carry a non-blank rejectionReason");
        }
    }
}
