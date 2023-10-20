/*
 * Copyright 2021 <your company/name>.
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
 */

package com.integri.atlas.engine.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @author Ivica Cardic
 */
@AutoConfigureMockMvc
@SpringBootTest
public class TaskControllerIntTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetTaskDescriptor() throws Exception {
        mockMvc
            .perform(get("/tasks/task1").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(
                content()
                    .json(
                        """
                {
                    "authentication":null,
                    "description":null,
                    "displayName":null,
                    "name":"task1",
                    "icon":null,
                    "properties":null,
                    "subtitle":null,
                    "version":1.0
                }
                """
                    )
            );
    }

    @Test
    public void testGetTaskDescriptors() throws Exception {
        mockMvc
            .perform(get("/tasks").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(
                content()
                    .json(
                        """
                [
                    {
                        "authentication":null,
                        "description":null,
                        "displayName":null,
                        "name":"task1",
                        "icon":null,
                        "properties":null,
                        "subtitle":null,
                        "version":1.0
                    },
                    {
                        "authentication":null,
                        "description":null,
                        "displayName":null,
                        "name":"task2",
                        "icon":null,
                        "properties":null,
                        "subtitle":null,
                        "version":1.0
                    }
                ]
                """
                    )
            );
    }
}
