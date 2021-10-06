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
 *//*
 * Copyright (C) Creactiviti LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Arik Cohen <arik@creactiviti.com>, June 2017
 */
package com.integri.atlas.workflow.taskhandler.media;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.arakelian.jq.ImmutableJqLibrary;
import com.arakelian.jq.ImmutableJqRequest;
import com.arakelian.jq.JqResponse;
import com.integri.atlas.workflow.core.task.TaskExecution;
import com.integri.atlas.workflow.core.task.TaskHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component("media/dar")
class Dar implements TaskHandler<String> {

  private final Mediainfo mediainfo = new Mediainfo();
  private final ObjectMapper jsonMapper = new ObjectMapper ();

  @Override
  public String handle (TaskExecution aTask) throws Exception {
    Map<?,?> mediainfoResult = mediainfo.handle(aTask);

    JqResponse response = ImmutableJqRequest.builder() //
        .lib(ImmutableJqLibrary.of())
        .input(jsonMapper.writeValueAsString(mediainfoResult))
        .filter(".media.track[]  | select(.\"@type\" == \"Video\") | .DisplayAspectRatio")
        .build()
        .execute();

    return response.getOutput();
  }

}

