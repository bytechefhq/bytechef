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

package com.integri.atlas.taskhandler.io;

import com.google.common.io.Files;
import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RmTest {

    @Test
    public void test1() throws IOException {
        Rm rm = new Rm();
        SimpleTaskExecution task = new SimpleTaskExecution();
        File tempDir = Files.createTempDir();
        Assertions.assertTrue(tempDir.exists());
        task.set("path", tempDir);
        rm.handle(task);
        Assertions.assertFalse(tempDir.exists());
    }
}
