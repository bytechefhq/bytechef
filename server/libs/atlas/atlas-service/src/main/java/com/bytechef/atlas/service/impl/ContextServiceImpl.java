
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

package com.bytechef.atlas.service.impl;

import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.repository.ContextRepository;
import com.bytechef.atlas.service.ContextService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Map;

@Transactional
public class ContextServiceImpl implements ContextService {

    private final ContextRepository contextRepository;

    @SuppressFBWarnings("EI2")
    public ContextServiceImpl(ContextRepository contextRepository) {
        this.contextRepository = contextRepository;
    }

    @Override
    public void push(long stackId, Context.Classname classname, @NonNull Context context) {
        Assert.notNull(context, "'context' must not be null.");

        context.setClassnameId(classname.getId());
        context.setId(null);
        context.setNew(true);
        context.setStackId(stackId);

        contextRepository.save(context);
    }

    @Override
    public void push(
        @NonNull long stackId, @NonNull int subStackId, Context.Classname classname, @NonNull Context context) {
        Assert.notNull(context, "'context' must not be null.");

        context.setClassnameId(classname.getId());
        context.setId(null);
        context.setNew(true);
        context.setSubStackId(subStackId);
        context.setStackId(stackId);

        contextRepository.save(context);
    }

    @Override
    public Context push(long stackId, Context.Classname classname, Map<String, Object> value) {
        Context context = new Context(stackId, classname, value);

        context.setNew(true);

        return contextRepository.save(context);
    }

    @Override
    @Transactional(readOnly = true)
    public Context peek(long stackId, Context.Classname classname) {
        return contextRepository.findTop1ByStackIdAndClassnameIdOrderByCreatedDateDesc(stackId, classname.getId());
    }

    @Override
    public Context peek(long stackId, int subStackId, Context.Classname classname) {
        return contextRepository.findTop1ByStackIdAndSubStackIdAndClassnameIdOrderByCreatedDateDesc(stackId, subStackId,
            classname.getId());
    }
}
