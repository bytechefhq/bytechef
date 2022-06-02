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

package com.integri.atlas.task.handler.random;

import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * a {@link TaskHandler} implementaion which can
 * throw an exception based on a probabilty
 * value.
 *
 * @author Arik Cohen
 * @since Mar 30, 2017
 */
@Component("random/rogue")
class Rogue implements TaskHandler<Object> {

    @Override
    public Object handle(TaskExecution aTask) throws Exception {
        float nextFloat = RandomUtils.nextFloat(0, 1);
        float probability = aTask.getFloat("probability", 0.5f);
        Assert.isTrue(probability >= 0 && probability <= 1, "probability must be a value between 0 and 1");
        if (nextFloat <= probability) {
            throw new IllegalStateException("I'm a rogue exception");
        }
        return null;
    }
}
