/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AuditFailureCounter {

    static final String METER_NAME = "bytechef.audit.failed";

    private static final String EVENT_TAG = "event";

    private final @Nullable MeterRegistry meterRegistry;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public AuditFailureCounter(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    public void increment(@Nullable String eventType) {
        if (meterRegistry == null) {
            return;
        }

        meterRegistry.counter(METER_NAME, EVENT_TAG, eventType == null ? "UNKNOWN" : eventType)
            .increment();
    }
}
