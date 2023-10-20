
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

package com.bytechef.component.csvfile.action;

import com.bytechef.component.csvfile.CsvFileComponentHandlerTest;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.util.MapValueUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Files;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.bytechef.component.csvfile.constant.CsvFileConstants.ROWS;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

/**
 * @author Ivica Cardic
 */
public class CsvFileWriteActionTest {

    private static final Context context = Mockito.mock(Context.class);

    @Test
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    public void testPerformWriteCSV() throws IOException {
        try (MockedStatic<MapValueUtils> mockedStatic = Mockito.mockStatic(MapValueUtils.class)) {
            String jsonContent = Files.contentOf(getFile("sample.json"), StandardCharsets.UTF_8);

            CsvFileWriteAction.perform(
                getWriteParameters((List) new JSONArray(jsonContent).toList(), mockedStatic), context);

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
                .isEqualTo("file.csv");
        }
    }

    private File getFile(String fileName) {
        return new File(CsvFileComponentHandlerTest.class
            .getClassLoader()
            .getResource("dependencies/" + fileName)
            .getFile());
    }

    private Map<String, Object> getWriteParameters(List<Map<?, ?>> items, MockedStatic<MapValueUtils> mockedStatic) {
        mockedStatic.when(() -> MapValueUtils.getList(Mockito.anyMap(), Mockito.eq(ROWS), Mockito.eq(List.of())))
            .thenReturn(items);

        return Map.of();
    }

    private List<Map<String, Object>> read(InputStream inputStream) throws IOException {
        return CsvFileReadAction.read(
            inputStream,
            new CsvFileReadAction.ReadConfiguration(",", true, true, 0, Integer.MAX_VALUE, false));
    }
}
