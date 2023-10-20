
/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.component.delay;

import com.bytechef.component.delay.action.DelayDelayAction;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.Parameters;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Arik Cohen
 */
public class DelayComponentHandlerTest {

    @Test
    public void testGetComponentDefinition() {
        JsonFileAssert.assertEquals("definition/delay_v1.json", new DelayComponentHandler().getDefinition());
    }

    @Test
    public void test1() {
        long now = System.currentTimeMillis();

        Parameters parameters = Mockito.mock(Parameters.class);

        Mockito.when(parameters.containsKey("millis"))
            .thenReturn(false);
        Mockito.when(parameters.containsKey("duration"))
            .thenReturn(true);
        Mockito.when(parameters.getDuration("duration"))
            .thenReturn(Duration.of(1500, ChronoUnit.MILLIS));

        DelayDelayAction.performDelay(Mockito.mock(Context.class), parameters);

        long delta = System.currentTimeMillis() - now;

        Assertions.assertTrue(
            delta >= 1500 && delta < 4000, String.format("Period %dms does not meet range [1500,4000>", delta));
    }

    @Test
    public void test2() {
        long now = System.currentTimeMillis();

        Parameters parameters = Mockito.mock(Parameters.class);

        Mockito.when(parameters.containsKey("millis"))
            .thenReturn(true);
        Mockito.when(parameters.getLong("millis"))
            .thenReturn(500L);

        DelayDelayAction.performDelay(Mockito.mock(Context.class), parameters);

        long delta = System.currentTimeMillis() - now;

        Assertions.assertTrue(
            delta >= 500 && delta < 600, String.format("Period %dms does not meet range [500,600>", delta));
    }

    @Test
    public void test3() {
        long now = System.currentTimeMillis();

        DelayDelayAction.performDelay(Mockito.mock(Context.class), Mockito.mock(Parameters.class));

        long delta = System.currentTimeMillis() - now;

        Assertions.assertTrue(
            delta >= 1000 && delta < 1500, String.format("Period %dms does not meet range [1000,1500>", delta));
    }
}
