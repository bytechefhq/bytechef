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

package com.bytechef.component.docusign.action;

import static com.bytechef.component.docusign.constant.DocuSignConstants.ACCOUNT_ID;
import static com.bytechef.component.docusign.constant.DocuSignConstants.DOCUMENT_ID;
import static com.bytechef.component.docusign.constant.DocuSignConstants.ENVELOPE_ID;
import static com.bytechef.component.docusign.constant.DocuSignConstants.FROM_DATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class DocuSignDownloadEnvelopeDocumentActionTest {

    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(
        ACCOUNT_ID, List.of(), FROM_DATE, LocalDate.of(2025, 6, 4), ENVELOPE_ID, "1", DOCUMENT_ID, "1"));
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @Test
    void testPerform() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedFileEntry);

        FileEntry result = DocuSignDownloadEnvelopeDocumentAction.perform(
            mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedFileEntry, result);
    }
}
