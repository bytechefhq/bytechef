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

package com.bytechef.component.google.docs.action;

import static com.bytechef.component.google.docs.constant.GoogleDocsConstants.DESTINATION_FILE;
import static com.bytechef.component.google.docs.constant.GoogleDocsConstants.IMAGES;
import static com.bytechef.component.google.docs.constant.GoogleDocsConstants.VALUES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.google.docs.util.GoogleDocsUtils;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.ReplaceAllTextRequest;
import com.google.api.services.docs.v1.model.ReplaceImageRequest;
import com.google.api.services.docs.v1.model.Request;
import com.google.api.services.docs.v1.model.SubstringMatchCriteria;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
class GoogleDocsCreateDocumentBasedOnTemplateActionTest extends AbstractGoogleDocsActionTest {

    private final ArgumentCaptor<String> destinationFileArgumentCaptor = ArgumentCaptor.forClass(String.class);
    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> requestsArgumentCaptor = ArgumentCaptor.forClass(List.class);

    @Test
    @SuppressWarnings("unchecked")
    void perform() throws IOException {
        when(mockedParameters.getMap(VALUES, String.class, Map.of()))
            .thenReturn(Map.of("textKey1", "textValue1"));
        when(mockedParameters.getMap(IMAGES, String.class, Map.of()))
            .thenReturn(Map.of("imageId1", "url1"));
        when(mockedParameters.getRequiredString(DESTINATION_FILE))
            .thenReturn("destinationFile");


        try (MockedStatic<GoogleDocsUtils> googleDocsUtilsMockedStatic = mockStatic(GoogleDocsUtils.class)) {
            googleDocsUtilsMockedStatic
                .when(() -> GoogleDocsUtils.writeToDocument(any(Docs.class), destinationFileArgumentCaptor.capture(), requestsArgumentCaptor.capture()))
                .thenAnswer(Answers.RETURNS_DEFAULTS);

            Object result = GoogleDocsCreateDocumentBasedOnTemplateAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertNull(result);

            assertEquals("destinationFile", destinationFileArgumentCaptor.getValue());

            List<Request> requests = requestsArgumentCaptor.getValue();

            assertEquals(2, requests.size());

            ReplaceAllTextRequest replaceAllText = requests.getFirst().getReplaceAllText();

            assertEquals("textValue1", replaceAllText.getReplaceText());

            SubstringMatchCriteria substringMatchCriteria = replaceAllText.getContainsText();

            assertEquals("[[textKey1]]", substringMatchCriteria.getText());
            assertTrue(substringMatchCriteria.getMatchCase());

            ReplaceImageRequest replaceImageRequest = requests.get(1).getReplaceImage();

            assertEquals("imageId1", replaceImageRequest.getImageObjectId());
            assertEquals("url1", replaceImageRequest.getUri());
        }
    }
}
