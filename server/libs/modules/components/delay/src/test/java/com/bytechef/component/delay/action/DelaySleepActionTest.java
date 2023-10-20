
/*
 * Copyright 2021 <your company/name>.
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

import com.bytechef.hermes.component.Context;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class DelaySleepActionTest {

    @Test
    public void test1() {
        long now = System.currentTimeMillis();

        Map<String, ?> inputParameters = Map.of("duration", Duration.of(1500, ChronoUnit.MILLIS));

        DelaySleepAction.executeDelay(Mockito.mock(Context.class), inputParameters);

        long delta = System.currentTimeMillis() - now;

        Assertions.assertTrue(
            delta >= 1500 && delta < 4000, String.format("Period %dms does not meet range [1500,4000>", delta));
    }

    @Test
    public void test2() {
        long now = System.currentTimeMillis();

        Map<String, ?> inputParameters = Map.of("millis", 500L);

        DelaySleepAction.executeDelay(Mockito.mock(Context.class), inputParameters);

        long delta = System.currentTimeMillis() - now;

        Assertions.assertTrue(
            delta >= 500 && delta < 600, String.format("Period %dms does not meet range [500,600>", delta));
    }

    @Test
    public void test3() {
        long now = System.currentTimeMillis();

        DelaySleepAction.executeDelay(Mockito.mock(Context.class), Map.of());

        long delta = System.currentTimeMillis() - now;

        Assertions.assertTrue(
            delta >= 1000 && delta < 1500, String.format("Period %dms does not meet range [1000,1500>", delta));
    }
}
