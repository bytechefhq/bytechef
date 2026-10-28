/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ee.platform.ai.agent.eval.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bytechef.ee.platform.ai.agent.eval.constant.AiAgentEvalResultConclusion;
import com.bytechef.ee.platform.ai.agent.eval.constant.AiAgentEvalResultStatus;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AiAgentEvalResultTest {

    @Test
    void testStoredConclusionWins() {
        AiAgentEvalResult evalResult = buildResult(AiAgentEvalResultStatus.COMPLETED, null);

        evalResult.setConclusion(AiAgentEvalResultConclusion.INCONCLUSIVE);

        assertEquals(AiAgentEvalResultConclusion.INCONCLUSIVE, evalResult.getConclusion());
    }

    @Test
    void testRowWithoutConclusionIsDerivedFromScore() {
        assertEquals(
            AiAgentEvalResultConclusion.PASS,
            buildResult(AiAgentEvalResultStatus.COMPLETED, 1.0).getConclusion());
        assertEquals(
            AiAgentEvalResultConclusion.FAIL,
            buildResult(AiAgentEvalResultStatus.COMPLETED, 0.5).getConclusion());
    }

    @Test
    void testRowWithoutConclusionOrScoreHasNoConclusion() {
        assertNull(buildResult(AiAgentEvalResultStatus.COMPLETED, null).getConclusion());
        assertNull(buildResult(AiAgentEvalResultStatus.FAILED, 1.0).getConclusion());
    }

    private static AiAgentEvalResult buildResult(AiAgentEvalResultStatus status, Double score) {
        AiAgentEvalResult evalResult = new AiAgentEvalResult();

        evalResult.setStatus(status);
        evalResult.setScore(score);

        return evalResult;
    }
}
