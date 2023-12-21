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

package com.bytechef.component.xlsx.file.action;

import static com.bytechef.component.xlsx.file.constant.XlsxFileConstants.FILENAME;
import static com.bytechef.component.xlsx.file.constant.XlsxFileConstants.ROWS;
import static com.bytechef.component.xlsx.file.constant.XlsxFileConstants.SHEET_NAME;

import com.bytechef.component.xlsx.file.XlsxFileComponentHandlerTest;
import com.bytechef.component.xlsx.file.action.XlsxFileReadAction.ReadConfiguration;
import com.bytechef.component.xlsx.file.constant.XlsxFileConstants;
import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.Context;
import com.bytechef.hermes.component.definition.Parameters;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Files;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * @author Ivica Cardic
 */
@Disabled
public class XlsxFileWriteActionTest {

    @Test
    public void testPerformWriteXLSX() throws IOException, JSONException {
        ActionContext context = Mockito.mock(ActionContext.class);
        String jsonContent = Files.contentOf(getFile("sample.json"), StandardCharsets.UTF_8);
        Parameters parameters = Mockito.mock(Parameters.class);

        Parameters inputParameters = getWriteParameters(new JSONArray(jsonContent).toList(), parameters);

        XlsxFileWriteAction.perform(inputParameters, inputParameters, context);

        ArgumentCaptor<ByteArrayInputStream> inputStreamArgumentCaptor = ArgumentCaptor.forClass(
            ByteArrayInputStream.class);
        ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(context)
            .file(
                file1 -> file1.storeContent(filenameArgumentCaptor.capture(), inputStreamArgumentCaptor.capture()));

        JSONAssert.assertEquals(
            new JSONArray(jsonContent), new JSONArray(read(inputStreamArgumentCaptor.getValue(), context)), true);
        getEqualTo(filenameArgumentCaptor);
    }

    private static AbstractStringAssert<?> getEqualTo(ArgumentCaptor<String> filenameArgumentCaptor) {
        return Assertions.assertThat(filenameArgumentCaptor.getValue())
            .isEqualTo("file.xlsx");
    }

    private File getFile(String filename) {
        return new File(XlsxFileComponentHandlerTest.class
            .getClassLoader()
            .getResource("dependencies/xlsx-file/" + filename)
            .getFile());
    }

    private Parameters getWriteParameters(List<?> items, Parameters parameters) {
        Mockito.when(parameters.getString(Mockito.eq(FILENAME), Mockito.anyString()))
            .thenReturn("file.xlsx");
        Mockito.when(
            parameters.getList(Mockito.eq(ROWS), Mockito.any(Context.TypeReference.class), Mockito.eq(List.of())))
            .thenReturn(items);
        Mockito.when(parameters.getString(Mockito.eq(SHEET_NAME), Mockito.eq("Sheet")))
            .thenReturn("Sheet");

        return parameters;
    }

    private static List<Map<String, ?>> read(InputStream inputStream, Context context) throws IOException {
        return XlsxFileReadAction.read(
            XlsxFileConstants.FileFormat.XLSX, inputStream,
            new ReadConfiguration(true, true, 0, Integer.MAX_VALUE, false, "Sheet"), context);
    }
}
