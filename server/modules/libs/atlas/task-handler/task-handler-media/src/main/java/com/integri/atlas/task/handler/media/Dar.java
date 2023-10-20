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

package com.integri.atlas.task.handler.media;

import com.arakelian.jq.ImmutableJqLibrary;
import com.arakelian.jq.ImmutableJqRequest;
import com.arakelian.jq.JqResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @author Arik Cohen
 */
@Component("media/dar")
class Dar implements TaskHandler<String> {

    private final Mediainfo mediainfo = new Mediainfo();
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @Override
    public String handle(TaskExecution aTask) throws Exception {
        Map<?, ?> mediainfoResult = mediainfo.handle(aTask);

        JqResponse response = ImmutableJqRequest
            .builder() //
            .lib(ImmutableJqLibrary.of())
            .input(jsonMapper.writeValueAsString(mediainfoResult))
            .filter(".media.track[]  | select(.\"@type\" == \"Video\") | .DisplayAspectRatio")
            .build()
            .execute();

        return response.getOutput();
    }
}
