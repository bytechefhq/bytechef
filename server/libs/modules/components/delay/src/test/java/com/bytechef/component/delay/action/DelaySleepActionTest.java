
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
import com.bytechef.hermes.component.util.MapValueUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static com.bytechef.component.delay.constant.DelayConstants.MILLIS;

/**
 * @author Ivica Cardic
 */
public class DelaySleepActionTest {

    @Test
    public void test1() {
        try (MockedStatic<MapValueUtils> mapValueUtilsMockedStatic = Mockito.mockStatic(MapValueUtils.class)) {
            mapValueUtilsMockedStatic.when(() -> MapValueUtils.getDuration(Mockito.anyMap(), Mockito.eq("duration")))
                .thenReturn(Duration.of(1500, ChronoUnit.MILLIS));

            long now = System.currentTimeMillis();

            DelaySleepAction.perform(Map.of("duration", "1.5S"), Mockito.mock(Context.class));

            long delta = System.currentTimeMillis() - now;

            Assertions.assertTrue(
                delta >= 1500 && delta < 16000, String.format("Period %dms does not meet range [1500,16000>", delta));
        }
    }

    @Test
    public void test2() {
        try (MockedStatic<MapValueUtils> mapValueUtilsMockedStatic = Mockito.mockStatic(MapValueUtils.class)) {

            mapValueUtilsMockedStatic.when(() -> MapValueUtils.getLong(Mockito.anyMap(), Mockito.eq(MILLIS)))
                .thenReturn(500L);

            long now = System.currentTimeMillis();

            DelaySleepAction.perform(Map.of(MILLIS, 500L), Mockito.mock(Context.class));

            long delta = System.currentTimeMillis() - now;

            Assertions.assertTrue(
                delta >= 500 && delta < 600, String.format("Period %dms does not meet range [500,600>", delta));
        }
    }

    @Test
    public void test3() {
        long now = System.currentTimeMillis();

        DelaySleepAction.perform(Map.of(), Mockito.mock(Context.class));

        long delta = System.currentTimeMillis() - now;

        Assertions.assertTrue(
            delta >= 1000 && delta < 1500, String.format("Period %dms does not meet range [1000,1500>", delta));
    }
}
