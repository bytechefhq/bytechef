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

package com.integri.atlas.task.handler.io;

import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;

/**
 * Gets the full path from a full filename, which is the prefix + path, and also excluding the final directory separator.
 *
 * This method will handle a file in either Unix or Windows format. The method is entirely text based,
 * and returns the text before the last forward or backslash.
 *
 * @author Arik Cohen
 * @since May 6, 2018
 */
@Component("io/filepath")
class FilePath implements TaskHandler<Object> {

    @Override
    public Object handle(TaskExecution aTask) {
        return FilenameUtils.getFullPathNoEndSeparator(aTask.getRequiredString("filename"));
    }
}
