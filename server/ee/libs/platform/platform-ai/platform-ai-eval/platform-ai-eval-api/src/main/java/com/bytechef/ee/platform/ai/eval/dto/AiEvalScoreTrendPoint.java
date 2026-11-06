/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.dto;

/**
 * @version ee
 */
public record AiEvalScoreTrendPoint(long day, Double average, int count) {
}
