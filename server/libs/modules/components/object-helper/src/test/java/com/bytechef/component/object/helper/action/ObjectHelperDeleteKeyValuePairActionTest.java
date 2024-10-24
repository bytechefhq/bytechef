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

package com.bytechef.component.object.helper.action;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.object.helper.constant.ObjectHelperConstants;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Test class for ObjectHelperDeleteKeyValuePairAction.
 *
 * @author Kristián Stutiak
 */
class ObjectHelperDeleteKeyValuePairActionTest {

    @Test
    void testPerformDeleteKeyValuePair() {
        Context context = Mockito.mock(Context.class); // Mocking the Context class
        Parameters parameters = Mockito.mock(Parameters.class); // Mocking the Parameters class

        // Setup test input data
        Map<String, Object> inputData = Map.of("key1", "value1", "key2", "value2");
        String keyToDelete = "key1";

        // Mock the behavior of parameters to return the input data and key
        Mockito.when(parameters.getRequired(Mockito.eq(ObjectHelperConstants.INPUT)))
            .thenReturn(inputData);
        Mockito.when(parameters.getRequired(Mockito.eq(ObjectHelperConstants.KEY)))
            .thenReturn(keyToDelete);

        // Perform the action and assert the output
        Map<String, Object> result =
            (Map<String, Object>) ObjectHelperDeleteKeyValuePairAction.perform(parameters, parameters, context);

        // Verify that the key-value pair was deleted
        assertThat(result).isEqualTo(Map.of("key2", "value2"));
    }

    @Test
    void testPerformDeleteNonExistentKey() {
        Context context = Mockito.mock(Context.class);
        Parameters parameters = Mockito.mock(Parameters.class);

        // Setup input data
        Map<String, Object> inputData = Map.of("key1", "value1");
        String keyToDelete = "nonExistentKey"; // Key that does not exist

        // Mock the parameters
        Mockito.when(parameters.getRequired(Mockito.eq(ObjectHelperConstants.INPUT)))
            .thenReturn(inputData);
        Mockito.when(parameters.getRequired(Mockito.eq(ObjectHelperConstants.KEY)))
            .thenReturn(keyToDelete);

        // Perform the action and assert the output
        Map<String, Object> result =
            (Map<String, Object>) ObjectHelperDeleteKeyValuePairAction.perform(parameters, parameters, context);

        // Verify that the input remains unchanged as the key does not exist
        assertThat(result).isEqualTo(inputData); // The output should be the same as input since the key was not found
    }
}
