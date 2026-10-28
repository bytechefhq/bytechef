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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.ee.platform.ai.agent.eval.constant.AiAgentJudgeVerdictStatus;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
@SuppressWarnings("deprecation")
class AiAgentJudgeVerdictTest {

    @Test
    void testSetStatusAlsoWritesPassedForThePreviousRelease() {
        AiAgentJudgeVerdict passedVerdict = new AiAgentJudgeVerdict();

        passedVerdict.setStatus(AiAgentJudgeVerdictStatus.PASS);

        assertTrue(passedVerdict.isPassed());

        AiAgentJudgeVerdict erroredVerdict = new AiAgentJudgeVerdict();

        erroredVerdict.setStatus(AiAgentJudgeVerdictStatus.ERROR);

        assertFalse(erroredVerdict.isPassed());
    }

    @Test
    void testRowWithoutStatusFallsBackToPassed() throws ReflectiveOperationException {
        AiAgentJudgeVerdict passedRow = new AiAgentJudgeVerdict();

        setField(passedRow, "passed", true);

        assertEquals(AiAgentJudgeVerdictStatus.PASS, passedRow.getStatus());
        assertEquals(AiAgentJudgeVerdictStatus.FAIL, new AiAgentJudgeVerdict().getStatus());
    }

    @Test
    void testScoreIsStoredNonNullButHiddenForUndecidedVerdicts() throws ReflectiveOperationException {
        AiAgentJudgeVerdict erroredVerdict = new AiAgentJudgeVerdict();

        erroredVerdict.setStatus(AiAgentJudgeVerdictStatus.ERROR);
        erroredVerdict.setScore(null);

        assertNull(erroredVerdict.getScore());
        assertEquals(0.0, getField(erroredVerdict, "score"));

        AiAgentJudgeVerdict failedVerdict = new AiAgentJudgeVerdict();

        failedVerdict.setStatus(AiAgentJudgeVerdictStatus.FAIL);
        failedVerdict.setScore(0.75);

        assertEquals(0.75, failedVerdict.getScore());
    }

    private static Object getField(AiAgentJudgeVerdict verdict, String name) throws ReflectiveOperationException {
        Field field = AiAgentJudgeVerdict.class.getDeclaredField(name);

        field.setAccessible(true);

        return field.get(verdict);
    }

    private static void setField(AiAgentJudgeVerdict verdict, String name, Object value)
        throws ReflectiveOperationException {

        Field field = AiAgentJudgeVerdict.class.getDeclaredField(name);

        field.setAccessible(true);

        field.set(verdict, value);
    }
}
