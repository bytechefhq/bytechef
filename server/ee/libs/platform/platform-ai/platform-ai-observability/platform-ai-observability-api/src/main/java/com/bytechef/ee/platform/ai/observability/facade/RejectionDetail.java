/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.facade;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Ivica Cardic
 * @version ee
 */
public record RejectionDetail(Integer index, RejectionCode code, String reason) {

    public static final int MAX_REASON_LENGTH = 1024;

    private static final String TRUNCATION_SUFFIX = "…";

    public RejectionDetail {
        if (code == null) {
            throw new IllegalArgumentException("code must not be null");
        }

        if (StringUtils.isBlank(reason)) {
            throw new IllegalArgumentException("reason must not be blank");
        }

        if (index != null && index < 0) {
            throw new IllegalArgumentException("index must be >= 0 when present; got " + index);
        }

        if (reason.length() > MAX_REASON_LENGTH) {
            reason = reason.substring(0, MAX_REASON_LENGTH - TRUNCATION_SUFFIX.length()) + TRUNCATION_SUFFIX;
        }
    }

    public static RejectionDetail withoutIndex(RejectionCode code, String reason) {
        return new RejectionDetail(null, code, reason);
    }

    public static RejectionDetail at(int index, RejectionCode code, String reason) {
        return new RejectionDetail(index, code, reason);
    }

    public static RejectionDetail at(int index, int batchSize, RejectionCode code, String reason) {
        if (batchSize < 0) {
            throw new IllegalArgumentException("batchSize must be >= 0; got " + batchSize);
        }

        if (index >= batchSize) {
            throw new IllegalArgumentException(
                "index (" + index + ") must be < batchSize (" + batchSize + ")");
        }

        return new RejectionDetail(index, code, reason);
    }
}
