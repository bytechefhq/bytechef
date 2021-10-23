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

package com.integri.atlas.taskhandler.random;

import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.worker.task.TaskHandler;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.stereotype.Component;

/**
 * a {@link TaskHandler} implementaion which generates a
 * random integer.
 *
 * @author Arik Cohen
 * @since Mar 30, 2017
 */
@Component("random/int")
public class RandomInt implements TaskHandler<Object> {

    @Override
    public Object handle(TaskExecution aTask) throws Exception {
        int startInclusive = aTask.getInteger("startInclusive", 0);
        int endInclusive = aTask.getInteger("endInclusive", 100);
        return RandomUtils.nextInt(startInclusive, endInclusive);
    }
}
