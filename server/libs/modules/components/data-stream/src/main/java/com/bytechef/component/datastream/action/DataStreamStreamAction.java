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

package com.bytechef.component.datastream.action;

import static com.bytechef.component.datastream.constant.DataStreamConstants.STREAM;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.datastream.action.definition.DataStreamStreamActionDefinition;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;

/**
 * @author Ivica Cardic
 */
public class DataStreamStreamAction {

    public final DataStreamStreamActionDefinition actionDefinition;

    public DataStreamStreamAction(Job job, JobLauncher jobLauncher) {
        actionDefinition = new DataStreamStreamActionDefinition(
            action(STREAM)
                .title("Stream Data")
                .description("Stream large volume of data between source and destination applications.")
                .properties(
                    integer("transformation")
                        .description(
                            "Choose between transformation: simple - define source and destination fields, script - define custom transformation script")
                        .label("Transformation")
                        .options(
                            option("Simple", 1),
                            option("Script", 2))),
            job, jobLauncher);
    }

}
