/*
 * Copyright 2021 <your company/name>.
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
 */

package com.integri.atlas.task.handler.binary.file;

import com.integri.atlas.engine.core.binary.Binary;
import com.integri.atlas.engine.core.binary.BinaryHelper;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component("binaryFile")
public class BinaryFileTaskHandler implements TaskHandler<Object> {

    private enum Operation {
        READ,
        WRITE,
    }

    private final BinaryHelper binaryHelper;

    public BinaryFileTaskHandler(BinaryHelper binaryHelper) {
        this.binaryHelper = binaryHelper;
    }

    @Override
    public Object handle(TaskExecution taskExecution) throws Exception {
        Object result;

        String fileName = taskExecution.getRequired("fileName");
        Operation operation = Operation.valueOf(taskExecution.getRequired("operation"));

        if (operation == Operation.READ) {
            try (InputStream inputStream = new FileInputStream(fileName)) {
                result = binaryHelper.writeBinaryData(fileName, inputStream);
            }
        } else {
            Binary binary = taskExecution.getRequired("binary", Binary.class);

            try (InputStream inputStream = binaryHelper.openDataInputStream(binary)) {
                result =
                    Map.of("bytes", Files.copy(inputStream, Path.of(fileName), StandardCopyOption.REPLACE_EXISTING));
            }
        }

        return result;
    }
}
