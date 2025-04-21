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

package com.bytechef.component.random.helper.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class RandomHelperRandomUuidActionTest {

    private final Context mockedContext = mock(Context.class);
    private final Parameters mockedParameters = mock(Parameters.class);

    @Test
    void testPerform() {
        UUID uuid = UUID.randomUUID();

        try (MockedStatic<UUID> uuidMockedStatic = mockStatic(UUID.class)) {
            uuidMockedStatic.when(UUID::randomUUID)
                .thenReturn(uuid);

            String result = RandomHelperRandomUuidAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(String.valueOf(uuid), result);
        }
    }
}
