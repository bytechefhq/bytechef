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

package com.bytechef.component.csv.file.action;

import static com.bytechef.component.csv.file.constant.CsvFileConstants.ROWS;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.bytechef.component.csv.file.CsvFileComponentHandlerTest;
import com.bytechef.component.csv.file.util.ReadConfiguration;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
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
class CsvFileWriteActionTest {

    private static final ActionContext context = Mockito.mock(ActionContext.class);

    @Test
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    void testPerformWriteCSV() throws IOException {
        String jsonContent = Files.contentOf(getFile("expected_output.json"), StandardCharsets.UTF_8);

        Parameters parameters = Mockito.mock(Parameters.class);

        CsvFileWriteAction.perform(
            getWriteParameters((List) new JSONArray(jsonContent).toList(), parameters), parameters, context);

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
            .getResource("dependencies/csv-file/" + fileName)
            .getFile());
    }

    private Parameters getWriteParameters(List<Map<?, ?>> items, Parameters parameters) {
        Mockito.when(
            parameters.getList(Mockito.eq(ROWS), Mockito.any(TypeReference.class),
                Mockito.eq(List.of())))
            .thenReturn(items);

        return parameters;
    }

    private List<Map<String, String>> read(InputStream inputStream, Context context) throws IOException {
        return CsvFileReadAction.read(
            inputStream, new ReadConfiguration(",", null, true, true, 0, Integer.MAX_VALUE, false), context);
    }
}
