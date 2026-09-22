/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.metric;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;

/**
 * The {@code bytechef_audit_failure} counters, one per {@link Reason}, tagged {@code reason}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AuditFailureCounters {

    public static final String METRIC_NAME = "bytechef_audit_failure";
    public static final String REASON_TAG = "reason";

    private final Map<Reason, Counter> counters;

    private AuditFailureCounters(Map<Reason, Counter> counters) {
        this.counters = counters;
    }

    public static AuditFailureCounters register(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        MeterRegistry meterRegistry = Objects.requireNonNullElseGet(
            meterRegistryProvider.getIfAvailable(), SimpleMeterRegistry::new);

        Map<Reason, Counter> counters = new EnumMap<>(Reason.class);

        for (Reason reason : Reason.values()) {
            counters.put(
                reason,
                Counter.builder(METRIC_NAME)
                    .description(
                        "Number of audit events that failed to be captured, mapped or persisted. Non-zero values "
                            + "indicate a gap in the audit trail.")
                    .tag(REASON_TAG, reason.getTagValue())
                    .register(meterRegistry));
        }

        return new AuditFailureCounters(counters);
    }

    public void increment(Reason reason) {
        increment(reason, 1);
    }

    public void increment(Reason reason, double amount) {
        Counter counter = counters.get(reason);

        counter.increment(amount);
    }

    public enum Reason {

        BUILD, CAPTURE, MAPPER, PERSIST, READ_ONLY, ROLLBACK, WRITE;

        public String getTagValue() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
