
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

package com.bytechef.component.odsfile.action;

import com.bytechef.component.odsfile.OdsFileComponentHandlerTest;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.InputParameters;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Files;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.bytechef.component.odsfile.constant.OdsFileConstants.FILENAME;
import static com.bytechef.component.odsfile.constant.OdsFileConstants.ROWS;
import static com.bytechef.component.odsfile.constant.OdsFileConstants.SHEET_NAME;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

/**
 * @author Ivica Cardic
 */
public class OdsFileWriteActionTest {

    private static final Context context = Mockito.mock(Context.class);

    @Test
    public void testExecuteWriteODS() throws JSONException, IOException {
        String jsonContent = Files.contentOf(getFile("sample.json"), StandardCharsets.UTF_8);

        InputParameters inputParameters = getWriteParameters(new JSONArray(jsonContent).toList());

        OdsFileWriteAction.executeWrite(context, inputParameters);

        ArgumentCaptor<ByteArrayInputStream> inputStreamArgumentCaptor = ArgumentCaptor
            .forClass(ByteArrayInputStream.class);
        ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(context)
            .storeFileContent(filenameArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());

        assertEquals(
            new JSONArray(jsonContent),
            new JSONArray(read(inputStreamArgumentCaptor.getValue())),
            true);
        Assertions.assertThat(filenameArgumentCaptor.getValue())
            .isEqualTo("file.ods");
    }

    private File getFile(String filename) {
        return new File(OdsFileComponentHandlerTest.class
            .getClassLoader()
            .getResource("dependencies/" + filename)
            .getFile());
    }

    @SuppressWarnings("unchecked")
    private InputParameters getWriteParameters(List items) {
        InputParameters inputParameters = Mockito.mock(InputParameters.class);

        Mockito.when(inputParameters.getString(FILENAME, "file.ods"))
            .thenReturn("file.ods");
        Mockito.when(inputParameters.getList(ROWS, Map.class, List.of()))
            .thenReturn(items);
        Mockito.when(inputParameters.getString(SHEET_NAME, "Sheet"))
            .thenReturn("Sheet");

        return inputParameters;
    }

    private static List<Map<String, ?>> read(InputStream inputStream) throws IOException {
        return OdsFileReadAction.read(
            inputStream,
            new OdsFileReadAction.ReadConfiguration(true, true, 0, Integer.MAX_VALUE, false, "Sheet"));
    }
}
