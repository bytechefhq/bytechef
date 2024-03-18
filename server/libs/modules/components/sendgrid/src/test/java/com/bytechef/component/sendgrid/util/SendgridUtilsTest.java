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

package com.bytechef.component.sendgrid.util;

import static com.bytechef.component.sendgrid.constant.SendgridConstants.ATTACHMENTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.sendgrid.action.AbstractSendgridActionTest;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.objects.Attachments;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

/**
 * @author Marko Krišković
 */
class SendgridUtilsTest extends AbstractSendgridActionTest {
    @Test
    void testGetAttachments() {
        List<FileEntry> fileEntries = new ArrayList<>();

        FileEntry fileEntry1 = mock(FileEntry.class);
        FileEntry fileEntry2 = mock(FileEntry.class);

        fileEntries.add(fileEntry1);
        fileEntries.add(fileEntry2);

        when(fileEntry1.getName()).thenReturn("file1");
        when(fileEntry2.getName()).thenReturn("file2");
        when(mockedParameters.getFileEntries(ATTACHMENTS, List.of())).thenReturn(fileEntries);
        when(mockedContext.file(any())).thenReturn(new byte[] {});

        List<Attachments> result = SendgridUtils.getAttachments(mockedParameters, mockedContext);

        assertEquals(2, result.size());
        assertEquals(fileEntry1.getName(), result.get(0)
            .getFilename());
        assertEquals(fileEntry2.getName(), result.get(1)
            .getFilename());
    }

    @Test
    public void testGetTemplates() throws IOException {
        Parameters connectionParameters = mock(Parameters.class);
        when(connectionParameters.getRequiredString(anyString())).thenReturn("token");
        //String s = "{\"templates\":[{\"id\":\"1\",\"name\":\"template1\",\"other\":\"other1\"},{\"id\":\"2\",\"name\":\"template2\",\"other\":\"other2\"}]}";
        Map<String, List<Map<String, String>>> map = Map.of("templates",
            List.of(
                Map.of("id", "1", "name", "template1", "other", "other1"),
                Map.of("id", "2", "name", "template2", "other", "other2")));

        when(mockedContext.json(any())).thenReturn(map);

        List<Option<String>> options =
            SendgridUtils.getTemplates(mockedParameters, connectionParameters, "searchText", mockedContext);

        assertEquals(2, options.size());
        assertEquals("1", options.get(0)
            .getValue());
        assertEquals("template1", options.get(0)
            .getLabel());
        assertEquals("2", options.get(1)
            .getValue());
        assertEquals("template2", options.get(1)
            .getLabel());
    }
}
