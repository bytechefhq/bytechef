
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

package com.bytechef.atlas.file.storage.facade;

import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.file.storage.domain.FileEntry;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
public interface WorkflowFileStorageFacade {

    Map<String, ?> readContextValue(@NonNull FileEntry fileEntry);

    Map<String, ?> readJobOutputs(@NonNull FileEntry fileEntry);

    Object readTaskExecutionOutput(@NonNull FileEntry fileEntry);

    FileEntry storeContextValue(long stackId, @NonNull Context.Classname classname, @NonNull Map<String, ?> value);

    FileEntry storeContextValue(
        long stackId, int subStackId, @NonNull Context.Classname classname, @NonNull Map<String, ?> value);

    FileEntry storeJobOutputs(long jobId, @NonNull Map<String, ?> outputs);

    FileEntry storeTaskExecutionOutput(long taskExecutionId, @NonNull Object output);
}
