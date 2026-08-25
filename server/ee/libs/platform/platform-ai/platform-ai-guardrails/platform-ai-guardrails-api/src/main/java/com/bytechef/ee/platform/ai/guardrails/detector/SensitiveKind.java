/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.detector;

/**
 * The two categories of sensitive data the guardrail policy layer can toggle independently. Deliberately closed: these
 * two values map one-to-one onto the {@code redactPii} / {@code redactSecrets} settings that already exist on both
 * {@code AiGuardrailsWorkspaceSettings} and the gateway's {@code AiGatewayProjectSettings}, and a third value would
 * leave those toggles unable to decide which spans they govern. The open axis is {@link SensitiveSpan#category()} — a
 * detector reporting a new entity type varies the category, never the kind.
 *
 * <p>
 * Not persisted anywhere, so ordinal stability is not a concern here.
 * </p>
 *
 * @version ee
 */
public enum SensitiveKind {

    PII,
    SECRET
}
