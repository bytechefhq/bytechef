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

package com.bytechef.component.image.helper.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class ImageHelperImageToBase64ActionTest {

    private final Context mockedContext = mock(Context.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of());

    @Test
    void testPerform() {
        byte[] fileContent = new byte[] {
            1, 2, 3
        };

        String encodeToString = EncodingUtils.base64EncodeToString(fileContent);

        when(mockedContext.file(any()))
            .thenReturn(fileContent);
        when(mockedContext.encoder(any()))
            .thenReturn(encodeToString);

        String result = ImageHelperImageToBase64Action.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(encodeToString, result);
    }
}
