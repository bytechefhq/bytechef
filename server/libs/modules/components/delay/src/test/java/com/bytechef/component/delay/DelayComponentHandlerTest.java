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

import static com.bytechef.hermes.component.definition.Action.ACTION;

import com.bytechef.hermes.component.test.mock.MockContext;
import com.bytechef.hermes.component.test.mock.MockExecutionParameters;
import com.bytechef.test.jsonasssert.AssertUtils;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Arik Cohen
 */
public class DelayComponentHandlerTest {

    private static final MockContext context = new MockContext();

    @Test
    public void testGetComponentDefinition() {
        AssertUtils.assertEquals("definition/delay_v1.json", new DelayComponentHandler().getDefinition());
    }

    @Test
    public void test1() {
        long now = System.currentTimeMillis();
        DelayComponentHandler sleepComponentAccessor = new DelayComponentHandler();

        sleepComponentAccessor.sleep(context, new MockExecutionParameters(Map.of("duration", "1.5s", ACTION, "sleep")));

        long delta = System.currentTimeMillis() - now;

        Assertions.assertTrue(
                delta >= 1500 && delta < 1900, String.format("Period %dms does not meet range [1500,1900>", delta));
    }

    @Test
    public void test2() {
        long now = System.currentTimeMillis();
        DelayComponentHandler sleepComponentAccessor = new DelayComponentHandler();

        sleepComponentAccessor.sleep(context, new MockExecutionParameters(Map.of("millis", 500, ACTION, "sleep")));

        long delta = System.currentTimeMillis() - now;

        Assertions.assertTrue(
                delta >= 500 && delta < 600, String.format("Period %dms does not meet range [500,600>", delta));
    }

    @Test
    public void test3() {
        long now = System.currentTimeMillis();
        DelayComponentHandler sleepComponentAccessor = new DelayComponentHandler();

        sleepComponentAccessor.sleep(context, new MockExecutionParameters(Map.of(ACTION, "sleep")));

        long delta = System.currentTimeMillis() - now;

        Assertions.assertTrue(
                delta >= 1000 && delta < 1500, String.format("Period %dms does not meet range [1000,1500>", delta));
    }
}
