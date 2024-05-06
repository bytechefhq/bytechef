/*
 * Copyright 2023-present ByteChef Inc.
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
import com.bytechef.atlas.execution.repository.ContextRepository;
import com.bytechef.file.storage.domain.FileEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.Validate;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ContextServiceImpl implements ContextService {

    private final ContextRepository contextRepository;

    @SuppressFBWarnings("EI2")
    public ContextServiceImpl(ContextRepository contextRepository) {
        this.contextRepository = contextRepository;
    }

    @Override
    public void push(long stackId, @NonNull Context.Classname classname, @NonNull FileEntry value) {
        Validate.notNull(classname, "'classname' must not be null");
        Validate.notNull(value, "'value' must not be null");

        Context context = new Context(stackId, classname, value);

        contextRepository.save(context);
    }

    @Override
    public void push(long stackId, int subStackId, @NonNull Context.Classname classname, @NonNull FileEntry value) {
        Validate.notNull(classname, "'classname' must not be null");
        Validate.notNull(value, "'value' must not be null");

        Context context = new Context(stackId, subStackId, classname, value);

        contextRepository.save(context);
    }

    @Override
    @Transactional(readOnly = true)
    public FileEntry peek(long stackId, @NonNull Context.Classname classname) {
        Context context = contextRepository.findTop1ByStackIdAndClassnameIdOrderByCreatedDateDesc(
            stackId, classname.ordinal());

        Validate.notNull(context, "context");

        return context.getValue();
    }

    @Override
    public FileEntry peek(long stackId, int subStackId, @NonNull Context.Classname classname) {
        Context context = contextRepository.findTop1ByStackIdAndSubStackIdAndClassnameIdOrderByCreatedDateDesc(
            stackId, subStackId, classname.ordinal());

        Validate.notNull(context, "context");

        return context.getValue();
    }
}
