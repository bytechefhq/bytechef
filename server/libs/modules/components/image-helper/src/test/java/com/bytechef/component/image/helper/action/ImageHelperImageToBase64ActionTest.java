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

import static com.bytechef.component.image.helper.constant.ImageHelperConstants.IMAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class ImageHelperImageToBase64ActionTest {

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Context.File, ?>> contextFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Context.Encoder, ?>> encoderFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final Context mockedContext = mock(Context.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(IMAGE, mockedFileEntry));
    private final Context.File mockedContextFile = mock(Context.File.class);
    private final Context.Encoder mockedContextEncoder = mock(Context.Encoder.class);

    @Test
    void testPerform() throws Exception {
        byte[] fileContent = new byte[] {
            1, 2, 3
        };

        String encodeToString = EncodingUtils.base64EncodeToString(fileContent);

        when(mockedContext.file(contextFunctionArgumentCaptor.capture()))
            .thenAnswer(invocation -> {
                ContextFunction<Context.File, ?> function = invocation.getArgument(0);

                return function.apply(mockedContextFile);
            });
        when(mockedContextFile.readAllBytes(mockedFileEntry))
            .thenReturn(fileContent);
        when(mockedContext.encoder(encoderFunctionArgumentCaptor.capture()))
            .thenAnswer(invocation -> {
                ContextFunction<Context.Encoder, ?> function = invocation.getArgument(0);

                return function.apply(mockedContextEncoder);
            });
        when(mockedContextEncoder.base64Encode(fileContent))
            .thenReturn(encodeToString);

        String result = ImageHelperImageToBase64Action.perform(mockedParameters, any(Parameters.class), mockedContext);

        assertEquals(encodeToString, result);

        ContextFunction<Context.File, ?> contextFunction = contextFunctionArgumentCaptor.getValue();

        assertEquals(fileContent, contextFunction.apply(mockedContextFile));

        ContextFunction<Context.Encoder, ?> encoderFunction = encoderFunctionArgumentCaptor.getValue();

        assertEquals(encodeToString, encoderFunction.apply(mockedContextEncoder));
    }
}
