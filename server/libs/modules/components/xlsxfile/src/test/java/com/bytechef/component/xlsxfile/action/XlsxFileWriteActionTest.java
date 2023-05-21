
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

package com.bytechef.component.xlsxfile.action;

import com.bytechef.component.xlsxfile.XlsxFileComponentHandlerTest;
import com.bytechef.component.xlsxfile.constant.XlsxFileConstants;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.util.MapValueUtils;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Files;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.bytechef.component.xlsxfile.constant.XlsxFileConstants.FILENAME;
import static com.bytechef.component.xlsxfile.constant.XlsxFileConstants.ROWS;
import static com.bytechef.component.xlsxfile.constant.XlsxFileConstants.SHEET_NAME;

/**
 * @author Ivica Cardic
 */
public class XlsxFileWriteActionTest {

    private static final Context context = Mockito.mock(Context.class);

    @Test
    public void testExecuteWriteXLSX() throws IOException, JSONException {
        String jsonContent = Files.contentOf(getFile("sample.json"), StandardCharsets.UTF_8);

        try (MockedStatic<MapValueUtils> mockedStatic = Mockito.mockStatic(MapValueUtils.class)) {
            Map<String, ?> inputParameters = getWriteParameters(new JSONArray(jsonContent).toList(), mockedStatic);

            XlsxFileWriteAction.executeWrite(context, inputParameters);

            ArgumentCaptor<ByteArrayInputStream> inputStreamArgumentCaptor = ArgumentCaptor
                .forClass(ByteArrayInputStream.class);
            ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

            Mockito.verify(context)
                .storeFileContent(filenameArgumentCaptor.capture(), inputStreamArgumentCaptor.capture());

            JSONAssert.assertEquals(
                new JSONArray(jsonContent),
                new JSONArray(read(inputStreamArgumentCaptor.getValue())),
                true);
            getEqualTo(filenameArgumentCaptor);
        }
    }

    private static AbstractStringAssert<?> getEqualTo(ArgumentCaptor<String> filenameArgumentCaptor) {
        return Assertions.assertThat(filenameArgumentCaptor.getValue())
            .isEqualTo("file.xlsx");
    }

    private File getFile(String filename) {
        return new File(XlsxFileComponentHandlerTest.class
            .getClassLoader()
            .getResource("dependencies/" + filename)
            .getFile());
    }

    private Map<String, ?> getWriteParameters(List<?> items, MockedStatic<MapValueUtils> mockedStatic) {
        mockedStatic.when(() -> MapValueUtils.getString(Mockito.anyMap(), Mockito.eq(FILENAME), Mockito.anyString()))
            .thenReturn("file.xlsx");
        mockedStatic.when(() -> MapValueUtils.getList(Mockito.anyMap(), Mockito.eq(ROWS), Mockito.eq(List.of())))
            .thenReturn(items);
        mockedStatic.when(() -> MapValueUtils.getString(Mockito.anyMap(), Mockito.eq(SHEET_NAME), Mockito.eq("Sheet")))
            .thenReturn("Sheet");

        return Map.of();
    }

    private static List<Map<String, ?>> read(InputStream inputStream) throws IOException {
        return XlsxFileReadAction.read(
            XlsxFileConstants.FileFormat.XLSX, inputStream,
            new XlsxFileReadAction.ReadConfiguration(true, true, 0, Integer.MAX_VALUE, false, "Sheet"));
    }
}
