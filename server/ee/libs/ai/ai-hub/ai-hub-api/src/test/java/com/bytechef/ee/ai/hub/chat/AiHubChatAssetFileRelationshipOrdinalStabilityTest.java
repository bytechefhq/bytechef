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
