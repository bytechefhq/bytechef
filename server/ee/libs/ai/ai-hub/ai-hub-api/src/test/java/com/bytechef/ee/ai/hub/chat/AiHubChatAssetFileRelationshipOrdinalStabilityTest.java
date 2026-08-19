/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import com.bytechef.test.assertion.OrdinalStabilityAssertions;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Pins the ordinal of {@link AiHubChatAssetFileRelationship}. Lives in the automation module because the enum itself is
 * automation-coupled (asset files are an automation domain concept). Companion to
 * {@code com.bytechef.ee.ai.hub.util.EnumOrdinalStabilityTest} for the platform-side enums.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubChatAssetFileRelationshipOrdinalStabilityTest {

    @Test
    void testChatAssetFileRelationshipOrdinalsAreStable() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("AUTHORED", 0);
        expected.put("ATTACHED", 1);
        expected.put("MENTIONED", 2);
        expected.put("READ_BY_TOOL", 3);

        OrdinalStabilityAssertions.assertOrdinalsMatch(
            AiHubChatAssetFileRelationship.values(), expected,
            AiHubChatAssetFileRelationship.class.getSimpleName());
    }
}
