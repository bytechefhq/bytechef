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
import com.bytechef.atlas.execution.repository.ContextRepository;
import com.bytechef.file.storage.domain.FileEntry;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Transactional
public class ContextServiceImpl implements ContextService {

    private final ContextRepository contextRepository;

    public ContextServiceImpl(ContextRepository contextRepository) {
        this.contextRepository = contextRepository;
    }

    @Override
    public void push(long stackId, Context.Classname classname, FileEntry value) {
        Assert.notNull(classname, "'classname' must not be null");
        Assert.notNull(value, "'value' must not be null");

        Context context = new Context(stackId, classname, value);

        contextRepository.save(context);
    }

    @Override
    public void push(long stackId, int subStackId, Context.Classname classname, FileEntry value) {
        Assert.notNull(classname, "'classname' must not be null");
        Assert.notNull(value, "'value' must not be null");

        Context context = new Context(stackId, subStackId, classname, value);

        contextRepository.save(context);
    }

    @Override
    @Transactional(readOnly = true)
    public FileEntry peek(long stackId, Context.Classname classname) {
        Context context = contextRepository
            .findTop1ByStackIdAndClassnameIdOrderByCreatedDateDesc(stackId, classname.ordinal())
            .orElseThrow(
                () -> new IllegalArgumentException("Unable to locate context with stackId: %s".formatted(stackId)));

        return context.getValue();
    }

    @Override
    public FileEntry peek(long stackId, int subStackId, Context.Classname classname) {
        Context context = contextRepository
            .findTop1ByStackIdAndSubStackIdAndClassnameIdOrderByCreatedDateDesc(
                stackId, subStackId, classname.ordinal())
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "Unable to locate context with stackId: %s, subStackId: %s".formatted(stackId, subStackId)));

        return context.getValue();
    }
}
