/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.taskhandler.time;

import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SleepTests {

    @Test
    public void test1() throws InterruptedException {
        Sleep sleep = new Sleep();
        long now = System.currentTimeMillis();
        sleep.handle(SimpleTaskExecution.of("duration", "1s"));
        long delta = System.currentTimeMillis() - now;
        Assertions.assertTrue(delta >= 1000 && delta <= 1100);
    }

    @Test
    public void test2() throws InterruptedException {
        Sleep sleep = new Sleep();
        long now = System.currentTimeMillis();
        sleep.handle(SimpleTaskExecution.of("millis", 500));
        long delta = System.currentTimeMillis() - now;
        Assertions.assertTrue(delta >= 500 && delta <= 600);
    }

    @Test
    public void test3() throws InterruptedException {
        Sleep sleep = new Sleep();
        long now = System.currentTimeMillis();
        sleep.handle(new SimpleTaskExecution());
        long delta = System.currentTimeMillis() - now;
        Assertions.assertTrue(delta >= 1000 && delta <= 1100);
    }
}
