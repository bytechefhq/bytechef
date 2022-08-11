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

package com.bytechef.task.handler.io;

import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

/**
 * @author Arik Cohen
 * @since Feb, 21 2020
 */
@Component("io/createTempDir")
class CreateTempDir implements TaskHandler<String> {

    @Override
    public String handle(TaskExecution aTask) throws TaskExecutionException {
        try {
            Path path = Files.createTempDirectory("createTempDir_");

            File file = path.toFile();

            return file.getAbsolutePath();
        } catch (IOException ioException) {
            throw new TaskExecutionException("Unable to create temorary directory " + aTask, ioException);
        }
    }
}
