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

package com.bytechef.component.csvfile.action;

import static com.bytechef.component.csvfile.constant.CsvFileConstants.ROWS;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.bytechef.component.csvfile.CsvFileComponentHandlerTest;
import com.bytechef.component.csvfile.action.CsvFileReadAction.ReadConfiguration;
import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.Context;
import com.bytechef.hermes.component.definition.ParameterMap;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Files;
import org.json.JSONArray;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
@Disabled
public class CsvFileWriteActionTest {

    private static final ActionContext context = Mockito.mock(ActionContext.class);

    @Test
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    public void testPerformWriteCSV() throws IOException {
        String jsonContent = Files.contentOf(getFile("sample.json"), StandardCharsets.UTF_8);

        ParameterMap parameterMap = Mockito.mock(ParameterMap.class);

        CsvFileWriteAction.perform(
            getWriteParameters((List) new JSONArray(jsonContent).toList(), parameterMap), parameterMap, context);

        ArgumentCaptor<ByteArrayInputStream> inputStreamArgumentCaptor = ArgumentCaptor.forClass(
            ByteArrayInputStream.class);
        ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(context)
            .file(
                file -> file.storeContent(filenameArgumentCaptor.capture(), inputStreamArgumentCaptor.capture()));

        assertEquals(new JSONArray(jsonContent), new JSONArray(
            read(inputStreamArgumentCaptor.getValue(), context)), true);
        Assertions.assertThat(filenameArgumentCaptor.getValue())
            .isEqualTo("file.csv");
    }

    private File getFile(String fileName) {
        return new File(CsvFileComponentHandlerTest.class
            .getClassLoader()
            .getResource("dependencies/" + fileName)
            .getFile());
    }

    private ParameterMap getWriteParameters(List<Map<?, ?>> items, ParameterMap parameterMap) {
        Mockito.when(
            parameterMap.getList(Mockito.eq(ROWS), Mockito.any(Context.TypeReference.class), Mockito.eq(List.of())))
            .thenReturn(items);

        return parameterMap;
    }

    private List<Map<String, Object>> read(InputStream inputStream, Context context) throws IOException {
        return CsvFileReadAction.read(
            inputStream, new ReadConfiguration(",", true, true, 0, Integer.MAX_VALUE, false), context);
    }
}
