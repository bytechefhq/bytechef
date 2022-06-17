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

package com.bytechef.task.handler.mysql;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONParser;

/**
 * @author Ivica Cardic
 */
public class MySQLAuthenticationDescriptorHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper() {
        {
            setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
    };

    @Test
    public void testGetMySQLAuthenticationDescriptors() throws JsonProcessingException {
        JSONAssert.assertEquals(
                """
            {"taskName":"mysql","authenticationDescriptors":[{"displayName":"MySQL","name":"mysql","properties":[{"displayName":"Host","name":"host","required":true,"type":"STRING"},{"displayName":"Port","name":"port","required":true,"type":"INTEGER"},{"displayName":"Username","name":"username","required":true,"type":"STRING"},{"displayName":"Password","name":"password","required":true,"type":"STRING"}]}]}
            """,
                (JSONObject) JSONParser.parseJSON(objectMapper.writeValueAsString(
                        new MySQLAuthenticationDescriptorHandler().getAuthenticationDescriptors())),
                true);
    }
}
