/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.math.BigDecimal;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;

/**
 * Typed per-strategy configuration for an {@link AiGatewayRoutingPolicy}. Replaces the opaque {@code config} JSON blob
 * that historically was shared by all 9 strategies: {@code SIMPLE} rows carried no config, {@code PRIORITY_FALLBACK}
 * rows piggybacked on a {@code fallbackModel} column only it understood, and the rest of the strategies threaded their
 * knobs (targets, thresholds, weightings) through a stringly-typed JSON map with no compile-time checking.
 *
 * <p>
 * The persistence layer keeps the flat {@code (strategy, config, fallbackModel)} columns intact; this sealed hierarchy
 * is the round-trip view. Jackson serializes/deserializes via the {@code strategyType} discriminator so a row's
 * declared {@link AiGatewayRoutingStrategyType} always matches the variant reconstructed from its {@code config} JSON.
 * Construction of a mismatched pair (e.g. {@code WEIGHTED_RANDOM} strategy with a {@code PriorityFallback} config) is
 * refused at the domain boundary, not at runtime in the router.
 *
 * <p>
 * <b>Today's routers derive behavior from {@link AiGatewayModelDeployment} fields</b> (weight, priorityOrder) rather
 * than from this config — so most variants are empty today. The types are still worth expressing so that:
 * <ol>
 * <li>new tunables (e.g. latency SLOs, cost caps) have a designated home instead of growing the opaque map, and</li>
 * <li>the policy-to-strategy coupling becomes a compile-time refactor target when we rewire the router to read its
 * knobs from the policy instead of duplicating them on every deployment.</li>
 * </ol>
 *
 * @version ee
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

    /**
     * No tunables. The router falls back to the first enabled deployment in insertion order.
     */
    record Simple() implements AiGatewayRoutingStrategyConfig {
        @Override
        public AiGatewayRoutingStrategyType strategyType() {
            return AiGatewayRoutingStrategyType.SIMPLE;
        }
    }

    /**
     * Additional deployment-level tag filters beyond the policy's attached tags. Optional; null defers to the policy's
     * tag set only.
     */
    record TagBased(@Nullable String requireTag) implements AiGatewayRoutingStrategyConfig {
        @Override
        public AiGatewayRoutingStrategyType strategyType() {
            return AiGatewayRoutingStrategyType.TAG_BASED;
        }
    }

    /**
     * Weights live on {@link AiGatewayModelDeployment#getWeight()} today; this config is currently empty but reserved
     * so a future router can express "biasTowardsNewerDeployments" or similar without a new JSON shape.
     */
    record WeightedRandom() implements AiGatewayRoutingStrategyConfig {
        @Override
        public AiGatewayRoutingStrategyType strategyType() {
            return AiGatewayRoutingStrategyType.WEIGHTED_RANDOM;
        }
    }

    /**
     * Name of the fallback model to select when no deployment matches. Required — a fallback-less priority policy is a
     * {@link Simple} one in disguise.
     */
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

    /**
     * Blends cost and quality. Weights must be non-negative; they are normalized by the router, so {@code (0.3, 0.7)}
     * behaves identically to {@code (3, 7)}.
     */
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

    /**
     * Returns the appropriate empty config for a strategy type. Used when callers upgrade a raw-columns policy that has
     * no {@code config} JSON yet.
     */
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
