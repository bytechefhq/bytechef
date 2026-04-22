/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.dto;

/**
 * One day in a score time-series. {@code day} is the start-of-day UTC epoch millis; {@code average} is the arithmetic
 * mean of the day's numeric score values (null for non-NUMERIC score types or empty days).
 *
 * @version ee
 */
public record AiEvalScoreTrendPoint(long day, Double average, int count) {
}
