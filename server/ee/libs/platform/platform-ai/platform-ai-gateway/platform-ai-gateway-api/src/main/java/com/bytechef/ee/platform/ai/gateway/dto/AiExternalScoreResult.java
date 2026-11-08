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
