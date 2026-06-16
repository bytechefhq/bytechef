/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative connection audit annotation. Methods annotated with this will automatically publish a
 * {@link ConnectionAuditEvent} after successful completion. SpEL expressions in {@link #connectionId()} and
 * {@link AuditData#value()} are evaluated against method parameters ({@code #paramName}) and the return value
 * ({@code #result}).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AuditConnection {

    /**
     * The audit event type to publish.
     */
    ConnectionAuditEvent event();

    /**
     * SpEL expression resolving to the connection ID. Examples: {@code "#connectionId"}, {@code "#result"}.
     */
    String connectionId();

    /**
     * Additional key-value pairs to include in the audit event data. Each value is a SpEL expression.
     */
    AuditData[] data() default {};

    /**
     * When {@code true}, the aspect opens a correlation scope (see {@code AuditCorrelation}) around the method
     * invocation. Nested audited calls picked up during the invocation inherit the correlation ID in their emitted
     * event data under the {@code correlationId} key, letting downstream consumers reassemble the parent/child
     * relationship. Defaults to {@code false}; enable only on "umbrella" facade methods (e.g. bulk-share replacement)
     * whose audit event is the parent of several per-row child events.
     */
    boolean establishCorrelation() default false;

    /**
     * A key-value pair for audit event data. The value is a SpEL expression evaluated against method parameters and the
     * return value.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface AuditData {

        String key();

        /**
         * SpEL expression. Examples: {@code "#projectId"}, {@code "'PRIVATE'"}, {@code "#result.name()"}.
         */
        String value();
    }
}
