
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

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
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
        DelayComponentHandler delayComponentHandler = new DelayComponentHandler();

        ExecutionParameters executionParameters = Mockito.mock(ExecutionParameters.class);

        Mockito.when(executionParameters.containsKey("millis"))
            .thenReturn(false);
        Mockito.when(executionParameters.containsKey("duration"))
            .thenReturn(true);
        Mockito.when(executionParameters.getDuration("duration"))
            .thenReturn(Duration.of(1500, ChronoUnit.MILLIS));

        delayComponentHandler.performSleep(Mockito.mock(Context.class), executionParameters);

        long delta = System.currentTimeMillis() - now;

        Assertions.assertTrue(
            delta >= 1500 && delta < 3000, String.format("Period %dms does not meet range [1500,1900>", delta));
    }

    @Test
    public void test2() {
        long now = System.currentTimeMillis();
        DelayComponentHandler delayComponentHandler = new DelayComponentHandler();

        ExecutionParameters executionParameters = Mockito.mock(ExecutionParameters.class);

        Mockito.when(executionParameters.containsKey("millis"))
            .thenReturn(true);
        Mockito.when(executionParameters.getLong("millis"))
            .thenReturn(500L);

        delayComponentHandler.performSleep(Mockito.mock(Context.class), executionParameters);

        long delta = System.currentTimeMillis() - now;

        Assertions.assertTrue(
            delta >= 500 && delta < 600, String.format("Period %dms does not meet range [500,600>", delta));
    }

    @Test
    public void test3() {
        long now = System.currentTimeMillis();
        DelayComponentHandler delayComponentHandler = new DelayComponentHandler();

        delayComponentHandler.performSleep(Mockito.mock(Context.class), Mockito.mock(ExecutionParameters.class));

        long delta = System.currentTimeMillis() - now;

        Assertions.assertTrue(
            delta >= 1000 && delta < 1500, String.format("Period %dms does not meet range [1000,1500>", delta));
    }
}
