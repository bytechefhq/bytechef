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

package com.bytechef.task.handler.media;

import com.arakelian.jq.ImmutableJqLibrary;
import com.arakelian.jq.ImmutableJqRequest;
import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Arik Cohen
 */
@Component("media/vduration")
class Vduration implements TaskHandler<Double> {

    private final Ffprobe ffprobe = new Ffprobe();
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @Override
    public Double handle(TaskExecution aTask) throws TaskExecutionException {
        Map<?, ?> ffprobeResult = ffprobe.handle(aTask);

        try {
            // attempt to get the duration from the codec_type = video stream
            String output = ImmutableJqRequest.builder()
                    .lib(ImmutableJqLibrary.of())
                    .input(jsonMapper.writeValueAsString(ffprobeResult))
                    .filter(".streams[] | select (.codec_type==\"video\") | .duration")
                    .build()
                    .execute()
                    .getOutput();

            // fallback to the container's format.duration
            if (StringUtils.isBlank(output) || output.equals("null")) {
                output = ImmutableJqRequest.builder()
                        .lib(ImmutableJqLibrary.of())
                        .input(jsonMapper.writeValueAsString(ffprobeResult))
                        .filter(".format.duration")
                        .build()
                        .execute()
                        .getOutput();
            }

            return Double.valueOf(output.replaceAll("[^0-9\\.]", ""));
        } catch (Exception exception) {
            throw new TaskExecutionException("Unable to handle task " + aTask, exception);
        }
    }
}
