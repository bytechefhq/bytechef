/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.rocketchat.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.rocketchat.constant.RocketchatConstants.NAME;
import static com.bytechef.component.rocketchat.constant.RocketchatConstants.USERNAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Marija Horvat
 */
class RocketchatUtilsTest {

    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @BeforeEach
    void beforeEach() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @Test
    void testGetUsersOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(
                "users", List.of(
                    Map.of(USERNAME, "user1"),
                    Map.of(USERNAME, "user2"))));

        List<Option<String>> expectedOptions = List.of(
            option("user1", "user1"),
            option("user2", "user2"));

        List<Option<String>> result = RocketchatUtils.getUsersOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(result, expectedOptions);
    }

    @Test
    void testGetChannelsOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(
                "channels", List.of(
                    Map.of(NAME, "channel1"),
                    Map.of(NAME, "channel2"))));

        List<Option<String>> expectedOptions = List.of(
            option("channel1", "#channel1"),
            option("channel2", "#channel2"));

        List<Option<String>> result = RocketchatUtils.getChannelsOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(result, expectedOptions);
    }
}
