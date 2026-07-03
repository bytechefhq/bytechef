/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.personalagent;

/**
 * Lifecycle kind for {@code AiHubPersonalAgentSchedule} — UI affordance: did the user pick "Recurring" or "Number of
 * runs"? The effective behavior is governed by {@code max_runs} (null = unbounded). Persisted as INT ordinal;
 * append-only.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum ScheduleLifecycleKind {
    RECURRING,
    NUMBER_OF_RUNS
}
