/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.detector;

import java.util.List;

/**
 * Constructs the detectors the guardrail engine ships with, for callers that assemble a {@link SensitiveDataRedactor}
 * outside a Spring context — {@code AiGuardrails}' legacy constructor and unit tests. In a running application the same
 * two detectors are contributed as beans and injected instead; both detectors are stateless, so the two paths are
 * interchangeable.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class SensitiveDataDetectors {

    private SensitiveDataDetectors() {
    }

    /**
     * Returns the built-in regex detectors.
     *
     * @return the PII and secret detectors
     */
    public static List<SensitiveDataDetector> builtIn() {
        return List.of(new RegexPiiDetector(), new RegexSecretDetector());
    }
}
