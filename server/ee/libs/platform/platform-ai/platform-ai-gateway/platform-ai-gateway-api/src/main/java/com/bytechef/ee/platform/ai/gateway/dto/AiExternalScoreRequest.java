/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.dto;

import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreDataType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Ivica Cardic
 * @version ee
 */
@SuppressFBWarnings("EI")
public record AiExternalScoreRequest(
    String name,
    Object value,
    AiEvalScoreDataType dataType,
    String comment,
    String source,
    Map<String, Object> metadata) {

    public AiExternalScoreRequest {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("name must not be blank");
        }

        if (dataType == null) {
            throw new IllegalArgumentException("dataType must not be null");
        }

        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }

        ScoreValue.from(dataType, value);

        if (metadata != null) {
            metadata = Map.copyOf(metadata);
        }
    }

    public ScoreValue scoreValue() {
        return ScoreValue.from(dataType, value);
    }
}
