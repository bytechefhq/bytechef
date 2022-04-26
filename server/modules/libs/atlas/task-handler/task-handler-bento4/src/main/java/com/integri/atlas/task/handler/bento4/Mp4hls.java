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

package com.integri.atlas.task.handler.bento4;

import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import java.util.List;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 * @since Feb, 19 2020
 */
@Component("bento4/mp4hls")
class Mp4hls implements TaskHandler<Object> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Object handle(TaskExecution aTask) throws Exception {
        List<String> options = aTask.getList("options", String.class);
        CommandLine cmd = new CommandLine("mp4hls");
        options.forEach(o -> cmd.addArgument(o));
        logger.debug("{}", cmd);
        DefaultExecutor exec = new DefaultExecutor();
        int exitValue = exec.execute(cmd);
        Assert.isTrue(exitValue == 0, "exit value: " + exitValue);
        return null;
    }
}
