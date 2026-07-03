/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.personalagent;

/**
 * Cadence kind for {@code AiHubPersonalAgentSchedule}. Persisted as INT ordinal. <strong>Append new values at the end
 * only.</strong> Ordinal stability is pinned by {@code EnumOrdinalStabilityTest}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum ScheduleFrequencyKind {
    EVERY_X_MINUTES,
    HOURLY,
    DAILY,
    WEEKLY,
    MONTHLY,
    CUSTOM_CRON
}
