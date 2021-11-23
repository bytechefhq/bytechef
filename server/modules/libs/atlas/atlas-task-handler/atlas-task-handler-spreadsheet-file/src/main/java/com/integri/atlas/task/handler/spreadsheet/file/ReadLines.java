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

package com.integri.atlas.task.handler.spreadsheet.file;

import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Igor Beslic
 * @since Oct, 27 2021
 */
@Component("csv/readLines")
class ReadLines implements TaskHandler<List<String>> {

    private static final Logger logger = LoggerFactory.getLogger(ReadLines.class);

    @Override
    public List<String> handle(TaskExecution taskExecution) throws IOException {
        Path root = Paths.get(taskExecution.getRequiredString("path"));

        Boolean containsHeader = taskExecution.getBoolean("containsHeader");

        return getLines(root, containsHeader);
    }

    private List<String> getLines(Path path, boolean containsHeader) throws IOException {
        List<String> lines = new ArrayList<>();

        try (FileReader fileReader = new FileReader(path.toFile())) {
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            String line = null;

            if (containsHeader) {
                line = bufferedReader.readLine();

                logger.trace("Skipp header content {}", line);
            }

            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line);
            }
        }

        return lines;
    }
}
