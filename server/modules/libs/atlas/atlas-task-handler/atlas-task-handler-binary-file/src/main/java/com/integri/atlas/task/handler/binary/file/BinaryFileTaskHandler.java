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

package com.integri.atlas.task.handler.binary.file;

import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.json.item.BinaryItem;
import com.integri.atlas.json.item.BinaryItemHelper;
import com.integri.atlas.json.item.JSONItem;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component("binaryFile")
public class BinaryFileTaskHandler implements TaskHandler<JSONObject> {

    private enum Operation {
        READ,
        WRITE,
    }

    private final BinaryItemHelper binaryNodeHelper;

    public BinaryFileTaskHandler(BinaryItemHelper binaryNodeHelper) {
        this.binaryNodeHelper = binaryNodeHelper;
    }

    @Override
    public JSONObject handle(TaskExecution taskExecution) throws Exception {
        JSONObject jsonObject;

        String fileName = taskExecution.getRequired("fileName");
        Operation operation = Operation.valueOf(taskExecution.getRequired("operation"));

        if (operation == Operation.READ) {
            try (InputStream inputStream = new FileInputStream(fileName)) {
                jsonObject = binaryNodeHelper.writeBinaryData(fileName, inputStream);
            }
        } else {
            BinaryItem binaryItem = BinaryItem.of(taskExecution.getRequired("binaryItem", String.class));

            try (InputStream inputStream = binaryNodeHelper.openDataInputStream(binaryItem)) {
                jsonObject =
                    JSONItem.of(
                        "bytes",
                        Files.copy(inputStream, Path.of(fileName), StandardCopyOption.REPLACE_EXISTING)
                    );
            }
        }

        return jsonObject;
    }
}
