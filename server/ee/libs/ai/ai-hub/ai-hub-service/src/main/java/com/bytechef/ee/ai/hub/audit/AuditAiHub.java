/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative AI Hub audit annotation. Methods annotated with this publish an {@link AiHubAuditEvent} after successful
 * completion. SpEL expressions in {@link AuditData#value()} are evaluated against method parameters
 * ({@code #paramName}) and the return value ({@code #result}). Unlike {@code @AuditConnection}, this annotation has no
 * first-class subject-id slot — AI Hub events don't share a single subject, so everything goes through {@link #data()}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AuditAiHub {

    AiHubAuditEvent event();

    AuditData[] data() default {};

    /**
     * When {@code true}, the aspect opens an {@code AuditCorrelation} scope around the method so nested audited calls
     * inherit the same correlation ID under {@code data["correlationId"]}. Reserved for umbrella facade methods; the
     * annotation default is {@code false}.
     */
    boolean establishCorrelation() default false;

    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface AuditData {

        String key();

        /**
         * SpEL expression. Examples: {@code "#workspaceId"}, {@code "#result.name"}, {@code "'voiceWebhookUrl'"}.
         */
        String value();
    }
}
