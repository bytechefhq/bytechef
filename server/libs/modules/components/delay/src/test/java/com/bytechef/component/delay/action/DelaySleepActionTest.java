/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.delay.action;

import static com.bytechef.component.delay.constant.DelayConstants.MILLIS;

import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ParameterMap;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
public class DelaySleepActionTest {

    @Test
    public void test1() {
        ParameterMap parameterMap = Mockito.mock(ParameterMap.class);

        Mockito.when(parameterMap.containsKey(Mockito.eq("duration")))
            .thenReturn(true);
        Mockito.when(parameterMap.getDuration(Mockito.eq("duration")))
            .thenReturn(Duration.of(1500, ChronoUnit.MILLIS));

        long now = System.currentTimeMillis();

        DelaySleepAction.perform(parameterMap, parameterMap, Mockito.mock(ActionContext.class));

        long delta = System.currentTimeMillis() - now;

        Assertions.assertTrue(
            delta >= 1500 && delta < 16000, String.format("Period %dms does not meet range [1500,16000>", delta));
    }

    @Test
    public void test2() {
        ParameterMap parameterMap = Mockito.mock(ParameterMap.class);

        Mockito.when(parameterMap.containsKey(Mockito.eq(MILLIS)))
            .thenReturn(true);
        Mockito.when(parameterMap.getLong(Mockito.eq(MILLIS)))
            .thenReturn(500L);

        long now = System.currentTimeMillis();

        DelaySleepAction.perform(parameterMap, parameterMap, Mockito.mock(ActionContext.class));

        long delta = System.currentTimeMillis() - now;

        Assertions.assertTrue(
            delta >= 500 && delta < 700, String.format("Period %dms does not meet range [500,700>", delta));
    }

    @Test
    public void test3() {
        long now = System.currentTimeMillis();

        DelaySleepAction.perform(
            Mockito.mock(ParameterMap.class), Mockito.mock(ParameterMap.class), Mockito.mock(ActionContext.class));

        long delta = System.currentTimeMillis() - now;

        Assertions.assertTrue(
            delta >= 1000 && delta < 1500, String.format("Period %dms does not meet range [1000,1500>", delta));
    }
}
