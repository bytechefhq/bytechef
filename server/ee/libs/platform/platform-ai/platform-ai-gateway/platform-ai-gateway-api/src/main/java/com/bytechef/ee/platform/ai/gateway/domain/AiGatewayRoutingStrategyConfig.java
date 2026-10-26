/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.math.BigDecimal;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "strategyType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AiGatewayRoutingStrategyConfig.Simple.class, name = "SIMPLE"),
    @JsonSubTypes.Type(value = AiGatewayRoutingStrategyConfig.TagBased.class, name = "TAG_BASED"),
    @JsonSubTypes.Type(value = AiGatewayRoutingStrategyConfig.WeightedRandom.class, name = "WEIGHTED_RANDOM"),
    @JsonSubTypes.Type(value = AiGatewayRoutingStrategyConfig.PriorityFallback.class, name = "PRIORITY_FALLBACK"),
    @JsonSubTypes.Type(value = AiGatewayRoutingStrategyConfig.LatencyOptimized.class, name = "LATENCY_OPTIMIZED"),
    @JsonSubTypes.Type(value = AiGatewayRoutingStrategyConfig.CostOptimized.class, name = "COST_OPTIMIZED"),
    @JsonSubTypes.Type(
        value = AiGatewayRoutingStrategyConfig.IntelligentBalanced.class, name = "INTELLIGENT_BALANCED"),
    @JsonSubTypes.Type(value = AiGatewayRoutingStrategyConfig.IntelligentCost.class, name = "INTELLIGENT_COST"),
    @JsonSubTypes.Type(value = AiGatewayRoutingStrategyConfig.IntelligentQuality.class, name = "INTELLIGENT_QUALITY")
})
public sealed interface AiGatewayRoutingStrategyConfig {

    AiGatewayRoutingStrategyType strategyType();

    record Simple() implements AiGatewayRoutingStrategyConfig {
        @Override
        public AiGatewayRoutingStrategyType strategyType() {
            return AiGatewayRoutingStrategyType.SIMPLE;
        }
    }

    record TagBased(@Nullable String requireTag) implements AiGatewayRoutingStrategyConfig {
        @Override
        public AiGatewayRoutingStrategyType strategyType() {
            return AiGatewayRoutingStrategyType.TAG_BASED;
        }
    }

    record WeightedRandom() implements AiGatewayRoutingStrategyConfig {
        @Override
        public AiGatewayRoutingStrategyType strategyType() {
            return AiGatewayRoutingStrategyType.WEIGHTED_RANDOM;
        }
    }

    record PriorityFallback(String fallbackModel) implements AiGatewayRoutingStrategyConfig {
        public PriorityFallback {
            Validate.notBlank(fallbackModel, "PRIORITY_FALLBACK requires a fallbackModel");
        }

        @Override
        public AiGatewayRoutingStrategyType strategyType() {
            return AiGatewayRoutingStrategyType.PRIORITY_FALLBACK;
        }
    }

    record LatencyOptimized(@Nullable Integer p95TargetMs) implements AiGatewayRoutingStrategyConfig {
        public LatencyOptimized {
            if (p95TargetMs != null) {
                Validate.isTrue(p95TargetMs > 0, "p95TargetMs must be positive");
            }
        }

        @Override
        public AiGatewayRoutingStrategyType strategyType() {
            return AiGatewayRoutingStrategyType.LATENCY_OPTIMIZED;
        }
    }

    record CostOptimized(@Nullable BigDecimal maxCostPerCallUsd) implements AiGatewayRoutingStrategyConfig {
        public CostOptimized {
            if (maxCostPerCallUsd != null) {
                Validate.isTrue(maxCostPerCallUsd.signum() > 0, "maxCostPerCallUsd must be positive");
            }
        }

        @Override
        public AiGatewayRoutingStrategyType strategyType() {
            return AiGatewayRoutingStrategyType.COST_OPTIMIZED;
        }
    }

    record IntelligentBalanced(double costWeight, double qualityWeight) implements AiGatewayRoutingStrategyConfig {
        public IntelligentBalanced {
            Validate.isTrue(costWeight >= 0, "costWeight must be non-negative");
            Validate.isTrue(qualityWeight >= 0, "qualityWeight must be non-negative");
            Validate.isTrue(costWeight + qualityWeight > 0, "at least one of costWeight/qualityWeight must be > 0");
        }

        @Override
        public AiGatewayRoutingStrategyType strategyType() {
            return AiGatewayRoutingStrategyType.INTELLIGENT_BALANCED;
        }
    }

    record IntelligentCost() implements AiGatewayRoutingStrategyConfig {
        @Override
        public AiGatewayRoutingStrategyType strategyType() {
            return AiGatewayRoutingStrategyType.INTELLIGENT_COST;
        }
    }

    record IntelligentQuality() implements AiGatewayRoutingStrategyConfig {
        @Override
        public AiGatewayRoutingStrategyType strategyType() {
            return AiGatewayRoutingStrategyType.INTELLIGENT_QUALITY;
        }
    }

    static AiGatewayRoutingStrategyConfig defaultFor(AiGatewayRoutingStrategyType strategy) {
        return switch (strategy) {
            case SIMPLE -> new Simple();
            case TAG_BASED -> new TagBased(null);
            case WEIGHTED_RANDOM -> new WeightedRandom();
            case PRIORITY_FALLBACK -> throw new IllegalArgumentException(
                "PRIORITY_FALLBACK has no default — fallbackModel must be supplied");
            case LATENCY_OPTIMIZED -> new LatencyOptimized(null);
            case COST_OPTIMIZED -> new CostOptimized(null);
            case INTELLIGENT_BALANCED -> new IntelligentBalanced(0.5, 0.5);
            case INTELLIGENT_COST -> new IntelligentCost();
            case INTELLIGENT_QUALITY -> new IntelligentQuality();
        };
    }
}
