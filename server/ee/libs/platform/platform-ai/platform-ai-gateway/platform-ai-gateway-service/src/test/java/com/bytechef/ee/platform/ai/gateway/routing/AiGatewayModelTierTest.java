/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModelTier;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Tests for the domain tier classifier. Located in the {@code routing} package for co-location with the routing
 * strategy tests that consume {@link AiGatewayModelTier}.
 *
 * @version ee
 */
class AiGatewayModelTierTest {

    @Test
    void testClassifyFrontierAtBoundary() {
        assertEquals(AiGatewayModelTier.FRONTIER, AiGatewayModelTier.classify(new BigDecimal("5.00")));
        assertEquals(AiGatewayModelTier.FRONTIER, AiGatewayModelTier.classify(new BigDecimal("30.00")));
    }

    @Test
    void testClassifyAdvanced() {
        assertEquals(AiGatewayModelTier.ADVANCED, AiGatewayModelTier.classify(new BigDecimal("2.00")));
        assertEquals(AiGatewayModelTier.ADVANCED, AiGatewayModelTier.classify(new BigDecimal("4.99")));
    }

    @Test
    void testClassifyStandard() {
        assertEquals(AiGatewayModelTier.STANDARD, AiGatewayModelTier.classify(new BigDecimal("1.50")));
        assertEquals(AiGatewayModelTier.STANDARD, AiGatewayModelTier.classify(new BigDecimal("1.99")));
    }

    @Test
    void testClassifyEfficient() {
        assertEquals(AiGatewayModelTier.EFFICIENT, AiGatewayModelTier.classify(new BigDecimal("0.10")));
        assertEquals(AiGatewayModelTier.EFFICIENT, AiGatewayModelTier.classify(new BigDecimal("1.49")));
    }

    @Test
    void testClassifyBasic() {
        assertEquals(AiGatewayModelTier.BASIC, AiGatewayModelTier.classify(new BigDecimal("0.099")));
        assertEquals(AiGatewayModelTier.BASIC, AiGatewayModelTier.classify(BigDecimal.ZERO));
    }

    @Test
    void testClassifyNullPricingTreatedAsMostCapable() {
        assertEquals(AiGatewayModelTier.FRONTIER, AiGatewayModelTier.classify(null));
    }

    @Test
    void testCapabilityOrderingAscending() {
        assertEquals(0, AiGatewayModelTier.BASIC.ordinal());
        assertEquals(4, AiGatewayModelTier.FRONTIER.ordinal());
    }
}
