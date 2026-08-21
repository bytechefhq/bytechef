# AI Gateway Intelligent Routing — Cost-Tier Mapping Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the AI Gateway's `INTELLIGENT_*` routing strategies classify candidate models into five cost tiers and map a 0–1 prompt-complexity score across those tiers, matching Merge Gateway's intelligent routing.

**Architecture:** Add an `AiGatewayModelTier` enum (five cost tiers) and a `PromptComplexityScorer` interface with a deterministic, model-free implementation. Rewrite `IntelligentRoutingStrategy` to map the score to a present tier per strategy (the curve generalizes today's 0.7/0.5/0.3 thresholds to N tiers and degenerates to today's behavior at two tiers). Wire the scorer into `AiGatewayFacadeImpl`, replacing the inline char-length heuristic; on scorer failure the facade falls back to score `1.0` (most-capable tier). No API/DTO/DB/config changes.

**Tech Stack:** Java 25, Spring (`@Component`, `@ConditionalOnEEVersion`), JUnit 5, Gradle. ByteChef EE license header + `@version ee` on every new file.

---

## File Structure

| File | Module | Action | Responsibility |
|------|--------|--------|----------------|
| `.../gateway/domain/AiGatewayModelTier.java` | platform-ai-gateway-api | Create | Five cost tiers (capability-ascending) + `classify(BigDecimal)` |
| `.../gateway/routing/PromptComplexityScorer.java` | platform-ai-gateway-api | Create | `double score(request)` interface |
| `.../gateway/routing/DeterministicPromptComplexityScorer.java` | platform-ai-gateway-service | Create | Model-free weighted heuristic, `@Component` |
| `.../gateway/routing/IntelligentRoutingStrategy.java` | platform-ai-gateway-service | Modify | Score → present-tier mapping per axis |
| `.../routing/AiGatewayModelTierTest.java` | platform-ai-gateway-service (test) | Create | Tier boundary tests |
| `.../routing/DeterministicPromptComplexityScorerTest.java` | platform-ai-gateway-service (test) | Create | Scorer signal tests |
| `.../routing/AiGatewayRoutingStrategyTest.java` | platform-ai-gateway-service (test) | Modify | Add multi-tier mapping tests |
| `.../gateway/facade/AiGatewayFacadeImpl.java` | automation-ai-gateway-service | Modify | Inject scorer; replace inline method |

**Path prefixes** (to keep tasks readable):
- `PLATFORM_API = server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-api/src/main/java/com/bytechef/ee/platform/ai/gateway`
- `PLATFORM_SVC = server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-service/src/main/java/com/bytechef/ee/platform/ai/gateway`
- `PLATFORM_TEST = server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-service/src/test/java/com/bytechef/ee/platform/ai/gateway`
- `AUTO_SVC = server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway`

**Gradle project paths:**
- Platform: `:server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-service`
- Automation: `:server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service`

---

## Task 1: `AiGatewayModelTier` enum + classifier

**Files:**
- Create: `$PLATFORM_API/domain/AiGatewayModelTier.java`
- Test: `$PLATFORM_TEST/routing/AiGatewayModelTierTest.java`

- [ ] **Step 1: Write the failing test**

Create `$PLATFORM_TEST/routing/AiGatewayModelTierTest.java`:

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-service:test --tests '*AiGatewayModelTierTest'`
Expected: FAIL — `AiGatewayModelTier` does not exist (compilation error).

- [ ] **Step 3: Write minimal implementation**

Create `$PLATFORM_API/domain/AiGatewayModelTier.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.domain;

import java.math.BigDecimal;

/**
 * Cost tier classification for gateway models, ordered by capability ascending (BASIC cheapest,
 * FRONTIER most capable). Thresholds are USD per 1M output tokens, mirroring Merge Gateway's
 * intelligent routing tiers. A model with unknown (null) pricing is treated as the most capable
 * tier so it is never silently selected as the "cheap" option.
 *
 * @version ee
 */
public enum AiGatewayModelTier {

    BASIC,
    EFFICIENT,
    STANDARD,
    ADVANCED,
    FRONTIER;

    private static final BigDecimal EFFICIENT_FLOOR = new BigDecimal("0.10");
    private static final BigDecimal STANDARD_FLOOR = new BigDecimal("1.50");
    private static final BigDecimal ADVANCED_FLOOR = new BigDecimal("2.00");
    private static final BigDecimal FRONTIER_FLOOR = new BigDecimal("5.00");

    public static AiGatewayModelTier classify(BigDecimal outputCostPerMTokens) {
        if (outputCostPerMTokens == null) {
            return FRONTIER;
        }

        if (outputCostPerMTokens.compareTo(FRONTIER_FLOOR) >= 0) {
            return FRONTIER;
        }

        if (outputCostPerMTokens.compareTo(ADVANCED_FLOOR) >= 0) {
            return ADVANCED;
        }

        if (outputCostPerMTokens.compareTo(STANDARD_FLOOR) >= 0) {
            return STANDARD;
        }

        if (outputCostPerMTokens.compareTo(EFFICIENT_FLOOR) >= 0) {
            return EFFICIENT;
        }

        return BASIC;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-service:test --tests '*AiGatewayModelTierTest'`
Expected: PASS (7 tests).

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-api/src/main/java/com/bytechef/ee/platform/ai/gateway/domain/AiGatewayModelTier.java \
        server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-service/src/test/java/com/bytechef/ee/platform/ai/gateway/routing/AiGatewayModelTierTest.java
git commit -m "4749 Add AiGatewayModelTier cost-tier classification"
```

---

## Task 2: `PromptComplexityScorer` interface + deterministic implementation

**Files:**
- Create: `$PLATFORM_API/routing/PromptComplexityScorer.java`
- Create: `$PLATFORM_SVC/routing/DeterministicPromptComplexityScorer.java`
- Test: `$PLATFORM_TEST/routing/DeterministicPromptComplexityScorerTest.java`

- [ ] **Step 1: Write the failing test**

Create `$PLATFORM_TEST/routing/DeterministicPromptComplexityScorerTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatMessage;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatRole;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayTool;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class DeterministicPromptComplexityScorerTest {

    private final DeterministicPromptComplexityScorer scorer = new DeterministicPromptComplexityScorer();

    private static AiGatewayChatCompletionRequest request(
        List<AiGatewayChatMessage> messages, Integer maxTokens, List<AiGatewayTool> tools) {

        return new AiGatewayChatCompletionRequest(
            "gpt-4", messages, null, maxTokens, null, false, null, null, null, tools);
    }

    private static AiGatewayChatMessage user(String content) {
        return new AiGatewayChatMessage(AiGatewayChatRole.USER, content);
    }

    @Test
    void testShortPromptScoresLow() {
        double score = scorer.score(request(List.of(user("hi")), null, null));

        assertTrue(score < 0.2, "expected low score, got " + score);
    }

    @Test
    void testLongMultiToolPromptScoresHigh() {
        String longText = "x".repeat(8000);

        AiGatewayTool tool = new AiGatewayTool(
            "function", new AiGatewayTool.AiGatewayToolFunction("f", "d", java.util.Map.of()));

        double score = scorer.score(
            request(List.of(user(longText)), 4000, List.of(tool, tool, tool, tool, tool, tool, tool, tool)));

        assertTrue(score > 0.7, "expected high score, got " + score);
    }

    @Test
    void testCodeContentScoresAboveEqualLengthProse() {
        String prose = "please summarize the following text ".repeat(10);
        String code = "```json\n{\"a\": 1, \"b\": 2}\n``` ".repeat(10);

        double proseScore = scorer.score(request(List.of(user(prose)), null, null));
        double codeScore = scorer.score(request(List.of(user(code)), null, null));

        assertTrue(codeScore > proseScore, "code " + codeScore + " should exceed prose " + proseScore);
    }

    @Test
    void testScoreClampedToUnitInterval() {
        String huge = "x".repeat(100000);

        double score = scorer.score(request(List.of(user(huge)), 100000, null));

        assertTrue(score <= 1.0 && score >= 0.0, "score out of range: " + score);
    }

    @Test
    void testNullContentAndNullFieldsContributeZero() {
        AiGatewayChatMessage empty = new AiGatewayChatMessage(AiGatewayChatRole.USER, null);

        double score = scorer.score(request(List.of(empty), null, null));

        assertEquals(0.0, score, 0.0001);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-service:test --tests '*DeterministicPromptComplexityScorerTest'`
Expected: FAIL — `PromptComplexityScorer` / `DeterministicPromptComplexityScorer` do not exist.

- [ ] **Step 3a: Write the interface**

Create `$PLATFORM_API/routing/PromptComplexityScorer.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.routing;

import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;

/**
 * Produces a 0.0 (simple) to 1.0 (complex) complexity score for a chat completion request, used by
 * the intelligent routing strategies to map a request onto a model cost tier. The default
 * implementation is deterministic and model-free; this interface is the seam for a future
 * embedding-based scorer.
 *
 * @version ee
 */
public interface PromptComplexityScorer {

    double score(AiGatewayChatCompletionRequest request);
}
```

- [ ] **Step 3b: Write the implementation**

Create `$PLATFORM_SVC/routing/DeterministicPromptComplexityScorer.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.routing;

import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatMessage;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayTool;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Deterministic, model-free prompt complexity scorer. Combines prompt size, tool count, structured
 * (code/JSON) content, conversation turns, and requested output size into a 0.0–1.0 score. No LLM
 * or embedding call is made, keeping the routing hot path cheap.
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class DeterministicPromptComplexityScorer implements PromptComplexityScorer {

    private static final double SIZE_WEIGHT = 0.35;
    private static final double TOOL_WEIGHT = 0.25;
    private static final double STRUCTURED_WEIGHT = 0.20;
    private static final double TURNS_WEIGHT = 0.10;
    private static final double OUTPUT_WEIGHT = 0.10;

    private static final double SIZE_TOKEN_CEILING = 2000.0;
    private static final double TOOL_COUNT_CEILING = 8.0;
    private static final double TURNS_CEILING = 19.0;
    private static final double OUTPUT_TOKEN_CEILING = 4000.0;

    @Override
    public double score(AiGatewayChatCompletionRequest request) {
        List<AiGatewayChatMessage> messages = request.messages();

        int totalChars = 0;
        boolean structured = false;

        for (AiGatewayChatMessage message : messages) {
            String content = message.content();

            if (content != null) {
                totalChars += content.length();

                if (containsStructuredContent(content)) {
                    structured = true;
                }
            }
        }

        int estimatedTokens = totalChars / 4;
        int toolCount = request.tools() == null ? 0 : request.tools()
            .size();
        int messageCount = messages.size();
        Integer maxTokens = request.maxTokens();

        double sizeScore = Math.min(estimatedTokens / SIZE_TOKEN_CEILING, 1.0);
        double toolScore = Math.min(toolCount / TOOL_COUNT_CEILING, 1.0);
        double structuredScore = structured ? 1.0 : 0.0;
        double turnsScore = Math.min(Math.max(messageCount - 1, 0) / TURNS_CEILING, 1.0);
        double outputScore = maxTokens == null ? 0.0 : Math.min(maxTokens / OUTPUT_TOKEN_CEILING, 1.0);

        double weighted = SIZE_WEIGHT * sizeScore
            + TOOL_WEIGHT * toolScore
            + STRUCTURED_WEIGHT * structuredScore
            + TURNS_WEIGHT * turnsScore
            + OUTPUT_WEIGHT * outputScore;

        return Math.max(0.0, Math.min(weighted, 1.0));
    }

    private static boolean containsStructuredContent(String content) {
        if (content.contains("```")) {
            return true;
        }

        return content.contains("{") && content.contains("}") && content.contains(":");
    }
}
```

> Note: verify the import for `@ConditionalOnEEVersion` matches the one used by `AiGatewayRouterImpl` in the same package (`com.bytechef.platform.annotation.ConditionalOnEEVersion`). If it differs there, use that exact import.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-service:test --tests '*DeterministicPromptComplexityScorerTest'`
Expected: PASS (5 tests).

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-api/src/main/java/com/bytechef/ee/platform/ai/gateway/routing/PromptComplexityScorer.java \
        server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-service/src/main/java/com/bytechef/ee/platform/ai/gateway/routing/DeterministicPromptComplexityScorer.java \
        server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-service/src/test/java/com/bytechef/ee/platform/ai/gateway/routing/DeterministicPromptComplexityScorerTest.java
git commit -m "4749 Add deterministic prompt complexity scorer"
```

---

## Task 3: Rewrite `IntelligentRoutingStrategy` for multi-tier mapping

**Files:**
- Modify: `$PLATFORM_SVC/routing/IntelligentRoutingStrategy.java` (full replacement)
- Test: `$PLATFORM_TEST/routing/AiGatewayRoutingStrategyTest.java` (add tests)

**Mapping algorithm:** classify each deployment's model into an `AiGatewayModelTier`; collect the distinct tiers present in the policy, sorted capability-ascending (enum ordinal); transform the score per strategy, then `index = clamp(floor(effective * presentTierCount), 0, presentTierCount - 1)`; pick the cheapest deployment within the selected tier (deterministic). Per-strategy transform:
- `INTELLIGENT_COST`: `effective = score²` (skews cheap; at 2 tiers, switches at score ≈ 0.707)
- `INTELLIGENT_BALANCED`: `effective = score` (linear; at 2 tiers, switches at 0.5)
- `INTELLIGENT_QUALITY`: `effective = 1 − (1 − score)²` (skews capable; at 2 tiers, switches at score ≈ 0.293)

This reproduces today's 0.7/0.5/0.3 thresholds when exactly two tiers are present, so the existing `testIntelligent*` tests remain valid.

- [ ] **Step 1: Write the failing tests (append to existing test class)**

In `$PLATFORM_TEST/routing/AiGatewayRoutingStrategyTest.java`, add these methods before the final closing brace:

```java
    @Test
    void testIntelligentBalancedThreeTiersMidScoreRoutesMiddleTier() {
        IntelligentRoutingStrategy strategy = new IntelligentRoutingStrategy(
            AiGatewayRoutingStrategyType.INTELLIGENT_BALANCED);

        AiGatewayModelDeployment basicDeployment = new AiGatewayModelDeployment(1L, 100L);
        AiGatewayModelDeployment standardDeployment = new AiGatewayModelDeployment(1L, 200L);
        AiGatewayModelDeployment frontierDeployment = new AiGatewayModelDeployment(1L, 300L);

        AiGatewayModel basicModel = new AiGatewayModel(1L, "basic-model");

        basicModel.setOutputCostPerMTokens(new BigDecimal("0.05"));

        AiGatewayModel standardModel = new AiGatewayModel(2L, "standard-model");

        standardModel.setOutputCostPerMTokens(new BigDecimal("1.75"));

        AiGatewayModel frontierModel = new AiGatewayModel(3L, "frontier-model");

        frontierModel.setOutputCostPerMTokens(new BigDecimal("6.00"));

        Map<Long, AiGatewayModel> modelMap = Map.of(100L, basicModel, 200L, standardModel, 300L, frontierModel);

        AiGatewayRoutingContext context = new AiGatewayRoutingContext(
            Map.of(), modelMap, 0.5, Map.of(), Map.of());

        AiGatewayModelDeployment selected = strategy.selectDeployment(
            List.of(basicDeployment, standardDeployment, frontierDeployment), context);

        assertEquals(standardDeployment, selected);
    }

    @Test
    void testIntelligentQualityThreeTiersHighScoreRoutesFrontier() {
        IntelligentRoutingStrategy strategy = new IntelligentRoutingStrategy(
            AiGatewayRoutingStrategyType.INTELLIGENT_QUALITY);

        AiGatewayModelDeployment basicDeployment = new AiGatewayModelDeployment(1L, 100L);
        AiGatewayModelDeployment standardDeployment = new AiGatewayModelDeployment(1L, 200L);
        AiGatewayModelDeployment frontierDeployment = new AiGatewayModelDeployment(1L, 300L);

        AiGatewayModel basicModel = new AiGatewayModel(1L, "basic-model");

        basicModel.setOutputCostPerMTokens(new BigDecimal("0.05"));

        AiGatewayModel standardModel = new AiGatewayModel(2L, "standard-model");

        standardModel.setOutputCostPerMTokens(new BigDecimal("1.75"));

        AiGatewayModel frontierModel = new AiGatewayModel(3L, "frontier-model");

        frontierModel.setOutputCostPerMTokens(new BigDecimal("6.00"));

        Map<Long, AiGatewayModel> modelMap = Map.of(100L, basicModel, 200L, standardModel, 300L, frontierModel);

        AiGatewayRoutingContext context = new AiGatewayRoutingContext(
            Map.of(), modelMap, 0.95, Map.of(), Map.of());

        AiGatewayModelDeployment selected = strategy.selectDeployment(
            List.of(basicDeployment, standardDeployment, frontierDeployment), context);

        assertEquals(frontierDeployment, selected);
    }

    @Test
    void testIntelligentScoreOneRoutesMostCapableTier() {
        IntelligentRoutingStrategy strategy = new IntelligentRoutingStrategy(
            AiGatewayRoutingStrategyType.INTELLIGENT_COST);

        AiGatewayModelDeployment basicDeployment = new AiGatewayModelDeployment(1L, 100L);
        AiGatewayModelDeployment frontierDeployment = new AiGatewayModelDeployment(1L, 300L);

        AiGatewayModel basicModel = new AiGatewayModel(1L, "basic-model");

        basicModel.setOutputCostPerMTokens(new BigDecimal("0.05"));

        AiGatewayModel frontierModel = new AiGatewayModel(3L, "frontier-model");

        frontierModel.setOutputCostPerMTokens(new BigDecimal("6.00"));

        Map<Long, AiGatewayModel> modelMap = Map.of(100L, basicModel, 300L, frontierModel);

        AiGatewayRoutingContext context = new AiGatewayRoutingContext(
            Map.of(), modelMap, 1.0, Map.of(), Map.of());

        AiGatewayModelDeployment selected = strategy.selectDeployment(
            List.of(basicDeployment, frontierDeployment), context);

        assertEquals(frontierDeployment, selected);
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-service:test --tests '*AiGatewayRoutingStrategyTest'`
Expected: FAIL — new tests fail because the current binary strategy never selects the middle tier (`testIntelligentBalancedThreeTiers...` returns the cheapest, not standard).

- [ ] **Step 3: Replace the strategy implementation**

Replace the entire body of `$PLATFORM_SVC/routing/IntelligentRoutingStrategy.java` with:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.routing;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModel;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModelDeployment;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModelTier;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRoutingStrategyType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Tier-based intelligent routing. Classifies each candidate deployment's model into an
 * {@link AiGatewayModelTier} cost tier, then maps the request's 0.0–1.0 prompt complexity score onto
 * the tiers actually present in the policy. The per-axis score transform skews the mapping:
 * <ul>
 * <li>INTELLIGENT_COST: complexity² — most prompts go to cheaper tiers</li>
 * <li>INTELLIGENT_BALANCED: complexity — linear spread across tiers</li>
 * <li>INTELLIGENT_QUALITY: 1 − (1 − complexity)² — most prompts go to capable tiers</li>
 * </ul>
 * With exactly two present tiers these transforms reproduce the legacy 0.7 / 0.5 / 0.3 thresholds.
 *
 * @version ee
 */
class IntelligentRoutingStrategy implements AiGatewayRoutingStrategy {

    private final AiGatewayRoutingStrategyType axis;

    IntelligentRoutingStrategy(AiGatewayRoutingStrategyType axis) {
        this.axis = axis;
    }

    @Override
    public AiGatewayModelDeployment selectDeployment(
        List<AiGatewayModelDeployment> deployments, AiGatewayRoutingContext context) {

        if (deployments.size() == 1) {
            return deployments.getFirst();
        }

        List<AiGatewayModelTier> presentTiers = deployments.stream()
            .map(deployment -> tierOf(deployment, context))
            .distinct()
            .sorted(Comparator.comparingInt(Enum::ordinal))
            .toList();

        double effective = transform(context.promptComplexityScore());

        int index = (int) Math.floor(effective * presentTiers.size());

        index = Math.max(0, Math.min(index, presentTiers.size() - 1));

        AiGatewayModelTier selectedTier = presentTiers.get(index);

        return deployments.stream()
            .filter(deployment -> tierOf(deployment, context) == selectedTier)
            .min(Comparator
                .comparing((AiGatewayModelDeployment deployment) -> outputCost(deployment, context))
                .thenComparing(AiGatewayModelDeployment::getModelId))
            .orElseGet(deployments::getFirst);
    }

    private double transform(double score) {
        return switch (axis) {
            case INTELLIGENT_COST -> score * score;
            case INTELLIGENT_BALANCED -> score;
            case INTELLIGENT_QUALITY -> 1.0 - (1.0 - score) * (1.0 - score);
            default -> throw new IllegalArgumentException(
                "IntelligentRoutingStrategy does not support axis: " + axis);
        };
    }

    private static AiGatewayModelTier tierOf(AiGatewayModelDeployment deployment, AiGatewayRoutingContext context) {
        return AiGatewayModelTier.classify(outputCost(deployment, context));
    }

    private static BigDecimal outputCost(AiGatewayModelDeployment deployment, AiGatewayRoutingContext context) {
        AiGatewayModel model = context.modelMap()
            .get(deployment.getModelId());

        if (model == null) {
            return null;
        }

        return model.getOutputCostPerMTokens();
    }
}
```

> Note: `outputCost` may return `null`; `AiGatewayModelTier.classify(null)` returns FRONTIER, and `Comparator.comparing` with a null key would throw. The `min(...)` only ever compares deployments **within the same selected tier**, but a tier can still contain multiple null-priced models. To be safe, the tie-break comparator below sorts nulls last. Replace the `.min(...)` chain with the null-safe form:

```java
        return deployments.stream()
            .filter(deployment -> tierOf(deployment, context) == selectedTier)
            .min(Comparator
                .comparing(
                    (AiGatewayModelDeployment deployment) -> outputCost(deployment, context),
                    Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(AiGatewayModelDeployment::getModelId))
            .orElseGet(deployments::getFirst);
```

Use the null-safe form. Remove the `ArrayList` import if unused after editing (Spotless/checkstyle will flag unused imports).

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-service:test --tests '*AiGatewayRoutingStrategyTest'`
Expected: PASS — all legacy `testIntelligent*` tests plus the three new multi-tier tests pass.

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-service/src/main/java/com/bytechef/ee/platform/ai/gateway/routing/IntelligentRoutingStrategy.java \
        server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-service/src/test/java/com/bytechef/ee/platform/ai/gateway/routing/AiGatewayRoutingStrategyTest.java
git commit -m "4749 Map intelligent routing across five cost tiers"
```

---

## Task 4: Wire the scorer into `AiGatewayFacadeImpl`

**Files:**
- Modify: `$AUTO_SVC/facade/AiGatewayFacadeImpl.java`

The facade currently computes the score inline via `estimatePromptComplexity(request)` and defines that private static method. Replace with the injected `PromptComplexityScorer`, falling back to score `1.0` (most-capable tier) on failure.

- [ ] **Step 1: Add the import**

Add to the import block (alphabetical position among `com.bytechef.ee.platform.ai.gateway.*` imports):

```java
import com.bytechef.ee.platform.ai.gateway.routing.PromptComplexityScorer;
```

- [ ] **Step 2: Add the field**

Among the `private final` fields (placed alphabetically; it sorts just after the `aiGateway*` fields and before `applicationEventPublisher`/`meterRegistryProvider` — match the file's existing ordering), add:

```java
    private final PromptComplexityScorer promptComplexityScorer;
```

- [ ] **Step 3: Add the constructor parameter and assignment**

In the `AiGatewayFacadeImpl(...)` constructor, add a parameter `PromptComplexityScorer promptComplexityScorer` (same relative position as the field) and, in the body, the assignment:

```java
        this.promptComplexityScorer = promptComplexityScorer;
```

- [ ] **Step 4: Replace the inline score computation**

Find (currently around line 1000):

```java
        double promptComplexityScore = estimatePromptComplexity(request);
```

Replace with:

```java
        double promptComplexityScore;

        try {
            promptComplexityScore = promptComplexityScorer.score(request);
        } catch (Exception exception) {
            log.warn("Prompt complexity scoring failed; falling back to most capable model", exception);

            promptComplexityScore = 1.0;
        }
```

- [ ] **Step 5: Delete the obsolete private method**

Delete the entire `private static double estimatePromptComplexity(AiGatewayChatCompletionRequest request) { ... }` method (currently around lines 1103–1119, ending at its closing brace).

- [ ] **Step 6: Compile and run module checks**

Run:
```bash
./gradlew spotlessApply
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:compileJava
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:test
```
Expected: compiles; existing facade tests pass. If a facade unit test constructs `AiGatewayFacadeImpl` directly, add a `PromptComplexityScorer` argument (e.g. `new DeterministicPromptComplexityScorer()` or a mock returning a fixed score) to the constructor call in that test.

- [ ] **Step 7: Commit**

```bash
git add server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/facade/AiGatewayFacadeImpl.java
git commit -m "4749 Inject prompt complexity scorer into AI gateway facade"
```

---

## Final Verification

- [ ] **Run both module test suites + static analysis**

```bash
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-service:check \
          :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:check
```
Expected: BUILD SUCCESSFUL — all tests, spotless, checkstyle, PMD, SpotBugs pass.

- [ ] **Confirm scope:** no changes to `*.graphqls`, public-rest OpenAPI, Liquibase changelogs, or `AiGatewayRoutingStrategyType` enum. `git diff --name-only <base>..HEAD` should list only the eight files from the File Structure table.

---

## Spec Coverage Check

- Five cost tiers (verbatim thresholds) → Task 1 (`AiGatewayModelTier.classify`).
- Score → tier mapping per strategy (COST ~70% cheap / BALANCED ~50/50 / QUALITY only-simple-cheap) → Task 3 (`transform` curves + present-tier indexing).
- Deterministic model-free scorer behind a swappable interface → Task 2 (`PromptComplexityScorer` + `DeterministicPromptComplexityScorer`); continuous score replaces `{0.2,0.5,0.8}` buckets.
- Fallback to most-capable on scorer failure → Task 4 (try/catch → score `1.0`, which maps to the top present tier under every axis transform).
- No API/DTO/DB/config changes → Final Verification scope check.
- Agent-judge kept out of routing → no LLM/`AiEvalExecutor` reference anywhere in these tasks.
