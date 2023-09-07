
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

package com.bytechef.atlas.file.storage;

import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.file.storage.domain.FileEntry;

import java.io.InputStream;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public interface WorkflowFileStorage {

    InputStream getFileStream(String filename, String url);

    Map<String, ?> readContextValue(FileEntry fileEntry);

    String readFileToString(String filename, String url);

    Map<String, ?> readJobOutputs(FileEntry fileEntry);

    Object readTaskExecutionOutput(FileEntry fileEntry);

    FileEntry storeContextValue(long stackId, Context.Classname classname, Map<String, ?> value);

    FileEntry storeContextValue(long stackId, int subStackId, Context.Classname classname, Map<String, ?> value);

    FileEntry storeFileContent(String fileName, String data);

    FileEntry storeFileContent(String fileName, InputStream inputStream);

    FileEntry storeJobOutputs(long jobId, Map<String, ?> outputs);

    FileEntry storeTaskExecutionOutput(Long taskExecutionId, Object output);
}
