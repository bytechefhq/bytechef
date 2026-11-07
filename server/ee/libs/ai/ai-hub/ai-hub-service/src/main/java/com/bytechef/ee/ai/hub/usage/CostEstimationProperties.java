/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.usage;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ConfigurationProperties(prefix = "bytechef.ai-hub.cost-estimation")
public class CostEstimationProperties {

    private final Map<String, BigDecimal> toolRates = new LinkedHashMap<>();

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Map<String, BigDecimal> getToolRates() {
        return toolRates;
    }
}
