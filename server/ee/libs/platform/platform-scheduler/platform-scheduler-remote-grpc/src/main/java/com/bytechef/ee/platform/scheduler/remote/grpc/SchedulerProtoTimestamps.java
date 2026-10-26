/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.remote.grpc;

import com.google.protobuf.Timestamp;
import java.time.Instant;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class SchedulerProtoTimestamps {

    private SchedulerProtoTimestamps() {
    }

    public static Instant toInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    public static Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder()
            .setSeconds(instant.getEpochSecond())
            .setNanos(instant.getNano())
            .build();
    }
}
