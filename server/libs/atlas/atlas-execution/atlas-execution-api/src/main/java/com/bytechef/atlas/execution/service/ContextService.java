/*
 * Copyright 2025 ByteChef
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

package com.bytechef.atlas.execution.service;

import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.file.storage.domain.FileEntry;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface ContextService {

    /**
     * Deletes every context row for the given stack (a job id or a task execution id). Used by job deletion and
     * retention purging; in-memory and remote-client deployments may not support it and throw
     * {@link UnsupportedOperationException}.
     */
    void deleteStackContexts(long stackId);

    /**
     * Every context value stored for the given stack, all classnames and sub-stacks included — the blobs job deletion
     * must release before removing the rows. May throw {@link UnsupportedOperationException} like
     * {@link #deleteStackContexts(long)}.
     */
    List<FileEntry> getStackFileEntries(long stackId);

    FileEntry peek(long stackId, Context.Classname classname);

    FileEntry peek(long stackId, int subStackId, Context.Classname classname);

    void push(long stackId, Context.Classname classname, FileEntry value);

    void push(long stackId, int subStackId, Context.Classname classname, FileEntry value);
}
